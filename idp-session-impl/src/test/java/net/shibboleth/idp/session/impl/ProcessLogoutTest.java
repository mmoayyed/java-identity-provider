/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.session.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.idp.session.impl.testing.SessionManagerBaseTestCase;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;
import net.shibboleth.utilities.java.support.primitive.NonnullSupplier;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/** {@link ProcessLogout} unit test. */
@SuppressWarnings("javadoc")
public class ProcessLogoutTest extends SessionManagerBaseTestCase {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private ProcessLogout action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        
        action = new ProcessLogout();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return requestProxy;}});
        action.setHttpServletResponseSupplier(new NonnullSupplier<> () {public HttpServletResponse get() { return responseProxy;}});
        action.setSessionResolver(sessionManager);
        action.initialize();
    }
    
    /** {@inheritDoc} */
    @Override
    protected void adjustProperties() throws ComponentInitializationException {
        sessionManager.setTrackSPSessions(true);
        sessionManager.setSecondaryServiceIndex(true);
        sessionManager.setSessionSlop(Duration.ofSeconds(900));
        final SPSessionSerializerRegistry registry = new SPSessionSerializerRegistry();
        registry.setMappings(
                Collections.<Class<? extends SPSession>,StorageSerializer<? extends SPSession>>singletonMap(
                        BasicSPSession.class, new BasicSPSessionSerializer(Duration.ofSeconds(900))));
        registry.initialize();
        sessionManager.setSPSessionSerializerRegistry(registry);
    }
    
    @Test public void testNoSession() {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
    }
    
    @Test public void testSessionNoSPSessions() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(subjectCtx);
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        Assert.assertNotNull(logoutCtx);
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 1);
        Assert.assertEquals(logoutCtx.getIdPSessions().iterator().next().getId(), session.getId());
        Assert.assertTrue(logoutCtx.getSessionMap().isEmpty());
        
        sessionManager.destroySession(session.getId(), false);
    }

    @Test public void testSessionSPSessions() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        // Limit granularity to milliseconds for storage roundtrip.
        final Instant creation = Instant.ofEpochMilli(System.currentTimeMillis());
        final Instant expiration = creation.plusSeconds(3600);

        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
        session.addSPSession(new BasicSPSession("https://sp.example.org", creation, expiration));
        session.addSPSession(new BasicSPSession("https://sp2.example.org", creation, expiration));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(subjectCtx);
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        Assert.assertNotNull(logoutCtx);
        Assert.assertEquals(logoutCtx.getIdPSessions().size(), 1);
        Assert.assertEquals(logoutCtx.getIdPSessions().iterator().next().getId(), session.getId());
        
        BasicSPSession sp = (BasicSPSession) logoutCtx.getSessions("https://sp.example.org").iterator().next();
        Assert.assertNotNull(sp);
        Assert.assertEquals(sp.getCreationInstant(), creation);
        Assert.assertEquals(sp.getExpirationInstant(), expiration);

        sp = (BasicSPSession) logoutCtx.getSessions("https://sp2.example.org").iterator().next();
        Assert.assertNotNull(sp);
        Assert.assertEquals(sp.getCreationInstant(), creation);
        Assert.assertEquals(sp.getExpirationInstant(), expiration);
        
        sessionManager.destroySession(session.getId(), false);
}
    
    @Test public void testAddressRebind() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setRemoteAddr("::1");
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(subjectCtx);
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        
        sessionManager.destroySession(session.getId(), false);
    }
    
    @Test public void testAddressMismatch() throws SessionException {
        final Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setRemoteAddr("192.168.1.1");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
        Assert.assertNull(prc.getSubcontext(LogoutContext.class));
    }

    @Test public void testAddressLookup() throws ComponentInitializationException, SessionException, ResolverException {
        action = new ProcessLogout();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return requestProxy;}});
        action.setHttpServletResponseSupplier(new NonnullSupplier<> () {public HttpServletResponse get() { return responseProxy;}});
        action.setSessionResolver(sessionManager);
        action.setAddressLookupStrategy(input -> requestProxy.getHeader("User-Agent"));
        action.initialize();
        
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).addHeader("User-Agent", "UnitTest-Client");
        
        final IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext subjectCtx = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(subjectCtx);
        Assert.assertEquals(subjectCtx.getPrincipalName(), "joe");
        Assert.assertTrue(session.checkAddress("UnitTest-Client"));
        
        sessionManager.destroySession(session.getId(), false);
    }

}
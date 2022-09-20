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
import java.util.Collections;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.Cookie;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.shared.net.impl.HttpServletRequestResponseContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/** {@link DestroySessions} unit test. */
public class DestroySessionsTest extends SessionManagerBaseTestCase {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private DestroySessions action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        
        action = new DestroySessions();
        action.setSessionManager(sessionManager);
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
    
    @Test public void testNoContext() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testNoSessions() {
        prc.getSubcontext(LogoutContext.class, true);
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }
    
    @Test public void testOneSessionNoUnbind() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
        final String sessionId = session.getId();
        
        prc.getSubcontext(LogoutContext.class, true).getIdPSessions().add(session);

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        Assert.assertNotNull(logoutCtx);
        Assert.assertTrue(logoutCtx.getIdPSessions().isEmpty());
        
        session = sessionManager.resolveSingle(new CriteriaSet(new SessionIdCriterion(sessionId)));
        Assert.assertNull(session);
    }

    @Test public void testOneSessionUnbind() throws SessionException, ResolverException {
        final Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
        final String sessionId = session.getId();
        
        prc.getSubcontext(LogoutContext.class, true).getIdPSessions().add(session);
        prc.getSubcontext(SessionContext.class, true).setIdPSession(session);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        Assert.assertNotNull(logoutCtx);
        Assert.assertTrue(logoutCtx.getIdPSessions().isEmpty());
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        Assert.assertNull(sessionCtx);
        
        session = sessionManager.resolveSingle(new CriteriaSet(new SessionIdCriterion(sessionId)));
        Assert.assertNull(session);
    }

    @Test public void testOneSessionDifferent() throws SessionException, ResolverException {
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);

        prc.getSubcontext(LogoutContext.class, true).getIdPSessions().add(session);

        cookie = createSession("joe");

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
                
        prc.getSubcontext(SessionContext.class, true).setIdPSession(session);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        Assert.assertNotNull(logoutCtx);
        Assert.assertTrue(logoutCtx.getIdPSessions().isEmpty());
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        Assert.assertNotNull(sessionCtx);
        Assert.assertNotNull(sessionCtx.getIdPSession());
    }

    @Test public void testTwoSessions() throws SessionException, ResolverException {
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        IdPSession session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);

        prc.getSubcontext(LogoutContext.class, true).getIdPSessions().add(session);

        cookie = createSession("joe");

        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);

        session = sessionManager.resolveSingle(new CriteriaSet(new HttpServletRequestCriterion()));
        Assert.assertNotNull(session);
                
        prc.getSubcontext(LogoutContext.class, true).getIdPSessions().add(session);
        prc.getSubcontext(SessionContext.class, true).setIdPSession(session);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class);
        Assert.assertNotNull(logoutCtx);
        Assert.assertTrue(logoutCtx.getIdPSessions().isEmpty());
        
        final SessionContext sessionCtx = prc.getSubcontext(SessionContext.class);
        Assert.assertNull(sessionCtx);
    }

}
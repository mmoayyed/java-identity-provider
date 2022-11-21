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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.impl.testing.SessionManagerBaseTestCase;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;
import net.shibboleth.utilities.java.support.primitive.NonnullSupplier;

/** {@link PopulateSessionContext} unit test. */
@SuppressWarnings("javadoc")
public class PopulateSessionContextTest extends SessionManagerBaseTestCase {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private PopulateSessionContext action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        
        action = new PopulateSessionContext();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return requestProxy;}});
        action.setHttpServletResponseSupplier(new NonnullSupplier<> () {public HttpServletResponse get() { return responseProxy;}});
        action.setSessionResolver(sessionManager);
        action.initialize();
    }
    
    @Test public void testNoSession() {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SessionContext.class, false));
    }
    
    @Test public void testSession() throws SessionException {
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        SessionContext sessionCtx = prc.getSubcontext(SessionContext.class, false);
        Assert.assertNotNull(sessionCtx);
        
        Assert.assertEquals(sessionCtx.getIdPSession().getPrincipalName(), "joe");
    }

    @Test public void testAddressRebind() throws SessionException {
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setRemoteAddr("::1");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        SessionContext sessionCtx = prc.getSubcontext(SessionContext.class, false);
        Assert.assertNotNull(sessionCtx);
        
        Assert.assertEquals(sessionCtx.getIdPSession().getPrincipalName(), "joe");
    }
    
    @Test public void testAddressMismatch() throws SessionException {
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setRemoteAddr("192.168.1.1");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SessionContext.class, false));
    }

    @Test public void testTimeout() throws SessionException, InterruptedException {
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        
        Thread.sleep(16000);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(SessionContext.class, false));
    }
    
    @Test public void testAddressLookup() throws ComponentInitializationException, SessionException {
        action = new PopulateSessionContext();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return requestProxy;}});
        action.setHttpServletResponseSupplier(new NonnullSupplier<> () {public HttpServletResponse get() { return responseProxy;}});
        action.setSessionResolver(sessionManager);
        action.setAddressLookupStrategy(input -> requestProxy.getHeader("User-Agent"));
        action.initialize();
        
        Cookie cookie = createSession("joe");
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).addHeader("User-Agent", "UnitTest-Client");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        SessionContext sessionCtx = prc.getSubcontext(SessionContext.class, false);
        Assert.assertNotNull(sessionCtx);
        
        Assert.assertEquals(sessionCtx.getIdPSession().getPrincipalName(), "joe");
        Assert.assertTrue(sessionCtx.getIdPSession().checkAddress("UnitTest-Client"));
    }
    
}
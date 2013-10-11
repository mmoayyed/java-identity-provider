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

import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.CookieManager;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;
import net.shibboleth.utilities.java.support.net.ThreadLocalHttpServletRequestProxy;
import net.shibboleth.utilities.java.support.net.ThreadLocalHttpServletResponseProxy;
import net.shibboleth.utilities.java.support.security.SecureRandomIdentifierGenerationStrategy;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link PopulateSessionContext} unit test. */
public class PopulateSessionContextTest {
    
    private static MemoryStorageService storageService;
    
    private static StorageBackedSessionManager manager;

    private ProfileRequestContext prc;
    
    private PopulateSessionContext action;
    
    @BeforeClass public static void setUp() throws ComponentInitializationException {
        storageService = new MemoryStorageService();
        storageService.initialize();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setHttpServletRequest(new ThreadLocalHttpServletRequestProxy());
        cookieManager.setHttpServletResponse(new ThreadLocalHttpServletResponseProxy());
        
        manager = new StorageBackedSessionManager();
        manager.setSessionTimeout(5000);
        manager.setStorageService(storageService);
        manager.setIDGenerator(new SecureRandomIdentifierGenerationStrategy());
        manager.setHttpServletRequest(new ThreadLocalHttpServletRequestProxy());
        manager.setHttpServletResponse(new ThreadLocalHttpServletResponseProxy());
        manager.setCookieManager(cookieManager);
        manager.initialize();
    }
    
    @AfterClass public static void tearDown() {
        manager.destroy();
        storageService.destroy();
    }
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        prc = new ProfileRequestContext();
        
        action = new PopulateSessionContext();
        action.setHttpServletRequest(new ThreadLocalHttpServletRequestProxy());
        action.setHttpServletResponse(new ThreadLocalHttpServletResponseProxy());
        action.setSessionResolver(manager);
        action.initialize();
    }
    
    @Test public void testNoSession() throws ProfileException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNull(prc.getSubcontext(SessionContext.class, false));
    }
    
    @Test public void testSession() throws ProfileException, SessionException {
        Cookie cookie = createSession();
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        SessionContext sessionCtx = prc.getSubcontext(SessionContext.class, false);
        Assert.assertNotNull(sessionCtx);
        
        Assert.assertEquals(sessionCtx.getIdPSession().getPrincipalName(), "joe");
    }

    @Test public void testAddressRebind() throws ProfileException, SessionException {
        Cookie cookie = createSession();
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setRemoteAddr("::1");
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        SessionContext sessionCtx = prc.getSubcontext(SessionContext.class, false);
        Assert.assertNotNull(sessionCtx);
        
        Assert.assertEquals(sessionCtx.getIdPSession().getPrincipalName(), "joe");
    }
    
    @Test public void testAddressMismatch() throws ProfileException, SessionException {
        Cookie cookie = createSession();
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setRemoteAddr("192.168.1.1");
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNull(prc.getSubcontext(SessionContext.class, false));
    }

    @Test public void testTimeout() throws ProfileException, SessionException, InterruptedException {
        Cookie cookie = createSession();
        
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        ((MockHttpServletRequest) HttpServletRequestResponseContext.getRequest()).setCookies(cookie);
        
        Thread.sleep(5000);
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNull(prc.getSubcontext(SessionContext.class, false));
    }
    
    /**
     * Creates a session and returns the cookie established by the SessionManager for it.
     * 
     * @return
     * @throws SessionException
     */
    private Cookie createSession() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        manager.createSession("joe");
        Cookie cookie = ((MockHttpServletResponse) HttpServletRequestResponseContext.getResponse()).getCookies()[0];
        HttpServletRequestResponseContext.clearCurrent();
        return cookie;
    }
}
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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.time.Duration;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.impl.StorageBackedSessionManager;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.net.CookieManager;
import net.shibboleth.shared.net.impl.HttpServletRequestResponseContext;
import net.shibboleth.shared.net.impl.ThreadLocalHttpServletRequestProxy;
import net.shibboleth.shared.net.impl.ThreadLocalHttpServletResponseProxy;
import net.shibboleth.shared.security.impl.SecureRandomIdentifierGenerationStrategy;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/** Base class for tests requiring a SessionManager component to be set up. */
public class SessionManagerBaseTestCase extends OpenSAMLInitBaseTestCase {
    
    protected MemoryStorageService storageService;
    
    protected StorageBackedSessionManager sessionManager;
    
    protected ThreadLocalHttpServletRequestProxy requestProxy;
    
    protected ThreadLocalHttpServletResponseProxy responseProxy;
    
    @BeforeClass public void setUp() throws ComponentInitializationException {
        requestProxy = new ThreadLocalHttpServletRequestProxy();
        responseProxy = new ThreadLocalHttpServletResponseProxy();
        
        storageService = new MemoryStorageService();
        storageService.setId("TestStorageService");

        CookieManager cookieManager = new CookieManager();
        cookieManager.setHttpServletRequestSupplier(new Supplier<>() {public HttpServletRequest get() { return requestProxy;}});
        cookieManager.setHttpServletResponseSupplier(new Supplier<>() {public HttpServletResponse get() { return responseProxy;}});
        cookieManager.initialize();
        
        sessionManager = new StorageBackedSessionManager();
        sessionManager.setSessionTimeout(Duration.ofSeconds(15));
        sessionManager.setStorageService(storageService);
        sessionManager.setIDGenerator(new SecureRandomIdentifierGenerationStrategy());
        sessionManager.setHttpServletRequestSupplier(new Supplier<>() {public HttpServletRequest get() {return requestProxy;}});
        sessionManager.setCookieManager(cookieManager);
        sessionManager.setId("Test Session Manager");


        adjustProperties();
        
        storageService.initialize();
        sessionManager.initialize();
    }
    
    /**
     * Allows override of component properties before initializing them.
     * 
     * @throws ComponentInitializationException ...
     */
    protected void adjustProperties() throws ComponentInitializationException {
        
    }
    
    @AfterClass public void tearDown() {
        sessionManager.destroy();
        storageService.destroy();
    }
    
    /**
     * Creates a session and returns the cookie established by the SessionManager for it.
     * 
     * @param principalName name of principal for session
     * 
     * @return the cookie established by the SessionManager
     * 
     * @throws SessionException ...
     */
    protected Cookie createSession(@Nonnull @NotEmpty final String principalName) throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        sessionManager.createSession(principalName);
        Cookie cookie = ((MockHttpServletResponse) HttpServletRequestResponseContext.getResponse()).getCookies()[0];
        HttpServletRequestResponseContext.clearCurrent();
        return cookie;
    }
}
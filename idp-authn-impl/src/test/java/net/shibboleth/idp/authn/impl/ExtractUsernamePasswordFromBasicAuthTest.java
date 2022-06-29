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

package net.shibboleth.idp.authn.impl;


import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.net.HttpHeaders;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** {@link ExtractUsernamePasswordFromBasicAuth} unit test. */
public class ExtractUsernamePasswordFromBasicAuthTest extends BaseAuthenticationContextTest {
    
    private ExtractUsernamePasswordFromBasicAuth action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new ExtractUsernamePasswordFromBasicAuth();
        action.setHttpServletRequest(new MockHttpServletRequest());
        action.initialize();
    }
    
    @Test public void testNoServlet() throws ComponentInitializationException {
        action = new ExtractUsernamePasswordFromBasicAuth();
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingIdentity() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingIdentity2() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(HttpHeaders.AUTHORIZATION, "foo");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testInvalid() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(HttpHeaders.AUTHORIZATION, "Basic foo:bar");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }

    @Test public void testInvalid2() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(HttpHeaders.AUTHORIZATION, "Basic Zm9vOg==");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }
    
    /* Test invalid base64 trailing bits. */
    @Test public void testInvalidBase64() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(HttpHeaders.AUTHORIZATION, "Basic AB==");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }
    
    @Test public void testValid() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(HttpHeaders.AUTHORIZATION, "Basic Zm9vOmJhcg==");
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        UsernamePasswordContext upCtx = authCtx.getSubcontext(UsernamePasswordContext.class, false);
        Assert.assertNotNull(upCtx, "No UsernamePasswordContext attached");
        Assert.assertEquals(upCtx.getUsername(), "foo");
        Assert.assertEquals(upCtx.getPassword(), "bar");
    }

    @Test public void idp1968() {
        ((MockHttpServletRequest) action.getHttpServletRequest()).addHeader(HttpHeaders.AUTHORIZATION, "Basic Zm9vOuKYr++4j2Jhcg==");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        UsernamePasswordContext upCtx = authCtx.getSubcontext(UsernamePasswordContext.class, false);
        Assert.assertNotNull(upCtx, "No UsernamePasswordContext attached");
        Assert.assertEquals(upCtx.getUsername(), "foo");
        Assert.assertEquals(upCtx.getPassword(), "☯️bar");
    }
}
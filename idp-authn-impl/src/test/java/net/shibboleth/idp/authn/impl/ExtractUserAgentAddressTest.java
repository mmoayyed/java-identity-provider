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


import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UserAgentContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ExtractUserAgentAddress} unit test. */
public class ExtractUserAgentAddressTest extends InitializeAuthenticationContextTest {
    
    private ExtractUserAgentAddress action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new ExtractUserAgentAddress();
        action.setHttpServletRequest(new MockHttpServletRequest());
        action.initialize();
    }
    
    @Test public void testMissingAddress() throws ProfileException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).setRemoteAddr(null);
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testValidAddress() throws ProfileException {
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        UserAgentContext uaCtx = authCtx.getSubcontext(UserAgentContext.class, false);
        Assert.assertNotNull(uaCtx, "No UserAgentContext attached");
        Assert.assertEquals(uaCtx.getAddress().getHostAddress(), MockHttpServletRequest.DEFAULT_REMOTE_ADDR);
    }

    @Test public void testInvalidAddress() throws ProfileException {
        ((MockHttpServletRequest) action.getHttpServletRequest()).setRemoteAddr("zorkmids");
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }
}
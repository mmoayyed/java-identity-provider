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

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.net.HttpHeaders;

/** {@link SendHTTPBasicAuthChallenge} unit test. */
public class SendHTTPBasicAuthChallengeTest extends InitializeAuthenticationContextTest {
    
    private SendHTTPBasicAuthChallenge action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new SendHTTPBasicAuthChallenge();
    }
    
    @Test public void testNoServlet() throws Exception {
        action.initialize();
        
        action.execute(prc);
        
        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_PROFILE_CTX);
    }

    @Test public void testDefault() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        prc.setHttpResponse(response);
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(response.getHeader(HttpHeaders.WWW_AUTHENTICATE),
                SendHTTPBasicAuthChallenge.BASIC + " " + SendHTTPBasicAuthChallenge.REALM + "=\"default\"");
    }

    @Test public void testNonDefault() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        prc.setHttpResponse(response);
        action.setRealm("foo");
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(response.getHeader(HttpHeaders.WWW_AUTHENTICATE),
                SendHTTPBasicAuthChallenge.BASIC + " " + SendHTTPBasicAuthChallenge.REALM + "=\"foo\"");
    }
}
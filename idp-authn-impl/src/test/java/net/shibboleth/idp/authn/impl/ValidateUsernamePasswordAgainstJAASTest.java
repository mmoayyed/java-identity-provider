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


import javax.security.auth.login.LoginException;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ValidateUsernamePasswordAgainstJAAS} unit test. */
public class ValidateUsernamePasswordAgainstJAASTest extends InitializeAuthenticationContextTest {
    
    private ValidateUsernamePasswordAgainstJAAS action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new ValidateUsernamePasswordAgainstJAAS();
    }

    @Test public void testMissingFlow() throws Exception {
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_AUTHN_CTX);
    }
    
    @Test public void testMissingUser() throws Exception {
        prc.getSubcontext(AuthenticationContext.class, false).setAttemptedFlow(authenticationFlows.get(0));
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws Exception {
        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        ac.getSubcontext(UsernamePasswordContext.class, true);
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testBadConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("username", "foo");
        request.addParameter("password", "bar");
        prc.setHttpRequest(request);

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        action.initialize();
        
        doExtract(prc);
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertTrue(ac.getLoginException() instanceof LoginException);
    }

    /*
    @Test public void testAuthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteUser("baz");
        prc.setHttpRequest(request);

        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract(prc);
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "baz");
    }
    */

    private void doExtract(ProfileRequestContext prc) throws Exception {
        ExtractUsernamePasswordFromFormRequest extract = new ExtractUsernamePasswordFromFormRequest();
        extract.initialize();
        extract.execute(prc);
    }
}
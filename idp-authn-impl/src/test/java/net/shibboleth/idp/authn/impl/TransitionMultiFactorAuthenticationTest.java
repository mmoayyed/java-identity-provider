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

import javax.security.auth.Subject;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.ActionTestingSupport;

/** {@link TransitionMultiFactorAuthentication} unit test. */
public class TransitionMultiFactorAuthenticationTest extends BaseMultiFactorAuthenticationContextTest {
    
    private TransitionMultiFactorAuthentication action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new TransitionMultiFactorAuthentication();
        action.initialize();
    }

    @Test public void testNoTransition() {
        mfa.setNextFlowId("none");
        Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_POTENTIAL_FLOW);
    }

    @Test public void testNoResult() {
        mfa.setNextFlowId("authn/test2");
        Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }

    @Test public void testTransitions() {
        Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(mfa.getNextFlowId(), "authn/test1");
        Assert.assertNull(ac.getAuthenticationResult());
        
        ac.setAuthenticationResult(new AuthenticationResult("authn/test1", new UsernamePrincipal("foo")));
        
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(mfa.getNextFlowId(), "interim");
        Assert.assertNull(ac.getAuthenticationResult());
        
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(mfa.getNextFlowId(), "authn/test2");
        Assert.assertNull(ac.getAuthenticationResult());
        
        ac.setAuthenticationResult(new AuthenticationResult("authn/test2", new UsernamePrincipal("foo2")));
        
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(mfa.getNextFlowId());
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertEquals(ac.getAuthenticationResult().getAuthenticationFlowId(), "authn/MFA");
        final Subject subject = ac.getAuthenticationResult().getSubject();
        Assert.assertEquals(subject.getPrincipals().size(), 4);
        Assert.assertEquals(subject.getPrincipals(UsernamePrincipal.class).size(), 2);
        Assert.assertEquals(subject.getPrincipals(AuthenticationResultPrincipal.class).size(), 2);
    }
    
}
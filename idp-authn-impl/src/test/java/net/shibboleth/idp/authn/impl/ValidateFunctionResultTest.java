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

import java.security.Principal;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Functions;

/** {@link ValidateFunctionResult} unit test. */
public class ValidateFunctionResultTest extends BaseAuthenticationContextTest {
    
    private ValidateFunctionResult action;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        prc.getSubcontext(AuthenticationContext.class).setAttemptedFlow(authenticationFlows.get(0));

        action = new ValidateFunctionResult();
    }

    @Test public void testMissingFlow() throws ComponentInitializationException {
        prc.getSubcontext(AuthenticationContext.class).setAttemptedFlow(null);
        
        action.setResultLookupStrategy(FunctionSupport.<ProfileRequestContext,Object>constant(null));
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }
    
    @Test public void testNoCredentials() throws ComponentInitializationException {
        
        action.setResultLookupStrategy(FunctionSupport.<ProfileRequestContext,Object>constant(null));
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testInvalidType() throws ComponentInitializationException {
        
        action.setResultLookupStrategy(Functions.<ProfileRequestContext>identity());
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }

    @Test public void testPrincipalName() throws ComponentInitializationException {
        action.setResultLookupStrategy(FunctionSupport.<ProfileRequestContext,String>constant("foo"));
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertFalse(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "foo");
    }
    
    @Test public void testPrincipal() throws ComponentInitializationException {
        action.setResultLookupStrategy(FunctionSupport.<ProfileRequestContext,Principal>constant(new TestPrincipal("foo")));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertFalse(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                TestPrincipal.class).iterator().next().getName(), "foo");
    }

    @Test public void testSubject() throws ComponentInitializationException {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new TestPrincipal("foo"));

        action.setResultLookupStrategy(FunctionSupport.<ProfileRequestContext,Subject>constant(subject));
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(ac.getAuthenticationResult());
        Assert.assertFalse(ac.getAuthenticationResult().isPreviousResult());
        Assert.assertEquals(ac.getAuthenticationResult().getSubject().getPrincipals(
                TestPrincipal.class).iterator().next().getName(), "foo");
    }

}
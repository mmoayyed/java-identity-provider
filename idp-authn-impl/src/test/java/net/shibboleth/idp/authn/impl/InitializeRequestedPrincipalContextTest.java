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
import java.util.Collections;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.testing.MockAuthenticationProfileConfiguration;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.IdPEventIds;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.MockProfileConfiguration;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link InitializeRequestedPrincipalContext} unit test. */
public class InitializeRequestedPrincipalContextTest {

    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private InitializeRequestedPrincipalContext action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        action = new InitializeRequestedPrincipalContext();
        action.initialize();
    }

    /**
     * Test that the action bails if set not to replace.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoReplace() throws Exception {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, true);
        authCtx.getSubcontext(RequestedPrincipalContext.class, true).setOperator("foo");

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getSubcontext(RequestedPrincipalContext.class).getOperator(), "foo");
    }
    
    /**
     * Test that the action errors out properly if there is no relying party context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoRelyingPartyContext() throws Exception {
        prc.removeSubcontext(RelyingPartyContext.class);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, true);

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_RELYING_PARTY_CTX);
        Assert.assertNull(authCtx.getSubcontext(RequestedPrincipalContext.class));
    }

    /**
     * Test that the action errors out properly if there is no relying party configuration.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoProfileConfiguration() throws Exception {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, true);

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_PROFILE_CONFIG);
        Assert.assertNull(authCtx.getSubcontext(RequestedPrincipalContext.class));
    }

    /**
     * Test that the action errors out properly if the desired profile configuration is not configured.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testInvalidProfileConfiguration() throws Exception {
        src = new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                Collections.<ProfileConfiguration>singleton(new MockProfileConfiguration("mock"))).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, true);

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_PROFILE_CONFIG);
        Assert.assertNull(authCtx.getSubcontext(RequestedPrincipalContext.class));
    }

    /**
     * Test that the action works with no methods supplied.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoMethods() throws Exception {
        final MockAuthenticationProfileConfiguration mock =
                new MockAuthenticationProfileConfiguration("mock", Collections.<Principal>emptyList());
        src = new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                Collections.<ProfileConfiguration>singleton(mock)).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, true);
        
        authCtx.getSubcontext(RequestedPrincipalContext.class, true).setOperator("foo");

        action = new InitializeRequestedPrincipalContext();
        action.setReplaceExistingContext(true);
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getSubcontext(RequestedPrincipalContext.class).getOperator(), "foo");
    }
    
    /**
     * Test that the action works with methods supplied.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testWithMethods() throws Exception {
        final Principal method = new TestPrincipal("test");
        final MockAuthenticationProfileConfiguration mock =
                new MockAuthenticationProfileConfiguration("mock", Collections.singletonList(method));
        src = new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                Collections.<ProfileConfiguration>singleton(mock)).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, true);

        authCtx.getSubcontext(RequestedPrincipalContext.class, true).setOperator("foo");

        action = new InitializeRequestedPrincipalContext();
        action.setReplaceExistingContext(true);
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        RequestedPrincipalContext rpCtx = authCtx.getSubcontext(RequestedPrincipalContext.class, false);
        Assert.assertNotNull(rpCtx);
        Assert.assertEquals(rpCtx.getOperator(), "exact");
        Assert.assertEquals(rpCtx.getRequestedPrincipals().size(), 1);
        Assert.assertSame(method, rpCtx.getRequestedPrincipals().get(0));
    }
    
}

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

package net.shibboleth.idp.saml.impl.profile.saml1;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.profile.IdPEventIds;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.idp.saml.profile.saml1.SAML1ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Response;
import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AddAuthenticationStatementToAssertion} unit test. */
public class AddAuthenticationStatementToAssertionTest extends OpenSAMLInitBaseTestCase {

    private AddAuthenticationStatementToAssertion action;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        action = new AddAuthenticationStatementToAssertion();
        action.setHttpServletRequest(new MockHttpServletRequest());
        action.setId("test");
        action.initialize();
    }
    
    /** Test that the action errors out properly if there is no authentication context. */
    @Test public void testNoAuthnContext() throws Exception {
        final ProfileRequestContext profileCtx = new ProfileRequestContext();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        final ProfileRequestContext profileCtx = new ProfileRequestContext();
        profileCtx.getSubcontext(AuthenticationContext.class, true);

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, IdPEventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action errors out properly if there is no response. */
    @Test public void testNoResponse() throws Exception {
        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();
        profileCtx.getSubcontext(AuthenticationContext.class, true).setAuthenticationResult(
                new AuthenticationResult("Test", new AuthenticationMethodPrincipal("Test")));

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, EventIds.INVALID_MSG_CTX);
    }

    /** Test that the action proceeds properly returning no assertions if there is no authentication result. */
    @Test public void testNoAuthenticationStatement() throws Exception {
        final ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(SAML1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();
        profileCtx.getSubcontext(AuthenticationContext.class, true);

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    /** Test that the authentication statement is properly added. */
    @Test public void testAddAuthenticationStatement() throws Exception {
        final ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(SAML1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        final long now = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);
        
        profileCtx.getSubcontext(AuthenticationContext.class, true).setAuthenticationResult(
                new AuthenticationResult("Test", new AuthenticationMethodPrincipal("Test")));

        ((MockHttpServletRequest) action.getHttpServletRequest()).setRemoteAddr("127.0.0.1");
        
        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        Assert.assertNotNull(profileCtx.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileCtx.getOutboundMessageContext().getMessage() instanceof Response);

        final Response response = (Response) profileCtx.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        final Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getAuthenticationStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthenticationStatements().get(0));

        final AuthenticationStatement authenticationStatement = assertion.getAuthenticationStatements().get(0);
        Assert.assertTrue(authenticationStatement.getAuthenticationInstant().getMillis() > now);
        Assert.assertEquals(authenticationStatement.getAuthenticationMethod(), "Test");
        
        Assert.assertNotNull(authenticationStatement.getSubjectLocality());
        Assert.assertEquals(authenticationStatement.getSubjectLocality().getIPAddress(), "127.0.0.1");
    }
    
    /** Test that the authentication statement is properly added with the right method. */
    @Test public void testAddAuthenticationStatementAndMethod() throws Exception {
        final ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(SAML1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        final Subject subject = new Subject();
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("Foo"));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("Bar"));
        profileCtx.getSubcontext(AuthenticationContext.class, true).setAuthenticationResult(
                new AuthenticationResult("Test", subject));
        final RequestedPrincipalContext requested = new RequestedPrincipalContext();
        requested.setMatchingPrincipal(new AuthenticationMethodPrincipal("Bar"));
        profileCtx.getSubcontext(AuthenticationContext.class, false).addSubcontext(requested);
        
        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        Assert.assertNotNull(profileCtx.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileCtx.getOutboundMessageContext().getMessage() instanceof Response);

        final Response response = (Response) profileCtx.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getAuthenticationStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthenticationStatements().get(0));

        final AuthenticationStatement authenticationStatement = assertion.getAuthenticationStatements().get(0);
        Assert.assertEquals(authenticationStatement.getAuthenticationMethod(), "Bar");
    }
    
}
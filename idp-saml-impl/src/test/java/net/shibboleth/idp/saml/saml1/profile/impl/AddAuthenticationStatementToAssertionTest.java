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

package net.shibboleth.idp.saml.saml1.profile.impl;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.storage.StorageSerializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.DefaultAuthenticationResultSerializer;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.servlet.impl.HttpServletRequestResponseContext;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link AddAuthenticationStatementToAssertion} unit test. */
public class AddAuthenticationStatementToAssertionTest extends OpenSAMLInitBaseTestCase {

    private RequestContext rc;
    
    private ProfileRequestContext prc;

    private AddAuthenticationStatementToAssertion action;
    
    @Nonnull private MockHttpServletRequest getMockHttpServletRequest() {
        final MockHttpServletRequest result = (MockHttpServletRequest) action.getHttpServletRequest();
        assert result  != null;
        return result;
    }

    /**
     * Test setup.
     * 
     * @throws ComponentInitializationException
     */
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().setOutboundMessage(
                SAML1ActionTestingSupport.buildResponse()).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);

        action = new AddAuthenticationStatementToAssertion();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();
    }
    
    /**
     * Test that the action errors out properly if there is no authentication context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAuthnContext() throws Exception {
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    /**
     * Test that the action errors out properly if there is no relying party context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoRelyingPartyContext() throws Exception {
        prc.getSubcontext(AuthenticationContext.class, true);
        prc.removeSubcontext(RelyingPartyContext.class);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    /**
     * Test that the action errors out properly if there is no context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoContext() throws Exception {
        prc.setOutboundMessageContext(null);
        prc.getOrCreateSubcontext(AuthenticationContext.class).setAuthenticationResult(
                new AuthenticationResult("Test", new AuthenticationMethodPrincipal("Test")));

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }

    /**
     * Test that the action proceeds properly returning no assertions if there is no authentication result.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAuthenticationStatement() throws Exception {
        prc.getSubcontext(AuthenticationContext.class, true);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    /**
     * Test that the authentication statement is properly added.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testAddAuthenticationStatement() throws Exception {
        final Instant now = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);
        
        final StorageSerializer<AuthenticationResult> serializer = new DefaultAuthenticationResultSerializer();
        serializer.initialize();
        
        final AuthenticationFlowDescriptor fd = new AuthenticationFlowDescriptor();
        fd.setId("Test");
        fd.setResultSerializer(serializer);
        fd.initialize();
        
        prc.getOrCreateSubcontext(AuthenticationContext.class).getAvailableFlows().put("Test", fd);
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac!=null;
        ac.setAuthenticationResult(
                new AuthenticationResult("Test", new AuthenticationMethodPrincipal("Test")));

        getMockHttpServletRequest().setRemoteAddr("127.0.0.1");
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;

        Assert.assertNotNull(omc.getMessage());
        Assert.assertTrue(omc.getMessage() instanceof Response);

        final Response response = (Response) omc.getMessage();
        assert response !=null;
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        final Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getAuthenticationStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthenticationStatements().get(0));

        final AuthenticationStatement authenticationStatement = assertion.getAuthenticationStatements().get(0);
        Assert.assertTrue(authenticationStatement.getAuthenticationInstant().isAfter(now));
        Assert.assertEquals(authenticationStatement.getAuthenticationMethod(), "Test");
        
        Assert.assertNotNull(authenticationStatement.getSubjectLocality());
        Assert.assertEquals(authenticationStatement.getSubjectLocality().getIPAddress(), "127.0.0.1");
    }
    
    /**
     * Test that the authentication statement is properly added with the right method.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testAddAuthenticationStatementAndMethod() throws Exception {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("Foo"));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("Bar"));
        prc.getOrCreateSubcontext(AuthenticationContext.class).setAuthenticationResult(
                new AuthenticationResult("Test", subject));
        final RequestedPrincipalContext requested = new RequestedPrincipalContext();
        requested.setMatchingPrincipal(new AuthenticationMethodPrincipal("Bar"));
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, false);
        assert ac!=null;
        ac.addSubcontext(requested);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;

        Assert.assertNotNull(omc.getMessage());
        Assert.assertTrue(omc.getMessage() instanceof Response);

        final Response response = (Response) omc.getMessage();
        assert response  != null;
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getAuthenticationStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthenticationStatements().get(0));

        final AuthenticationStatement authenticationStatement = assertion.getAuthenticationStatements().get(0);
        Assert.assertEquals(authenticationStatement.getAuthenticationMethod(), "Bar");
    }
    
}

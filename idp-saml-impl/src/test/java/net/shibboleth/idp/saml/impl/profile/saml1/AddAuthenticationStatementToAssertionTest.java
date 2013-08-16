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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.ActionTestingSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionTestingSupport;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Response;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AddAuthenticationStatementToAssertion} unit test. */
public class AddAuthenticationStatementToAssertionTest extends OpenSAMLInitBaseTestCase {

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        AddAuthenticationStatementToAssertion action = new AddAuthenticationStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action errors out properly if there is no response. */
    @Test public void testNoResponse() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        AddAuthenticationStatementToAssertion action = new AddAuthenticationStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, SamlEventIds.NO_RESPONSE);
    }

    /** Test that the action proceeds properly returning no assertions if there is no authentication context. */
    @Test public void testNoAuthenticationStatement() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        AddAuthenticationStatementToAssertion action = new AddAuthenticationStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(profileCtx.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileCtx.getOutboundMessageContext().getMessage() instanceof Response);

        Response response = (Response) profileCtx.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 0);
    }

    /** Test that the action throws an exception if the authentication context lacks an attempted workflow. */
    @Test public void testNoAttemptedWorkflow() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        AuthenticationContext authCtx = new AuthenticationContext();
        profileCtx.addSubcontext(authCtx);

        AddAuthenticationStatementToAssertion action = new AddAuthenticationStatementToAssertion();
        action.setId("test");
        action.initialize();

        try {
            action.doExecute(new MockRequestContext(), profileCtx);
            Assert.fail();
        } catch (NullPointerException e) {
            // ok
        }
    }

    /** Test that the authentication statement is properly added. */
    @Test public void testAddAuthenticationStatement() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        AuthenticationContext authCtx = new AuthenticationContext();
        authCtx.setAttemptedFlow(new AuthenticationFlowDescriptor("test"));

        long now = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);
        authCtx.setCompletionInstant();

        profileCtx.addSubcontext(authCtx);

        AddAuthenticationStatementToAssertion action = new AddAuthenticationStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(profileCtx.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileCtx.getOutboundMessageContext().getMessage() instanceof Response);

        Response response = (Response) profileCtx.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getAuthenticationStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthenticationStatements().get(0));

        AuthenticationStatement authenticationStatement = assertion.getAuthenticationStatements().get(0);
        Assert.assertTrue(authenticationStatement.getAuthenticationInstant().getMillis() > now);
        Assert.assertEquals(authenticationStatement.getAuthenticationMethod(), "test");
    }
}

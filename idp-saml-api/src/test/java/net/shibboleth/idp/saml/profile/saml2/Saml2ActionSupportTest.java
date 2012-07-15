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

package net.shibboleth.idp.saml.profile.saml2;

import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.saml.profile.ActionTestSupportAction;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Function;

/**
 * @link Saml1ActionSupport} unit test.
 */
public class Saml2ActionSupportTest extends OpenSAMLInitBaseTestCase {

    /** Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}. */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy =
            new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);

    /**
     * Test that when an assertion is added to response it goes there.
     * 
     * @throws ComponentInitializationException if we cannot setup our environment.
     */
    @Test public void testAddAssertionToResponse() throws ComponentInitializationException {

        final Response response = Saml2ActionTestingSupport.buildResponse();

        RequestContext springRequestContext =
                new RequestContextBuilder().setOutboundMessage(response)
                        .setRelyingPartyProfileConfigurations(Saml2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        final ProfileRequestContext<Object, Response> profileRequestContext =
                (ProfileRequestContext<Object, Response>) springRequestContext.getConversationScope().get(
                        ProfileRequestContext.BINDING_KEY);

        ActionTestSupportAction action = new ActionTestSupportAction();
        RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);

        Assert.assertEquals(response.getAssertions().size(), 0, "Expected zarro assertions before insert");
        Assertion assertion = Saml2ActionSupport.addAssertionToResponse(action, relyingPartyCtx, response);
        Assert.assertEquals(response.getAssertions().size(), 1, "Expected but one assertion after insert");
        Assert.assertTrue(response.getAssertions().contains(assertion), "Inserted assertion should be there");
        Assertion second = Saml2ActionSupport.addAssertionToResponse(action, relyingPartyCtx, response);
        Assert.assertEquals(response.getAssertions().size(), 2, "Expected two assertions after two inserts");
        Assert.assertTrue(response.getAssertions().contains(assertion), "Inserted assertion should be there");
        Assert.assertNotSame(second, assertion, "Two separate assertions should have been added");
    }

    /**
     * Test that only one Conditions is added to an assertion.
     * 
     * @throws ComponentInitializationException if problems setting up our environment .
     */
    @Test public void testAddConditionsToAssertion() throws ComponentInitializationException {
        ActionTestSupportAction action = new ActionTestSupportAction();
        Assertion assertion = Saml2ActionTestingSupport.buildAssertion();

        Assert.assertNull(assertion.getConditions(), "No conditions on empty assertion");
        Conditions conditions = Saml2ActionSupport.addConditionsToAssertion(action, assertion);
        Assert.assertEquals(assertion.getConditions(), conditions, "Added conditions - should be what we got back");
        Conditions second = Saml2ActionSupport.addConditionsToAssertion(action, assertion);
        Assert.assertEquals(conditions, second, "Added conditions twice - should return the same value twice");

    }
}

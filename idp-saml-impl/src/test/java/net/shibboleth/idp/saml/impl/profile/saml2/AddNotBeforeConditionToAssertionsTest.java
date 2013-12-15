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

package net.shibboleth.idp.saml.impl.profile.saml2;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.saml.profile.SAMLEventIds;
import net.shibboleth.idp.saml.profile.saml2.Saml2ActionTestingSupport;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AddNotBeforeConditionToAssertions} unit test. */
public class AddNotBeforeConditionToAssertionsTest  extends OpenSAMLInitBaseTestCase {

    /** Test that action errors out properly if there is no response. */
    @Test
    public void testNoResponse() throws Exception {
        AddNotBeforeConditionToAssertions action = new AddNotBeforeConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(new RequestContextBuilder().buildRequestContext());

        ActionTestingSupport.assertEvent(result, SAMLEventIds.NO_RESPONSE);
    }

    /** Test that action errors out properly if there is no assertion in the response. */
    @Test
    public void testNoAssertion() throws Exception {
        RequestContext springRequestContext =
                new RequestContextBuilder().setOutboundMessage(Saml2ActionTestingSupport.buildResponse())
                        .setRelyingPartyProfileConfigurations(Saml2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        AddNotBeforeConditionToAssertions action = new AddNotBeforeConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);

        ActionTestingSupport.assertEvent(result, SAMLEventIds.NO_ASSERTION);
    }

    /**
     * Test that the condition is properly added if there is a single assertion, without a Conditions element, in the
     * response.
     */
    @Test
    public void testSingleAssertion() throws Exception {
        Assertion assertion = Saml2ActionTestingSupport.buildAssertion();

        Response response = Saml2ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);

        RequestContext springRequestContext =
                new RequestContextBuilder().setOutboundMessage(response)
                        .setRelyingPartyProfileConfigurations(Saml2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        AddNotBeforeConditionToAssertions action = new AddNotBeforeConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertEquals(response.getAssertions().size(), 1);

        Assert.assertNotNull(assertion.getConditions());
        Assert.assertNotNull(assertion.getConditions().getNotBefore());
    }

    /**
     * Test that the condition is properly added if there is a single assertion, with a Conditions element, in the
     * response.
     */
    @Test
    public void testSingleAssertionWithExistingConditions() throws Exception {
        SAMLObjectBuilder<Conditions> conditionsBuilder =
                (SAMLObjectBuilder<Conditions>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(Conditions.TYPE_NAME);
        Conditions conditions = conditionsBuilder.buildObject();

        Assertion assertion = Saml2ActionTestingSupport.buildAssertion();
        assertion.setConditions(conditions);

        Response response = Saml2ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);

        RequestContext springRequestContext =
                new RequestContextBuilder().setOutboundMessage(response)
                        .setRelyingPartyProfileConfigurations(Saml2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        AddNotBeforeConditionToAssertions action = new AddNotBeforeConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(assertion.getConditions());
        Assert.assertSame(assertion.getConditions(), conditions);
        Assert.assertNotNull(assertion.getConditions().getNotBefore());
    }

    /** Test that the condition is properly added if there are multiple assertions in the response. */
    @Test
    public void testMultipleAssertion() throws Exception {
        Response response = Saml2ActionTestingSupport.buildResponse();
        response.getAssertions().add(Saml2ActionTestingSupport.buildAssertion());
        response.getAssertions().add(Saml2ActionTestingSupport.buildAssertion());
        response.getAssertions().add(Saml2ActionTestingSupport.buildAssertion());

        RequestContext springRequestContext =
                new RequestContextBuilder().setOutboundMessage(response)
                        .setRelyingPartyProfileConfigurations(Saml2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        AddNotBeforeConditionToAssertions action = new AddNotBeforeConditionToAssertions();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertEquals(response.getAssertions().size(), 3);

        for (Assertion assertion : response.getAssertions()) {
            Assert.assertNotNull(assertion.getConditions());
            Assert.assertNotNull(assertion.getConditions().getNotBefore());
        }
    }
}
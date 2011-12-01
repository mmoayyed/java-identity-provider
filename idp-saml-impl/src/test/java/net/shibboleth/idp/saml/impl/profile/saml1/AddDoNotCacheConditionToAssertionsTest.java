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

import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.InvalidOutboundMessageException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.impl.profile.SamlActionTestingSupport;

import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.Conditions;
import org.opensaml.saml1.core.DoNotCacheCondition;
import org.opensaml.saml1.core.Response;
import org.opensaml.xml.Configuration;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AddDoNotCacheConditionToAssertions} unit test. */
public class AddDoNotCacheConditionToAssertionsTest {

    // TODO need to init OpenSAML

    /** Test that action errors out properly if there is no response. */
    @Test
    public void testNoResponse() {
        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();

        SamlActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, "http://example.org", null);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddDoNotCacheConditionToAssertions action = new AddDoNotCacheConditionToAssertions();
        Event result = action.execute(springRequestContext);
        
        Assert.assertEquals(result.getId(), ActionSupport.ERROR_EVENT_ID);
        Assert.assertTrue(InvalidOutboundMessageException.class.isInstance(ActionSupport.getEventError(result)));
    }
    
    /** Test that action errors out properly if there is no assertion in the response. */
    @Test
    public void testNoAssertion() {
        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        
        profileRequestContext.getOutboundMessageContext().setMessage(Saml1ActionTestingSupport.buildResponse());

        SamlActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, "http://example.org", null);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddDoNotCacheConditionToAssertions action = new AddDoNotCacheConditionToAssertions();
        Event result = action.execute(springRequestContext);
        
        Assert.assertEquals(result.getId(), ActionSupport.ERROR_EVENT_ID);
        Assert.assertTrue(InvalidOutboundMessageException.class.isInstance(ActionSupport.getEventError(result)));
    }

    /**
     * Test that the condition is properly added if there is a single assertion, without a Conditions element, in the
     * response.
     */
    @Test
    public void testSingleAssertion() {        
        Assertion assertion = Saml1ActionTestingSupport.buildAssertion();

        Response response = Saml1ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);
        
        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        SamlActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, "http://example.org", null);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddDoNotCacheConditionToAssertions action = new AddDoNotCacheConditionToAssertions();
        action.execute(springRequestContext);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertEquals(response.getAssertions().size(), 1);

        Assert.assertNotNull(assertion.getConditions());
        Assert.assertNotNull(assertion.getConditions().getDoNotCacheConditions());
        Assert.assertEquals(assertion.getConditions().getDoNotCacheConditions().size(), 1);
    }

    /**
     * Test that the condition is properly added if there is a single assertion, with a Conditions element, in the
     * response.
     */
    @Test
    public void testSingleAssertionWithExistingCondition() {
        SAMLObjectBuilder<DoNotCacheCondition> dncConditionBuilder =
                (SAMLObjectBuilder<DoNotCacheCondition>) Configuration.getBuilderFactory().getBuilder(
                        DoNotCacheCondition.TYPE_NAME);
        DoNotCacheCondition dncCondition = dncConditionBuilder.buildObject();

        SAMLObjectBuilder<Conditions> conditionsBuilder =
                (SAMLObjectBuilder<Conditions>) Configuration.getBuilderFactory().getBuilder(Conditions.TYPE_NAME);
        Conditions conditions = conditionsBuilder.buildObject();
        conditions.getDoNotCacheConditions().add(dncCondition);

        Assertion assertion = Saml1ActionTestingSupport.buildAssertion();
        assertion.setConditions(conditions);

        Response response = Saml1ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);

        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        SamlActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, "http://example.org", null);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddDoNotCacheConditionToAssertions action = new AddDoNotCacheConditionToAssertions();
        action.execute(springRequestContext);

        Assert.assertNotNull(assertion.getConditions());
        Assert.assertNotNull(assertion.getConditions().getDoNotCacheConditions());
        Assert.assertEquals(assertion.getConditions().getDoNotCacheConditions().size(), 1);
    }

    /** Test that an addition DoNotCache is not added if an assertion already contains one. */
    @Test
    public void testSingleAssertionWithExistingDoNotCacheCondition() {
        SAMLObjectBuilder<DoNotCacheCondition> dncConditionBuilder =
                (SAMLObjectBuilder<DoNotCacheCondition>) Configuration.getBuilderFactory().getBuilder(
                        DoNotCacheCondition.TYPE_NAME);
        DoNotCacheCondition dncCondition = dncConditionBuilder.buildObject();

        SAMLObjectBuilder<Conditions> conditionsBuilder =
                (SAMLObjectBuilder<Conditions>) Configuration.getBuilderFactory().getBuilder(Conditions.TYPE_NAME);
        Conditions conditions = conditionsBuilder.buildObject();
        conditions.getDoNotCacheConditions().add(dncCondition);

        Assertion assertion = Saml1ActionTestingSupport.buildAssertion();
        assertion.setConditions(conditions);

        Response response = Saml1ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);

        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        SamlActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, "http://example.org", null);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddDoNotCacheConditionToAssertions action = new AddDoNotCacheConditionToAssertions();
        action.execute(springRequestContext);

        Assert.assertNotNull(assertion.getConditions());
        Assert.assertNotNull(assertion.getConditions().getDoNotCacheConditions());
        Assert.assertEquals(assertion.getConditions().getDoNotCacheConditions().size(), 1);
    }
    
    /** Test that the condition is properly added if there are multiple assertions in the response. */
    @Test
    public void testMultipleAssertion() {
        Response response = Saml1ActionTestingSupport.buildResponse();
        response.getAssertions().add(Saml1ActionTestingSupport.buildAssertion());
        response.getAssertions().add(Saml1ActionTestingSupport.buildAssertion());
        response.getAssertions().add(Saml1ActionTestingSupport.buildAssertion());

        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        SamlActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, "http://example.org", null);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddDoNotCacheConditionToAssertions action = new AddDoNotCacheConditionToAssertions();
        action.execute(springRequestContext);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertEquals(response.getAssertions().size(), 3);

        for (Assertion assertion : response.getAssertions()) {
            Assert.assertNotNull(assertion.getConditions());
            Assert.assertNotNull(assertion.getConditions().getDoNotCacheConditions());
            Assert.assertEquals(assertion.getConditions().getDoNotCacheConditions().size(), 1);
        }
    }
}
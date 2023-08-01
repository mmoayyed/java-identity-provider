/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.test.flows.admin;

import static org.testng.Assert.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.impl.ResolverTestRequestDecoder;
import net.shibboleth.idp.test.flows.AbstractFlowTest;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;

/**
 * resolvertest flow test.
 */
public class ResolverTestFlowTest extends AbstractFlowTest {

    /** The flow id. */
    @Nonnull public final static String FLOW_ID = "admin/resolvertest";

    /**
     * Test the resolvertest flow.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testJSONResolverTestFlow() throws Exception {

        buildRequest(null);

        overrideEndStateOutput(FLOW_ID, "ResponseView");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "ResponseView");
        assertTrue(responseBody.contains("jdoe@example.org"));
    }

    /**
     * Test the resolvertest flow using SAML 1.1.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1ResolverTestFlow() throws Exception {

        buildRequest(SAMLConstants.SAML11P_NS);

        overrideEndStateOutput(FLOW_ID, "ResponseView");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "ResponseView");
        assertTrue(responseBody.contains("<saml1:Attribute AttributeName=\"urn:mace:dir:attribute-def:eduPersonPrincipalName\""));
        assertTrue(responseBody.contains("<saml1:AttributeValue Scope=\"example.org\">jdoe</saml1:AttributeValue>"));
    }

    /**
     * Test the resolvertest flow using SAML 2.0.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2ResolverTestFlow() throws Exception {

        buildRequest(SAMLConstants.SAML20P_NS);

        overrideEndStateOutput(FLOW_ID, "ResponseView");

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        final String responseBody = response.getContentAsString();
        final FlowExecutionOutcome outcome = result.getOutcome();
        assertEquals(outcome.getId(), "ResponseView");
        assertTrue(responseBody.contains("<saml2:Attribute FriendlyName=\"eduPersonPrincipalName\" Name=\"urn:oid:1.3.6.1.4.1.5923.1.1.1.6\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">"));
        assertTrue(responseBody.contains("<saml2:AttributeValue>jdoe@example.org</saml2:AttributeValue>"));
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @param protocol optional protocol parameter
     */
    private void buildRequest(@Nullable final String protocol) {
        request.setMethod("GET");
        request.addParameter(ResolverTestRequestDecoder.REQUESTER_ID_PARAM, SP_ENTITY_ID);
        request.addParameter(ResolverTestRequestDecoder.PRINCIPAL_PARAM, "jdoe");
        if (protocol != null) {
            request.addParameter(ResolverTestRequestDecoder.PROTOCOL_PARAM, protocol);
        }
    }
}

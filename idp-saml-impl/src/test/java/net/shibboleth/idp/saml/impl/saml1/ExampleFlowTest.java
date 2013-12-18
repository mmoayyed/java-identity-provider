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

package net.shibboleth.idp.saml.impl.saml1;


import net.shibboleth.idp.saml.impl.profile.BaseIdPInitiatedSSORequestMessageDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.config.FlowDefinitionResourceFactory;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.execution.AbstractXmlFlowExecutionTests;

// TODO incorrect
public class ExampleFlowTest extends AbstractXmlFlowExecutionTests {

    private final Logger log = LoggerFactory.getLogger(ExampleFlowTest.class);

    /** {@inheritDoc} */
    protected FlowDefinitionResource getResource(FlowDefinitionResourceFactory resourceFactory) {
        return resourceFactory.createFileResource("src/main/resources/flows/Shibboleth/SSO/saml1-flow.xml");
    }

    @Override protected FlowDefinitionResource[] getModelResources(FlowDefinitionResourceFactory resourceFactory) {
        return new FlowDefinitionResource[] {
                resourceFactory.createFileResource("src/main/resources/flows/parent-flow.xml"),
                resourceFactory.createFileResource("src/main/resources/flows/Shibboleth/SSO/saml1-flow-beans.xml")};
    }

    public void brokenTest() throws Exception {

        MockExternalContext context = new MockExternalContext();

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        context.setNativeRequest(mockRequest);

        mockRequest.setParameter(BaseIdPInitiatedSSORequestMessageDecoder.PROVIDER_ID_PARAM, "entityId");

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        context.setNativeResponse(mockResponse);      

        log.info("start flow");

        startFlow(context);

        log.info("getFlowExecutionOutcome().getId() {}", getFlowExecutionOutcome().getId());

        assertFlowExecutionOutcomeEquals("end");
    }
    
    public void testNothing() {
        // TODO remove later
    }

}

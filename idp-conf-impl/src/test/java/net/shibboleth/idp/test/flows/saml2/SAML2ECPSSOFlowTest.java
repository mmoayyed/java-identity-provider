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

package net.shibboleth.idp.test.flows.saml2;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import net.shibboleth.shared.xml.SerializeSupport;

/**
 * SAML 2 ECP SSO flow test.
 */
@SuppressWarnings({"javadoc", "null"})
public class SAML2ECPSSOFlowTest extends AbstractSAML2SSOFlowTest {

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "SAML2/SOAP/ECP";

    /**
     * Test the SAML 2 Redirect SSO flow.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testSAML2ECPFlow() throws Exception {

        buildRequest();

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        validateResult(result, FLOW_ID);
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @throws Exception if an error occurs
     */
    public void buildRequest() throws Exception {

        request.setMethod("POST");
        request.setRequestURI("/idp/profile/" + FLOW_ID);
        request.setContentType("text/xml");

        final AuthnRequest authnRequest = buildAuthnRequest(request, getAcsUrl(request, "/sp/SAML2/PAOS/ACS"),
                SAMLConstants.SAML2_PAOS_BINDING_URI);
        authnRequest.setDestination(getDestinationECP(request));

        final MessageContext messageContext =
                buildOutboundMessageContext(authnRequest, SAMLConstants.SAML2_SOAP11_BINDING_URI);
        final SAMLObject message = (SAMLObject) messageContext.getMessage();
        assert message!=null;
        request.setContent(encodeMessage(message).getBytes("UTF-8"));
    }

    /**
     * Wrap the SAML message in a SOAP envelope.
     * 
     * @param message the SAML message
     * @return wrapped message
     * @throws MarshallingException if there is a problem marshalling the XMLObject
     * @throws IOException if an I/O error has occurred
     */
    @Nonnull public String encodeMessage(@Nonnull final SAMLObject message) throws MarshallingException, IOException {

        final String pre = "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body>";
        final String post = "</S:Body></S:Envelope>";

        final Element domMessage = XMLObjectSupport.marshall(message);
        final String messageXML = SerializeSupport.nodeToString(domMessage);

        return pre + messageXML.substring(messageXML.indexOf("<saml2p:AuthnRequest")) + post;
    }

}
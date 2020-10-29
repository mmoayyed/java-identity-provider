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

package net.shibboleth.idp.saml.saml2.profile.delegation.messaging.impl;

import net.shibboleth.idp.saml.saml2.profile.delegation.impl.LibertyConstants;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.soap.messaging.SOAPMessagingSupport;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.wsaddressing.Action;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for Liberty SAML 2 SOAP 1.1 message encoder.
 */
public class LibertyHTTPSOAP11EncoderTest extends XMLObjectBaseTestCase {

    /**
     * Tests encoding a SAML message to an servlet response.
     * 
     * @throws Exception if something goes wrong
     */
    @Test
    public void testResponseEncoding() throws Exception {
        SAMLObjectBuilder<StatusCode> statusCodeBuilder =
                (SAMLObjectBuilder<StatusCode>) builderFactory.<StatusCode>getBuilderOrThrow(
                        StatusCode.DEFAULT_ELEMENT_NAME);
        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(StatusCode.SUCCESS);

        SAMLObjectBuilder<Status> statusBuilder =
                (SAMLObjectBuilder<Status>) builderFactory.<Status>getBuilderOrThrow(
                        Status.DEFAULT_ELEMENT_NAME);
        Status responseStatus = statusBuilder.buildObject();
        responseStatus.setStatusCode(statusCode);

        SAMLObjectBuilder<Response> responseBuilder =
                (SAMLObjectBuilder<Response>) builderFactory.<Response>getBuilderOrThrow(
                        Response.DEFAULT_ELEMENT_NAME);
        Response samlMessage = responseBuilder.buildObject();
        samlMessage.setID("foo");
        samlMessage.setVersion(SAMLVersion.VERSION_20);
        samlMessage.setIssueInstant(Instant.ofEpochMilli(0));
        samlMessage.setStatus(responseStatus);

        SAMLObjectBuilder<AssertionConsumerService> endpointBuilder =
                (SAMLObjectBuilder<AssertionConsumerService>) builderFactory.<AssertionConsumerService>getBuilderOrThrow(
                        AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        Endpoint samlEndpoint = endpointBuilder.buildObject();
        samlEndpoint.setLocation("http://example.org");
        samlEndpoint.setResponseLocation("http://example.org/response");
        
        final MessageContext messageContext = new MessageContext();
        messageContext.setMessage(samlMessage);
        SAMLBindingSupport.setRelayState(messageContext, "relay");
        messageContext.getSubcontext(SAMLPeerEntityContext.class, true)
            .getSubcontext(SAMLEndpointContext.class, true).setEndpoint(samlEndpoint);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        LibertyHTTPSOAP11Encoder encoder = new LibertyHTTPSOAP11Encoder();
        encoder.setMessageContext(messageContext);
        encoder.setHttpServletResponse(response);
        
        encoder.initialize();
        encoder.prepareContext();
        
        Action action = buildXMLObject(Action.ELEMENT_NAME);
        action.setURI(LibertyConstants.SSOS_RESPONSE_WSA_ACTION_URI);
        SOAPMessagingSupport.addHeaderBlock(messageContext, action);
        
        encoder.encode();

        Assert.assertEquals(response.getContentType(), "text/xml;charset=UTF-8", "Unexpected content type");
        Assert.assertEquals("UTF-8", response.getCharacterEncoding(), "Unexpected character encoding");
        Assert.assertEquals(response.getHeader("Cache-control"), "no-cache, no-store", "Unexpected cache controls");
        Assert.assertEquals(response.getHeader("SOAPAction"), LibertyConstants.SSOS_RESPONSE_WSA_ACTION_URI);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(response.getContentAsByteArray())) {
            XMLObject xmlObject = XMLObjectSupport.unmarshallFromInputStream(parserPool, inputStream);
            Assert.assertNotNull(xmlObject);
            Assert.assertTrue(xmlObject instanceof Envelope);
            Envelope envelope = (Envelope) xmlObject;
            Assert.assertNotNull(envelope.getHeader());
            Assert.assertEquals(envelope.getHeader().getUnknownXMLObjects().size(), 1);
            Action outboundAction = (Action) envelope.getHeader().getUnknownXMLObjects().get(0);
            Assert.assertNotNull(outboundAction);
            Assert.assertNotNull(envelope.getBody());
            Assert.assertEquals(envelope.getBody().getUnknownXMLObjects().size(), 1);
            Response outboundResponse = (Response) envelope.getBody().getUnknownXMLObjects().get(0);
            outboundResponse.releaseDOM();
            outboundResponse.releaseChildrenDOM(true);
            outboundResponse.setParent(null);
            assertXMLEquals(XMLObjectSupport.marshall(outboundResponse).getOwnerDocument(), samlMessage);
        }
    }
}

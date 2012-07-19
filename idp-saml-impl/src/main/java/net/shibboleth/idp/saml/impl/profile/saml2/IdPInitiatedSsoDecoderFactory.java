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

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.profile.HttpServletRequestMessageDecoderFactory;
import net.shibboleth.idp.saml.impl.profile.BaseIdpInitiatedSsoRequestMessageDecoder;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;

//TODO need to deal with relay state

/** Factory that produces {@link AuthnRequest} message decoders. */
public class IdPInitiatedSsoDecoderFactory implements HttpServletRequestMessageDecoderFactory<AuthnRequest> {

    /** {@inheritDoc} */
    public MessageDecoder<AuthnRequest> newDecoder(HttpServletRequest httpRequest) throws MessageDecodingException {
        IdpInitiatedSsoRequestMessageDecoder decoder = new IdpInitiatedSsoRequestMessageDecoder();
        decoder.setHttpServletRequest(httpRequest);
        return decoder;
    }

    /** Decodes an incoming Shibboleth Authentication Request message. */
    private static class IdpInitiatedSsoRequestMessageDecoder extends
            BaseIdpInitiatedSsoRequestMessageDecoder<AuthnRequest> {

        /** Builder of SAML 2 {@link AuthnRequest} objects. */
        private final SAMLObjectBuilder<AuthnRequest> requestBuilder =
                (SAMLObjectBuilder<AuthnRequest>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        AuthnRequest.DEFAULT_ELEMENT_NAME);

        /** Builder of SAML 2 {@link Issuer} objects. */
        private final SAMLObjectBuilder<Issuer> issuerBuilder =
                (SAMLObjectBuilder<Issuer>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Issuer.DEFAULT_ELEMENT_NAME);

        /** {@inheritDoc} */
        protected void doDecode() throws MessageDecodingException {
            final Issuer requestIssuer = issuerBuilder.buildObject();
            requestIssuer.setValue(getEntityId(getHttpServletRequest()));

            final AuthnRequest authnRequest = requestBuilder.buildObject();
            authnRequest.setAssertionConsumerServiceURL(getAcsUrl(getHttpServletRequest()));
            authnRequest.setID(UUID.randomUUID().toString());
            authnRequest
                    .setIssueInstant(new DateTime(getTime(getHttpServletRequest()), ISOChronology.getInstanceUTC()));
            authnRequest.setIssuer(requestIssuer);
            authnRequest.setVersion(SAMLVersion.VERSION_20);

            final BasicMessageMetadataContext msgMetadata = new BasicMessageMetadataContext();
            msgMetadata.setMessageId(authnRequest.getID());
            msgMetadata.setMessageIssueInstant(getTime(getHttpServletRequest()));
            msgMetadata.setMessageIssuer(getEntityId(getHttpServletRequest()));

            final MessageContext<AuthnRequest> messageContext = new MessageContext<AuthnRequest>();
            messageContext.setMessage(authnRequest);
        }
    }
}
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

import net.shibboleth.idp.saml.impl.profile.BaseIdpInitiatedSsoRequestMessageDecoder;
import net.shibboleth.idp.saml.impl.profile.IdpInitatedSsoRequest;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.saml.common.messaging.context.SamlBindingContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/** Decodes an incoming Shibboleth Authentication Request message. */
public class IdpInitiatedSsoRequestMessageDecoder extends BaseIdpInitiatedSsoRequestMessageDecoder<SAMLObject> 
    implements SAMLMessageDecoder {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(IdpInitiatedSsoRequestMessageDecoder.class);

    /** Builder of SAML 2 {@link AuthnRequest} objects. */
    private final SAMLObjectBuilder<AuthnRequest> requestBuilder =
            (SAMLObjectBuilder<AuthnRequest>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                    AuthnRequest.DEFAULT_ELEMENT_NAME);

    /** Builder of SAML 2 {@link Issuer} objects. */
    private final SAMLObjectBuilder<Issuer> issuerBuilder =
            (SAMLObjectBuilder<Issuer>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                    Issuer.DEFAULT_ELEMENT_NAME);
    
    /** Builder of SAML 2 {@link NameIDPolicy} objects. */
    private final SAMLObjectBuilder<NameIDPolicy> nipBuilder = 
            (SAMLObjectBuilder<NameIDPolicy>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                    NameIDPolicy.DEFAULT_ELEMENT_NAME);
    
    /** The IdP-initiated request structure parsed from the inbound request. */
    private IdpInitatedSsoRequest ssoRequest;

    /** {@inheritDoc} */
    public String getBindingURI() {
        return "urn:mace:shibboleth:2.0:profiles:AuthnRequest";
    }
    
    /**
     * Get the internally constructed instance of {@link IdpInitatedSsoRequest}.
     * 
     * @return the internal SSO request instance
     */
    protected IdpInitatedSsoRequest getIdPInitiatedSsoRequest() {
       return ssoRequest; 
    }
    
    /** {@inheritDoc} */
    protected void doDecode() throws MessageDecodingException {
        ssoRequest = buildIdpInitiatedSsoRequest();
        
        final MessageContext<SAMLObject> messageContext = new MessageContext<SAMLObject>();
        messageContext.setMessage(buildAuthnRequest());
        
        populateBindingContext(messageContext);
        
        setMessageContext(messageContext);
    }
    
    /**
     * Build a synthetic AuthnRequest instance from the IdP-initiated SSO request structure.
     * 
     * @return the synthetic AuthnRequest message instance
     * 
     * @throws MessageDecodingException if the inbound request does not contain an entityID value
     */
    protected  AuthnRequest buildAuthnRequest() throws MessageDecodingException {
        final AuthnRequest authnRequest = requestBuilder.buildObject();
        
        final Issuer requestIssuer = issuerBuilder.buildObject();
        requestIssuer.setValue(ssoRequest.getEntityId());
        authnRequest.setIssuer(requestIssuer);
        
        final NameIDPolicy nip = nipBuilder.buildObject();
        nip.setAllowCreate(true);
        authnRequest.setNameIDPolicy(nip);
        
        // TODO see UnsolicitedSSODecoder in v2. Do we need to support adding a statically configured ProtocolBinding,
        // and dynamically resolved ACS URL if not in the request? The latter we might not do in the decoder.
        
        authnRequest.setAssertionConsumerServiceURL(ssoRequest.getAcsUrl());
        authnRequest.setIssueInstant(new DateTime(ssoRequest.getTime(), ISOChronology.getInstanceUTC()));
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setID(getMessageID());
        
        return authnRequest;
    }
    
    /**
     * Populate the context which carries information specific to this binding.
     * 
     * @param messageContext the current message context
     */
    protected void populateBindingContext(MessageContext<SAMLObject> messageContext) {
        String relayState = ssoRequest.getRelayState();
        log.debug("Decoded SAML relay state of: {}", relayState);
        
        SamlBindingContext bindingContext = messageContext.getSubcontext(SamlBindingContext.class, true);
        bindingContext.setRelayState(relayState);
        
        bindingContext.setBindingUri(getBindingURI());
        bindingContext.setHasBindingSignature(false);
        bindingContext.setIntendedDestinationEndpointUriRequired(false);
    }

    /** {@inheritDoc} */
    protected String getMessageToLog() {
        SAMLObject message = getMessageContext().getMessage();
        if (message == null) {
            log.warn("Decoded message was null, nothing to log");
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("SAML 2 IdP-initiated request was: " + ssoRequest.toString());
        builder.append("\nSynthetically constructed SAML 2 AuthnRequest was: \n");
        
        try {
            Element dom = XMLObjectSupport.marshall(message);
            builder.append(SerializeSupport.prettyPrintXML(dom));
            return builder.toString();
        } catch (MarshallingException e) {
            log.error("Unable to marshall message for logging purposes", e);
            return null;
        }
    }
}
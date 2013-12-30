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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.impl.profile.BaseIdPInitiatedSSORequestMessageDecoder;
import net.shibboleth.idp.saml.impl.profile.IdPInitatedSSORequest;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Decodes an incoming Shibboleth Authentication Request message. */
public class IdPInitiatedSSORequestMessageDecoder extends 
        BaseIdPInitiatedSSORequestMessageDecoder<IdPInitatedSSORequest> {
    
    /** Protocol binding implemented by this decoder. */
    @Nonnull @NotEmpty private static final String BINDING_URI = "urn:mace:shibboleth:1.0:profiles:AuthnRequest";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IdPInitiatedSSORequestMessageDecoder.class);
    
    /**
     * Get the SAML binding URI supported by this decoder.
     * 
     * @return SAML binding URI supported by this decoder
     */
    @Nonnull @NotEmpty public String getBindingURI() {
        return BINDING_URI;
    }

    /** {@inheritDoc} */
    @Override
    protected void doDecode() throws MessageDecodingException {
        IdPInitatedSSORequest ssoRequest = buildIdPInitiatedSSORequest();

        MessageContext<IdPInitatedSSORequest> messageContext = new MessageContext<>();
        messageContext.setMessage(ssoRequest);
        
        messageContext.getSubcontext(SAMLPeerEntityContext.class, true).setEntityId(ssoRequest.getEntityId());
        
        SAMLMessageInfoContext msgInfoContext = messageContext.getSubcontext(SAMLMessageInfoContext.class, true);
        msgInfoContext.setMessageIssueInstant(new DateTime(ssoRequest.getTime(), ISOChronology.getInstanceUTC()));
        msgInfoContext.setMessageId(getMessageID());
        
        populateBindingContext(messageContext);

        setMessageContext(messageContext);
    }
    
    /**
     * Populate the context which carries information specific to this binding.
     * 
     * @param messageContext the current message context
     */
    protected void populateBindingContext(@Nonnull final MessageContext<IdPInitatedSSORequest> messageContext) {
        final String relayState = messageContext.getMessage().getRelayState();
        log.debug("Decoded SAML relay state: {}", relayState);
        
        SAMLBindingContext bindingContext = messageContext.getSubcontext(SAMLBindingContext.class, true);
        bindingContext.setRelayState(relayState);
        bindingContext.setBindingUri(getBindingURI());
        bindingContext.setHasBindingSignature(false);
        bindingContext.setIntendedDestinationEndpointURIRequired(false);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getMessageToLog() {
        return "SAML 1 IdP-initiated request was: " + getMessageContext().getMessage().toString();
    }
    
}
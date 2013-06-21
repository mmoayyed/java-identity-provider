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

import net.shibboleth.idp.saml.impl.profile.BaseIdpInitiatedSsoRequestMessageDecoder;
import net.shibboleth.idp.saml.impl.profile.IdpInitatedSsoRequest;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.messaging.context.SamlBindingContext;
import org.opensaml.saml.common.messaging.context.SamlMessageInfoContext;
import org.opensaml.saml.common.messaging.context.SamlPeerEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Decodes an incoming Shibboleth Authentication Request message. */
public class IdpInitiatedSsoRequestMessageDecoder extends 
        BaseIdpInitiatedSsoRequestMessageDecoder<IdpInitatedSsoRequest> {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(IdpInitiatedSsoRequestMessageDecoder.class);
    
    /**
     * Gets the SAML binding URI supported by this decoder.
     * 
     * @return SAML binding URI supported by this decoder
     */
    public String getBindingURI() {
        return "urn:mace:shibboleth:1.0:profiles:AuthnRequest";
    }

    /** {@inheritDoc} */
    protected void doDecode() throws MessageDecodingException {
        IdpInitatedSsoRequest ssoRequest = buildIdpInitiatedSsoRequest();

        MessageContext<IdpInitatedSsoRequest> messageContext = new MessageContext<IdpInitatedSsoRequest>();
        messageContext.setMessage(ssoRequest);
        
        messageContext.getSubcontext(SamlPeerEntityContext.class, true).setEntityId(ssoRequest.getEntityId());
        
        SamlMessageInfoContext msgInfoContext = messageContext.getSubcontext(SamlMessageInfoContext.class, true);
        if (ssoRequest.getTime() > 0) {
            msgInfoContext.setMessageIssueInstant(new DateTime(ssoRequest.getTime(), ISOChronology.getInstanceUTC()));
        } else {
            msgInfoContext.setMessageIssueInstant(new DateTime(ISOChronology.getInstanceUTC()));
        }
        msgInfoContext.setMessageId(getMessageID());
        
        populateBindingContext(messageContext);

        setMessageContext(messageContext);
    }
    
    /**
     * Populate the context which carries information specific to this binding.
     * 
     * @param messageContext the current message context
     */
    protected void populateBindingContext(MessageContext<IdpInitatedSsoRequest> messageContext) {
        String relayState = messageContext.getMessage().getRelayState();
        log.debug("Decoded SAML relay state of: {}", relayState);
        
        SamlBindingContext bindingContext = messageContext.getSubcontext(SamlBindingContext.class, true);
        bindingContext.setRelayState(relayState);

        bindingContext.setBindingUri(getBindingURI());
        bindingContext.setHasBindingSignature(false);
        bindingContext.setIntendedDestinationEndpointUriRequired(false);
    }

    /** {@inheritDoc} */
    protected String getMessageToLog() {
        return "SAML 1 IdP-initiated request was: " + getMessageContext().getMessage().toString();
    }
}
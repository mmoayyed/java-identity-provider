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

package net.shibboleth.idp.profile.impl;

import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.HttpServletRequestMessageDecoderFactory;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UninitializedComponentException;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * An IdP action that decodes the incoming message and populates
 * {@link ProfileRequestContext#getInboundMessageContext()}.
 * 
 * @param <InboundMessageType> the inbound message type
 * @param <OutboundMessageType> this is meaningless for this action but needed to fill out the class decleration
 */
public final class DecodeMessage<InboundMessageType, OutboundMessageType> extends
        AbstractIdentityProviderAction<InboundMessageType, OutboundMessageType> implements UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(DecodeMessage.class);

    /** Factory used to create new {@link MessageDecoder} instances. */
    private HttpServletRequestMessageDecoderFactory<InboundMessageType> decoderFactory;

    /**
     * Gets the factory used to create new {@link MessageDecoder} instances.
     * 
     * @return factory used to create new {@link MessageDecoder} instances, never null after initialization
     */
    public HttpServletRequestMessageDecoderFactory<InboundMessageType> getDecoderFactory() {
        return decoderFactory;
    }

    /**
     * Sets the factory used to create new {@link MessageDecoder} instances.
     * 
     * @param factory factory used to create new {@link MessageDecoder} instances
     */
    public synchronized void setDecoderFactory(HttpServletRequestMessageDecoderFactory<InboundMessageType> factory) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(DecodeMessage.class.getName() + " " + getId()
                    + ": already initialized, decoder factory can no longer be changed.");
        }
        decoderFactory = factory;
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext,
            ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext) {
        if (!isInitialized()) {
            throw new UninitializedComponentException("DecodeMessage " + getId()
                    + ": has not been initialized and can not yet be used.");
        }

        try {
            HttpServletRequest httpRequest = profileRequestContext.getHttpRequest();
            if (httpRequest == null) {
                log.error("DecodeMessage {}: ProfileRequestContext did not contain the required HttpServletRequest",
                        getId());
                throw new MessageDecodingException("ProfileRequestContext did not contain an HttpServletRequest");
            }

            log.debug("DecodeMessage {}: creating new message decoder", getId());
            MessageDecoder<InboundMessageType> decoder =
                    decoderFactory.newDecoder(profileRequestContext.getHttpRequest());

            decoder.initialize();

            log.debug("DecodeMessage {}: decoding message", getId());
            decoder.decode();
            profileRequestContext.setInboundMessageContext(decoder.getMessageContext());
            log.debug("DecodeMessage {}: successfully decoded message", getId());

            decoder.destroy();

            return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
        } catch (MessageDecodingException e) {
            log.error("DecodeMessage {}: was unable to decode the incoming request", getId(), e);
            return ActionSupport.buildErrorEvent(this, e, "Unable to decode incoming message");
        } catch (ComponentInitializationException e) {
            log.error("DecodeMessage {}: error initializing the message decoder", getId(), e);
            return ActionSupport.buildErrorEvent(this, e, "Unable to decode incoming message");
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (decoderFactory == null) {
            throw new ComponentInitializationException("Message decoder factory can not be null");
        }
    }
}
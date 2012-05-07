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

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.HttpServletRequestMessageDecoderFactory;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
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
        AbstractProfileAction<InboundMessageType, OutboundMessageType> implements UnmodifiableComponent {

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
    public synchronized void setDecoderFactory(
            @Nonnull final HttpServletRequestMessageDecoderFactory<InboundMessageType> factory) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        decoderFactory = Constraint.isNotNull(factory, "Message decoder factory can not be null");
    }

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext)
            throws ProfileException {

        try {
            log.debug("DecodeMessage {}: creating new message decoder", getId());
            final MessageDecoder<InboundMessageType> decoder =
                    decoderFactory.newDecoder(profileRequestContext.getHttpRequest());

            decoder.initialize();

            log.debug("DecodeMessage {}: decoding message", getId());
            decoder.decode();
            profileRequestContext.setInboundMessageContext(decoder.getMessageContext());
            log.debug("DecodeMessage {}: successfully decoded message", getId());

            decoder.destroy();

            return ActionSupport.buildProceedEvent(this);
        } catch (MessageDecodingException e) {
            log.error("DecodeMessage {}: was unable to decode the incoming request", getId(), e);
            throw new UnableToDecodeMessageException("Unable to decode incoming message");
        } catch (ComponentInitializationException e) {
            log.error("DecodeMessage {}: error initializing the message decoder", getId(), e);
            throw new UnableToDecodeMessageException("Unable to decode incoming message");
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (decoderFactory == null) {
            throw new ComponentInitializationException("Message decoder factory can not be null");
        }
    }

    /** Exception thrown if there is a problem decoding the incoming message. */
    public static class UnableToDecodeMessageException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -7866899349347664342L;

        /** Constructor. */
        public UnableToDecodeMessageException() {
            super();
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public UnableToDecodeMessageException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param wrappedException exception to be wrapped by this one
         */
        public UnableToDecodeMessageException(Exception wrappedException) {
            super(wrappedException);
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         * @param wrappedException exception to be wrapped by this one
         */
        public UnableToDecodeMessageException(String message, Exception wrappedException) {
            super(message, wrappedException);
        }
    }
}
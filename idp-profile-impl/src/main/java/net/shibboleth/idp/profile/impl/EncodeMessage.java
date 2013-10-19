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
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncoder;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/** A profile stage that encodes an outbound response from the outbound {@link MessageContext}. 
 * 
 * <p>
 * If the supplied instance of {@link MessageEncoder} is not already initialized, this action will
 * handle supplying the message context to encode via {@link MessageEncoder#setMessageContext(MessageContext)}, 
 * followed by invoking {@link MessageEncoder#initialize()}. If the encoder is already initialized,
 * these operations will be skipped.
 * </p>
 * 
 * */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EncodeMessage.UNABLE_TO_ENCODE, description = "An error occured trying to encode the message")})
public class EncodeMessage extends AbstractProfileAction {

    /** ID of the event returned if incoming message could not be decoded. */
    public static final String UNABLE_TO_ENCODE = "UnableToEncode";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(EncodeMessage.class);

    /** The {@link MessageDecoder} instance used to decode the incoming message. */
    private final MessageEncoder encoder;
    
    /** An optional {@link MessageHandler} instance to be invoked after 
     * {@link MessageEncoder#prepareContext()} and prior to {@link MessageEncoder#encode()}.
     * May be null. */
    private final MessageHandler messageHandler;

    /**
     * Constructor.
     * 
     * 
     * @param messageEncoder the {@link MessageEncoder} used for the outbound response
     */
    public EncodeMessage(@Nonnull final MessageEncoder messageEncoder) {
        encoder = Constraint.isNotNull(messageEncoder, "Message encoder can not be null");
        messageHandler = null;
    }
    
    /**
     * Constructor.
     * 
     * <p>
     * The supplied {@link MessageHandler} will be invoked on the {@link MessageContext} after 
     * {@link MessageEncoder#prepareContext()}, and prior to invoking {@link MessageEncoder#encode()}.
     * Its use is optional and primarily used for transport/binding-specific message handling, 
     * as opposed to more generalized message handling operations which would typically be invoked 
     * earlier than this action.  For more details see {@link MessageEncoder}.
     * </p>
     * 
     * @param messageEncoder the {@link MessageEncoder} used for the outbound response
     * @param handler the {@link MessageHandler} used for the outbound response
     */
    public EncodeMessage(@Nonnull final MessageEncoder messageEncoder, @Nullable final MessageHandler handler) {
        encoder = Constraint.isNotNull(messageEncoder, "Message encoder can not be null");
        messageHandler = handler;
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event
            doExecute(@Nonnull final RequestContext springRequestContext,
                    @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        try {
            log.debug("Action {}: Using message encoder of type {} for this response", getId(), 
                    encoder.getClass().getName());

            log.debug("Action {}: Encoding outbound response", getId());         
            final MessageContext msgContext = profileRequestContext.getOutboundMessageContext();
            
            if (!encoder.isInitialized()) {
                log.debug("Encoder was not initialized, injecting MessageContext and initializing");
                encoder.setMessageContext(msgContext);
                encoder.initialize();
            } else {
                log.debug("Encoder was already initialized, skipping MessageContext injection and init");
            }
            
            
            encoder.prepareContext();
            
            if (messageHandler != null) {
                log.debug("Action {}: Invoking message handler of type {} for this response", getId(), 
                        messageHandler.getClass().getName());
                messageHandler.invoke(msgContext);
            }
            
            encoder.encode();
            
            log.debug("Action {}: Outbound response encoded from a message of type {}", getId(),
                    msgContext.getMessage().getClass().getName());
            
            // Could also do this as an 'end' state expression in WebFlow.
            springRequestContext.getExternalContext().recordResponseComplete();

            return ActionSupport.buildProceedEvent(this);
        } catch (MessageEncodingException | ComponentInitializationException | MessageHandlerException e) {
            log.debug("Action {}: Unable to encode outbound response", getId(), e);
            return ActionSupport.buildEvent(this, UNABLE_TO_ENCODE);
        } finally {
            encoder.destroy();
        }
        
    }
}
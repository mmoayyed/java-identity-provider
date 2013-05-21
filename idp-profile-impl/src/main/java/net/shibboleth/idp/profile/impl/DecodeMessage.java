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

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/** A profile stage that decodes an incoming request into a given {@link MessageContext}. */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = DecodeMessage.UNABLE_TO_DECODE, description = "An error occured trying to decode the message")})
public class DecodeMessage extends AbstractProfileAction {

    /** ID of the event returned if incoming message could not be decoded. */
    public static final String UNABLE_TO_DECODE = "UnableToDecode";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(DecodeMessage.class);

    /** The {@link MessageDecoder} instance used to decode the incoming message. */
    private final MessageDecoder decoder;

    /**
     * Constructor.
     * 
     * @param messageDecoder the {@link MessageDecoder} used for the incoming request
     */
    public DecodeMessage(@Nonnull final MessageDecoder messageDecoder) {
        decoder = Constraint.isNotNull(messageDecoder, "Message decoder can not be null");
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event
            doExecute(@Nonnull final RequestContext springRequestContext,
                    @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        try {
            log.debug("Action {}: Using message decoder of type {} for this request", getId(), decoder.getClass()
                    .getName());

            log.debug("Action {}: Decoding incoming request", getId());         
            decoder.decode();
            final MessageContext msgContext = decoder.getMessageContext();
            decoder.destroy();
            log.debug("Action {}: Incoming request decoded into a message of type {}", getId(), 
                    msgContext.getMessage().getClass().getName());

            profileRequestContext.setInboundMessageContext(msgContext);
            return ActionSupport.buildProceedEvent(this);
        } catch (MessageDecodingException e) {
            log.debug("Action {}: Unable to decode incoming request", getId(), e);
            return ActionSupport.buildEvent(this, UNABLE_TO_DECODE);
        }
    }
}
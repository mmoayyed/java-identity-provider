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
import net.shibboleth.idp.profile.InvalidProfileRequestContextStateException;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.util.storage.ReplayCache;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** Checks that the given message has not be replayed. */
public final class CheckMessageReplay extends AbstractProfileAction {

    /** Cache used to store message issuer/id pairs and check to see if a message is being replayed. */
    private ReplayCache replayCache;

    /**
     * Strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message context.
     */
    private Function<MessageContext, BasicMessageMetadataContext> messageMetadataContextLookupStrategy;

    /**
     * Constructor.
     * 
     * Initializes {@link #messageMetadataContextLookupStrategy} to {@link ChildContextLookup}.
     */
    public CheckMessageReplay() {
        super();

        messageMetadataContextLookupStrategy =
                new ChildContextLookup<MessageContext, BasicMessageMetadataContext>(BasicMessageMetadataContext.class,
                        false);
    }

    /**
     * Gets the strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     * context.
     * 
     * @return strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     *         context
     */
    public Function<MessageContext, BasicMessageMetadataContext> getMessageMetadataContextLookupStrategy() {
        return messageMetadataContextLookupStrategy;
    }

    /**
     * Sets the strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     * context.
     * 
     * @param strategy strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound
     *            message context
     */
    public synchronized void setMessageMetadataContextLookupStrategy(
            @Nonnull final Function<MessageContext, BasicMessageMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        messageMetadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "Message metadata context lookup strategy can not be null");
    }

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext profileRequestContext) throws ProfileException {

        final BasicMessageMetadataContext messageSubcontext =
                messageMetadataContextLookupStrategy.apply(profileRequestContext.getInboundMessageContext());

        if (messageSubcontext.getMessageIssuer() == null) {
            throw new InvalidProfileRequestContextStateException(
                    "Basic message metadata subcontext does not contain a message issuer");
        }

        if (messageSubcontext.getMessageId() == null) {
            throw new InvalidProfileRequestContextStateException(
                    "Basic message metadata subcontext does not contain a message ID");
        }

        if (replayCache.isReplay(messageSubcontext.getMessageIssuer(), messageSubcontext.getMessageId())) {
            throw new ReplayedMessageException(messageSubcontext.getMessageId(), messageSubcontext.getMessageIssuer());
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /** Profile processing error that occurred because the given request was detected as a replay. */
    public class ReplayedMessageException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -7358811994922990143L;

        /**
         * Constructor.
         * 
         * @param messageId ID of the message, never null
         * @param messageIssuer issuer of the message, never null
         */
        public ReplayedMessageException(String messageId, String messageIssuer) {
            super("Action " + getId() + ": Message ID " + messageId + " from issuer " + messageIssuer
                    + " is a replayed message");
        }
    }
}
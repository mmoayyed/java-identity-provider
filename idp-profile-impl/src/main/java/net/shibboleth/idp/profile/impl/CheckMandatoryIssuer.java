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
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.springframework.webflow.execution.Event;

import com.google.common.base.Function;

/** Checks that the incoming message has an issuer. */
public final class CheckMandatoryIssuer extends AbstractProfileAction {

    /**
     * Strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message context.
     */
    private Function<MessageContext, BasicMessageMetadataContext> messageMetadataContextLookupStrategy;

    /**
     * Constructor.
     * 
     * Initializes {@link #messageMetadataContextLookupStrategy} to {@link ChildContextLookup}.
     */
    public CheckMandatoryIssuer() {
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
            ProfileRequestContext profileRequestContext) throws ProfileException {

        final BasicMessageMetadataContext messageSubcontext =
                messageMetadataContextLookupStrategy.apply(profileRequestContext.getInboundMessageContext());

        if (messageSubcontext.getMessageIssuer() == null) {
            throw new NoMessageIssuerException();
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /** A profile processing exception that occurs when the inbound message has no identified message issuer. */
    public class NoMessageIssuerException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -1485366995695842339L;

        /** Constructor. */
        public NoMessageIssuerException() {
            super("Action " + getId() + ": Inbound message basic message metadata does not contain a message issuer");
        }
    }
}
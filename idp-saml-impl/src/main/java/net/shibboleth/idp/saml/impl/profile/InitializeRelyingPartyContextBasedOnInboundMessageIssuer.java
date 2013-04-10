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

package net.shibboleth.idp.saml.impl.profile;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * Adds a {@link RelyingPartyContext} to the current {@link ProfileRequestContext}. The relying party ID is assumed to
 * be the inbound message issuer as determined by the {@link BasicMessageMetadataContext#getMessageIssuer()} located on
 * the {@link ProfileRequestContext#getInboundMessageContext()}.
 */
public class InitializeRelyingPartyContextBasedOnInboundMessageIssuer extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(InitializeRelyingPartyContextBasedOnInboundMessageIssuer.class);

    /**
     * Strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message context.
     */
    private Function<MessageContext, BasicMessageMetadataContext> messageMetadataContextLookupStrategy;

    /** Constructor. */
    public InitializeRelyingPartyContextBasedOnInboundMessageIssuer() {
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
    @Nonnull public Function<MessageContext, BasicMessageMetadataContext> getMessageMetadataContextLookupStrategy() {
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
    protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final BasicMessageMetadataContext messageSubcontext =
                messageMetadataContextLookupStrategy.apply(profileRequestContext.getInboundMessageContext());

        log.debug("Action {}: Attaching RelyingPartySubcontext with relying party ID {} to ProfileRequestContext",
                getId(), messageSubcontext.getMessageIssuer());

        profileRequestContext.addSubcontext(new RelyingPartyContext(messageSubcontext.getMessageIssuer()));

        return ActionSupport.buildProceedEvent(this);
    }
}
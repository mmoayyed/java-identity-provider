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

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Action that adds a {@link RelyingPartyContext} to the current {@link ProfileRequestContext}.
 * The relying party ID is set to the inbound message issuer as determined by
 * {@link BasicMessageMetadataContext#getMessageIssuer()} via {@link ProfileRequestContext#getInboundMessageContext()}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post ProfileRequestContext.getSubcontext(RelyingPartyContext.class, false) != null
 * @post ProfileRequestContext.getSubcontext(RelyingPartyContext.class, false).getRelyingPartyId() is set per above.
 */
public class InitializeRelyingPartyContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeRelyingPartyContext.class);

    /**
     * Strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message context.
     */
    @Nonnull private Function<MessageContext, BasicMessageMetadataContext> messageMetadataContextLookupStrategy;

    /** Constructor. */
    public InitializeRelyingPartyContext() {
        messageMetadataContextLookupStrategy = new ChildContextLookup<>(BasicMessageMetadataContext.class, false);
    }

    /**
     * Set the strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound message
     * context.
     * 
     * @param strategy strategy used to look up the {@link BasicMessageMetadataContext} associated with the inbound
     *            message context
     */
    public void setMessageMetadataContextLookupStrategy(
            @Nonnull final Function<MessageContext, BasicMessageMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        messageMetadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "Message metadata context lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final RelyingPartyContext rpContext = profileRequestContext.getSubcontext(RelyingPartyContext.class, true);
        final BasicMessageMetadataContext messageSubcontext =
                messageMetadataContextLookupStrategy.apply(profileRequestContext.getInboundMessageContext());
        if (messageSubcontext != null) {
            log.debug("{} Attaching RelyingPartyContext with relying party ID {} to ProfileRequestContext",
                    getLogPrefix(), messageSubcontext.getMessageIssuer());
    
            rpContext.setRelyingPartyId(messageSubcontext.getMessageIssuer());
        }
    }
    
}
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

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Action that adds a {@link RelyingPartyContext} to the current {@link ProfileRequestContext} tree
 * via a creation function. The relying party ID is set via a lookup strategy, defaulting to
 * {@link BasicMessageMetadataContext#getMessageIssuer()} via {@link ProfileRequestContext#getInboundMessageContext()}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @post ProfileRequestContext.getSubcontext(RelyingPartyContext.class) != null
 * @post ProfileRequestContext.getSubcontext(RelyingPartyContext.class).getRelyingPartyId() is set per above.
 */
public class InitializeRelyingPartyContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeRelyingPartyContext.class);

    /** Strategy that will return or create a {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextCreationStrategy;
    
    /** Strategy used to look up the {@link BasicMessageMetadataContext} to draw from. */
    @Nonnull private Function<ProfileRequestContext,BasicMessageMetadataContext> messageMetadataContextLookupStrategy;

    /** Constructor. */
    public InitializeRelyingPartyContext() {
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class, true);
        messageMetadataContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(BasicMessageMetadataContext.class), new InboundMessageContextLookup());
    }

    /**
     * Set the strategy used to return or create the {@link RelyingPartyContext}.
     * 
     * @param strategy creation strategy
     */
    public void setRelyingPartyContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextCreationStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext creation strategy cannot be null");
    }
    
    /**
     * Set the strategy used to look up the {@link BasicMessageMetadataContext} to draw from.
     * 
     * @param strategy strategy used to look up the {@link BasicMessageMetadataContext}
     */
    public void setMessageMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, BasicMessageMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        messageMetadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "Message metadata context lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return;
        }
        
        final BasicMessageMetadataContext messageSubcontext =
                messageMetadataContextLookupStrategy.apply(profileRequestContext);
        if (messageSubcontext != null) {
            log.debug("{} Attaching RelyingPartyContext with relying party ID {} to ProfileRequestContext",
                    getLogPrefix(), messageSubcontext.getMessageIssuer());
            rpContext.setRelyingPartyId(messageSubcontext.getMessageIssuer());
        } else {
            log.debug("{} Attaching RelyingPartyContext with no relying party ID to ProfileRequestContext",
                    getLogPrefix());
        }
    }
    
}
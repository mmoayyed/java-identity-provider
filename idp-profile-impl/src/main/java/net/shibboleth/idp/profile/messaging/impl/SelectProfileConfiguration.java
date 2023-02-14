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

package net.shibboleth.idp.profile.messaging.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.RecursiveTypedParentContextLookup;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * Action that selects the {@link ProfileConfiguration} for the given message context and sets it in the looked-up
 * {@link RelyingPartyContext}.
 * 
 * 
 * @post InOutOperationContext.getSubcontext(RelyingPartyContext.class).getProfileConfiguration() != null
 */
public class SelectProfileConfiguration extends AbstractMessageHandler {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectProfileConfiguration.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link MessageContext}.
     */
    @NonnullAfterInit private Function<MessageContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /**
     * Strategy used to locate the {@link ProfileRequestContext} associated with a given {@link MessageContext}.
     */
    @Nonnull private Function<MessageContext,ProfileRequestContext> profileRequestContextLookupStrategy;
    
    /**
     * Strategy used to locate the effective profile ID associated with a given {@link MessageContext}.
     */
    @Nonnull private Function<MessageContext,String> profileIdLookupStrategy;

    /** The RelyingPartyContext to operate on. */
    @Nullable private RelyingPartyContext rpCtx;
    
    /** Constructor. */
    public SelectProfileConfiguration() {
        
        profileRequestContextLookupStrategy =
                new RecursiveTypedParentContextLookup<>(ProfileRequestContext.class);

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<>(RelyingPartyContext.class).compose(
                        new RecursiveTypedParentContextLookup<>(InOutOperationContext.class));
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link MessageContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *         {@link MessageContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<MessageContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Get the strategy used to locate the {@link ProfileRequestContext} associated with a given
     * {@link MessageContext}.
     * 
     * @return lookup strategy
     */
    @Nonnull public Function<MessageContext,ProfileRequestContext> getProfileRequestContextLookupStrategy() {
        return profileRequestContextLookupStrategy;
    }

    /**
     * Set the strategy used to locate the {@link ProfileRequestContext} associated with a given
     * {@link MessageContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setProfileRequestContextLookupStrategy(
            @Nonnull final Function<MessageContext,ProfileRequestContext> strategy) {
        profileRequestContextLookupStrategy =
                Constraint.isNotNull(strategy, "ProfileRequestContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the effective profile ID associated with a given
     * {@link MessageContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *         {@link MessageContext}
     */
    public void setProfiledIdLookupStrategy(@Nonnull final Function<MessageContext,String> strategy) {
        checkSetterPreconditions();
        profileIdLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (profileIdLookupStrategy == null) {
            throw new ComponentInitializationException("Profile ID lookup strategy was null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {
        
        if (!super.doPreInvoke(messageContext)) {
            return false;
        }
        
        rpCtx = relyingPartyContextLookupStrategy.apply(messageContext);
        if (rpCtx == null) {
            log.debug("{} No relying party context associated with this profile request", getLogPrefix());
            throw new MessageHandlerException("No relying party context associated with this message context");
        }

        if (rpCtx.getConfiguration() == null) {
            log.debug("{} No relying party configuration associated with this profile request", getLogPrefix());
            throw new MessageHandlerException("No relying party configuration associated with this message context");
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {

        final RelyingPartyConfiguration rpConfig = rpCtx.getConfiguration();

        final String profileId = profileIdLookupStrategy.apply(messageContext);
        if (profileId == null) {
            log.warn("{} Profile ID is not available from message context for RP configuration (RPID {})",
                    new Object[] {getLogPrefix(), rpConfig.getId(), rpCtx.getRelyingPartyId(),});
            throw new MessageHandlerException("Profile ID is not available from message context");
        }
        
        final ProfileConfiguration profileConfiguration =
                rpConfig.getProfileConfiguration(profileRequestContextLookupStrategy.apply(messageContext), profileId);
        if (profileConfiguration == null) {
            log.warn("{} Profile {} is not available for RP configuration {} (RPID {})",
                    new Object[] {getLogPrefix(), profileId, rpConfig.getId(), rpCtx.getRelyingPartyId(),});
            throw new MessageHandlerException("Profile is not available for RP configuration");
        }
        rpCtx.setProfileConfig(profileConfiguration);
    }
    
}
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

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.RecursiveTypedParentContextLookup;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.slf4j.Logger;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.profile.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.profile.relyingparty.VerifiedProfileCriterion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;

/**
 * This message handler attempts to resolve a {@link RelyingPartyConfiguration} and adds it to the 
 * {@link RelyingPartyContext} that was looked up.
 * 
 * @post If a {@link RelyingPartyContext} is located, it will be populated with a non-null result of applying
 * the supplied 
 * {@link RelyingPartyConfigurationResolver} to the {@link RelyingPartyContext#getRelyingPartyId()}.
 */
public final class SelectRelyingPartyConfiguration extends AbstractMessageHandler {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectRelyingPartyConfiguration.class);

    /** Resolver used to look up relying party configurations. */
    @NonnullAfterInit private ReloadableService<RelyingPartyConfigurationResolver> rpConfigResolver;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link MessageContext}.
     */
    @Nonnull private Function<MessageContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** The {@link RelyingPartyContext} to manipulate. */
    @Nullable private RelyingPartyContext relyingPartyCtx;
    
    /** Constructor. */
    public SelectRelyingPartyConfiguration() {
        relyingPartyContextLookupStrategy =
                new ChildContextLookup<>(RelyingPartyContext.class).compose(
                        new RecursiveTypedParentContextLookup<>(InOutOperationContext.class));
    }

    /**
     * Set the relying party config resolver to use.
     * 
     * @param resolver  the resolver to use
     */
    public void setRelyingPartyConfigurationResolver(
            @Nonnull final ReloadableService<RelyingPartyConfigurationResolver> resolver) {
        checkSetterPreconditions();
        rpConfigResolver = Constraint.isNotNull(resolver, "Relying party configuration resolver cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link MessageContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link MessageContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<MessageContext, RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (rpConfigResolver == null) {
            throw new ComponentInitializationException("RelyingPartyConfigurationResolver cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean doPreInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {
        relyingPartyCtx = relyingPartyContextLookupStrategy.apply(messageContext);
        if (relyingPartyCtx == null) {
            log.debug("{} No relying party context available", getLogPrefix());
            throw new MessageHandlerException("No relying party context available");
        }
        
        if (relyingPartyCtx.getRelyingPartyId() == null) {
            log.debug("{} No relying party ID available", getLogPrefix());
            throw new MessageHandlerException("No relying party ID available");
        }
        
        return super.doPreInvoke(messageContext);
    }
    
    /** {@inheritDoc} */
    @Override
    public void doInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {

        try {
            // Implicitly "verified", so we include the criterion for that.
            final CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(relyingPartyCtx.getRelyingPartyId()),
                    new VerifiedProfileCriterion(true));
            final RelyingPartyConfiguration config =
                    rpConfigResolver.getServiceableComponent().getComponent().resolveSingle(criteria);
            if (config == null) {
                log.debug("{} No relying party configuration applies to this request", getLogPrefix());
                throw new MessageHandlerException("No relying party configuration resolved for this request");
            }

            log.debug("{} Found relying party configuration {} for request", getLogPrefix(), config.getId());
            relyingPartyCtx.setConfiguration(config);
        } catch (final ResolverException e) {
            log.error("{} Error trying to resolve relying party configuration: {}", getLogPrefix(), e.getMessage());
            throw new MessageHandlerException("Error trying to resolve relying party configuration", e);
        } catch (final ServiceException e) {
            log.error("{} Invalid relying party configuration: {}", getLogPrefix(), e.getMessage());
            throw new MessageHandlerException("Invalid relying party configuration", e);
        }
    }
}
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
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.relyingparty.CriteriaRelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * This message handler attempts to resolve a {@link RelyingPartyConfiguration} and adds it to the {@link RelyingPartyContext}
 * that was looked up.
 * 
 * @post If a {@link RelyingPartyContext} is located, it will be populated with a non-null result of applying
 * the supplied {@link CriteriaRelyingPartyConfigurationResolver} to the {@link RelyingPartyContext#getRelyingPartyId()}.
 */
public final class SelectRelyingPartyConfiguration extends AbstractMessageHandler {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectRelyingPartyConfiguration.class);

    /** Resolver used to look up relying party configurations. */
    @NonnullAfterInit private CriteriaRelyingPartyConfigurationResolver rpConfigResolver;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link MessageContext}.
     */
    @Nonnull private Function<MessageContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** The {@link RelyingPartyContext} to manipulate. */
    @Nullable private RelyingPartyContext relyingPartyCtx;
    
    /** Constructor. */
    public SelectRelyingPartyConfiguration() {
        relyingPartyContextLookupStrategy = Functions.compose(
                new ChildContextLookup<InOutOperationContext, RelyingPartyContext>(RelyingPartyContext.class),
                new RecursiveTypedParentContextLookup<MessageContext,InOutOperationContext>(InOutOperationContext.class)
                );
    }

    /**
     * Set the relying party config resolver to use.
     * 
     * @param resolver  the resolver to use
     */
    public void setRelyingPartyConfigurationResolver(@Nonnull final CriteriaRelyingPartyConfigurationResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

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
            final CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(relyingPartyCtx.getRelyingPartyId()));
            final RelyingPartyConfiguration config = rpConfigResolver.resolveSingle(criteria);
            if (config == null) {
                log.debug("{} No relying party configuration applies to this request", getLogPrefix());
                throw new MessageHandlerException("No relying party configuration resolved for this request");
            }

            log.debug("{} Found relying party configuration {} for request", getLogPrefix(), config.getId());
            relyingPartyCtx.setConfiguration(config);
        } catch (final ResolverException e) {
            log.error("{} Error trying to resolve relying party configuration", getLogPrefix(), e);
            throw new MessageHandlerException("Error trying to resolve relying party configuration");
        }
    }
}
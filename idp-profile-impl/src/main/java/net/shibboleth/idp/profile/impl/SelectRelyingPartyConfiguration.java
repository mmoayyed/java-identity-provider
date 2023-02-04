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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.criterion.ProfileRequestContextCriterion;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.relyingparty.CriteriaRelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.Resolver;
import net.shibboleth.shared.resolver.ResolverException;

/**
 * This action attempts to resolve a {@link RelyingPartyConfiguration} and adds it to the {@link RelyingPartyContext}
 * that was looked up.
 * 
 * <p>Both the original and the later-added criteria-driven resolvers are supported.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CONFIG}
 * 
 * @post If a {@link RelyingPartyContext} is located, it will be populated with a non-null result of applying
 * the supplied {@link RelyingPartyConfigurationResolver} to the {@link ProfileRequestContext}.
 */
public final class SelectRelyingPartyConfiguration extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectRelyingPartyConfiguration.class);

    /** Resolver used to look up relying party configurations. */
    @NonnullAfterInit private Resolver<RelyingPartyConfiguration,?> rpConfigResolver;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** The {@link RelyingPartyContext} to manipulate. */
    @Nullable private RelyingPartyContext relyingPartyCtx;
    
    /** Constructor. */
    public SelectRelyingPartyConfiguration() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }

    /**
     * Set the relying party config resolver to use.
     * 
     * @param resolver  the resolver to use
     */
    public void setRelyingPartyConfigurationResolver(@Nonnull final Resolver<RelyingPartyConfiguration,?> resolver) {
        checkSetterPreconditions();
        rpConfigResolver = Constraint.isNotNull(resolver, "Relying party configuration resolver cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
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
    public boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.debug("{} No relying party context available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        try {
            final RelyingPartyConfiguration config;
            if (rpConfigResolver instanceof RelyingPartyConfigurationResolver) {
                // Old style.
                config = ((RelyingPartyConfigurationResolver) rpConfigResolver).resolveSingle(profileRequestContext);
            } else if (rpConfigResolver instanceof CriteriaRelyingPartyConfigurationResolver) {
                // New style.
                final CriteriaSet criteria = new CriteriaSet();
                if (relyingPartyCtx.getParent() == profileRequestContext) {
                    // Works as is.
                    criteria.add(new ProfileRequestContextCriterion(profileRequestContext));
                    config = ((CriteriaRelyingPartyConfigurationResolver) rpConfigResolver).resolveSingle(criteria);
                } else {
                    // Temporarily re-root for compatibility.
                    final ProfileRequestContext newPRC = new ProfileRequestContext();
                    final BaseContext originalParent = relyingPartyCtx.getParent();
                    newPRC.addSubcontext(relyingPartyCtx);
                    criteria.add(new ProfileRequestContextCriterion(newPRC));
                    config = ((CriteriaRelyingPartyConfigurationResolver) rpConfigResolver).resolveSingle(criteria);
                    if (originalParent != null) {
                        originalParent.addSubcontext(relyingPartyCtx);
                    }
                }
            } else {
                log.error("{} Unsupported resolver type: {}", getLogPrefix(), rpConfigResolver.getClass().getName());
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
                return;
            }
            
            if (config == null) {
                log.debug("{} No relying party configuration applies to this request", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
                return;
            }

            log.debug("{} Found relying party configuration {} for request", getLogPrefix(), config.getId());
            relyingPartyCtx.setConfiguration(config);
        } catch (final ResolverException e) {
            log.error("{} Error trying to resolve relying party configuration", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
        }
    }
    
}
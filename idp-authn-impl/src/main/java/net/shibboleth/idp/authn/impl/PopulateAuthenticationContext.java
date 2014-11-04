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

package net.shibboleth.idp.authn.impl;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * An action that populates an {@link AuthenticationContext} with the {@link AuthenticationFlowDescriptor}
 * objects configured into the IdP, and optionally a customized {@link PrincipalEvalPredicateFactoryRegistry}.
 * 
 * <p>The set of flows will be filtered by {@link AuthenticationProfileConfiguration#getAuthenticationFlows()}
 * if such a configuration is available from a {@link RelyingPartyContext} obtained via a lookup strategy,
 * by default the child of the {@link ProfileRequestContext}. Each flow's attached predicate is also
 * applied.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @post The AuthenticationContext is modified as above.
 */
public class PopulateAuthenticationContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateAuthenticationContext.class);
    
    /** The flows to make available for possible use. */
    @Nonnull @NonnullElements private Collection<AuthenticationFlowDescriptor> availableFlows;
    
    /** The registry of predicate factories for custom principal evaluation. */
    @Nullable private PrincipalEvalPredicateFactoryRegistry evalRegistry;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Profile configuration source for requested principals. */
    @Nullable private AuthenticationProfileConfiguration authenticationProfileConfig;
    
    /** Constructor. */
    PopulateAuthenticationContext() {
        availableFlows = Lists.newArrayList();
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }
    
    /**
     * Set the flows available for possible use.
     * 
     * @param flows the flows available for possible use
     */
    public void setAvailableFlows(@Nonnull @NonnullElements final Collection<AuthenticationFlowDescriptor> flows) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(flows, "Flow collection cannot be null");
        
        availableFlows.clear();
        availableFlows.addAll(Collections2.filter(flows, Predicates.notNull()));
    }
    
    /**
     * Get the registry of predicate factories for custom principal evaluation.
     * 
     * @return predicate factory registry
     */
    @Nonnull public PrincipalEvalPredicateFactoryRegistry getPrincipalPredicateFactoryEvalRegistry() {
        return evalRegistry;
    }
    
    /**
     * Set the registry of predicate factories for custom principal evaluation.
     * 
     * @param registry predicate factory registry
     */
    public void setPrincipalEvalPredicateFactoryRegistry(
            @Nonnull final PrincipalEvalPredicateFactoryRegistry registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        evalRegistry = Constraint.isNotNull(registry, "PrincipalEvalPredicateFactoryRegistry cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx != null) {
            final ProfileConfiguration config = rpCtx.getProfileConfig();
            if (config != null) {
                if (config instanceof AuthenticationProfileConfiguration) {
                    authenticationProfileConfig = (AuthenticationProfileConfiguration) config;
                }
            }
        }
        
        return super.doPreExecute(profileRequestContext, authenticationContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (evalRegistry != null) {
            log.debug("{} Installing custom PrincipalEvalPredicateFactoryRegistry into AuthenticationContext",
                    getLogPrefix());
            authenticationContext.setPrincipalEvalPredicateFactoryRegistry(evalRegistry);
        }

        if (authenticationProfileConfig != null
                && !authenticationProfileConfig.getAuthenticationFlows().isEmpty()) {
            for (final AuthenticationFlowDescriptor desc : availableFlows) {
                if (authenticationProfileConfig.getAuthenticationFlows().contains(desc.getId())) {
                    if (desc.apply(profileRequestContext)) {
                        authenticationContext.getPotentialFlows().put(desc.getId(), desc);
                    } else {
                        log.debug("{} Filtered out authentication flow {} due to attached condition", getLogPrefix(),
                                desc.getId());
                    }
                } else {
                    log.debug("{} Filtered out authentication flow {} due to profile configuration", getLogPrefix(),
                            desc.getId());
                }
            }
        } else {
            for (final AuthenticationFlowDescriptor desc : availableFlows) {
                if (desc.apply(profileRequestContext)) {
                    authenticationContext.getPotentialFlows().put(desc.getId(), desc);
                } else {
                    log.debug("{} Filtered out authentication flow {} due to attached condition", getLogPrefix(),
                            desc.getId());
                }
            }
        }

        log.debug("{} Installed {} authentication flows into AuthenticationContext", getLogPrefix(),
                authenticationContext.getPotentialFlows().size());
    }
    
}
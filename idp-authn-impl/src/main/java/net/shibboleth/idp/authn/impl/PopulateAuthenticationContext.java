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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.config.navigate.AuthenticationFlowsLookupFunction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that populates an {@link AuthenticationContext} with the {@link AuthenticationFlowDescriptor}
 * objects configured into the IdP, potential flows filtered by flow IDs from a lookup function.
 * 
 * <p>If a {@link RequestedPrincipalContext} child is found, then optionally a customized
 * {@link PrincipalEvalPredicateFactoryRegistry} will be installed into it.</p>
 * 
 * <p>The flow IDs used for filtering must omit the {@link AuthenticationFlowDescriptor#FLOW_ID_PREFIX} prefix.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @post The AuthenticationContext is modified as above.
 */
public class PopulateAuthenticationContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateAuthenticationContext.class);
    
    /** All of the known flows in the system. */
    @Nonnull @NonnullElements private Collection<AuthenticationFlowDescriptor> availableFlows;

    /** The flows to make available for possible use. */
    @Nonnull @NonnullElements private Collection<AuthenticationFlowDescriptor> potentialFlows;
    
    /** Lookup function for the flow IDs to activate from within the available set. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> activeFlowsLookupStrategy;

    /** The registry of predicate factories for custom principal evaluation. */
    @Nullable private PrincipalEvalPredicateFactoryRegistry evalRegistry;
    
    /** Optional lookup strategy for triggering credential validators to return a fixed event for testing. */
    @Nullable private Function<ProfileRequestContext,String> fixedEventLookupStrategy;
    
    /** Constructor. */
    PopulateAuthenticationContext() {
        availableFlows = Collections.emptyList();
        potentialFlows = Collections.emptyList();
        activeFlowsLookupStrategy = new AuthenticationFlowsLookupFunction();
    }
    
    /**
     * Set the flows known to the system.
     * 
     * @param flows the flows known to the system
     */
    public void setAvailableFlows(@Nonnull @NonnullElements final Collection<AuthenticationFlowDescriptor> flows) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        availableFlows = List.copyOf(Constraint.isNotNull(flows, "Flow collection cannot be null"));
    }

    /**
     * Set the flows to make available for use (a subset of the available ones).
     * 
     * @param flows the flows to make available for use
     * 
     * @since 3.3.0
     */
    public void setPotentialFlows(@Nonnull @NonnullElements final Collection<AuthenticationFlowDescriptor> flows) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        potentialFlows = List.copyOf(Constraint.isNotNull(flows, "Flow collection cannot be null"));
    }
    
    /**
     * Set the lookup strategy to use for the authentication flows to activate.
     * 
     * @param strategy lookup strategy
     */
    public void setActiveFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        activeFlowsLookupStrategy = Constraint.isNotNull(strategy, "Flow lookup strategy cannot be null");
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
     * Set optional lookup strategy to return a fixed event to return from credential validation
     * to exercise error and warning logic.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.4.0
     */
    public void setFixedEventLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        fixedEventLookupStrategy = strategy;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (evalRegistry != null) {
            authenticationContext.setPrincipalEvalPredicateFactoryRegistry(evalRegistry);
            final RequestedPrincipalContext rpCtx =
                    authenticationContext.getSubcontext(RequestedPrincipalContext.class);
            if (rpCtx != null) {
                rpCtx.setPrincipalEvalPredicateFactoryRegistry(evalRegistry);
            }
        }
        
        if (fixedEventLookupStrategy != null) {
            authenticationContext.setFixedEventLookupStrategy(fixedEventLookupStrategy);
        }
        
        if (availableFlows.isEmpty()) {
            log.warn("{} No authentication flows are available", getLogPrefix());
            return;
        }
        
        // Install all the available flows for reference.
        for (final AuthenticationFlowDescriptor desc : availableFlows) {
            authenticationContext.getAvailableFlows().put(desc.getId(), desc);
        }
        
        // Now we have to filter the potential flows against the available and active flows and
        // check for applicability before including them.
        
        final Collection<String> activeFlows = activeFlowsLookupStrategy.apply(profileRequestContext);

        if (activeFlows != null && !activeFlows.isEmpty()) {
            for (final AuthenticationFlowDescriptor desc : potentialFlows) {
                final String flowId = desc.getId().substring(desc.getId().indexOf('/') + 1);
                if (activeFlows.contains(flowId)) {
                    if (authenticationContext.getAvailableFlows().containsKey(desc.getId())
                            && desc.test(profileRequestContext)) {
                        authenticationContext.getPotentialFlows().put(desc.getId(), desc);
                    } else {
                        log.debug("{} Filtered out authentication flow {}", getLogPrefix(), desc.getId());
                    }
                } else {
                    log.debug("{} Filtered out authentication flow {} due to profile configuration", getLogPrefix(),
                            desc.getId());
                }
            }
        } else {
            for (final AuthenticationFlowDescriptor desc : potentialFlows) {
                if (authenticationContext.getAvailableFlows().containsKey(desc.getId())
                        && desc.test(profileRequestContext)) {
                    authenticationContext.getPotentialFlows().put(desc.getId(), desc);
                } else {
                    log.debug("{} Filtered out authentication flow {}", getLogPrefix(), desc.getId());
                }
            }
        }

        if (authenticationContext.getPotentialFlows().isEmpty()) {
            log.warn("{} No authentication flows are active for this request", getLogPrefix());
        } else {
            log.debug("{} Installed {} potential authentication flows into AuthenticationContext", getLogPrefix(),
                    authenticationContext.getPotentialFlows().size());
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
}
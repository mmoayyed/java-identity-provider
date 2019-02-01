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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that creates and populates a {@link MultiFactorAuthenticationContext} with the set of
 * transition rules to use for coordinating activity, the executing {@link AuthenticationFlowDescriptor}
 * and with any active "factors" found, if an active result from the MFA flow is present in the
 * {@link AuthenticationContext}.
 * 
 * <p>If the lookup strategy supplies no transition rules to use, then the {@link AuthnEventIds#RESELECT_FLOW}
 * event is signaled.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#RESELECT_FLOW}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @post <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getSubcontext(
 *  MultiFactorAuthenticationContext.class) != null</pre>
 */
public class PopulateMultiFactorAuthenticationContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateMultiFactorAuthenticationContext.class);
    
    /** Lookup strategy for obtaining the map of transition rules to use. */
    @Nonnull
    private Function<ProfileRequestContext,Map<String,MultiFactorAuthenticationTransition>> transitionMapLookupStrategy;
    
    /** Lookup/creation function for the context to populate. */
    @Nonnull
    private Function<ProfileRequestContext,MultiFactorAuthenticationContext> multiFactorContextCreationStrategy;
    
    /** Lookup strategy for active "factors" that may already be usable. */
    @Nullable private Function<ProfileRequestContext,Collection<AuthenticationResult>> activeResultLookupStrategy;
    
    /** Constructor. */
    PopulateMultiFactorAuthenticationContext() {
        transitionMapLookupStrategy = FunctionSupport.constant(null);
        multiFactorContextCreationStrategy =
                new ChildContextLookup(MultiFactorAuthenticationContext.class, true).compose(
                        new ChildContextLookup(AuthenticationContext.class));
        activeResultLookupStrategy = new DefaultResultLookupStrategy();
    }
    
    /**
     * Set the strategy to lookup the map of transition rules to apply.
     * 
     * @param strategy lookup strategy
     */
    public void setTransitionMapLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Map<String,MultiFactorAuthenticationTransition>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        transitionMapLookupStrategy = Constraint.isNotNull(strategy, "Transition map lookup strategy cannot be null");
    }
    
    /**
     * Set the lookup/creation strategy to use for the context to populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setMultiFactorContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,MultiFactorAuthenticationContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        multiFactorContextCreationStrategy = Constraint.isNotNull(strategy,
                "MultiFactorAuthenticationContext creation strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy for any active "factors" that may be reusable.
     * 
     * <p>The default strategy is to look for an active {@link AuthenticationResult} of the flow
     * currently being attempted, and check within it for {@link AuthenticationResultPrincipal} objects.</p>
     * 
     * @param strategy lookup strategy
     */
    public void setActiveResultLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<AuthenticationResult>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        activeResultLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    // CheckStyle: ReturnCount OFF
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Map<String,MultiFactorAuthenticationTransition> transitionMap =
                transitionMapLookupStrategy.apply(profileRequestContext);
        if (transitionMap == null) {
            log.info("No map of transition rules was returned");
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.RESELECT_FLOW);
            return;
        }
        
        final MultiFactorAuthenticationContext mfaCtx = multiFactorContextCreationStrategy.apply(profileRequestContext);
        if (mfaCtx == null) {
            log.error("{} Unable to create/access MultiFactorAuthenticationContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        mfaCtx.setAuthenticationFlowDescriptor(authenticationContext.getAttemptedFlow());
        mfaCtx.setTransitionMap(transitionMap);
        mfaCtx.setNextFlowId(null);
        mfaCtx.getActiveResults().clear();
        
        if (activeResultLookupStrategy != null) {
            final Collection<AuthenticationResult> results = activeResultLookupStrategy.apply(profileRequestContext);
            if (results != null) {
                for (final AuthenticationResult result : results) {
                    mfaCtx.getActiveResults().put(result.getAuthenticationFlowId(), result);
                }
            }
            log.debug("{} {} active result(s) extracted for possible reuse", getLogPrefix(),
                    results != null ? results.size() : 0);
        } else {
            log.debug("{} No lookup strategy provided, no active results will be made available", getLogPrefix());
        }
    }
    // CheckStyle: ReturnCount ON
    
    /**
     * Default strategy function to extract embedded {@link AuthenticationResult}s from inside
     * the {@link AuthenticationResultPrincipal} collection of an active {@link AuthenticationResult}
     * of the currently executing flow.
     */
    private class DefaultResultLookupStrategy
            implements Function<ProfileRequestContext,Collection<AuthenticationResult>> {

        /** {@inheritDoc} */
        @Override
        @Nullable public Collection<AuthenticationResult> apply(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final AuthenticationContext ac = input.getSubcontext(AuthenticationContext.class);
                if (ac != null && ac.getAttemptedFlow() != null) {
                    final AuthenticationResult mfaResult = ac.getActiveResults().get(ac.getAttemptedFlow().getId());
                    if (mfaResult != null) {
                        if (ac.isForceAuthn()) {
                            log.debug("{} Ignoring active result due to forced authentication requirement",
                                    getLogPrefix());
                            return null;
                        }
                        final Set<AuthenticationResultPrincipal> resultPrincipals =
                                mfaResult.getSubject().getPrincipals(AuthenticationResultPrincipal.class);
                        if (!resultPrincipals.isEmpty()) {
                            final Collection<AuthenticationResult> results = new ArrayList<>(resultPrincipals.size());
                            
                            for (final AuthenticationResultPrincipal resultPrincipal : resultPrincipals) {
                                // Reset the last-used time to match the MFA result.
                                // This makes longer timeouts irrelevant but might honor a shorter timeout in
                                // a small number of edge cases.
                                resultPrincipal.getAuthenticationResult().setLastActivityInstant(
                                        mfaResult.getLastActivityInstant());
                                processActiveResult(input, ac, results, resultPrincipal.getAuthenticationResult());
                            }
                            
                            return results;
                        }
                    }
                }
            }
            
            return null;
        }
        
        /**
         * Check an active result for possible inclusion in the returned collection.
         * 
         * @param profileRequestContext current profile request context
         * @param authenticationContext current authentication context
         * @param results the collection to add to
         * @param candidate the result to evaluate
         */
        void processActiveResult(@Nonnull final ProfileRequestContext profileRequestContext,
                @Nonnull final AuthenticationContext authenticationContext,
                @Nonnull final Collection<AuthenticationResult> results,
                @Nonnull final AuthenticationResult candidate) {
            
            final AuthenticationFlowDescriptor descriptor = authenticationContext.getAvailableFlows().get(
                    candidate.getAuthenticationFlowId());
            if (descriptor != null) {
                if (descriptor.test(profileRequestContext)) {
                    if (descriptor.isResultActive(candidate)) {
                        if (authenticationContext.getMaxAge() > 0
                                && candidate.getAuthenticationInstant() + authenticationContext.getMaxAge()
                                    < System.currentTimeMillis()) {
                            log.debug("{} Ignoring active result from login flow {} due to maxAge on request",
                                    getLogPrefix(), candidate.getAuthenticationFlowId());
                        } else {
                            results.add(candidate);
                        }
                    } else {
                        log.debug("{} Result from login flow {} has expired", getLogPrefix(), descriptor.getId());
                    }
                } else {
                    log.debug("{} Ignoring active result from login flow {} due to activation condition",
                            getLogPrefix(), candidate.getAuthenticationFlowId());
                }
            } else {
                log.warn("{} Ignoring active result from undefined login flow {}", getLogPrefix(),
                        candidate.getAuthenticationFlowId());
            }
            
        }
    }
    
}
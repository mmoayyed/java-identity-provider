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

import java.security.Principal;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An authentication action that selects an authentication flow to invoke, or re-uses an
 * existing result for SSO.
 * 
 * <p>This is the heart of the authentication processing sequence, and runs after the
 * {@link AuthenticationContext} has been fully populated. It uses the potential flows,
 * the {@link RequestedPrincipalContext} (if any), and the active results, to decide how
 * to proceed.</p>
 * 
 * <p>If there is no {@link RequestedPrincipalContext}, then an active result will be
 * reused with the default "proceed" event returned, unless the request requires forced
 * authentication. If not possible, then a potential flow will be selected and its ID
 * returned as the result of the action.</p>
 * 
 * <p>If there are requested principals, then the results or flows chosen must "match" the
 * request information according to the {@link net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry}
 * attached to the context. The "favorSSO" option determines whether to select a flow
 * specifically in the order specified by the {@link RequestedPrincipalContext}, or to favor
 * an active but matching result, over a new flow. Forced authentication trumps the
 * use of any active result.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID} (reuse of a result, i.e., SSO)
 * @event {@link AuthnEventIds#NO_PASSIVE}
 * @event {@link AuthnEventIds#NO_POTENTIAL_FLOW}
 * @event {@link AuthnEventIds#REQUEST_UNSUPPORTED}
 * @event Selected flow ID to execute
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @pre The content of {@link AuthenticationContext#getPotentialFlows()} are assumed to be acceptable
 * with respect to passive and forced authentication requirements. 
 * @post If a result is reused, {@link AuthenticationContext#getAuthenticationResult()} will return
 * that result. Otherwise, {@link AuthenticationContext#getAttemptedFlow()} will return the flow
 * selected for execution and returned as an event.
 */
public class SelectAuthenticationFlow extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectAuthenticationFlow.class);

    /** Whether SSO trumps explicit relying party flow preference. */
    private boolean favorSSO;
    
    /** A subordinate RequestedPrincipalContext, if any. */
    @Nullable private RequestedPrincipalContext requestedPrincipalCtx; 
    
    /**
     * Get whether SSO should trump explicit relying party flow preference.
     * 
     * @return whether SSO should trump explicit relying party flow preference
     */
    public boolean getFavorSSO() {
        return favorSSO;
    }

    /**
     * Set whether SSO should trump explicit relying party flow preference.
     * 
     * @param flag whether SSO should trump explicit relying party flow preference
     */
    public void setFavorSSO(final boolean flag) {
        favorSSO = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        requestedPrincipalCtx = authenticationContext.getSubcontext(RequestedPrincipalContext.class, false);
        
        // Detect a previous attempted flow, and move it to the intermediate collection.
        // This will prevent re-selecting the same (probably failed) flow again.
        if (authenticationContext.getAttemptedFlow() != null) {
            log.info("{} Moving incomplete flow {} to intermediate set, reselecting a different one", getLogPrefix(),
                    authenticationContext.getAttemptedFlow().getId());
            authenticationContext.getIntermediateFlows().put(
                    authenticationContext.getAttemptedFlow().getId(), authenticationContext.getAttemptedFlow());
        }
        
        return super.doPreExecute(profileRequestContext, authenticationContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (requestedPrincipalCtx == null || requestedPrincipalCtx.getOperator() == null
                || requestedPrincipalCtx.getRequestedPrincipals().isEmpty()) {
            doSelectNoRequestedPrincipals(profileRequestContext, authenticationContext);
        } else {
            doSelectRequestedPrincipals(profileRequestContext, authenticationContext);
        }
    }

    /**
     * Executes the selection process in the absence of specific requested principals.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void doSelectNoRequestedPrincipals(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        log.debug("{} No specific Principals requested", getLogPrefix());
        
        if (authenticationContext.isForceAuthn()) {
            log.debug("{} forced authentication requested, selecting an inactive flow", getLogPrefix());
            final AuthenticationFlowDescriptor flow =
                    getUnattemptedInactiveFlow(profileRequestContext, authenticationContext);
            if (flow == null) {
                log.error("{} No potential flows left to choose from, authentication will fail", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext,
                        authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE : AuthnEventIds.NO_POTENTIAL_FLOW);
                return;
            }
            selectInactiveFlow(profileRequestContext, authenticationContext, flow);
            return;
        }

        // Pick a result to reuse if possible.
        for (final AuthenticationResult activeResult : authenticationContext.getActiveResults().values()) {
            final AuthenticationFlowDescriptor flow = authenticationContext.getPotentialFlows().get(
                    activeResult.getAuthenticationFlowId());
            if (flow != null && flow.apply(profileRequestContext)) {
                selectActiveResult(profileRequestContext, authenticationContext, activeResult);
                return;
            }
        }
        
        log.debug("{} No usable active results available, selecting an inactive flow", getLogPrefix());
        final AuthenticationFlowDescriptor flow =
                getUnattemptedInactiveFlow(profileRequestContext, authenticationContext);
        if (flow == null) {
            log.error("{} No potential flows left to choose from, authentication will fail", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext,
                    authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE : AuthnEventIds.NO_POTENTIAL_FLOW);
            return;
        }
        selectInactiveFlow(profileRequestContext, authenticationContext, flow);
    }

    /**
     * Return the first inactive potential flow not found in the intermediate flows collection. 
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @return an eligible flow, or null
     */
    @Nullable private AuthenticationFlowDescriptor getUnattemptedInactiveFlow(
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        for (final AuthenticationFlowDescriptor flow : authenticationContext.getPotentialFlows().values()) {
            if (!authenticationContext.getIntermediateFlows().containsKey(flow.getId())) {
                if (!flow.apply(profileRequestContext)) {
                    log.debug("{} Flow '{}' is inapplicable for this request", getLogPrefix(), flow.getId());
                    authenticationContext.getIntermediateFlows().put(flow.getId(), flow);
                } else {
                    return flow;
                }
            }
        }
        
        return null;
    }

    /**
     * Selects an inactive flow and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     * @param descriptor the flow to select
     */
    private void selectInactiveFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final AuthenticationFlowDescriptor descriptor) {

        log.debug("{} Selecting inactive authentication flow {}", getLogPrefix(), descriptor.getId());
        authenticationContext.setAttemptedFlow(descriptor);
        ActionSupport.buildEvent(profileRequestContext, descriptor.getId());
    }    
    
    /**
     * Selects an active result and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     * @param result the result to reuse
     */
    private void selectActiveResult(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final AuthenticationResult result) {

        log.debug("{} Reusing active result {}", getLogPrefix(), result.getAuthenticationFlowId());
        result.setLastActivityInstantToNow();
        authenticationContext.setAuthenticationResult(result);
        ActionSupport.buildProceedEvent(profileRequestContext);
    }

    /**
     * Executes the selection process in the presence of specific requested Principals, requiring
     * evaluation of potential flows and results for Principal-compatibility with request.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void doSelectRequestedPrincipals(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        log.debug("{} Specific principals requested with '{}' operator: {}", getLogPrefix(),
                requestedPrincipalCtx.getOperator(), requestedPrincipalCtx.getRequestedPrincipals());
        
        if (authenticationContext.isForceAuthn()) {
            log.debug("{} Forced authentication requested, selecting an inactive flow", getLogPrefix());
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
        } else if (authenticationContext.getActiveResults().isEmpty()) {
            log.debug("{} No active results available, selecting an inactive flow", getLogPrefix());
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
        } else {
            selectRequestedFlow(profileRequestContext, authenticationContext);
        }
    }

    /**
     * Selects an inactive flow in the presence of specific requested Principals, and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void selectRequestedInactiveFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Map<String,AuthenticationFlowDescriptor> potentialFlows = authenticationContext.getPotentialFlows();
        
        // Check each flow for compatibility with request. Don't check for an active result also.
        // Also omit anything in the intermediates collection already.
        for (final Principal p : requestedPrincipalCtx.getRequestedPrincipals()) {
            log.debug("{} Checking for an inactive flow compatible with operator '{}' and principal '{}'",
                    getLogPrefix(), requestedPrincipalCtx.getOperator(), p);
            final PrincipalEvalPredicateFactory factory =
                    authenticationContext.getPrincipalEvalPredicateFactoryRegistry().lookup(
                            p.getClass(), requestedPrincipalCtx.getOperator());
            if (factory != null) {
                final PrincipalEvalPredicate predicate = factory.getPredicate(p);
                for (final AuthenticationFlowDescriptor descriptor : potentialFlows.values()) {
                    if (!authenticationContext.getIntermediateFlows().containsKey(descriptor.getId())
                            && predicate.apply(descriptor) && descriptor.apply(profileRequestContext)) {
                        requestedPrincipalCtx.setMatchingPrincipal(predicate.getMatchingPrincipal());
                        selectInactiveFlow(profileRequestContext, authenticationContext, descriptor);
                        return;
                    }
                }
            } else {
                log.warn("{} Configuration does not support requested principal evaluation with "
                        + "operator '{}' and type '{}'", getLogPrefix(), requestedPrincipalCtx.getOperator(),
                        p.getClass());
            }
        }
        
        
        log.info("{} None of the potential authentication flows can satisfy the request", getLogPrefix());
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
    }
    
    /**
     * Selects a flow or an active result in the presence of specific requested Principals and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
// Checkstyle: MethodLength|CyclomaticComplexity OFF
    private void selectRequestedFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Map<String,AuthenticationResult> activeResults = authenticationContext.getActiveResults();

        if (favorSSO) {
            log.debug("{} Giving priority to active results that meet request requirements");
            
            // Check each active result for compatibility with request.
            for (final Principal p : requestedPrincipalCtx.getRequestedPrincipals()) {
                log.debug("{} Checking for an active result compatible with operator '{}' and principal '{}'",
                        getLogPrefix(), requestedPrincipalCtx.getOperator(), p);
                final PrincipalEvalPredicateFactory factory =
                        authenticationContext.getPrincipalEvalPredicateFactoryRegistry().lookup(
                                p.getClass(), requestedPrincipalCtx.getOperator());
                if (factory != null) {
                    final PrincipalEvalPredicate predicate = factory.getPredicate(p);
                    for (final AuthenticationResult result : activeResults.values()) {
                        if (predicate.apply(result)) {
                            final AuthenticationFlowDescriptor descriptor =
                                    authenticationContext.getPotentialFlows().get(result.getAuthenticationFlowId());
                            if (descriptor != null && descriptor.apply(profileRequestContext)) {
                                requestedPrincipalCtx.setMatchingPrincipal(predicate.getMatchingPrincipal());
                                selectActiveResult(profileRequestContext, authenticationContext, result);
                                return;
                            }
                        }
                    }
                } else {
                    log.warn("{} Configuration does not support requested principal evaluation with "
                            + "operator '{}' and type '{}'", getLogPrefix(), requestedPrincipalCtx.getOperator(),
                            p.getClass());
                }
            }
            
            // We know at this point there are no active results that will fit, so drop into inactive.
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
            return;

        } else {
            final Map<String,AuthenticationFlowDescriptor> potentialFlows = authenticationContext.getPotentialFlows();

            // In this branch, we check each flow for compatibility *and* then double check to see if an active
            // result from that flow also exists and is compatible. This favors a matching inactive flow that is
            // higher in request precedence than an active result.
            for (final Principal p : requestedPrincipalCtx.getRequestedPrincipals()) {
                log.debug("{} Checking for an inactive flow or active result compatible with "
                        + "operator '{}' and principal '{}'", getLogPrefix(), requestedPrincipalCtx.getOperator(), p);
                final PrincipalEvalPredicateFactory factory =
                        authenticationContext.getPrincipalEvalPredicateFactoryRegistry().lookup(
                                p.getClass(), requestedPrincipalCtx.getOperator());
                if (factory != null) {
                    final PrincipalEvalPredicate predicate = factory.getPredicate(p);
                    for (final AuthenticationFlowDescriptor descriptor : potentialFlows.values()) {
                        if (!authenticationContext.getIntermediateFlows().containsKey(descriptor.getId())
                                && predicate.apply(descriptor) && descriptor.apply(profileRequestContext)) {
                            
                            // Now check for an active result we can use from this flow. Not all results from a flow
                            // will necessarily match the request just because the flow might.
                            final AuthenticationResult result = activeResults.get(descriptor.getId());
                            if (result == null || !predicate.apply(result)) {
                                selectInactiveFlow(profileRequestContext, authenticationContext, descriptor);
                            } else {
                                selectActiveResult(profileRequestContext, authenticationContext, result);
                            }
                            requestedPrincipalCtx.setMatchingPrincipal(predicate.getMatchingPrincipal());
                            return;
                        }
                    }
                } else {
                    log.warn("{} Configuration does not support requested principal evaluation with "
                            + "operator '{}' and type '{}'", getLogPrefix(), requestedPrincipalCtx.getOperator(),
                            p.getClass());
                }
            }
            
            log.info("{} None of the potential authentication flows can satisfy the request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
        }
    }
// Checkstyle: MethodLength|CyclomaticComplexity ON
        
}
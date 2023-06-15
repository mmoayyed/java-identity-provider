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

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.PreferredPrincipalContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;

/**
 * An authentication action that selects an authentication flow to invoke, or re-uses an
 * existing result for SSO.
 * 
 * <p>This is the heart of the authentication processing sequence, and runs after the
 * {@link AuthenticationContext} has been fully populated. It uses the potential flows,
 * the {@link RequestedPrincipalContext} (if any), and the active results, to decide how
 * to proceed.</p>
 * 
 * <p>Normal processing behavior can be circumvented if {@link AuthenticationContext#getSignaledFlowId()}
 * is set, which causes an active result from that flow to be reused, or that flow to be invoked, if at
 * all possible, subject to the usual predicates and requested principal constraints noted below.</p>
 * 
 * <p>Otherwise, if there is no {@link RequestedPrincipalContext}, then an active result will be
 * reused, unless the request requires forced authentication. If not possible, then a potential
 * flow will be selected and its ID returned as the result of the action.</p>
 * 
 * <p>If there are requested principals, then the results or flows chosen must "match" the
 * request information according to the {@link net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry}
 * attached to the context. The "favorSSO" option determines whether to select a flow specifically
 * in the order specified by the {@link RequestedPrincipalContext}, or to favor an active but matching result
 * over a new flow. Forced authentication trumps the use of any active result.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID} (reuse of a result, i.e., SSO)
 * @event {@link AuthnEventIds#NO_PASSIVE}
 * @event {@link AuthnEventIds#NO_POTENTIAL_FLOW}
 * @event {@link AuthnEventIds#REQUEST_UNSUPPORTED}
 * @event Selected flow ID to execute
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @pre The content of {@link AuthenticationContext#getPotentialFlows()} are assumed to be acceptable
 * with respect to passive and forced authentication requirements, etc. 
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

    /** A subordinate PreferredPrincipalContext, if any. */
    @Nullable private PreferredPrincipalContext preferredPrincipalCtx;
    
    /** Tracks a proxy count of zero for the request. */
    private boolean noProxying;

    /**
     * Get whether SSO should trump explicit relying party requirements preference.
     * 
     * @return whether SSO should trump explicit relying party requirements preference
     */
    public boolean getFavorSSO() {
        return favorSSO;
    }

    /**
     * Set whether SSO should trump explicit relying party requirements preference.
     * 
     * @param flag whether SSO should trump explicit relying party requirements preference
     */
    public void setFavorSSO(final boolean flag) {
        checkSetterPreconditions();
        favorSSO = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        noProxying = authenticationContext.getProxyCount() != null && authenticationContext.getProxyCount() == 0;
        
        final RequestedPrincipalContext rpc = requestedPrincipalCtx =
                authenticationContext.getSubcontext(RequestedPrincipalContext.class);
        if (rpc != null) {
            if (rpc.getOperator() == null || rpc.getRequestedPrincipals().isEmpty()) {
                requestedPrincipalCtx = null;
            }
        }
        
        preferredPrincipalCtx = authenticationContext.getSubcontext(PreferredPrincipalContext.class);
        if (preferredPrincipalCtx != null && preferredPrincipalCtx.getPreferredPrincipals().isEmpty()) {
            preferredPrincipalCtx = null;
        }
        
        // Detect a previous attempted flow, and move it to the intermediate collection.
        // This will prevent re-selecting the same (probably failed) flow again as part of
        // general flow selection. A flow might signal to explicitly re-run another flow anyway
        final AuthenticationFlowDescriptor flow = authenticationContext.getAttemptedFlow();
        if (flow != null) {
            log.info("{} Moving incomplete flow {} to intermediate set", getLogPrefix(), flow.getId());
            authenticationContext.getIntermediateFlows().put(flow.ensureId(), authenticationContext.getAttemptedFlow());
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (authenticationContext.getSignaledFlowId() != null) {
            doSelectSignaledFlow(profileRequestContext, authenticationContext);
        } else if (requestedPrincipalCtx == null) {
            doSelectNoRequestedPrincipals(profileRequestContext, authenticationContext);
        } else {
            doSelectRequestedPrincipals(profileRequestContext, authenticationContext);
        }
    }
    
// Checkstyle: MethodLength|CyclomaticComplexity|ReturnCount OFF
    /**
     * Executes the selection process in the presence of an explicit flow signal.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void doSelectSignaledFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        // See if flow exists.
        final AuthenticationFlowDescriptor flow = authenticationContext.getPotentialFlows().get(
                authenticationContext.getSignaledFlowId());
        if (flow == null) {
            log.error("{} Signaled flow {} is not available", getLogPrefix(),
                    authenticationContext.getSignaledFlowId());
            ActionSupport.buildEvent(profileRequestContext,
                    authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE : AuthnEventIds.NO_POTENTIAL_FLOW);
            authenticationContext.setSignaledFlowId(null);
            return;
        }
        
        // Clear state.
        authenticationContext.setSignaledFlowId(null);
        
        log.debug("{} Attempting to honor signaled flow {}", getLogPrefix(), flow.getId());

        if (noProxying && flow.isProxyScopingEnforced()) {
            log.error("{} Signaled flow {} disallowed due to proxy count of zero", getLogPrefix(), flow.getId());
            ActionSupport.buildEvent(profileRequestContext,
                    authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE : AuthnEventIds.PROXY_COUNT_EXCEEDED);
            return;
        }

        // If not forced, check for an active result for that flow.
        
        AuthenticationResult activeResult = null;
        if (!authenticationContext.isForceAuthn()) {
            activeResult = authenticationContext.getActiveResults().get(flow.ensureId());
            if (!activeResult.test(profileRequestContext)) {
                log.debug("{} Active result for flow {} not reusable, ignoring", getLogPrefix(),
                        activeResult.getAuthenticationFlowId());
                activeResult = null;
            }
        }
        
        final RequestedPrincipalContext rpc = requestedPrincipalCtx;
        if (activeResult != null) {
            if (rpc != null) {
                for (final Principal p : rpc.getRequestedPrincipals()) {
                    assert p!= null;
                    final PrincipalEvalPredicate predicate = rpc.getPredicate(p);
                    if (predicate != null) {
                        if (predicate.test(activeResult)) {
                            selectActiveResult(profileRequestContext, authenticationContext, activeResult);
                            return;
                        }
                    } else {
                        log.warn("{} Configuration does not support requested principal evaluation with "
                                + "operator '{}' and type '{}'", getLogPrefix(),
                                rpc.getOperator(), p.getClass());
                    }
                }
            } else {
                selectActiveResult(profileRequestContext, authenticationContext, activeResult);
                return;
            }
        }
        
        // Try and use the inactive flow.

        // Check for IsPassive compatibility.
        if (authenticationContext.isPassive() && !flow.isPassiveAuthenticationSupported()) {
            log.error("{} Signaled flow {} does not support passive authentication", getLogPrefix(), flow.getId());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_PASSIVE);
            return;
        } else if (rpc != null) {
            for (final Principal p : rpc.getRequestedPrincipals()) {
                assert p != null;
                final PrincipalEvalPredicate predicate = rpc.getPredicate(p);
                if (predicate != null) {
                    if (predicate.test(flow) && flow.test(profileRequestContext)) {
                        selectInactiveFlow(profileRequestContext, authenticationContext, flow);
                        return;
                    }
                } else {
                    log.warn("{} Configuration does not support requested principal evaluation with "
                            + "operator '{}' and type '{}'", getLogPrefix(), rpc.getOperator(),
                            p.getClass());
                }
            }
        } else if (flow.test(profileRequestContext)) {
            selectInactiveFlow(profileRequestContext, authenticationContext, flow);
            return;
        }
        
        log.error("{} Signaled flow {} was not applicable to request", getLogPrefix(), flow.getId());
        ActionSupport.buildEvent(profileRequestContext,
                authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE : AuthnEventIds.NO_POTENTIAL_FLOW);
    }
// Checkstyle: MethodLength|CyclomaticComplexity|ReturnCount ON
    
// Checkstyle: ReturnCount|CyclomaticComplexity OFF
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
            log.debug("{} Forced authentication requested, selecting an inactive flow", getLogPrefix());
            final AuthenticationFlowDescriptor flow =
                    getUnattemptedInactiveFlow(profileRequestContext, authenticationContext);
            if (flow == null) {
                log.info("{} No potential flows left to choose from, authentication failed", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext,
                        authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE :
                            (noProxying ? AuthnEventIds.PROXY_COUNT_EXCEEDED : AuthnEventIds.NO_POTENTIAL_FLOW));
                return;
            }
            selectInactiveFlow(profileRequestContext, authenticationContext, flow);
            return;
        }

        // Pick a result to reuse if possible, honoring any preferences if necessary.
        
        AuthenticationResult resultToSelect = null;
        
        for (final AuthenticationResult activeResult : authenticationContext.getActiveResults().values()) {
            if (activeResult.test(profileRequestContext)) {
                resultToSelect = activeResult;
                final PreferredPrincipalContext ppCtx = preferredPrincipalCtx;
                if (ppCtx == null || ppCtx.isAcceptable(activeResult)) {
                    break;
                }
            } else {
                log.debug("{} Active result for flow {} not reusable, ignoring", getLogPrefix(),
                        activeResult.getAuthenticationFlowId());
            }
        }
        
        if (resultToSelect != null) {
            selectActiveResult(profileRequestContext, authenticationContext, resultToSelect);
            return;
        }
        
        log.debug("{} No usable active results available, selecting an inactive flow", getLogPrefix());
        final AuthenticationFlowDescriptor flow =
                getUnattemptedInactiveFlow(profileRequestContext, authenticationContext);
        if (flow == null) {
            log.info("{} No potential flows left to choose from, authentication failed", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext,
                    authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE :
                        (noProxying ? AuthnEventIds.PROXY_COUNT_EXCEEDED : AuthnEventIds.NO_POTENTIAL_FLOW));
            return;
        }
        selectInactiveFlow(profileRequestContext, authenticationContext, flow);
    }
// Checkstyle: ReturnCount|CyclomaticComplexity ON

    /**
     * Return the first inactive potential flow not found in the intermediate flows collection that applies
     * to the request.
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @return an eligible flow, or null
     */
    @Nullable private AuthenticationFlowDescriptor getUnattemptedInactiveFlow(
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        AuthenticationFlowDescriptor selectedFlow = null;
        
        for (final AuthenticationFlowDescriptor flow : authenticationContext.getPotentialFlows().values()) {
            if (!authenticationContext.getIntermediateFlows().containsKey(flow.ensureId())) {
                if (!authenticationContext.isPassive() || flow.isPassiveAuthenticationSupported()) {
                    if (!noProxying || !flow.isProxyScopingEnforced()) {
                        if (flow.test(profileRequestContext)) {
                            selectedFlow = flow;
                            final PreferredPrincipalContext ppCtx = preferredPrincipalCtx;
                            if (ppCtx == null || ppCtx.isAcceptable(flow)) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return selectedFlow;
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

        final String id = descriptor.ensureId();
        log.debug("{} Selecting inactive authentication flow {}", getLogPrefix(), id);
        authenticationContext.setAttemptedFlow(descriptor);
        ActionSupport.buildEvent(profileRequestContext, id);
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
        
        final RequestedPrincipalContext rpCtx = requestedPrincipalCtx;
        assert rpCtx != null;
        log.debug("{} Specific principals requested with '{}' operator: {}", getLogPrefix(),
                rpCtx.getOperator(), rpCtx.getRequestedPrincipals());

        
        if (authenticationContext.isForceAuthn()) {
            log.debug("{} Forced authentication requested, selecting an inactive flow", getLogPrefix());
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
        } else if (authenticationContext.getActiveResults().isEmpty()) {
            log.debug("{} No active results available, selecting an inactive flow", getLogPrefix());
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
        } else {
            selectRequestedFlow(profileRequestContext, authenticationContext, authenticationContext.getActiveResults());
        }
    }

// Checkstyle: CyclomaticComplexity OFF
    /**
     * Selects an inactive flow in the presence of specific requested Principals, and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void selectRequestedInactiveFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Map<String,AuthenticationFlowDescriptor> potentialFlows = authenticationContext.getPotentialFlows();
        final RequestedPrincipalContext rpCtx = requestedPrincipalCtx;
        assert rpCtx != null;
        
        // Check each flow for compatibility with request. Don't check for an active result also.
        // Also omit anything in the intermediates collection already.
        for (final Principal p : rpCtx.getRequestedPrincipals()) {
            log.debug("{} Checking for inactive flow compatible with operator '{}' and principal '{}'",
                    getLogPrefix(), rpCtx.getOperator(), p.getName());
            final PrincipalEvalPredicate predicate = rpCtx.getPredicate(p);
            if (predicate != null) {
                for (final AuthenticationFlowDescriptor descriptor : potentialFlows.values()) {
                    if (!authenticationContext.getIntermediateFlows().containsKey(descriptor.ensureId())
                            && predicate.test(descriptor) && descriptor.test(profileRequestContext)) {
                        if (!authenticationContext.isPassive() || descriptor.isPassiveAuthenticationSupported()) {
                            if (!noProxying || !descriptor.isProxyScopingEnforced()) {
                                selectInactiveFlow(profileRequestContext, authenticationContext, descriptor);
                                return;
                            }
                            log.debug("{} Flow '{}' disallowed by effective proxy count of zero", getLogPrefix(),
                                    descriptor.getId());
                        }
                    }
                }
            } else {
                log.warn("{} Configuration does not support requested principal evaluation with "
                        + "operator '{}' and type '{}'", getLogPrefix(), rpCtx.getOperator(),
                        p.getClass());
            }
        }
        
        log.info("{} None of the potential authentication flows can satisfy the request", getLogPrefix());
        ActionSupport.buildEvent(profileRequestContext,
                authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE :
                    (noProxying ? AuthnEventIds.PROXY_COUNT_EXCEEDED : AuthnEventIds.REQUEST_UNSUPPORTED));
    }
// Checkstyle: CyclomaticComplexity ON
    
    
// Checkstyle: MethodLength|CyclomaticComplexity|ReturnCount OFF
    /**
     * Selects a flow or an active result in the presence of specific requested Principals and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     * @param activeResults active results that may be reused
     */
    private void selectRequestedFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final Map<String,AuthenticationResult> activeResults) {

        final RequestedPrincipalContext rpCtx = requestedPrincipalCtx;
        assert rpCtx != null;

        if (favorSSO) {
            log.debug("{} Giving priority to active results that meet request requirements");
            
            // Check each active result for compatibility with request.
            for (final Principal p : rpCtx.getRequestedPrincipals()) {
                log.debug("{} Checking for an active result compatible with operator '{}' and principal '{}'",
                        getLogPrefix(), rpCtx.getOperator(), p.getName());
                final PrincipalEvalPredicate predicate = rpCtx.getPredicate(p);
                if (predicate != null) {
                    for (final AuthenticationResult result : activeResults.values()) {
                        if (result.test(profileRequestContext) &&
                                predicate.test(result)) {
                            selectActiveResult(profileRequestContext, authenticationContext, result);
                            return;
                        }
                        log.debug("{} Active result for flow {} not usable, ignoring", getLogPrefix(),
                                result.getAuthenticationFlowId());
                    }
                } else {
                    log.warn("{} Configuration does not support requested principal evaluation with "
                            + "operator '{}' and type '{}'", getLogPrefix(), rpCtx.getOperator(),
                            p.getClass());
                }
            }
            
            // We know at this point there are no active results that will fit, so drop into inactive.
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
            return;

        }
        
        final Map<String,AuthenticationFlowDescriptor> potentialFlows = authenticationContext.getPotentialFlows();

        // In this branch, we check each flow for compatibility *and* then double check to see if an active
        // result from that flow also exists and is compatible. This favors a matching inactive flow that is
        // higher in request precedence than an active result.
        for (final Principal p : rpCtx.getRequestedPrincipals()) {
            log.debug("{} Checking for an inactive flow or active result compatible with "
                    + "operator '{}' and principal '{}'", getLogPrefix(), rpCtx.getOperator(),
                    p.getName());
            final PrincipalEvalPredicate predicate = rpCtx.getPredicate(p);
            if (predicate != null) {
                for (final AuthenticationFlowDescriptor descriptor : potentialFlows.values()) {
                    if (!authenticationContext.getIntermediateFlows().containsKey(descriptor.ensureId())
                            && predicate.test(descriptor) && descriptor.test(profileRequestContext)) {
                        
                        // Now check for an active result we can use from this flow. Not all results from a flow
                        // will necessarily match the request just because the flow might.
                        final AuthenticationResult result = activeResults.get(descriptor.ensureId());
                        if (result == null || !result.test(profileRequestContext)
                                || !predicate.test(result)) {
                            if (result != null) {
                                log.debug("{} Active result for flow {} not usable, ignoring", getLogPrefix(),
                                        result.getAuthenticationFlowId());
                            }
                            if (!authenticationContext.isPassive() || descriptor.isPassiveAuthenticationSupported()) {
                                if (!noProxying || !descriptor.isProxyScopingEnforced()) {
                                    selectInactiveFlow(profileRequestContext, authenticationContext, descriptor);
                                    return;
                                }
                                log.debug("{} Flow '{}' disallowed by effective proxy count of zero", getLogPrefix(),
                                        descriptor.getId());
                            }
                        } else {
                            selectActiveResult(profileRequestContext, authenticationContext, result);
                            return;
                        }
                    }
                }
            } else {
                log.warn("{} Configuration does not support requested principal evaluation with "
                        + "operator '{}' and type '{}'", getLogPrefix(), rpCtx.getOperator(),
                        p.getClass());
            }
        }
        
        log.info("{} None of the potential authentication flows can satisfy the request", getLogPrefix());
        ActionSupport.buildEvent(profileRequestContext,
                authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE :
                    (noProxying ? AuthnEventIds.PROXY_COUNT_EXCEEDED : AuthnEventIds.REQUEST_UNSUPPORTED));
    }
// Checkstyle: MethodLength|CyclomaticComplexity|ReturnCount ON
        
}

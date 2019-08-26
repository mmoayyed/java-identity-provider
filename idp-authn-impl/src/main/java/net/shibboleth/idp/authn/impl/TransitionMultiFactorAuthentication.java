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
import net.shibboleth.idp.profile.context.navigate.WebFlowCurrentEventLookupFunction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An authentication action that acts as the master evaluation step regulating execution
 * of transitions between MFA stages.
 * 
 * <p>This is the heart of the MFA processing sequence, and runs after the
 * {@link MultiFactorAuthenticationContext} has been populated. It uses the current/previous
 * flow and the transition rules to decide when to transition to a new flow, when work is
 * complete, and the final event to signal in the event of a problem.</p>
 * 
 * <p>The execution of this function is driven by the {@link MultiFactorAuthenticationTransition}
 * rule associated with the flow that was most recently executed by this engine. If none (such as
 * during the first iteration), then the rule associated with a null flow ID is used. Failure to
 * locate a transition to use is fatal, resulting in {@link AuthnEventIds#NO_PASSIVE} or
 * {@link AuthnEventIds#NO_POTENTIAL_FLOW}.</p>
 * 
 * <p>Otherwise, a function is applied to obtain the "current" WebFlow event, and the event
 * is applied to the transition's rule map to obtain the name of the next flow to run. A
 * wildcard ('*') rule is used if a more specific rule isn't found.</p>
 * 
 * <p>If the transition signals a null/empty flow ID to run, then
 * {@link MultiFactorAuthenticationContext#getNextFlowId()} is cleared to signal the MFA flow
 * that it should complete itself. The result of the action is either
 * {@link MultiFactorAuthenticationContext#getEvent()} (if set), or the current WebFlow event.</p>
 * 
 * <p>If a flow is returned, it is populated into the {@link MultiFactorAuthenticationContext}.
 * The flow is checked for the "authn/" prefix, and a login flow is checked against the
 * active result map to determine if it can be reused, in which case the action recurses itself.
 * Otherwise {@link EventIds#PROCEED_EVENT_ID}is signaled to run that flow.</p>
 * 
 * <p>By default, login flow transitions are validated against the request's requirements
 * in terms of passive, forced re-authn, and non-browser compatibility.</p>
 * 
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getSubcontext(
 *      MultiFactorAuthenticationContext.class) != null</pre>
 * @post See above.
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#NO_PASSIVE}
 * @event {@link AuthnEventIds#NO_POTENTIAL_FLOW}
 * @event {@link AuthnEventIds#REQUEST_UNSUPPORTED}
 * @event (any event signaled by another called flow)
 */
public class TransitionMultiFactorAuthentication extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TransitionMultiFactorAuthentication.class);

    /** Lookup function for the context to evaluate. */
    @Nonnull
    private Function<ProfileRequestContext,MultiFactorAuthenticationContext> multiFactorContextLookupStrategy;
    
    /** Lookup function for current event context. */
    @Nonnull private Function<ProfileRequestContext,EventContext> eventContextLookupStrategy;
    
    /** Perform IsPassive, ForceAuthn, and non-browser checks when running login flows. */
    private boolean validateLoginTransitions;

    /** A subordinate {@link MultiFactorAuthenticationContext}, if any. */
    @Nullable private MultiFactorAuthenticationContext mfaContext;

    /** Constructor. */
    TransitionMultiFactorAuthentication() {
        multiFactorContextLookupStrategy =
                new ChildContextLookup<>(MultiFactorAuthenticationContext.class).compose(
                        new ChildContextLookup<>(AuthenticationContext.class));
        
        eventContextLookupStrategy = new WebFlowCurrentEventLookupFunction();
        
        validateLoginTransitions = true;
    }

    /**
     * Set the lookup strategy to use for the context to evaluate.
     * 
     * @param strategy lookup strategy
     */
    public void setMultiFactorContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,MultiFactorAuthenticationContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        multiFactorContextLookupStrategy = Constraint.isNotNull(strategy,
                "MultiFactorAuthenticationContext lookup strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy to use for the current event context.
     * 
     * @param strategy lookup strategy
     */
    public void setEventContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,EventContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        eventContextLookupStrategy = Constraint.isNotNull(strategy, "EventContext lookup strategy cannot be null");
    }
    
    /**
     * Set whether to validate transitions to a new login flow by evaluating the request
     * and ensuring options like IsPassive and ForceAuthn are compatible with the flow.
     * 
     * <p>Defaults to 'true', override if your custom transition logic handles these issues.</p>
     * 
     * @param flag flag to set
     */
    public void setValidateLoginTransitions(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        validateLoginTransitions = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        mfaContext = multiFactorContextLookupStrategy.apply(profileRequestContext);
        if (mfaContext == null) {
            log.error("{} No MultiFactorAuthenticationContext found by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        return true;
    }


// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        // Swap MFA flow back into top-level context so that other components see only MFA flow.
        authenticationContext.setAttemptedFlow(mfaContext.getAuthenticationFlowDescriptor());

        // Event transitions require normalizing empty/null events into "proceed".
        final EventContext eventCtx = eventContextLookupStrategy.apply(profileRequestContext);
        final String previousEvent = eventCtx != null && eventCtx.getEvent() != null
                ? eventCtx.getEvent().toString() : EventIds.PROCEED_EVENT_ID;

        // Check for an authentication result and move it into the MFA context.
        final AuthenticationResult result = authenticationContext.getAuthenticationResult();
        if (result != null) {
            if (EventIds.PROCEED_EVENT_ID.equals(previousEvent)) {                
                log.debug("{} Preserving authentication result from '{}' flow", getLogPrefix(),
                        result.getAuthenticationFlowId());
                mfaContext.getActiveResults().put(result.getAuthenticationFlowId(), result);
            } else {
                log.debug("{} Discarding incomplete authentication result from '{}' flow", getLogPrefix(),
                        result.getAuthenticationFlowId());
            }
            authenticationContext.setAuthenticationResult(null);
        }

        // The "next" flow here is the "previous" flow run by the system that we're branching from.
        // This value can be null (on the first entry) and a rule should be defined for the null value.
        final String prevFlowId = mfaContext.getNextFlowId();
        mfaContext.setNextFlowId(null);        
        if (prevFlowId == null) {
            log.debug("{} Applying MFA transition rule to determine initial state", getLogPrefix());
        } else {
            log.debug("{} Applying MFA transition rule to exit state '{}'", getLogPrefix(), prevFlowId);
        }

        String flowId = null;
        final MultiFactorAuthenticationTransition transition = mfaContext.getTransitionMap().get(prevFlowId);
        if (transition != null) {
            flowId = transition.getNextFlowStrategy(previousEvent).apply(profileRequestContext);
            if (flowId == null) {
                flowId = transition.getNextFlowStrategy("*").apply(profileRequestContext);
            }
        }
        if (flowId != null) {
            log.debug("{} MFA flow transition after '{}' event to '{}' flow", getLogPrefix(), previousEvent, flowId);
            mfaContext.setNextFlowId(flowId);
            doTransition(profileRequestContext, authenticationContext, transition);
        } else {
            final String event = mfaContext.getEvent() != null ? mfaContext.getEvent() : previousEvent;
            log.debug("{} MFA flow completing with event '{}'", getLogPrefix(), event);
            if (EventIds.PROCEED_EVENT_ID.equals(event)) {
                ActionSupport.buildProceedEvent(profileRequestContext);
            } else {
                ActionSupport.buildEvent(profileRequestContext, event);
            }
        }
    }
// Checkstyle: CyclomaticComplexity ON

// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /**
     * Respond to a signal to transition the MFA process to a new flow.
     * 
     * @param profileRequestContext profile request context
     * @param authenticationContext authentication context
     * @param transition transition rule to use
     */
    private void doTransition(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final MultiFactorAuthenticationTransition transition) {
        
        // Non-authentication flows can just be executed (via a "proceed" event).
        final String flowId = mfaContext.getNextFlowId();
        if (!flowId.startsWith("authn/")) {
            ActionSupport.buildProceedEvent(profileRequestContext);
            return;
        }

        final AuthenticationFlowDescriptor flow = authenticationContext.getAvailableFlows().get(flowId);
        if (flow == null) {
            log.error("{} Targeted login flow '{}' is not configured, check available flow descriptors",
                    getLogPrefix(), flowId);
            ActionSupport.buildEvent(profileRequestContext, authenticationContext.isPassive() ?
                    AuthnEventIds.NO_PASSIVE : AuthnEventIds.NO_POTENTIAL_FLOW);
            return;
        }
        
        // We have to check for an active result that would bypass re-running it. A ForceAuthn
        // constraint is assumed to be enforced by limiting which active results are made available.
        // To bypass, we just call ourselves again, implicitly looping back. The protection against
        // infinite recursion is the configuration of transitions supplied by the deployer.
        final AuthenticationResult activeResult = mfaContext.getActiveResults().get(flowId);
        if (activeResult != null) {
            if (flow.getReuseCondition().test(profileRequestContext)) {
                log.debug("{} Reusing active result for '{}' flow", getLogPrefix(), flowId);
                activeResult.setLastActivityInstantToNow();
                ActionSupport.buildProceedEvent(profileRequestContext);
                doExecute(profileRequestContext, authenticationContext);
                return;
            }
            log.debug("{} Condition blocked reuse of active result for '{}' flow", getLogPrefix(), flowId);
            mfaContext.getActiveResults().remove(flowId);
        }
     
        if (validateLoginTransitions) {
            if (authenticationContext.isPassive() && !flow.isPassiveAuthenticationSupported()) {
                log.error("{} Targeted login flow '{}' does not support passive authentication",
                        getLogPrefix(), flowId);
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_PASSIVE);
                return;
            } else if ((authenticationContext.isForceAuthn() || authenticationContext.getMaxAge() != null)
                    && !flow.isForcedAuthenticationSupported()) {
                log.error("{} Targeted login flow '{}' does not support forced re-authentication",
                        getLogPrefix(), flowId);
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
                return;
            } else if (!profileRequestContext.isBrowserProfile() && !flow.isNonBrowserSupported()) {
                log.error("{} Targeted login flow '{}' does not support non-browser authentication",
                        getLogPrefix(), flowId);
                ActionSupport.buildEvent(profileRequestContext, authenticationContext.isPassive() ?
                        AuthnEventIds.NO_PASSIVE : AuthnEventIds.REQUEST_UNSUPPORTED);
                return;
            }
        }
        
        // Set for compatibility with more standard runs of a login flow at the top level.
        authenticationContext.setAttemptedFlow(flow);
        ActionSupport.buildProceedEvent(profileRequestContext);
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON
    
}
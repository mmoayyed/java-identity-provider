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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;

/**
 * An authentication action that acts as the master evaluation step regulating execution
 * of transitions between MFA stages.
 * 
 * <p>This is the heart of the MFA processing sequence, and runs after the
 * {@link MultiFactorAuthenticationContext} has been fully populated. It uses the current/previous
 * flow, the transition and completion rules, and merging function to decide when to transition to
 * a new flow, when work is complete, and how to produce the final result.</p>
 * 
 * <p>The execution of this function is driven by the {@link MultiFactorAuthenticationTransition}
 * rule associated with the flow that was most recently executed by this engine. If none (such as
 * during the first iteration), then the rule associated with a null value is used. Failure to locate
 * a transition to use is fatal, resulting in {@link AuthnEventIds#NO_PASSIVE} or
 * {@link AuthnEventIds#NO_POTENTIAL_FLOW}.</p>
 * 
 * <p>If the transition signals completion, then the associated merging function is used to
 * produce a final {@link AuthenticationResult} and the context tree is mutated to store off
 * the result and prepare for subject canonicalization, in the fashion of most validation
 * actions. The {@link MultiFactorAutenticationContext}'s next flow is cleared and
 * {@link EventIds#PROCEED_EVENT_ID} is signaled.</p>
 * 
 * <p>Otherwise, a function is applied to obtain the "current" WebFlow event, and the event
 * is applied to the transition's rule map to obtain the name of the next flow to run. A
 * wildcard ('*') rule is used if a more specific rule isn't found. If no flow is returned,
 * then the current event is re-signaled as the result of this action, and ultimately the
 * result of the MFA flow.</p>
 * 
 * <p>If a flow is returned, it is populated into the {@link MultiFactorAutenticationContext}.
 * The flow is checked for the "authn/" prefix, and a login flow is checked against the
 * active result map to determine if it can be reused, in which case
 * {@link AuthnEventIds#RESELECT_FLOW} is signaled to recurse the process. Otherwise
 * {@link EventIds#PROCEED_EVENT_ID}is signaled to run the flow.</p>
 * 
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getSubcontext(
 *      MultiFactorAuthenticationContext.class) != null</pre>
 * @post See above.
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#NO_PASSIVE}
 * @event {@link AuthnEventIds#NO_POTENTIAL_FLOW}
 * @event {@link AuthnEventIds#RESELECT_FLOW}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 */
public class TransitionMultiFactorAuthentication extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TransitionMultiFactorAuthentication.class);

    /** Lookup function for the context to evaluate. */
    @Nonnull
    private Function<ProfileRequestContext,MultiFactorAuthenticationContext> multiFactorContextLookupStrategy;
    
    /** Lookup function for current event context. */
    @Nonnull private Function<ProfileRequestContext,EventContext> eventContextLookupStrategy;

    /** Predicate to apply when setting AuthenticationResult cacheability. */
    @Nullable private Predicate<ProfileRequestContext> resultCachingPredicate;

    /** Function used to obtain the requester ID. */
    @Nullable private Function<ProfileRequestContext,String> requesterLookupStrategy;

    /** Function used to obtain the responder ID. */
    @Nullable private Function<ProfileRequestContext,String> responderLookupStrategy;

    /** A subordinate {@link MultiFactorAuthenticationContext}, if any. */
    @Nullable private MultiFactorAuthenticationContext mfaContext;

    /** Constructor. */
    TransitionMultiFactorAuthentication() {
        multiFactorContextLookupStrategy = Functions.compose(
                new ChildContextLookup(MultiFactorAuthenticationContext.class),
                new ChildContextLookup(AuthenticationContext.class));
        
        eventContextLookupStrategy = new WebFlowCurrentEventLookupFunction();
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
     * Set predicate to apply to determine cacheability of {@link AuthenticationResult}.
     * 
     * @param predicate predicate to apply, or null
     */
    public void setResultCachingPredicate(@Nullable final Predicate<ProfileRequestContext> predicate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        resultCachingPredicate = predicate;
    }

    /**
     * Set the strategy used to locate the requester ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setRequesterLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        requesterLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate the responder ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setResponderLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responderLookupStrategy = strategy;
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
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        // Check for an authentication result and move it into the MFA context.
        final AuthenticationResult result = authenticationContext.getAuthenticationResult();
        if (result != null) {
            log.debug("{} Preserving authentication result from '{}' flow", getLogPrefix(),
                    result.getAuthenticationFlowId());
            mfaContext.getActiveResults().put(result.getAuthenticationFlowId(), result);
            authenticationContext.setAuthenticationResult(null);
        }

        // The "next" flow here is the "previous" flow run by the system that we're branching from.
        // This value can be null (on the first entry) and a rule should be defined for the null value.
        log.debug("{} Accessing MFA transition rule for exiting state {}", getLogPrefix(), mfaContext.getNextFlowId());
        final MultiFactorAuthenticationTransition transition = mfaContext.getTransitionMap().get(
                mfaContext.getNextFlowId());
        if (transition == null) {
            log.error("{} Unable to locate an MFA transition rule to exit state {}", getLogPrefix(),
                    mfaContext.getNextFlowId());
            ActionSupport.buildEvent(profileRequestContext, authenticationContext.isPassive() ? AuthnEventIds.NO_PASSIVE
                    : AuthnEventIds.NO_POTENTIAL_FLOW);
            return;
        }
        
        log.debug("{} Checking for MFA completion", getLogPrefix());
        if (transition.getCompletionCondition().apply(profileRequestContext)) {
            doCompletion(profileRequestContext, authenticationContext, transition);
            return;
        }
        
        // Event transitions require normalizing empty/null events into "proceed".
        final EventContext eventCtx = eventContextLookupStrategy.apply(profileRequestContext);
        final String previousEvent = eventCtx != null && eventCtx.getEvent() != null
                ? eventCtx.getEvent().toString() : EventIds.PROCEED_EVENT_ID;
        log.debug("{} MFA flow incomplete, checking for transition for '{}' event", getLogPrefix(), previousEvent);
        String flowId = transition.getNextFlowStrategy(previousEvent).apply(profileRequestContext);
        if (flowId == null) {
            flowId = transition.getNextFlowStrategy("*").apply(profileRequestContext);
        }
        if (flowId != null) {
            log.debug("{} MFA flow transition from '{}' event to '{}' flow", getLogPrefix(), previousEvent, flowId);
            mfaContext.setNextFlowId(flowId);
            doTransition(profileRequestContext, authenticationContext);
            return;
        }
        
        log.debug("{} No transition, MFA flow completing with '{}' event", getLogPrefix(), previousEvent);
        ActionSupport.buildEvent(profileRequestContext, previousEvent);
    }
        
    /**
     * Respond to a signal to complete the MFA process by computing a merged result and
     * preparing the context tree for subject c14n.
     * 
     * @param profileRequestContext profile request context
     * @param authenticationContext authentication context
     * @param transition transition rule to use for finalization
     */
    private void doCompletion(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final MultiFactorAuthenticationTransition transition) {
        
        log.debug("{} MFA complete, producing merged result", getLogPrefix());
        final AuthenticationResult result = transition.getResultMergingStrategy().apply(profileRequestContext);
        if (result == null) {
            log.warn("{} Unable to produce merged AuthenticationResult to complete state {}", getLogPrefix(),
                    mfaContext.getNextFlowId());
            ActionSupport.buildEvent(profileRequestContext, authenticationContext.isPassive() ?
                    AuthnEventIds.NO_PASSIVE : AuthnEventIds.INVALID_CREDENTIALS);
            return;
        }
        
        authenticationContext.setAuthenticationResult(result);
        
        // Override cacheability if a predicate is installed.
        if (authenticationContext.isResultCacheable() && resultCachingPredicate != null) {
            authenticationContext.setResultCacheable(resultCachingPredicate.apply(profileRequestContext));
            log.info("{} Predicate indicates authentication result {} be cacheable in a session", getLogPrefix(),
                    authenticationContext.isResultCacheable() ? "will" : "will not");
        }
        
        // Transfer the subject to a new c14n context.
        final SubjectCanonicalizationContext c14n = new SubjectCanonicalizationContext();
        c14n.setSubject(result.getSubject());
        if (requesterLookupStrategy != null) {
            c14n.setRequesterId(requesterLookupStrategy.apply(profileRequestContext));
        }
        if (responderLookupStrategy != null) {
            c14n.setResponderId(responderLookupStrategy.apply(profileRequestContext));
        }
        profileRequestContext.addSubcontext(c14n, true);

        mfaContext.setNextFlowId(null);
        ActionSupport.buildProceedEvent(profileRequestContext);
    }

    /**
     * Respond to a signal to transition the MFA process to a new flow.
     * 
     * @param profileRequestContext profile request context
     * @param authenticationContext authentication context
     */
    private void doTransition(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        // Non-authentication flows can just be executed (via a "proceed" event), but a login flow is a
        // special case. We have to check for an active result that would bypass re-running it. The ForceAuthn
        // constraint is assumed to be enforced by limiting which active results are made available.
        final String flowId = mfaContext.getNextFlowId();
        if (flowId.startsWith("authn/")) {
            if (mfaContext.getActiveResults().containsKey(flowId)) {
                log.debug("{} Reusing active result for '{}' flow", getLogPrefix(), flowId);
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.RESELECT_FLOW);
                return;
            }
        }
        
        ActionSupport.buildProceedEvent(profileRequestContext);
    }

}
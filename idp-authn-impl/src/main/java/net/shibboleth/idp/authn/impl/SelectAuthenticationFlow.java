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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An authentication action that selects an authentication flow to invoke, or reuses an
 * existing result for SSO.
 * 
 * <p>This is the heart of the authentication processing sequence, and runs after the
 * {@link AuthenticationContext} has been fully populated. It uses the potential flows,
 * the requested flows (if any), and the active results, to decide how to proceed.</p>
 * 
 * <p>If there are no requested flows, then an active result will be reused, unless
 * the request requires forced authentication. If not possible, then a potential flow
 * will be selected and its ID returned as the result of the action.</p>
 * 
 * <p>If there are requested flows, then the "favorSSO" option determines whether
 * to select a flow specifically in the order specified, or to favor an active, but
 * "qualifying" result, over a new one. Forced authentication trumps the
 * use of any active result.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID} (reuse of a result)
 * @event {@link AuthnEventIds#NO_POTENTIAL_FLOW}
 * @event {@link AuthnEventIds#NO_REQUESTED_FLOW}
 * @event Selected flow ID to execute
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If a result is reused, {@link AuthenticationContext#getAuthenticationResult()} will return
 * that result. Otherwise, {@link AuthenticationContext#getAttemptedFlow()} will return the flow
 * selected for execution and returned as an event.
 */
public class SelectAuthenticationFlow extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SelectAuthenticationFlow.class);

    /** Whether SSO trumps explicit relying party flow preference. */
    private boolean favorSSO;
    
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
    public void setFavorSSO(boolean flag) {
        favorSSO = flag;
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

        log.debug("{} reusing active result {}", getLogPrefix(), result.getAuthenticationFlowId());
        result.setLastActivityInstantToNow();
        authenticationContext.setAuthenticationResult(result);
        ActionSupport.buildProceedEvent(profileRequestContext);
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

        log.debug("{} selecting inactive authentication flow {}", getLogPrefix(), descriptor.getId());
        authenticationContext.setAttemptedFlow(descriptor);
        ActionSupport.buildEvent(profileRequestContext, descriptor.getId());
    }

    /**
     * Selects an inactive flow based on the requested flows and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void selectRequestedInactiveFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        Map<String,AuthenticationFlowDescriptor> potentialFlows = authenticationContext.getPotentialFlows();
        
        for (AuthenticationFlowDescriptor descriptor : authenticationContext.getRequestedFlows()) {
            if (potentialFlows.containsKey(descriptor.getId())) {
                selectInactiveFlow(profileRequestContext, authenticationContext, descriptor);
                return;
            }
        }
        
        log.info("{} none of the potential authentication flows can satisfy the request", getLogPrefix());
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_REQUESTED_FLOW);
    }
    
    /**
     * Selects a flow or an active result based on the requested flows and completes processing.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void selectRequestedFlow(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (favorSSO) {
            log.debug("{} giving priority to active results that meet requested requirements");
            
            Map<String,AuthenticationResult> activeResults = authenticationContext.getActiveResults();
            for (AuthenticationFlowDescriptor descriptor : authenticationContext.getRequestedFlows()) {
                if (activeResults.containsKey(descriptor.getId())) {
                    selectActiveResult(profileRequestContext, authenticationContext,
                            activeResults.get(descriptor.getId()));
                    return;
                }
            }
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
            return;
            
        } else {
            Map<String,AuthenticationResult> activeResults = authenticationContext.getActiveResults();
            Map<String,AuthenticationFlowDescriptor> potentialFlows = authenticationContext.getPotentialFlows();
            
            for (AuthenticationFlowDescriptor descriptor : authenticationContext.getRequestedFlows()) {
                if (activeResults.containsKey(descriptor.getId())) {
                    selectActiveResult(profileRequestContext, authenticationContext,
                            activeResults.get(descriptor.getId()));
                    return;
                } else if (potentialFlows.containsKey(descriptor.getId())) {
                    selectInactiveFlow(profileRequestContext, authenticationContext, descriptor);
                    return;
                }
            }
            
            log.info("{} none of the potential authentication flows can satisfy the request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_REQUESTED_FLOW);
        }
    }

    /**
     * Executes the selection process in the absence of specific requested flows.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void doNoRequestedFlows(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        log.debug("{} no specific flows requested", getLogPrefix());
        
        if (authenticationContext.isForceAuthn()) {
            log.debug("{} forced authentication requested, selecting an inactive flow", getLogPrefix());
            selectInactiveFlow(profileRequestContext, authenticationContext,
                    authenticationContext.getPotentialFlows().values().iterator().next());
        } else if (authenticationContext.getActiveResults().isEmpty()) {
            log.debug("{} no active results available, selecting an inactive flow", getLogPrefix());
            selectInactiveFlow(profileRequestContext, authenticationContext,
                    authenticationContext.getPotentialFlows().values().iterator().next());
        } else {
            // Pick a result to reuse.
            selectActiveResult(profileRequestContext, authenticationContext,
                    authenticationContext.getActiveResults().values().iterator().next());
        }
    }

    /**
     * Executes the selection process in the presence of specific requested flows.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @param authenticationContext the current authentication context
     */
    private void doRequestedFlows(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        final List<AuthenticationFlowDescriptor> requestedFlows = authenticationContext.getRequestedFlows();
        log.debug("{} requested flows: {}", getLogPrefix(), requestedFlows);
        
        if (authenticationContext.isForceAuthn()) {
            log.debug("{} forced authentication requested, selecting an inactive flow", getLogPrefix());
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
        } else if (authenticationContext.getActiveResults().isEmpty()) {
            log.debug("{} no active results available, selecting an inactive flow", getLogPrefix());
            selectRequestedInactiveFlow(profileRequestContext, authenticationContext);
        } else {
            selectRequestedFlow(profileRequestContext, authenticationContext);
        }
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (authenticationContext.getPotentialFlows().isEmpty()) {
            log.debug("{} no potential workflows available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_POTENTIAL_FLOW);
        }

        log.debug("{} selecting authentication flow from the following potential flows: {}", getLogPrefix(),
                authenticationContext.getPotentialFlows().keySet());

        if (authenticationContext.getRequestedFlows().isEmpty()) {
            doNoRequestedFlows(profileRequestContext, authenticationContext);
        } else {
            doRequestedFlows(profileRequestContext, authenticationContext);
        }
    }
    
}
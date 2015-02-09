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

package net.shibboleth.idp.session.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.LogoutPropagationFlowDescriptor;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * A profile action that selects a logout propagation flow to invoke.
 * 
 * <p>This is the heart of the logout propagation processing sequence, and runs after the
 * {@link net.shibboleth.idp.session.context.LogoutContext} has been populated. It uses the potential flows,
 * and their associated activation conditions to decide how to proceed.</p>
 * 
 * <p>This is a rare case in that the standard default event,
 * {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}, cannot be returned,
 * because the action must either dispatch to a flow by name, or signal an error.</p>
 * 
 * @event {@link AuthnEventIds#NO_POTENTIAL_FLOW}
 * @event Selected flow ID to execute
 */
public class SelectLogoutPropagationFlow extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectLogoutPropagationFlow.class);
    
    /** The available flows for use. */
    @Nonnull @NonnullElements private List<LogoutPropagationFlowDescriptor> availableFlows;

    /** Constructor. */
    public SelectLogoutPropagationFlow() {
        availableFlows = Collections.emptyList();
    }
    
    /**
     * Set the available flows to choose from.
     * 
     * @param flows available flows
     */
    public void setAvailableFlows(@Nonnull @NonnullElements final List<LogoutPropagationFlowDescriptor> flows) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(flows, "Available flow list cannot be null");
        
        availableFlows = new ArrayList<>(Collections2.filter(flows, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final LogoutPropagationFlowDescriptor flow = selectUnattemptedFlow(profileRequestContext);
        if (flow == null) {
            log.error("{} No potential flows to choose from, logout propagation will fail", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_POTENTIAL_FLOW);
            return;
        }

        log.debug("{} Selecting logout propagation flow {}", getLogPrefix(), flow.getId());
        ActionSupport.buildEvent(profileRequestContext, flow.getId());
    }

    /**
     * Select the first potential flow that is applicable to the context.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @return an eligible flow, or null
     */
    @Nullable private LogoutPropagationFlowDescriptor selectUnattemptedFlow(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        for (final LogoutPropagationFlowDescriptor flow : availableFlows) {
            log.debug("{} Checking logout propagation flow {} for applicability...", getLogPrefix(), flow.getId());
            if (flow.apply(profileRequestContext)) {
                return flow;
            }
            log.debug("{} Logout propagation flow {} was not applicable to this request", getLogPrefix(),
                    flow.getId());
        }
        
        return null;
    }
        
}
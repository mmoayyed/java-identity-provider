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

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationWorkflowDescriptor;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * An authentication action that selects an authentication workflow to invoke.
 * <p>
 * This action selects the workflow from the potential workflows given by
 * {@link AuthenticationContext#getPotentialWorkflows()}. It will first look to see if any active workflows, as
 * given by {@link AuthenticationContext#getActiveWorkflows()}, are potential workflows and if so will use one.
 * Otherwise it will simply pick on of the inactive potential workflows.
 * </p>
 * <p>
 * The event ID returned by this stage may either be {@link AuthnEventIds#NO_POTENTIAL_WORKFLOW} or the ID of the chosen
 * authentication workflow.
 * </p>
 */
public class SelectAuthenticationWorkflow extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SelectAuthenticationWorkflow.class);

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        final Map<String, AuthenticationWorkflowDescriptor> potentialWorkflows =
                authenticationContext.getPotentialWorkflows();
        if (potentialWorkflows.isEmpty()) {
            log.debug("Action {}: no potential workflows available");
            return ActionSupport.buildEvent(this, AuthnEventIds.NO_POTENTIAL_WORKFLOW);
        }

        log.debug("Action {}: selecting authentication workflow from the following potential workflows: {}", getId(),
                potentialWorkflows.keySet());

        AuthenticationWorkflowDescriptor descriptor;

        final Set<String> activeWorkflowIds = authenticationContext.getActiveWorkflows().keySet();
        if (!activeWorkflowIds.isEmpty()) {
            log.debug("Action {}: checking if any of the following active authentication workflows is suitable: {}",
                    getId(), activeWorkflowIds);
            for (String activeWorkflowId : activeWorkflowIds) {
                descriptor = potentialWorkflows.get(activeWorkflowId);
                if (descriptor != null) {
                    log.debug("Action {}: selecting active authentication workflow {}", getId(),
                            descriptor.getId());
                    authenticationContext.setAttemptedWorkflow(descriptor);
                    return ActionSupport.buildEvent(this, descriptor.getId());
                }
            }
        }

        log.debug("Action {}: no active workflows is suitable, selecting inactive workflow", getId());

        descriptor = potentialWorkflows.values().iterator().next();
        log.debug("Action {}: selecting inactive authentication workflow {}", getId(), descriptor.getId());
        authenticationContext.setAttemptedWorkflow(descriptor);
        return ActionSupport.buildEvent(this, descriptor.getId());
    }
}
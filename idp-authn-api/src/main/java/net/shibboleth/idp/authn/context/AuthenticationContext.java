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

package net.shibboleth.idp.authn.context;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.authn.AuthenticationEvent;
import net.shibboleth.idp.authn.AuthenticationWorkflowDescriptor;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/** A context representing the state of an authentication attempt. */
@ThreadSafe
public final class AuthenticationContext extends BaseContext {

    /** Time, in milliseconds since the epoch, when the authentication process started. */
    private final long initiationInstant;

    /** Whether to require fresh subject interaction to succeed. */
    private boolean forceAuthn;

    /** Whether authentication must not involve subject interaction. */
    private boolean isPassive;

    /** Authentication workflows configured and available for use. */
    @Nonnull private ImmutableMap<String, AuthenticationWorkflowDescriptor> activeWorkflows;
    
    /** Authentication workflows configured and available for use. */
    @Nonnull private final ImmutableMap<String, AuthenticationWorkflowDescriptor> availableWorkflows;

    /** Workflows that might potentially be used to authenticate the user. */
    @Nonnull private Map<String, AuthenticationWorkflowDescriptor> potentialWorkflows;

    /** Authentication workflows, in order of preference, requested by the relying party. */
    @Nonnull private Map<String, AuthenticationWorkflowDescriptor> requestedWorkflows;

    /** Authentication workflow that was attempted in order to authenticate the user. */
    @Nullable private AuthenticationWorkflowDescriptor attemptedWorkflow;

    /** Time, in milliseconds since the epoch, when authentication process completed. */
    private long completionInstant;

    /**
     * Constructor.
     *
     * @param availableFlows authentication workflows currently available
     */
    public AuthenticationContext(
            @Nullable @NonnullElements final Collection<AuthenticationWorkflowDescriptor> availableFlows) {
        super();

        initiationInstant = System.currentTimeMillis();

        final Builder<String, AuthenticationWorkflowDescriptor> flowsBuilder = new ImmutableMap.Builder<>();
        if (availableFlows != null) {
            for (AuthenticationWorkflowDescriptor descriptor : availableFlows) {
                flowsBuilder.put(descriptor.getId(), descriptor);
            }
        }
        availableWorkflows = flowsBuilder.build();
        potentialWorkflows = new HashMap<String, AuthenticationWorkflowDescriptor>(availableWorkflows);

        activeWorkflows = ImmutableMap.of();
        requestedWorkflows = Collections.emptyMap();
    }

    /**
     * Gets the time, in milliseconds since the epoch, when the authentication process started.
     * 
     * @return time when the authentication process started
     */
    public long getInitiationInstant() {
        return initiationInstant;
    }

    /**
     * Gets the authentication workflows currently active for the subject.
     * 
     * @return authentication workflows currently active for this user
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, AuthenticationWorkflowDescriptor> getActiveWorkflows() {
        return activeWorkflows;
    }

    /**
     * Sets the authentication workflows, in order of preference, that must be used if user authentication is required.
     * 
     * @param workflows authentication workflows, in order of preference, that must be used if user authentication is
     *            required
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setActiveWorkflows(
            @Nullable @NonnullElements final List<AuthenticationWorkflowDescriptor> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            activeWorkflows = ImmutableMap.of();
            return this;
        }

        Builder<String, AuthenticationWorkflowDescriptor> flowsBuilder = new ImmutableMap.Builder<>();
        for (AuthenticationWorkflowDescriptor descriptor : workflows) {
            flowsBuilder.put(descriptor.getId(), descriptor);
        }

        activeWorkflows = flowsBuilder.build();

        return this;
    }
    
    /**
     * Gets the authentication workflows currently configured and available.
     * 
     * @return authentication workflows currently configured and available
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, AuthenticationWorkflowDescriptor>
            getAvailableWorkflows() {
        return availableWorkflows;
    }

    /**
     * Gets the set of workflows that could potentially be used for user authentication. This collection initially
     * starts out with the same entries at {@link #availableWorkflows} but ongoing authentication request processing may
     * filter options out.
     * 
     * @return Returns the potentialWorkflows.
     */
    @Nonnull @Live public Map<String, AuthenticationWorkflowDescriptor> getPotentialWorkflows() {
        return potentialWorkflows;
    }

    /**
     * Get whether subject interaction is allowed.
     * 
     * @return whether subject interaction may occur
     */
    public boolean isPassive() {
        return isPassive;
    }

    /**
     * Set whether subject interaction is allowed.
     * 
     * @param passive whether subject interaction may occur
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setIsPassive(boolean passive) {
        isPassive = passive;
        return this;
    }

    /**
     * Get whether to require fresh subject interaction to succeed.
     * 
     * @return whether subject interaction must occur
     */
    public boolean isForceAuthn() {
        return forceAuthn;
    }

    /**
     * Set whether to require fresh subject interaction to succeed.
     * 
     * @param force whether subject interaction must occur
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setForceAuthn(boolean force) {
        forceAuthn = force;
        return this;
    }
    
    /**
     * Gets the unmodifiable list of authentication workflows, in order of preference, that must be used if user
     * authentication is required.
     * 
     * @return authentication workflows, in order of preference, that must be used if user authentication is required,
     *         never null nor containing null elements
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, AuthenticationWorkflowDescriptor>
            getRequestedWorkflows() {
        return requestedWorkflows;
    }

    /**
     * Sets the authentication workflows, in order of preference, that must be used if user authentication is required.
     * 
     * @param workflows authentication workflows, in order of preference, that must be used if user authentication is
     *            required
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setRequestedWorkflows(
            @Nullable @NonnullElements final List<AuthenticationWorkflowDescriptor> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            requestedWorkflows = Collections.emptyMap();
            return this;
        }

        Builder<String, AuthenticationWorkflowDescriptor> flowsBuilder = new ImmutableMap.Builder<>();
        for (AuthenticationWorkflowDescriptor descriptor : workflows) {
            flowsBuilder.put(descriptor.getId(), descriptor);
        }

        requestedWorkflows = flowsBuilder.build();

        return this;
    }

    /**
     * Gets the authentication workflow that was attempted in order to authenticate the user.
     * 
     * @return authentication workflow that was attempted in order to authenticate the user
     */
    @Nullable public AuthenticationWorkflowDescriptor getAttemptedWorkflow() {
        return attemptedWorkflow;
    }

    /**
     * Sets the authentication workflow that was attempted in order to authenticate the user.
     * 
     * @param workflow authentication workflow that was attempted in order to authenticate the user
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setAttemptedWorkflow(
            @Nullable final AuthenticationWorkflowDescriptor workflow) {
        attemptedWorkflow = workflow;
        return this;
    }

    /**
     * Gets the time, in milliseconds since the epoch, when the authentication process ended. A value of 0 indicates
     * that authentication has not yet completed.
     * 
     * @return time when the authentication process ended
     */
    public long getCompletionInstant() {
        return completionInstant;
    }

    /**
     * Sets the completion time of the authentication attempt to the current time.
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setCompletionInstant() {
        completionInstant = System.currentTimeMillis();
        return this;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("initiationInstant", new DateTime(initiationInstant))
                .add("availableWorkflows", availableWorkflows.keySet())
                .add("isPassive", isPassive).add("forceAuthn", forceAuthn)
                .add("requestedWorkflows", requestedWorkflows)
                .add("activeWorkflows", activeWorkflows.keySet())
                .add("completionInstant", new DateTime(completionInstant)).toString();
    }

    /**
     * Gets the currently active workflows based on the available workflows and the active session.
     * 
     * @param availableFlows currently available workflows
     * @param session currently active session
     * 
     * @return currently active workflows
     */
    @Nonnull @NonnullElements public ImmutableMap<String, AuthenticationWorkflowDescriptor> getActiveWorkflowsBySession(
            @Nonnull @NonnullElements final Map<String, AuthenticationWorkflowDescriptor> availableFlows,
            @Nonnull final IdPSession session) {
        final Builder<String, AuthenticationWorkflowDescriptor> activeFlowsBuilder = new ImmutableMap.Builder<>();

        AuthenticationWorkflowDescriptor descriptor;
        for (AuthenticationEvent event : session.getAuthenticateEvents()) {
            // TODO check if event is still active
            descriptor = availableFlows.get(event.getAuthenticationWorkflowId());
            if (descriptor == null) {
                continue;
            }
            activeFlowsBuilder.put(descriptor.getId(), descriptor);
        }

        return activeFlowsBuilder.build();
    }
}
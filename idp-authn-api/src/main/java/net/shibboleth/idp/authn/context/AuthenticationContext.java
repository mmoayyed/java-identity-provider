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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthenticationEvent;
import net.shibboleth.idp.authn.AuthenticationWorkflowDescriptor;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * A context representing the state of an authentication attempt, this is the primary
 * input/output context for the action flow responsible for authentication, and
 * within that flow, the individual workflows that carry out a specific kind of
 * authentication.
 */
public final class AuthenticationContext extends BaseContext {

    /** Time, in milliseconds since the epoch, when the authentication process started. */
    private final long initiationInstant;

    /** Whether to require fresh subject interaction to succeed. */
    private boolean forceAuthn;

    /** Whether authentication must not involve subject interaction. */
    private boolean isPassive;

    /** Authentication workflows associated with a preexisting session and available for (re)use. */
    @Nonnull @NonnullElements private ImmutableMap<String, AuthenticationWorkflowDescriptor> activeWorkflows;
    
    /** Workflows that could potentially be used to authenticate the user. */
    @Nonnull @NonnullElements private Map<String, AuthenticationWorkflowDescriptor> potentialWorkflows;

    /** Workflows, in order of preference, that satisfy an explicit requirement from the relying party. */
    @Nonnull @NonnullElements private ImmutableMap<String, AuthenticationWorkflowDescriptor> requestedWorkflows;

    /** Authentication workflow being attempted to authenticate the user. */
    @Nullable private AuthenticationWorkflowDescriptor attemptedWorkflow;

    /** A successfully processed authentication event (the output). */
    @Nullable private AuthenticationEvent authenticationResult;
    
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
        
        potentialWorkflows = new HashMap<>();

        if (availableFlows != null) {
            for (AuthenticationWorkflowDescriptor descriptor : availableFlows) {
                potentialWorkflows.put(descriptor.getId(), descriptor);
            }
        }

        activeWorkflows = ImmutableMap.of();
        requestedWorkflows = ImmutableMap.of();
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
     * @return authentication workflows currently active for the subject
     */
    @Nonnull @NonnullElements @Unmodifiable
    public Map<String, AuthenticationWorkflowDescriptor> getActiveWorkflows() {
        return activeWorkflows;
    }

    /**
     * Sets the authentication workflows currently active for the subject.
     * 
     * @param workflows authentication workflows currently active for the subject
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setActiveWorkflows(
            @Nonnull @NonnullElements final List<AuthenticationWorkflowDescriptor> workflows) {
        if (Constraint.isNotNull(workflows, "Workflow list cannot be null").isEmpty()) {
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
     * Gets the set of workflows that could potentially be used for user authentication.
     * 
     * @return the potentialWorkflows
     */
    @Nonnull @NonnullElements @Live public Map<String, AuthenticationWorkflowDescriptor> getPotentialWorkflows() {
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
     * Gets the list of authentication workflows, in order of preference, that must be used if user
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
     * Sets the workflows, in order of preference, that satisfy an explicit requirement from the relying party.
     * 
     * @param workflows authentication workflows, satisfy an explicit requirement from the relying party
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setRequestedWorkflows(
            @Nonnull @NonnullElements final List<AuthenticationWorkflowDescriptor> workflows) {
        
        if (Constraint.isNotNull(workflows, "Workflow list cannot be null").isEmpty()) {
            requestedWorkflows = ImmutableMap.of();
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
     * Get the authentication workflow that was attempted in order to authenticate the user.
     * 
     * @return authentication workflow that was attempted in order to authenticate the user
     */
    @Nullable public AuthenticationWorkflowDescriptor getAttemptedWorkflow() {
        return attemptedWorkflow;
    }

    /**
     * Set the authentication workflow that was attempted in order to authenticate the user.
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
     * Get the authentication event resulting from the request.
     * 
     * @return authentication workflow that was attempted in order to authenticate the user
     */
    @Nullable public AuthenticationEvent getAuthenticationResult() {
        return authenticationResult;
    }

    /**
     * Set the authentication event resulting from the request.
     * 
     * @param result authentication event resulting from the request
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationContext setAuthenticationResult(
            @Nullable final AuthenticationEvent result) {
        authenticationResult = result;
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
                .add("isPassive", isPassive).add("forceAuthn", forceAuthn)
                .add("potentialWorkflows", potentialWorkflows.keySet())
                .add("requestedWorkflows", requestedWorkflows.keySet())
                .add("activeWorkflows", activeWorkflows.keySet())
                .add("completionInstant", new DateTime(completionInstant)).toString();
    }

}
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

package net.shibboleth.idp.authn;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/** A context representing the state of an authentication attempt. */
@ThreadSafe
public final class AuthenticationRequestContext extends BaseContext {

    /** Time, in milliseconds since the epoch, when the authentication process started. */
    private final long initiationInstant;

    /** Whether authentication must occur even if an existing authentication event exists and is still valid. */
    private boolean forcingAuthentication;

    /** Currently active user session. */
    private final Optional<IdPSession> activeSession;

    /** Authentication workflows currently active for the user. */
    private final ImmutableMap<String, AuthenticationWorkflowDescriptor> activeWorkflows;

    /** Authentication workflows configured and available for use. */
    private final ImmutableMap<String, AuthenticationWorkflowDescriptor> availableWorkflows;

    /** Workflows that might potentially be used to authenticate the user. */
    private Map<String, AuthenticationWorkflowDescriptor> potentialWorkflows;

    /** Authentication workflows, in order of preference, requested by the relying party. */
    private Map<String, AuthenticationWorkflowDescriptor> requestedWorkflows;

    /** Authentication workflow that was attempted in order to authenticate the user. */
    private Optional<AuthenticationWorkflowDescriptor> attemptedWorkflow;

    /** The authenticated principal. */
    private Optional<Principal> authenticatedPrincipal;

    /** Time, in milliseconds since the epoch, when authentication process completed. */
    private long completionInstant;

    /**
     * Constructor.
     * 
     * @param session currently active user session, if any
     * @param availableFlows authentication workflows currently available
     */
    public AuthenticationRequestContext(@Nullable final IdPSession session,
            @Nullable @NullableElements final Collection<AuthenticationWorkflowDescriptor> availableFlows) {
        super();

        initiationInstant = System.currentTimeMillis();

        activeSession = Optional.fromNullable(session);

        final Builder<String, AuthenticationWorkflowDescriptor> flowsBuilder =
                new ImmutableMap.Builder<String, AuthenticationWorkflowDescriptor>();
        if (availableFlows != null) {
            for (AuthenticationWorkflowDescriptor descriptor : availableFlows) {
                if (descriptor != null) {
                    flowsBuilder.put(descriptor.getWorkflowId(), descriptor);
                }
            }
        }
        availableWorkflows = flowsBuilder.build();
        potentialWorkflows = new HashMap<String, AuthenticationWorkflowDescriptor>(availableWorkflows);

        if (activeSession.isPresent()) {
            activeWorkflows = getActiveWorkflowsBySession(availableWorkflows, session);
        } else {
            activeWorkflows = new Builder().build();
        }

        requestedWorkflows = Collections.emptyMap();

        attemptedWorkflow = Optional.absent();
        authenticatedPrincipal = Optional.absent();
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
     * Gets the currently active user session.
     * 
     * @return Returns the activeSession.
     */
    public Optional<IdPSession> getActiveSession() {
        return activeSession;
    }

    /**
     * Gets the authentication workflows currently active for this user. Note the workflows in this map are a proper
     * subset of the workflows given by {@link #getAvailableWorkflows()}. It is possible that the active session,
     * returned by {@link #getActiveSession()}, may contain {@link AuthenticationEvent} entries that are not represented
     * here. This would occur if an authentication workflow was available at some time in the past but is not available
     * now.
     * 
     * @return authentication workflows currently active for this user
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, AuthenticationWorkflowDescriptor> getActiveWorkflows() {
        return activeWorkflows;
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
     * Gets whether authentication must occur even if an existing authentication event exists and is still valid.
     * 
     * @return whether authentication must occur
     */
    public boolean isForcingAuthentication() {
        return forcingAuthentication;
    }

    /**
     * Sets whether authentication must occur even if an existing authentication event exists and is still valid.
     * 
     * @param force whether authentication must occur even if an existing authentication event exists and is still valid
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationRequestContext setForcingAuthentication(boolean force) {
        forcingAuthentication = force;
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
    @Nonnull public AuthenticationRequestContext setRequestedWorkflows(
            @Nullable @NullableElements final List<AuthenticationWorkflowDescriptor> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            requestedWorkflows = Collections.emptyMap();
            return this;
        }

        Builder<String, AuthenticationWorkflowDescriptor> flowsBuilder =
                new ImmutableMap.Builder<String, AuthenticationWorkflowDescriptor>();
        for (AuthenticationWorkflowDescriptor descriptor : workflows) {
            if (descriptor == null) {
                continue;
            }

            flowsBuilder.put(descriptor.getWorkflowId(), descriptor);
        }

        requestedWorkflows = flowsBuilder.build();

        return this;
    }

    /**
     * Gets the authentication workflow that was attempted in order to authenticate the user.
     * 
     * @return authentication workflow that was attempted in order to authenticate the user
     */
    @Nonnull public Optional<AuthenticationWorkflowDescriptor> getAttemptedWorkflow() {
        return attemptedWorkflow;
    }

    /**
     * Sets the authentication workflow that was attempted in order to authenticate the user.
     * 
     * @param workflow authentication workflow that was attempted in order to authenticate the user
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationRequestContext setAttemptedWorkflow(
            @Nonnull final AuthenticationWorkflowDescriptor workflow) {
        Constraint.isNotNull(workflow, "Authentication workflow descriptor can not be null");
        attemptedWorkflow = Optional.of(workflow);
        return this;
    }

    /**
     * Gets the principal that was authenticated.
     * 
     * @return principal that was authenticated
     */
    @Nonnull public Optional<Principal> getAuthenticatedPrincipal() {
        return authenticatedPrincipal;
    }

    /**
     * Sets the principal that was authenticated.
     * 
     * @param principal principal that was authenticated
     * 
     * @return this authentication request context
     */
    @Nonnull public AuthenticationRequestContext setAuthenticatedPrincipal(@Nonnull final Principal principal) {
        Constraint.isNotNull(principal, "Principal can not be null");
        authenticatedPrincipal = Optional.of(principal);
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
    @Nonnull public AuthenticationRequestContext setCompletionInstant() {
        completionInstant = System.currentTimeMillis();
        return this;
    }

    /**
     * Creates an authentication event based on the information in this context. Note, authentication must have
     * completed successfully in order to do this. Throws an {@link IllegalStateException} if {@link #attemptedWorkflow}
     * or {@link #authenticatedPrincipal} is {@link Optional#absent()}.
     * 
     * @return the constructed authentication event
     */
    @Nonnull public AuthenticationEvent buildAuthenticationEvent() {
        if (!attemptedWorkflow.isPresent()) {
            throw new IllegalStateException("No authentication workflow has been attempted");
        }

        if (!authenticatedPrincipal.isPresent()) {
            throw new IllegalStateException("No principal has been authenticated");
        }

        return new AuthenticationEvent(attemptedWorkflow.get().getWorkflowId(), authenticatedPrincipal.get());
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("initiationInstant", new DateTime(initiationInstant))
                .add("activeSession", activeSession.isPresent() ? activeSession.get().getId() : "null")
                .add("activeWorkflows", activeWorkflows.keySet())
                .add("availableWorkflows", availableWorkflows.keySet())
                .add("forcingAuthentication", forcingAuthentication).add("requestedWorkflows", requestedWorkflows)
                .add("attemptedWorkflow", attemptedWorkflow).add("authenticatedPrincipal", authenticatedPrincipal)
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
    protected ImmutableMap<String, AuthenticationWorkflowDescriptor> getActiveWorkflowsBySession(
            @Nonnull @NonnullElements @Unmodifiable final Map<String, AuthenticationWorkflowDescriptor> availableFlows,
            @Nonnull final IdPSession session) {
        final Builder<String, AuthenticationWorkflowDescriptor> activeFlowsBuilder =
                new ImmutableMap.Builder<String, AuthenticationWorkflowDescriptor>();

        AuthenticationWorkflowDescriptor descriptor;
        for (AuthenticationEvent event : session.getAuthenticateEvents()) {
            // TODO check if event is still active
            descriptor = availableFlows.get(event.getAuthenticationWorkflow());
            if (descriptor == null) {
                continue;
            }
            activeFlowsBuilder.put(descriptor.getWorkflowId(), descriptor);
        }

        return activeFlowsBuilder.build();
    }
}
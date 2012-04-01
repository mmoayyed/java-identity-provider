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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/** A context representing the state of an authentication attempt. */
@ThreadSafe
public final class AuthenticationRequestContext extends BaseContext {

    /** Time, in milliseconds since the epoch, when the authentication process started. */
    private final long initiationInstant;

    /** Whether authentication must occur even if an existing authentication event exists and is still valid. */
    private boolean forcingAuthentication;

    /** Authentication workflow used if user authentication is needed but no particular workflows are requested. */
    private Optional<AuthenticationWorkflowDescriptor> defaultWorfklow;

    /** Authentication workflows, in order of preference, that must be used if user authentication is required. */
    private List<AuthenticationWorkflowDescriptor> requestedWorkflows;

    /** Authentication workflow that was attempted in order to authenticate the user. */
    private Optional<AuthenticationWorkflowDescriptor> attemptedWorkflow;

    /** The authenticated principal. */
    private Optional<Principal> authenticatedPrincipal;

    /** Time, in milliseconds since the epoch, when authentication process completed. */
    private long completionInstant;

    /** Constructor. */
    public AuthenticationRequestContext() {
        super();

        initiationInstant = System.currentTimeMillis();

        defaultWorfklow = Optional.absent();
        requestedWorkflows = Collections.emptyList();
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
    public AuthenticationRequestContext setCompletionInstant() {
        completionInstant = System.currentTimeMillis();
        return this;
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
    public AuthenticationRequestContext setForcingAuthentication(boolean force) {
        forcingAuthentication = force;
        return this;
    }

    /**
     * Gets the authentication workflow to use if user authentication is needed but no particular workflows are
     * requested. If no default workflow is specified then any workflow that meets the passive/forced authentication
     * requirements for this request may be used.
     * 
     * @return authentication workflow to use if user authentication is needed but no particular workflows are
     *         requested, may be null
     */
    public Optional<AuthenticationWorkflowDescriptor> getDefaultAuthenticationWorfklow() {
        return defaultWorfklow;
    }

    /**
     * Sets the authentication workflow to use if user authentication is needed but no particular workflows are
     * requested. If no default workflow is specified then any workflow that meets the passive/forced authentication
     * requirements for this request may be used.
     * 
     * @param workflow authentication workflow to use if user authentication is needed but no particular workflows are
     *            requested
     * 
     * @return this authentication request context
     */
    public AuthenticationRequestContext setDefaultWorfklow(@Nullable final AuthenticationWorkflowDescriptor workflow) {
        defaultWorfklow = Optional.fromNullable(workflow);
        return this;
    }

    /**
     * Gets the unmodifiable list of authentication workflows, in order of preference, that must be used if user
     * authentication is required.
     * 
     * @return authentication workflows, in order of preference, that must be used if user authentication is required,
     *         never null nor containing null elements
     */
    @Nonnull @NonnullElements @Unmodifiable public List<AuthenticationWorkflowDescriptor> getRequestedWorkflows() {
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
    public AuthenticationRequestContext setRequestedWorkflows(
            @Nullable @NullableElements final List<AuthenticationWorkflowDescriptor> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            requestedWorkflows = Collections.emptyList();
            return this;
        }

        ArrayList<AuthenticationWorkflowDescriptor> descriptors = new ArrayList<AuthenticationWorkflowDescriptor>();
        for (AuthenticationWorkflowDescriptor descriptor : workflows) {
            if (descriptor == null) {
                continue;
            }

            if (descriptors.contains(descriptor)) {
                continue;
            }

            descriptors.add(descriptor);
        }

        if (descriptors.isEmpty()) {
            requestedWorkflows = Collections.emptyList();
        } else {
            requestedWorkflows = Collections.unmodifiableList(descriptors);
        }

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
    public AuthenticationRequestContext setAttemptedWorkflow(@Nonnull final AuthenticationWorkflowDescriptor workflow) {
        assert workflow != null : "Authentication workflow descriptor can not be null";
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
    public AuthenticationRequestContext setAuthenticatedPrincipal(@Nonnull final Principal principal) {
        assert principal != null : "Principal can not be null";
        authenticatedPrincipal = Optional.of(principal);
        return this;
    }

    /**
     * Creates an authentication event based on the information in this context. Note, authentication must have
     * completed successfully in order to do this. Throws an {@link IllegalStateException} if {@link #attemptedWorkflow}
     * or {@link #authenticatedPrincipal} is {@link Optional#absent()}.
     * 
     * @return the constructed authentication event
     */
    public AuthenticationEvent buildAuthenticationEvent() {
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
                .add("forcingAuthentication", forcingAuthentication).add("defaultWorfklow", defaultWorfklow)
                .add("requestedWorkflows", requestedWorkflows).add("attemptedWorkflow", attemptedWorkflow)
                .add("authenticatedPrincipal", authenticatedPrincipal)
                .add("completionInstant", new DateTime(completionInstant)).toString();
    }
}
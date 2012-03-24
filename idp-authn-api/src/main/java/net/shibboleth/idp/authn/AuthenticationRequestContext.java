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

import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.session.AuthenticationEvent;

import org.opensaml.messaging.context.BaseContext;

/** A context representing the state of an authentication attempt. */
@ThreadSafe
public final class AuthenticationRequestContext extends BaseContext {

    /** Time, in milliseconds since the epoch, when the authentication process started. */
    private final long initiationInstant;

    /** Whether authentication must occur even if an existing authentication event exists and is still valid. */
    private boolean forcingAuthentication;

    /** Authentication workflow used if user authentication is needed but no particular workflows are requested. */
    private AuthenticationWorkflowDescriptor defaultWorfklow;

    /** Authentication workflows, in order of preference, that must be used if user authentication is required. */
    private List<AuthenticationWorkflowDescriptor> requestedWorkflows;

    /** Authentication workflow that was attempted in order to authenticate the user. */
    private AuthenticationWorkflowDescriptor attemptedWorkflow;

    /** The authenticated principal. */
    private Principal authenticatedPrincipal;

    /** Time, in milliseconds since the epoch, when authentication process completed. */
    private long completionInstant;

    /** Constructor. */
    public AuthenticationRequestContext() {
        super();

        initiationInstant = System.currentTimeMillis();
        requestedWorkflows = Collections.emptyList();
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

    /** Sets the completion time of the authentication attempt to the current time. */
    public void setCompletionInstant() {
        completionInstant = System.currentTimeMillis();
    }

    /**
     * Gets whether authentication must occur even if an existing authentication event exists and is still valid.
     * 
     * @return Returns the forcingAuthentication.
     */
    public boolean isForcingAuthentication() {
        return forcingAuthentication;
    }

    /**
     * Sets whether authentication must occur even if an existing authentication event exists and is still valid.
     * 
     * @param isForcingAuthentication whether authentication must occur even if an existing authentication event exists
     *            and is still valid
     */
    public void setForcingAuthentication(boolean isForcingAuthentication) {
        forcingAuthentication = isForcingAuthentication;
    }

    /**
     * Gets the authentication workflow to use if user authentication is needed but no particular workflows are
     * requested. If no default workflow is specified then any workflow that meets the passive/forced authentication
     * requirements for this request may be used.
     * 
     * @return authentication workflow to use if user authentication is needed but no particular workflows are
     *         requested, may be null
     */
    public AuthenticationWorkflowDescriptor getDefaultAuthenticationWorfklow() {
        return defaultWorfklow;
    }

    /**
     * Sets the authentication workflow to use if user authentication is needed but no particular workflows are
     * requested. If no default workflow is specified then any workflow that meets the passive/forced authentication
     * requirements for this request may be used.
     * 
     * @param workflow authentication workflow to use if user authentication is needed but no particular workflows are
     *            requested, may be null
     */
    public void setDefaultWorfklow(AuthenticationWorkflowDescriptor workflow) {
        defaultWorfklow = workflow;
    }

    /**
     * Gets the unmodifiable list of authentication workflows, in order of preference, that must be used if user
     * authentication is required.
     * 
     * @return authentication workflows, in order of preference, that must be used if user authentication is required,
     *         never null nor containing null elements
     */
    public List<AuthenticationWorkflowDescriptor> getRequestedWorkflows() {
        return requestedWorkflows;
    }

    /**
     * Sets the authentication workflows, in order of preference, that must be used if user authentication is required.
     * 
     * @param workflows authentication workflows, in order of preference, that must be used if user authentication is
     *            required, may be null or contain null elements
     */
    public void setRequestedWorkflows(List<AuthenticationWorkflowDescriptor> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            requestedWorkflows = Collections.emptyList();
            return;
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
    }

    /**
     * Gets the authentication workflow that was attempted in order to authenticate the user.
     * 
     * @return authentication workflow that was attempted in order to authenticate the user, may be null
     */
    public AuthenticationWorkflowDescriptor getAttemptedWorkflow() {
        return attemptedWorkflow;
    }

    /**
     * Sets the authentication workflow that was attempted in order to authenticate the user.
     * 
     * @param workflow authentication workflow that was attempted in order to authenticate the user, may be null
     */
    public void setAttemptedWorkflow(AuthenticationWorkflowDescriptor workflow) {
        attemptedWorkflow = workflow;
    }

    /**
     * Gets the principal that was authenticated.
     * 
     * @return principal that was authenticated, may be null
     */
    public Principal getAuthenticatedPrincipal() {
        return authenticatedPrincipal;
    }

    /**
     * Sets the principal that was authenticated.
     * 
     * @param principal principal that was authenticated, may be null
     */
    public void setAuthenticatedPrincipal(Principal principal) {
        authenticatedPrincipal = principal;
    }

    /**
     * Creates an authentication event based on the information in this context. Note, authentication must have
     * completed successfully in order to do this.
     * 
     * @return the constructed authentication event
     */
    public AuthenticationEvent buildAuthenticationEvent() {
        return new AuthenticationEvent(attemptedWorkflow.getWorkflowId(), authenticatedPrincipal);
    }
}
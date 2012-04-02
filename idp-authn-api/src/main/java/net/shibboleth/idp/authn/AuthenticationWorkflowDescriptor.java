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

import javax.annotation.Nonnull;

import com.google.common.base.Objects;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** A descriptor of an authentication workflow. */
public class AuthenticationWorkflowDescriptor {

    /** The unique identifier of the authentication workflow. */
    private final String workflowId;

    /** Whether this workflow supports passive authentication. */
    private boolean supportsPassive;

    /** Whether this workflow supports forced authentication. */
    private boolean supportsForcedAuthentication;

    /** Maximum amount of time in milliseconds, since first usage, a workflow should be considered active. */
    private long lifetime;

    /** Maximum amount of time in milliseconds, since more recent usage, a workflow should be considered active. */
    private long timeout;

    /**
     * Constructor.
     * 
     * @param id unique ID of this workflow, can not be null or empty
     */
    public AuthenticationWorkflowDescriptor(@Nonnull @NotEmpty final String id) {
        workflowId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Workflow ID can not be null or empty");
    }

    /**
     * Gets the unique identifier of the authentication workflow.
     * 
     * @return unique identifier of the authentication workflow
     */
    @Nonnull @NotEmpty public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Gets whether this workflow supports passive authentication.
     * 
     * @return whether this workflow supports passive authentication
     */
    public boolean isPassiveAuthenticationSupported() {
        return supportsPassive;
    }

    /**
     * Sets whether this workflow supports passive authentication.
     * 
     * @param isSupported whether this workflow supports passive authentication
     */
    public void setPassiveAuthenticationSupported(boolean isSupported) {
        supportsPassive = isSupported;
    }

    /**
     * Gets whether this workflow supports forced authentication.
     * 
     * @return whether this workflow supports forced authentication
     */
    public boolean isForcedAuthenticationSupported() {
        return supportsForcedAuthentication;
    }

    /**
     * Sets whether this workflow supports forced authentication.
     * 
     * @param isSupported whether this workflow supports forced authentication.
     */
    public void setForcedAuthenticationSupported(boolean isSupported) {
        supportsForcedAuthentication = isSupported;
    }

    /**
     * Gets the maximum amount of time in milliseconds, since first usage, a workflow should be considered active. A
     * value of 0 indicates that their is no upper limit on the lifetime on an active workflow.
     * 
     * @return maximum amount of time in milliseconds a workflow should be considered active, never less than 0
     */
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Sets the maximum amount of time in milliseconds, since first usage, a workflow should be considered active. A
     * value of 0 indicates that their is no upper limit on the lifetime on an active workflow.
     * 
     * @param workflowLifetime the lifetime for the workflow, must be 0 or greater
     */
    public void setLifetime(long workflowLifetime) {
        lifetime = Constraint.isGreaterThanOrEqual(0, workflowLifetime, "Lifetime must be greater than or equal to 0");
    }

    /**
     * Gets the maximum amount of time in milliseconds, since more recent usage, a workflow should be considered active.
     * A value of 0 indicates that their is no inactivity timeout on an active workflow.
     * 
     * @return Returns the duration.
     */
    public long getInactivityTimeout() {
        return timeout;
    }

    /**
     * Sets the maximum amount of time in milliseconds, since more recent usage, a workflow should be considered active.
     * A value of 0 indicates that their is no inactivity timeout on an active workflow.
     * 
     * @param inactivityTimeout the workflow timeout, must be 0 or greater
     */
    public void setInactivityTimeout(long inactivityTimeout) {
        timeout =
                Constraint.isGreaterThanOrEqual(0, inactivityTimeout,
                        "Inactivity timeout must be greater than, or equal to, 0");
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return workflowId.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof AuthenticationWorkflowDescriptor) {
            return workflowId.equals(((AuthenticationWorkflowDescriptor) obj).getWorkflowId());
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("workflowId", workflowId).add("supportsPassive", supportsPassive)
                .add("supportsForcedAuthentication", supportsForcedAuthentication).add("lifetime", lifetime)
                .add("inactivityTimeout", timeout).toString();
    }
}
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
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import com.google.common.base.Objects;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A descriptor of an authentication workflow.
 * 
 * <p>A workflow models a subflow that performs authentication in a particular way and satisfies
 * various constraints that may apply to an authentication request. Some of these constraints are
 * directly exposed as properties of the workflow, and others can be found by examining the list
 * of extended {@link Principal}s that the workflow exposes.</p>
 */
public class AuthenticationWorkflowDescriptor implements IdentifiableComponent {

    /** The unique identifier of the authentication workflow. */
    private final String workflowId;

    /** Whether this workflow supports passive authentication. */
    private boolean supportsPassive;

    /** Whether this workflow supports forced authentication. */
    private boolean supportsForced;

    /** Maximum amount of time in milliseconds, since first usage, a workflow should be considered active. */
    private long lifetime;
    
    /** Supported principals, indexed by type, that the workflow can produce. */
    @Nonnull private Subject supportedPrincipals;

    /**
     * Constructor.
     * 
     * @param id unique ID of this workflow, can not be null or empty
     */
    public AuthenticationWorkflowDescriptor(@Nonnull @NotEmpty final String id) {
        workflowId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Workflow ID cannot be null or empty");
        supportedPrincipals = new Subject();
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getId() {
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
        return supportsForced;
    }

    /**
     * Sets whether this workflow supports forced authentication.
     * 
     * @param isSupported whether this workflow supports forced authentication.
     */
    public void setForcedAuthenticationSupported(boolean isSupported) {
        supportsForced = isSupported;
    }

    /**
     * Gets the maximum amount of time in milliseconds, since first usage, a workflow should be considered active. A
     * value of 0 indicates that there is no upper limit on the lifetime on an active workflow.
     * 
     * @return maximum amount of time in milliseconds a workflow should be considered active, never less than 0
     */
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Sets the maximum amount of time in milliseconds, since first usage, a workflow should be considered active. A
     * value of 0 indicates that there is no upper limit on the lifetime on an active workflow.
     * 
     * @param workflowLifetime the lifetime for the workflow, must be 0 or greater
     */
    public void setLifetime(long workflowLifetime) {
        lifetime = Constraint.isGreaterThanOrEqual(0, workflowLifetime, "Lifetime must be greater than or equal to 0");
    }

    /**
     * Get a set of supported non-user-specific principals that the workflow may produce when it operates.
     * 
     * @param <T> type of Principal to inquire on
     * @param c type of Principal to inquire on
     * 
     * @return a set of supported principals
     */
    @Nonnull @NonnullElements @Unmodifiable public <T extends Principal> Set<T> getSupportedPrincipals(Class<T> c) {
        return supportedPrincipals.getPrincipals(c);
    }
    
    /**
     * Set supported non-user-specific principals that the workflow may produce when it operates.
     * 
     * @param <T> a type of principal to add, if not generic
     * @param principals supported principals
     */
    public <T extends Principal> void setSupportedPrincipals(@Nonnull @NonnullElements final List<T> principals) {
        supportedPrincipals.getPrincipals().clear();
        supportedPrincipals.getPrincipals().addAll(principals);
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
            return workflowId.equals(((AuthenticationWorkflowDescriptor) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("workflowId", workflowId).add("supportsPassive", supportsPassive)
                .add("supportsForcedAuthentication", supportsForced).add("lifetime", lifetime).toString();
    }
}
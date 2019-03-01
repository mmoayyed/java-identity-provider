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
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Describes an act of authentication.
 *
 * <p>Any authentication flow that succeeds must produce a single instance of this object.
 * It may be composite, in the sense that it may represent a combination of separate exchanges
 * that make up a single overall result, but the IdP always acts on a single result as the
 * product of a given request for a login.</p>
 */
@ThreadSafe
public class AuthenticationResult implements PrincipalSupportingComponent {
    
    /** The Subject established by the authentication result. */
    @Nonnull private final Subject subject;

    /** The identifier of the flow used to produce this result. */
    @Nonnull @NotEmpty private final String authenticationFlowId;
    
    /** The time that the authentication completed. */
    @Nonnull private Instant authenticationInstant;

    /** The last time this result was used to bypass authentication. */
    @Nonnull private Instant lastActivityInstant;
    
    /** Tracks whether a result was loaded from a previous session or created as part of the current request. */
    private boolean previousResult;
    
    /**
     * Constructor.
     * 
     * <p>Sets the authentication instant to the current time.</p>
     * 
     * @param flowId the workflow used to authenticate the subject
     * @param newSubject a Subject identifying the authenticated entity
     */
    public AuthenticationResult(@Nonnull @NotEmpty final String flowId, @Nonnull final Subject newSubject) {

        authenticationFlowId = Constraint.isNotNull(StringSupport.trimOrNull(flowId),
                "Authentication flow ID cannot be null nor empty");
        subject = Constraint.isNotNull(newSubject, "Subject list cannot be null or empty");
        authenticationInstant = Instant.now();
        lastActivityInstant = authenticationInstant;
    }

    /**
     * Constructor. <p>Sets the authentication instant to the current time.</p>
     * 
     * @param flowId the workflow used to authenticate the subject
     * @param principal a Principal identifying the authenticated entity
     */
    public AuthenticationResult(@Nonnull @NotEmpty final String flowId, @Nonnull final Principal principal) {
        this(flowId, new Subject(false, ImmutableSet.of(Constraint.isNotNull(principal, "Principal cannot be null")),
                Collections.EMPTY_SET, Collections.EMPTY_SET));
    }
    
    /**
     * Get the Subject identifying the authenticated entity.
     * 
     * @return a Subject identifying the authenticated entity
     */
    @Nonnull public Subject getSubject() {
        return subject;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @Unmodifiable public <T extends Principal> Set<T> getSupportedPrincipals(
            @Nonnull final Class<T> c) {
        return subject.getPrincipals(c);
    }
    
    /**
     * Get the flow used to authenticate the principal.
     * 
     * @return flow used to authenticate the principal
     */
    @Nonnull @NotEmpty public String getAuthenticationFlowId() {
        return authenticationFlowId;
    }

    /**
     * Get the time that the authentication completed.
     * 
     * @return time that the authentication completed
     */
    @Nonnull public Instant getAuthenticationInstant() {
        return authenticationInstant;
    }

    /**
     * Set the time that the authentication completed.
     * 
     * @param instant time that the authentication completed, never non-positive
     */
    public void setAuthenticationInstant(@Nonnull final Instant instant) {
        authenticationInstant = Constraint.isNotNull(instant, "Authentication instant cannot be null");
    }
    
    /**
     * Get the last time this result was used for authentication.
     * 
     * @return last time this result was used for authentication
     */
    @Nonnull public Instant getLastActivityInstant() {
        return lastActivityInstant;
    }
    
    /**
     * Set the last time result was used for authentication.
     * 
     * @param instant last time result was used to bypass authentication
     */
    public void setLastActivityInstant(@Nonnull final Instant instant) {
        lastActivityInstant = Constraint.isNotNull(instant, "Last activity instant cannot be null");
    }

    /**
     * Set the last activity instant for this result to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = Instant.now();
    }
    
    /**
     * Get whether this result was loaded from a session as the product of a previous request.
     * 
     * @return true iff this result was produced as part of an earlier request
     * 
     * @since 3.3.0
     */
    public boolean isPreviousResult() {
        return previousResult;
    }
    
    /**
     * Set whether this result was loaded from a session as the product of a previous request.
     * 
     * @param flag flag to set
     * 
     * @since 3.3.0
     */
    public void setPreviousResult(final boolean flag) {
        previousResult = flag;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return authenticationFlowId.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof AuthenticationResult) {
            return Objects.equals(getAuthenticationFlowId(), ((AuthenticationResult) obj).getAuthenticationFlowId())
                    && getAuthenticationInstant().equals(((AuthenticationResult) obj).getAuthenticationInstant());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("authenticationFlowId", authenticationFlowId)
                .add("authenticatedPrincipal", getSubjectName())
                .add("authenticationInstant", authenticationInstant)
                .add("lastActivityInstant", lastActivityInstant)
                .add("previousResult", previousResult).toString();
    }
    
    /**
     * Get a suitable principal name for logging/debugging use.
     * 
     * @return a principal name for logging/debugging
     */
    @Nullable private String getSubjectName() {
        
        final Set<UsernamePrincipal> usernames = getSubject().getPrincipals(UsernamePrincipal.class);
        if (!usernames.isEmpty()) {
            return usernames.iterator().next().getName();
        }
        
        for (final Principal p : getSubject().getPrincipals()) {
            return p.getName();
        }
        
        return null;
    }
    
}
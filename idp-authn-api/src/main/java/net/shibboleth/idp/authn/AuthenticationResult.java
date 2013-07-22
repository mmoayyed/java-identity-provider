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
import javax.security.auth.Subject;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Describes an act of authentication. The result may be composite, in the sense that it
 * may represent a combination of separate exchanges that make up a single overall result. 
 */
@ThreadSafe
public final class AuthenticationResult {

    /** The Subjects established by the authentication result. */
    @Nonnull @NonnullElements @NotEmpty private final List<Subject> subjects;

    /** The identifier of the flow used to produce this result. */
    @Nonnull @NotEmpty private final String authenticationFlowId;

    /** The time, in milliseconds since the epoch, that the authentication completed. */
    private final long authenticationInstant;

    /** The last time, in milliseconds since the epoch, this result was used to bypass authentication. */
    private long lastActivityInstant;
    
    /**
     * Constructor. <p>Sets the authentication instant to the current time.</p>
     * 
     * @param flowId the workflow used to authenticate the subject
     * @param newSubjects a Subject collection identifying the authenticated entity
     */
    public AuthenticationResult(@Nonnull @NotEmpty final String flowId,
            @Nonnull @NotEmpty @NonnullElements final List<Subject> newSubjects) {

        authenticationFlowId = Constraint.isNotNull(StringSupport.trimOrNull(flowId),
                "Authentication flow ID cannot be null nor empty");
        subjects = new ArrayList(Constraint.isNotEmpty(newSubjects, "Subject list cannot be null or empty"));
        authenticationInstant = System.currentTimeMillis();
        lastActivityInstant = authenticationInstant;
    }

    /**
     * Constructor. <p>Sets the authentication instant to the current time.</p>
     * 
     * @param flowId the workflow used to authenticate the subject
     * @param subject a Subject identifying the authenticated entity
     */
    public AuthenticationResult(@Nonnull @NotEmpty final String flowId, @Nonnull final Subject subject) {
        this(flowId, ImmutableList.of(Constraint.isNotNull(subject, "Subject cannot be null")));
    }

    /**
     * Constructor. <p>Sets the authentication instant to the current time.</p>
     * 
     * @param flowId the workflow used to authenticate the subject
     * @param principal a Principal identifying the authenticated entity
     */
    public AuthenticationResult(@Nonnull @NotEmpty final String flowId, @Nonnull final Principal principal) {
        this(flowId, ImmutableList.of(
                new Subject(false, ImmutableSet.of(Constraint.isNotNull(principal, "Principal cannot be null")),
                        Collections.EMPTY_SET, Collections.EMPTY_SET)));
    }
    
    /**
     * Get the Subject collection identifying the authenticated entity.
     * 
     * @return a Subject collection
     */
    @Unmodifiable @Nonnull @NotEmpty @NonnullElements public List<Subject> getSubjects() {
        return Collections.unmodifiableList(subjects);
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
     * Get the time, in milliseconds since the epoch, that the authentication completed.
     * 
     * <p>Always greater or equal to 0.</p>
     * 
     * @return time, in milliseconds since the epoch, that the authentication completed, never less than 0
     */
    public long getAuthenticationInstant() {
        return authenticationInstant;
    }
    
    /**
     * Get the last time, in milliseconds since the epoch, this result was used to bypass authentication.
     * 
     * <p>Always greater or equal to 0.</p>
     * 
     * @return last time, in milliseconds since the epoch, this result was used to bypass authentication
     */
    public long getLastActivityInstant() {
        return lastActivityInstant;
    }
    
    /**
     * Set the last time, in milliseconds since the epoch, result was used to bypass authentication.
     * 
     * <p>Must be greater than 0.</p>
     * 
     * @param instant last time, in milliseconds since the epoch, result was used to bypass authentication
     */
    public void setLastActivityInstant(final long instant) {
        lastActivityInstant = Constraint.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for this result to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return authenticationFlowId.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof AuthenticationResult) {
            return Objects.equal(getAuthenticationFlowId(),
                    ((AuthenticationResult) obj).getAuthenticationFlowId());
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("authenticationFlowId", authenticationFlowId)
                .add("authenticatedPrincipal", getSubjectName())
                .add("authenticationInstant", new DateTime(authenticationInstant))
                .add("lastActivityInstant", new DateTime(lastActivityInstant)).toString();
    }
    
    /**
     * Get a suitable principal name for logging/debugging use.
     * 
     * @return a principal name for logging/debugging
     */
    @Nullable private String getSubjectName() {
        for (Subject s : subjects) {
            for (Principal p : s.getPrincipals()) {
                return p.getName();
            }
        }
        
        return null;
    }
    
}
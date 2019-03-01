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

package net.shibboleth.idp.cas.ticket;

import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Supplemental state data to be stored with a ticket.
 *
 * @author Marvin S. Addison
 */
public class TicketState {

    /** ID of session in which ticket is created. */
    @Nonnull
    private String sessId;

    /** Canonical authenticated principal name. */
    @Nonnull
    private String authenticatedPrincipalName;

    /** Authentication instant. */
    @Nonnull
    private Instant authenticationInstant;

    /** Authentication method ID/name/description. */
    @Nonnull
    private String authenticationMethod;


    /**
     * Creates a new instance with required fields.
     *
     * @param sessionId ID of the session in which the ticket was created
     * @param principalName canonical authenticated principal name
     * @param authnInstant instant at which the principal authenticated
     * @param authnMethod principal authentication method ID/name/description
     */
    public TicketState(
            @Nonnull final String sessionId,
            @Nonnull final String principalName,
            @Nonnull final Instant authnInstant,
            @Nonnull final String authnMethod) {
        sessId = Constraint.isNotNull(sessionId, "SessionID cannot be null");
        authenticatedPrincipalName = Constraint.isNotNull(principalName, "PrincipalName cannot be null");
        authenticationInstant = Constraint.isNotNull(authnInstant, "AuthnInstant cannot be null");
        authenticationMethod = Constraint.isNotNull(authnMethod, "AuthnMethod cannot be null");
    }

    /**
     * Get the ID of the session in which the ticket was created.
     *
     * @return IdP session ID.
     */
    @Nonnull
    public String getSessionId() {
        return sessId;
    }

    /**
     * Get the canonical authenticated principal name.
     *
     * @return Canonical principal.
     */
    @Nonnull
    public String getPrincipalName() {
        return authenticatedPrincipalName;
    }

    /**
     * Get the instant at which the principal authenticated.
     *
     * @return Principal authentication instant.
     */
    @Nonnull
    public Instant getAuthenticationInstant() {
        return authenticationInstant;
    }

    /**
     * Get the principal authentication method ID/name/description.
     *
     * @return Principal authentication method.
     */
    @Nonnull
    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TicketState) {
            final TicketState other = (TicketState) o;
            return sessId.equals(other.sessId) &&
                    authenticatedPrincipalName.equals(other.authenticatedPrincipalName) &&
                    authenticationInstant.equals(other.authenticationInstant) &&
                    authenticationMethod.equals(other.authenticationMethod);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessId, authenticatedPrincipalName, authenticationInstant, authenticationMethod);
    }
}

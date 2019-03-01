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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

/**
 * Generic CAS ticket that has a natural identifier and expiration. All CAS tickets are bound to an IdP session ID
 * that indicates the IdP session in which they were created.
 *
 * @author Marvin S. Addison
 */
public class Ticket {

    /** Ticket identifier. */
    @Nonnull
    private final String ticketId;

    /** Service/relying party that requested the ticket. */
    @Nonnull
    private final String ticketService;

    /** Expiration instant. */
    @Nonnull
    private final Instant expirationInstant;

    /** Supplemental ticket state data. */
    @Nullable
    private TicketState ticketState;

    /**
     * Deprecated. This constructor was formerly used to associate a ticket with an IdP session, but is no longer
     * supported. In order to associate an IdP session with a ticket, follow these steps:
     * <ol>
     *     <li>Create a ticket using the {@link Ticket#Ticket(String, String, Instant)} constructor.</li>
     *     <li>Create an instance of {@link TicketState}, which accepts an IdP session ID parameter.</li>
     *     <li>Call {@link #setTicketState(TicketState)} on the ticket instance.</li>
     * </ol>
     *
     * @param id Ticket ID.
     * @param sessionId This parameter is ignored.
     * @param service Service that requested the ticket.
     * @param expiration Expiration instant.
     */
    @Deprecated
    public Ticket(
            @Nonnull final String id,
            @Nullable final String sessionId,
            @Nonnull final String service,
            @Nonnull final Instant expiration) {
        ticketId = Constraint.isNotNull(id, "Id cannot be null");
        ticketService = Constraint.isNotNull(service, "Service cannot be null");
        expirationInstant = Constraint.isNotNull(expiration, "Expiration cannot be null");
        DeprecationSupport.warnOnce(ObjectType.METHOD, "Ticket constructor with sessionID", 
                null, "TicketState#setSessionId(String)");

    }

    /**
     * Creates a new ticket with the given parameters.
     *
     * @param id Ticket ID.
     * @param service Service that requested the ticket.
     * @param expiration Expiration instant.
     */
    public Ticket(
            @Nonnull final String id,
            @Nonnull final String service,
            @Nonnull final Instant expiration) {
        ticketId = Constraint.isNotNull(id, "Id cannot be null");
        ticketService = Constraint.isNotNull(service, "Service cannot be null");
        expirationInstant = Constraint.isNotNull(expiration, "Expiration cannot be null");
    }

    /**
     * Get the ticket ID.
     * 
     * @return ticket ID
     */
    @Nonnull public String getId() {
        return ticketId;
    }

    /**
     * Get the session ID.
     * 
     * @return session ID
     */
    @Nullable public String getSessionId() {
        if (ticketState != null) {
            return ticketState.getSessionId();
        }
        return null;
    }

    /**
     * Get the service that requested the ticket.
     * 
     * @return service that requested the ticket
     */
    @Nonnull public String getService() {
        return ticketService;
    }

    /**
     * Get the expiration instant.
     * 
     * @return expiration instant
     */
    @Nonnull public Instant getExpirationInstant() {
        return expirationInstant;
    }

    /**
     * Get the supplemental ticket state data.
     * 
     * @return ticket state
     */
    @Nullable public TicketState getTicketState() {
        return ticketState;
    }

    /**
     * Set the supplemental ticket state data.
     * 
     * @param state supplemental ticket state
     */
    public void setTicketState(@Nullable final TicketState state) {
        ticketState = state;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || !o.getClass().equals(getClass())) {
            return false;
        }
        final Ticket other = (Ticket) o;
        return other.ticketId.equals(ticketId);
    }

    @Override
    public int hashCode() {
        return 23 + 31 * ticketId.hashCode();
    }

    @Override
    public String toString() {
        return ticketId;
    }

    /**
     * Create a new ticket from this one with the given identifier.
     *
     * @param newId New ticket ID.
     *
     * @return Clone of this ticket with new ID.
     */
    public Ticket clone(@Nonnull final String newId) {
        final Ticket clone = newInstance(newId);
        clone.setTicketState(ticketState);
        return clone;
    }

    /**
     * Create a new ticket with this ticket's service and expiration.
     * 
     * @param newId new ticket ID
     * @return newly created ticket
     */
    protected Ticket newInstance(@Nonnull final String newId) {
        return new Ticket(newId, ticketService, expirationInstant);
    }

}

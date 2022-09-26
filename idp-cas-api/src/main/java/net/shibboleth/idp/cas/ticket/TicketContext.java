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

import org.opensaml.messaging.context.BaseContext;

import net.shibboleth.shared.logic.Constraint;

import javax.annotation.Nonnull;

/**
 * IdP context that stores a granted CAS ticket.
 * This context is typically a child of {@link org.opensaml.profile.context.ProfileRequestContext}.
 *
 * @author Marvin S. Addison
 */
public final class TicketContext extends BaseContext {
    /** Ticket held by this context. */
    @Nonnull private final Ticket t;

    /**
     * Creates a new ticket context to hold a CAS protocol ticket.
     *
     * @param ticket Ticket to hold.
     */
    public TicketContext(@Nonnull final Ticket ticket) {
        t = Constraint.isNotNull(ticket, "Ticket cannot be null");
    }

    /**
     * Get the ticket held by this context.
     * 
     * @return ticket held by this context
     */
    @Nonnull public Ticket getTicket() {
        return t;
    }
}

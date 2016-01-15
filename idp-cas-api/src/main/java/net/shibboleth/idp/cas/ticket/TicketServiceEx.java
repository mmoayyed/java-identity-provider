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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthenticationResult;
import org.joda.time.Instant;

/**
 * Extended CAS ticket management service.
 *
 * @author Marvin S. Addison
 */
public interface TicketServiceEx extends TicketService {
    /**
     * Creates and stores a ticket for the given service.
     *
     * @param id ID of ticket to create.
     * @param expiry Expiration date of service ticket.
     * @param state Additional state to be stored with the ticket.
     * @param service Service for which ticket is granted.
     * @param renew True to indicate the ticket was generated in response to a forced authentication, false otherwise.
     *
     * @return Created service ticket.
     */
    @Nonnull
    ServiceTicket createServiceTicket(
            @Nonnull String id,
            @Nonnull Instant expiry,
            @Nonnull String service,
            @Nonnull TicketState state,
            boolean renew);
}

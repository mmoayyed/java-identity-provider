/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

/**
 * CAS ticket management service.
 *
 * @author Marvin S. Addison
 */
public interface TicketService {
    
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
    @Nonnull ServiceTicket createServiceTicket(
            @Nonnull String id,
            @Nonnull Instant expiry,
            @Nonnull String service,
            @Nonnull TicketState state,
            boolean renew);

    /**
     * Removes the service ticket with the given identifier.
     *
     * @param id Identifier of ticket to remove.
     *
     * @return Removed ticket or null if not found.
     */
    @Nullable ServiceTicket removeServiceTicket(@Nonnull String id);

    /**
     * Creates a top-level proxy-granting ticket from a service ticket.
     *
     * @param id ID of proxy-granting ticket to create.
     * @param expiry Expiration date of proxy-granting ticket.
     * @param serviceTicket Successfully-validated service ticket.
     * @param pgtUrl Proxy callback URL used to authenticate and identify the proxying service.
     *
     * @return Created proxy-granting ticket.
     */
    @Nonnull ProxyGrantingTicket createProxyGrantingTicket(
            @Nonnull String id,
            @Nonnull Instant expiry,
            @Nonnull ServiceTicket serviceTicket,
            @Nonnull String pgtUrl);

    /**
     * Creates a chained proxy-granting ticket from a proxy ticket. The value of {@link ProxyTicket#getPgtId()}
     * defines the parent of the created ticket, which in turn determines its location in the proxy chain.
     *
     * @param id ID of proxy-granting ticket to create.
     * @param expiry Expiration date of proxy-granting ticket.
     * @param proxyTicket Successfully-validated proxy ticket.
     * @param pgtUrl Proxy callback URL used to authenticate and identify the proxying service.
     *
     * @return Created proxy-granting ticket.
     */
    @Nonnull ProxyGrantingTicket createProxyGrantingTicket(
            @Nonnull String id,
            @Nonnull Instant expiry,
            @Nonnull ProxyTicket proxyTicket,
            @Nonnull String pgtUrl);

    /**
     * Retrieves a proxy-granting ticket by its ID.
     *
     * @param id Proxy-granting ticket ID.
     *
     * @return Proxy-granting ticket or null if not found.
     */
    @Nullable ProxyGrantingTicket fetchProxyGrantingTicket(@Nonnull String id);

    /**
     * Removes the proxy-granting ticket with the given identifier.
     *
     * @param id Identifier of ticket to remove.
     *
     * @return Removed ticket or null if not found.
     */
    @Nullable ProxyGrantingTicket removeProxyGrantingTicket(@Nonnull String id);

    /**
     * Creates and stores a proxy ticket for the given service.
     *
     * @param id ID of proxy-granting ticket to create.
     * @param expiry Expiration date of proxy ticket.
     * @param pgt Proxy-granting ticket used to create proxy ticket.
     * @param service Service for which ticket is granted.
     *
     * @return Created proxy ticket.
     */
    @Nonnull ProxyTicket createProxyTicket(
            @Nonnull String id,
            @Nonnull Instant expiry,
            @Nonnull ProxyGrantingTicket pgt,
            @Nonnull String service);

    /**
     * Removes the proxy ticket with the given identifier.
     *
     * @param id Identifier of ticket to remove.
     *
     * @return Removed ticket or null if not found.
     */
    @Nullable ProxyTicket removeProxyTicket(@Nonnull String id);

}
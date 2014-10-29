/*
 * See LICENSE for licensing and NOTICE for copyright.
 */

package net.shibboleth.idp.cas.ticket;

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
     * @param sessionId ID of IdP session in which ticket was created.
     * @param service Service for which ticket is granted.
     * @param renew True to indicate the ticket was generated in response to a forced authentication, false otherwise.
     *
     * @return Created service ticket.
     */
    @Nonnull
    ServiceTicket createServiceTicket(@Nonnull String sessionId, @Nonnull String service, boolean renew);

    /**
     * Removes the service ticket with the given identifier.
     *
     * @param id Identifier of ticket to remove.
     *
     * @return Removed ticket or null if not found.
     */
    @Nullable
    ServiceTicket removeServiceTicket(@Nonnull String id);

    /**
     * Creates a top-level proxy-granting ticket from a service ticket.
     *
     * @param serviceTicket Successfully-validated service ticket.
     * @param pgtId ID of proxy-granting ticket to create.
     *
     * @return Created proxy-granting ticket.
     */
    @Nonnull
    ProxyGrantingTicket createProxyGrantingTicket(@Nonnull ServiceTicket serviceTicket, @Nonnull String pgtId);

    /**
     * Creates a chained proxy-granting ticket from a proxy ticket. The value of {@link ProxyTicket#getPgtId()}
     * defines the parent of the created ticket, which in turn determines its location in the proxy chain.
     *
     * @param proxyTicket Successfully-validated proxy ticket.
     * @param pgtId ID of proxy-granting ticket to create.
     *
     * @return Created proxy-granting ticket.
     */
    @Nonnull
    ProxyGrantingTicket createProxyGrantingTicket(@Nonnull ProxyTicket proxyTicket, @Nonnull String pgtId);

    /**
     * Retrieves a proxy-granting ticket by its ID.
     *
     * @param id Proxy-granting ticket ID.
     *
     * @return Proxy-granting ticket or null if not found.
     */
    @Nullable
    ProxyGrantingTicket fetchProxyGrantingTicket(@Nonnull String id);

    /**
     * Removes the proxy-granting ticket with the given identifier.
     *
     * @param id Identifier of ticket to remove.
     *
     * @return Removed ticket or null if not found.
     */
    @Nullable
    ProxyGrantingTicket removeProxyGrantingTicket(@Nonnull String id);

    /**
     * Creates and stores a proxy ticket for the given service.
     *
     * @param pgt Proxy-granting ticket used to create proxy ticket.
     * @param service Service for which ticket is granted.
     *
     * @return Created proxy ticket.
     */
    @Nonnull
    ProxyTicket createProxyTicket(@Nonnull ProxyGrantingTicket pgt, @Nonnull String service);

    /**
     * Removes the proxy ticket with the given identifier.
     *
     * @param id Identifier of ticket to remove.
     *
     * @return Removed ticket or null if not found.
     */
    @Nullable
    ProxyTicket removeProxyTicket(@Nonnull String id);
}

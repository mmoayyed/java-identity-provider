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

package net.shibboleth.idp.cas.ticket.impl;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.DataSealer;

import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ticket service that uses two different strategies for ticket persistence:
 *
 * <ol>
 *     <li>Service tickets, proxy tickets, and root proxy-granting tickets are persisted by serializing
 *     ticket data and encrypting it into the opaque part of the ticket ID using a {@link DataSealer}.</li>
 *     <li>Chained proxy-granting tickets are persisted using a {@link StorageService}.</li>
 * </ol>
 *
 * <p><strong>NOTE:</strong> The service tickets, proxy tickets, and root proxy-granting tickets produced by
 * this component do not support one-time use. More precisely, {@link #removeServiceTicket(String)} and
 * {@link #removeProxyTicket(String)} simply return a decoded ticket and do not invalidate the ticket in any way.
 * Since there is no backing store for those types of  tickets, they can be reused until one of the following
 * conditions is met:
 *
 * <ol>
 *     <li>The value of {@link Ticket#getExpirationInstant()} is exceeded.</li>
 *     <li>The {@link DataSealer} key used to encrypt data is revoked.</li>
 * </ol>
 *
 * @author Marvin S. Addison
 * @author Paul B. Henson
 * @since 3.3.0
 */
public class EncodingTicketService extends AbstractTicketService {

    /** Default service ticket prefix. */
    public static final String SERVICE_TICKET_PREFIX = "ST";

    /** Default proxy ticket prefix. */
    public static final String PROXY_TICKET_PREFIX = "PT";

    /** Default proxy granting ticket prefix. */
    public static final String PROXY_GRANTING_TICKET_PREFIX = "PGT-E";

    /** Non-null marker value for unused ServiceTicket#id field and storage context name. */
    private static final String NOT_USED = "na";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(EncodingTicketService.class);

    /** Data sealer that handles encryption of serialized service ticket data. */
    @Nonnull
    private final DataSealer dataSealer;

    /** Service ticket prefix. */
    @NotEmpty
    private String serviceTicketPrefix = SERVICE_TICKET_PREFIX;

    /** Proxy ticket prefix. */
    @NotEmpty
    private String proxyTicketPrefix = PROXY_TICKET_PREFIX;

    /** Proxy granting ticket prefix. */
    @NotEmpty
    private String proxyGrantingTicketPrefix = PROXY_GRANTING_TICKET_PREFIX;

    /**
     * Creates a new instance.
     *
     * @param service Storage service to which tickets are persisted.
     * @param sealer data sealer
     */
    public EncodingTicketService(@Nonnull @ParameterName(name="service") final StorageService service, 
            @Nonnull @ParameterName(name="sealer") final DataSealer sealer) {
        super(service);
        dataSealer = Constraint.isNotNull(sealer, "DataSealer cannot be null");
    }

    /**
     * Sets the service ticket prefix. Default is ST.
     *
     * @param prefix Service ticket prefix.
     */
    public void setServiceTicketPrefix(final String prefix) {
        serviceTicketPrefix = Constraint.isNotEmpty(prefix, "Prefix cannot be null or empty");
    }

    /**
     * Sets the proxy ticket prefix. Default is PT.
     *
     * @param prefix Proxy ticket prefix.
     */
    public void setProxyTicketPrefix(final String prefix) {
        proxyTicketPrefix = Constraint.isNotEmpty(prefix, "Prefix cannot be null or empty");
    }

    /**
     * Sets the proxy granting ticket prefix. Default is PGT-E. Note that this MUST be distinct from
     * the proxy granting ticket prefix used for regular proxy-granting ticket identifiers.
     *
     * @param prefix Proxy granting ticket prefix.
     */
    public void setProxyGrantingTicketPrefix(final String prefix) {
        proxyGrantingTicketPrefix = Constraint.isNotEmpty(prefix, "Prefix cannot be null or empty");
    }

    @Override
    @Nonnull
    public ServiceTicket createServiceTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final String service,
            @Nullable final TicketState state,
            final boolean renew) {
        Constraint.isNotNull(state, "State cannot be null");
        final ServiceTicket st = new ServiceTicket(
                Constraint.isNotNull(id, "ID cannot be null"),
                Constraint.isNotNull(service, "Service cannot be null"),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                renew);
        st.setTicketState(state);
        return encode(ServiceTicket.class, st, serviceTicketPrefix);
    }

    @Override
    @Nullable
    public ServiceTicket removeServiceTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        return decode(ServiceTicket.class, id, serviceTicketPrefix);
    }

    @Nonnull
    @Override
    public ProxyTicket createProxyTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final ProxyGrantingTicket pgt,
            @Nonnull final String service) {
        Constraint.isNotNull(pgt, "ProxyGrantingTicket cannot be null");
        final ProxyTicket pt = new ProxyTicket(
                NOT_USED,
                Constraint.isNotNull(service, "Service cannot be null"),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                pgt.getId());
        pt.setTicketState(pgt.getTicketState());
        return encode(ProxyTicket.class, pt, proxyTicketPrefix);
    }

    @Nullable
    @Override
    public ProxyTicket removeProxyTicket(final @Nonnull String id) {
        return decode(ProxyTicket.class, id, proxyTicketPrefix);
    }

    @Nullable
    @Override
    public ProxyGrantingTicket createProxyGrantingTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final ServiceTicket serviceTicket,
            @Nonnull final String pgtUrl) {
        Constraint.isNotNull(serviceTicket, "ServiceTicket cannot be null");
        final ProxyGrantingTicket pgt = new ProxyGrantingTicket(
                NOT_USED,
                serviceTicket.getService(),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                Constraint.isNotNull(pgtUrl, "pgtUrl cannot be null"),
                null);
        pgt.setTicketState(serviceTicket.getTicketState());
        return encode(ProxyGrantingTicket.class, pgt, proxyGrantingTicketPrefix);
    }

    @Nullable
    @Override
    public ProxyGrantingTicket fetchProxyGrantingTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        if (id.startsWith(proxyGrantingTicketPrefix + "-")) {
            return decode(ProxyGrantingTicket.class, id, proxyGrantingTicketPrefix);
        }
        return super.fetchProxyGrantingTicket(id);
    }

    @Override
    @Nullable
    public ProxyGrantingTicket removeProxyGrantingTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        if (id.startsWith(proxyGrantingTicketPrefix + "-")) {
            return decode(ProxyGrantingTicket.class, id, proxyGrantingTicketPrefix);
        }
        return super.removeProxyGrantingTicket(id);
    }

    /**
     * Encode a ticket.
     * 
     * @param ticketClass class of ticket
     * @param ticket ticket
     * @param prefix ticket ID prefix
     * @param <T> type of ticket
     * 
     * @return ticket encoded ticket
     */
    private <T extends Ticket> T encode(final Class<T> ticketClass, final T ticket, final String prefix) {
        final String opaque;
        try {
            opaque = dataSealer.wrap(serializer(ticketClass).serialize(ticket), ticket.getExpirationInstant());
        } catch (final Exception e) {
            throw new RuntimeException("Ticket encoding failed", e);
        }
        return ticketClass.cast(ticket.clone(prefix + '-' + opaque));
    }

    /**
     * Decode a ticket.
     * 
     * @param ticketClass class of ticket
     * @param id ticket ID
     * @param prefix ticket ID prefix
     * @param <T> type of ticket
     * 
     * @return decoded ticket
     */
    private <T extends Ticket> T decode(final Class<T> ticketClass, final String id, final String prefix) {
        try {
            final String decrypted = dataSealer.unwrap(id.substring(prefix.length() + 1));
            return serializer(ticketClass).deserialize(0, NOT_USED, id, decrypted, 0L);
        } catch (final Exception e) {
            log.warn("Ticket decoding failed with error: " + e.getMessage());
            log.debug("Ticket decoding failed", e);
        }
        return null;
    }
}

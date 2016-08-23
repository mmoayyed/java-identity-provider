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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.DataSealer;
import org.joda.time.Instant;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ticket service that uses two different strategies for ticket persistence:
 * <ol>
 *     <li>Service tickets are persisted by serializing data and encrypting it into the opaque section of the
 *     ticket ID using a {@link DataSealer}.</li>
 *     <li>Proxy-granting tickets and proxy tickets are persisted using a {@link StorageService}.</li>
 * </ol>
 * <p><strong>NOTE:</strong> This implementation does not support single-use service tickets. Since there is no
 * backing store for tickets, they can be reused indefinitely until their expiration date.</p>
 *
 * @author Marvin S. Addison
 * @since 3.3.0
 */
public class EncodingTicketService extends AbstractTicketService {

    /** Default service ticket prefix. */
    private static final String SERVICE_TICKET_PREFIX = "ST";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(EncodingTicketService.class);

    /** Data sealer that handles encryption of serialized service ticket data. */
    @Nonnull
    private final DataSealer dataSealer;

    /** Service ticket prefix. */
    @NotEmpty
    private String serviceTicketPrefix = SERVICE_TICKET_PREFIX;


    /**
     * Creates a new instance.
     *
     * @param service Storage service to which tickets are persisted.
     */
    public EncodingTicketService(@Nonnull final StorageService service, @Nonnull final DataSealer sealer) {
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
        final String opaque;
        try {
            opaque = dataSealer.wrap(
                    serializer(ServiceTicket.class).serialize(st), st.getExpirationInstant().getMillis());
        } catch (Exception e) {
            throw new RuntimeException("Ticket encoding failed", e);
        }
        final ServiceTicket encoded = new ServiceTicket(serviceTicketPrefix + '-' + opaque, service, expiry, renew);
        encoded.setTicketState(state);
        return encoded;
    }

    @Override
    @Nullable
    public ServiceTicket removeServiceTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        try {
            final String decrypted = dataSealer.unwrap(id.substring(serviceTicketPrefix.length() + 1));
            return serializer(ServiceTicket.class).deserialize(0, null, id, decrypted, 0L);
        } catch (Exception e) {
            log.warn("Ticket decoding failed with error: " + e.getMessage());
            log.debug("Ticket decoding failed", e);
        }
        return null;
    }
}

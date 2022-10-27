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

package net.shibboleth.idp.cas.ticket.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;

import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.config.ProxyConfiguration;
import net.shibboleth.idp.cas.config.ValidateConfiguration;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.cas.ticket.serialization.impl.ProxyGrantingTicketSerializer;
import net.shibboleth.idp.cas.ticket.serialization.impl.ProxyTicketSerializer;
import net.shibboleth.idp.cas.ticket.serialization.impl.ServiceTicketSerializer;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Abstract base class for ticket services that rely on {@link StorageService} for ticket storage.
 *
 * @author Marvin S. Addison
 * @since 3.3.0
 */
public abstract class AbstractTicketService implements TicketService {

    /** Map of ticket classes to context names. */
    @Nonnull private static final Map<Class<? extends Ticket>, String> CONTEXT_CLASS_MAP = new HashMap<>();

    /** Map of ticket classes to serializers. */
    @Nonnull  private static final Map<Class<? extends Ticket>, StorageSerializer<? extends Ticket>> SERIALIZER_MAP =
            new HashMap<>();

    /** Service ticket serializer. */
    @Nonnull private static final ServiceTicketSerializer ST_SERIALIZER = new ServiceTicketSerializer();
    
    /** Proxy ticket serialize. */
    @Nonnull private static final ProxyTicketSerializer PT_SERIALIZER = new ProxyTicketSerializer();
    
    /** Proxy granting ticket serializer. */
    @Nonnull private static final ProxyGrantingTicketSerializer PGT_SERIALIZER = new ProxyGrantingTicketSerializer();

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractTicketService.class);

    /** Storage service to which ticket persistence operations are delegated. */
    @Nonnull private final StorageService storageService;


    static {
        CONTEXT_CLASS_MAP.put(ServiceTicket.class, LoginConfiguration.PROFILE_ID);
        CONTEXT_CLASS_MAP.put(ProxyTicket.class, ProxyConfiguration.PROFILE_ID);
        CONTEXT_CLASS_MAP.put(ProxyGrantingTicket.class, ValidateConfiguration.PROFILE_ID);
        SERIALIZER_MAP.put(ServiceTicket.class, ST_SERIALIZER);
        SERIALIZER_MAP.put(ProxyTicket.class, PT_SERIALIZER);
        SERIALIZER_MAP.put(ProxyGrantingTicket.class, PGT_SERIALIZER);
    }

    /**
     * Creates a new instance.
     *
     * @param service Storage service to which tickets are persisted.
     */
    public AbstractTicketService(@Nonnull final StorageService service) {
        storageService = Constraint.isNotNull(service, "StorageService cannot be null.");
    }

    /** {@inheritDoc} */
    @Nonnull public ProxyGrantingTicket createProxyGrantingTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final ServiceTicket serviceTicket,
            @Nonnull final String pgtUrl) {
        Constraint.isNotNull(serviceTicket, "ServiceTicket cannot be null");
        final ProxyGrantingTicket pgt = new ProxyGrantingTicket(
                Constraint.isNotNull(id, "ID cannot be null"),
                serviceTicket.getService(),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                Constraint.isNotNull(pgtUrl, "pgtURL cannot be null"),
                null);
        pgt.setTicketState(serviceTicket.getTicketState());
        store(pgt);
        return pgt;
    }

    /** {@inheritDoc} */
    @Nonnull public ProxyGrantingTicket createProxyGrantingTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final ProxyTicket proxyTicket,
            @Nonnull final String pgtUrl) {
        Constraint.isNotNull(proxyTicket, "ProxyTicket cannot be null");
        final ProxyGrantingTicket pgt = new ProxyGrantingTicket(
                Constraint.isNotNull(id, "ID cannot be null"),
                proxyTicket.getService(),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
            Constraint.isNotNull(pgtUrl, "pgtURL cannot be null"),
                proxyTicket.getPgtId());
        pgt.setTicketState(proxyTicket.getTicketState());
        store(pgt);
        return pgt;
    }

    /** {@inheritDoc} */
    @Nullable public ProxyGrantingTicket fetchProxyGrantingTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        return read(id, ProxyGrantingTicket.class);
    }

    /** {@inheritDoc} */
    @Nullable public ProxyGrantingTicket removeProxyGrantingTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        final ProxyGrantingTicket pgt = delete(id, ProxyGrantingTicket.class);
        return pgt;
    }

    /**
     * Gets the storage service context name for the given ticket type.
     *
     * @param clazz Ticket class.
     *
     * @return Context name for ticket type.
     */
    @Nullable protected static String context(@Nonnull final Class<? extends Ticket> clazz) {
        return CONTEXT_CLASS_MAP.get(clazz);
    }

    /**
     * Gets the storage service serializer for the given ticket type.
     *
     * @param clazz Ticket class.
     * @param <T> type of object being serialized
     *
     * @return Storage service serializer.
     */
    @Nonnull protected static <T extends Ticket> StorageSerializer<T> serializer(@Nonnull final Class<T> clazz) {
        final StorageSerializer<T> result = (StorageSerializer<T>) Constraint.isNotNull(SERIALIZER_MAP.get(clazz),
                "Serializer for " + clazz + " not found");
        return result;
    }

    /**
     * Stores the given ticket in the storage service.
     *
     * @param ticket Ticket to store
     * @param <T> Type of ticket.
     */
    protected <T extends Ticket> void store(@Nonnull final T ticket) {
        try {
            final String sessionId = ticket.getSessionId();
            final long expiry = ticket.getExpirationInstant().toEpochMilli();
            final String ticketCtx;
            if (sessionId != null) {
                final String context = context(ticket.getClass());
                log.debug("Storing mapping of {} to {} in context {}", ticket, sessionId, context);
                if (!storageService.create(context, ticket.getId(), sessionId, expiry)) {
                    throw new RuntimeException("Failed to store ticket " + ticket);
                }
                ticketCtx = sessionId;
            } else {
                ticketCtx = ticket.getId();
            }
            log.debug("Storing {} in context {}", ticket, ticketCtx);
            if (!storageService.create(ticketCtx, ticket.getId(), ticket,
                    (StorageSerializer<T>) serializer(ticket.getClass()), expiry)) {
                throw new RuntimeException("Failed to store ticket " + ticket);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to store ticket " + ticket, e);
        }
    }

    /**
     * Retrieves a ticket by ID from the storage service.
     *
     * @param id Ticket identifier.
     * @param clazz Ticket type.
     * @param <T> Type of ticket.
     *
     * @return Ticket or null if ticket not found.
     */
    @Nullable protected <T extends Ticket> T read(@Nonnull final String id, @Nonnull final Class<T> clazz) {
        log.debug("Reading {}", id);
        final T ticket;
        try {
            final String context;
            final StorageRecord<T> sessionRecord = storageService.read(context(clazz), id);
            if (sessionRecord != null) {
                context = sessionRecord.getValue();
                log.debug("{} bound to session {}", id, context);
            } else {
                log.debug("{} not bound to any session. Using ticket ID for context.", id);
                context = id;
            }
            final StorageRecord<T> ticketRecord = storageService.read(context, id);
            if (ticketRecord == null) {
                log.debug("{} not found in context {}", id, context);
                return null;
            }
            ticket = ticketRecord.getValue(serializer(clazz), context, id);
        } catch (final IOException e) {
            throw new RuntimeException("Error reading ticket.");
        }
        return ticket;
    }

    /**
     * Retrieves a ticket by ID from the storage service and then deletes it.
     *
     * @param id Ticket identifier.
     * @param <T> Type of ticket.
     * @param clazz Ticket class
     *
     * @return Deleted ticket or null if ticket not found.
     */
    @Nullable protected <T extends Ticket> T delete(@Nonnull final String id, @Nonnull final Class<T> clazz) {
        final T ticket = read(id, clazz);
        if (ticket == null) {
            return null;
        }
        try {
            final String context = context(clazz);
            final String sessionId = ticket.getSessionId();
            final String ticketCtx;
            if (sessionId != null) {
                log.debug("Attempting to delete {} from context {}", id, context);
                if (!storageService.delete(context, id)) {
                    log.info("Failed deleting {} from context {}.", id, context);
                }
                ticketCtx = sessionId;
            } else {
                ticketCtx = id;
            }
            log.debug("Attempting to delete {} from context {}", id, ticketCtx);
            if (!storageService.delete(ticketCtx, id)) {
                log.info("Failed deleting {} from context {}.", id, ticketCtx);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error deleting ticket " + id, e);
        }
        return ticket;
    }

}
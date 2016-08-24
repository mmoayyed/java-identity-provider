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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.config.impl.LoginConfiguration;
import net.shibboleth.idp.cas.config.impl.ProxyConfiguration;
import net.shibboleth.idp.cas.config.impl.ValidateConfiguration;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketServiceEx;
import net.shibboleth.idp.cas.ticket.serialization.impl.ProxyGrantingTicketSerializer;
import net.shibboleth.idp.cas.ticket.serialization.impl.ProxyTicketSerializer;
import net.shibboleth.idp.cas.ticket.serialization.impl.ServiceTicketSerializer;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.joda.time.Instant;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for ticket services that rely on {@link StorageService} for ticket storage.
 *
 * @author Marvin S. Addison
 * @since 3.3.0
 */
public abstract class AbstractTicketService implements TicketServiceEx {

    /** Map of ticket classes to context names. */
    private static final Map<Class<? extends Ticket>, String> CONTEXT_CLASS_MAP = new HashMap<>();

    /** Map of ticket classes to serializers. */
    private static final Map<Class<? extends Ticket>, StorageSerializer<? extends Ticket>> SERIALIZER_MAP =
            new HashMap<>();

    private static final ServiceTicketSerializer ST_SERIALIZER = new ServiceTicketSerializer();
    private static final ProxyTicketSerializer PT_SERIALIZER = new ProxyTicketSerializer();
    private static final ProxyGrantingTicketSerializer PGT_SERIALIZER = new ProxyGrantingTicketSerializer();

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractTicketService.class);

    /** Storage service to which ticket persistence operations are delegated. */
    @Nonnull
    private final StorageService storageService;


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
        this.storageService = Constraint.isNotNull(service, "StorageService cannot be null.");
    }

    @Override
    @Nonnull
    public ServiceTicket createServiceTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final String sessionId,
            @Nonnull final String service,
            final boolean renew) {
        throw new UnsupportedOperationException("This version of createServiceTicket is not supported");
    }

    @Override
    @Nonnull
    public ProxyGrantingTicket createProxyGrantingTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final ServiceTicket serviceTicket) {
        Constraint.isNotNull(serviceTicket, "ServiceTicket cannot be null");
        final ProxyGrantingTicket pgt = new ProxyGrantingTicket(
                Constraint.isNotNull(id, "ID cannot be null"),
                serviceTicket.getService(),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                null);
        pgt.setTicketState(serviceTicket.getTicketState());
        store(pgt);
        return pgt;
    }

    @Override
    @Nonnull
    public ProxyGrantingTicket createProxyGrantingTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final ProxyTicket proxyTicket) {
        Constraint.isNotNull(proxyTicket, "ProxyTicket cannot be null");
        final ProxyGrantingTicket pgt = new ProxyGrantingTicket(
                Constraint.isNotNull(id, "ID cannot be null"),
                proxyTicket.getService(),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                proxyTicket.getPgtId());
        pgt.setTicketState(proxyTicket.getTicketState());
        store(pgt);
        return pgt;
    }

    @Override
    @Nullable
    public ProxyGrantingTicket fetchProxyGrantingTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        return read(id, ProxyGrantingTicket.class);
    }

    @Override
    @Nullable
    public ProxyGrantingTicket removeProxyGrantingTicket(@Nonnull final String id) {
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
    protected static String context(final Class<? extends Ticket> clazz) {
        return CONTEXT_CLASS_MAP.get(clazz);
    }

    /**
     * Gets the storage service serializer for the given ticket type.
     *
     * @param clazz Ticket class.
     *
     * @return Storage service serializer.
     */
    protected static <T extends Ticket> StorageSerializer<T> serializer(final Class<T> clazz) {
        return (StorageSerializer<T>) SERIALIZER_MAP.get(clazz);
    }

    /**
     * Stores the given ticket in the storage service.
     *
     * @param ticket Ticket to store
     * @param <T> Type of ticket.
     */
    protected <T extends Ticket> void store(final T ticket) {
        final String context = context(ticket.getClass());
        log.debug("Storing {} in context {}", ticket, context);
        try {
            if (!storageService.create(
                    context,
                    ticket.getId(),
                    ticket,
                    serializer(ticket.getClass()),
                    ticket.getExpirationInstant().getMillis())) {
                throw new RuntimeException("Failed to store ticket " + ticket);
            }
        } catch (IOException e) {
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
    protected <T extends Ticket> T read(final String id, final Class<T> clazz) {
        log.debug("Reading {}", id);
        final T ticket;
        try {
            final String context = context(clazz);
            final StorageRecord<T> record = storageService.read(context, id);
            if (record == null) {
                log.debug("{} not found in context {}", id, context);
                return null;
            }
            ticket = record.getValue(serializer(clazz), context, id);
        } catch (IOException e) {
            throw new RuntimeException("Error reading ticket.");
        }
        return ticket;
    }

    /**
     * Retrieves a ticket by ID from the storage service and then deletes it.
     *
     * @param id Ticket identifier.
     * @param <T> Type of ticket.
     *
     * @return Deleted ticket or null if ticket not found.
     */
    protected <T extends Ticket> T delete(final String id, final Class<T> clazz) {
        final T ticket = read(id, clazz);
        if (ticket == null) {
            return null;
        }
        try {
            final String context = context(clazz);
            log.debug("Attempting to delete {} from context {}", ticket, context);
            if (!storageService.delete(context, id)) {
                log.info("Failed deleting {}. Ticket probably expired from storage service.", id);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error deleting ticket " + id, e);
        }
        return ticket;
    }


}

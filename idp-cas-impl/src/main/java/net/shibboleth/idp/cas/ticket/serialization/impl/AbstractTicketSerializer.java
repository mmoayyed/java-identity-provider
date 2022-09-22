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

package net.shibboleth.idp.cas.ticket.serialization.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for ticket serializers that use a simple field-delimited serialization strategy.
 * Tickets are expected to be stored using the ticket ID as a key, so the ticket ID is not contained as part
 * of the serialized form.
 * 
 * @param <T> type of ticket
 *
 * @author Marvin S. Addison
 */
public abstract class AbstractTicketSerializer<T extends Ticket> implements StorageSerializer<T> {

    /** Service field name. */
    @Nonnull @NotEmpty private static final String SERVICE_FIELD = "rp";

    /** Expiration instant field name. */
    @Nonnull @NotEmpty private static final String EXPIRATION_FIELD = "exp";

    /** Supplemental ticket state field name. */
    @Nonnull @NotEmpty private static final String STATE_FIELD = "ts";

    /** Session ID field name. */
    @Nonnull @NotEmpty private static final String SESSION_FIELD = "sid";

    /** Authenticated canonical principal name field. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_FIELD = "p";

    /** Authentication instant field name. */
    @Nonnull @NotEmpty private static final String AUTHN_INSTANT_FIELD = "ai";

    /** Authentication method field name. */
    @Nonnull @NotEmpty private static final String AUTHN_METHOD_FIELD = "am";
    
    /** Consented attribute IDs field name. */
    @Nonnull @NotEmpty private static final String CONSENTED_ATTRS_FIELD = "con";

    /** Logger instance. */
    @Nonnull private final Logger logger = LoggerFactory.getLogger(AbstractTicketSerializer.class);

    /** JSON generator factory. */
    @Nonnull
    private final JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(null);

    /** JSON reader factory. */
    @Nonnull
    private final JsonReaderFactory readerFactory = Json.createReaderFactory(null);

    @Override
    public void initialize() throws ComponentInitializationException {}

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    @Nonnull
    public String serialize(@Nonnull final T ticket) throws IOException {
        final StringWriter buffer = new StringWriter(200);
        try (final JsonGenerator gen = generatorFactory.createGenerator(buffer)) {
            gen.writeStartObject()
                    .write(SERVICE_FIELD, ticket.getService())
                    .write(EXPIRATION_FIELD, ticket.getExpirationInstant().toEpochMilli());
            
            if (ticket.getTicketState() != null) {
                gen.writeStartObject(STATE_FIELD)
                        .write(SESSION_FIELD, ticket.getTicketState().getSessionId())
                        .write(PRINCIPAL_FIELD, ticket.getTicketState().getPrincipalName())
                        .write(AUTHN_INSTANT_FIELD, ticket.getTicketState().getAuthenticationInstant().toEpochMilli())
                        .write(AUTHN_METHOD_FIELD, ticket.getTicketState().getAuthenticationMethod());
                
                if (ticket.getTicketState().getConsentedAttributeIds() != null) {
                    gen.writeStartArray(CONSENTED_ATTRS_FIELD);
                    for (final String id : ticket.getTicketState().getConsentedAttributeIds()) {
                        gen.write(id);
                    }
                    gen.writeEnd();
                }
                
                gen.writeEnd();
            }
            serializeInternal(gen, ticket);
            gen.writeEnd();
        } catch (final JsonException e) {
            logger.error("Exception serializing {}", ticket, e);
            throw new IOException("Exception serializing ticket", e);
        }
        return buffer.toString();
    }

    @Override
    @Nonnull
    public T deserialize(
            final long version,
            @Nonnull @NotEmpty final String context,
            @Nonnull @NotEmpty final String key,
            @Nonnull @NotEmpty final String value,
            @Nullable final Long expiration) throws IOException {

        try (final JsonReader reader = readerFactory.createReader(new StringReader(value))) {
            final JsonObject to = reader.readObject();
            final String service = to.getString(SERVICE_FIELD);
            final Instant expiry = Instant.ofEpochMilli(to.getJsonNumber(EXPIRATION_FIELD).longValueExact());
            final JsonObject so = to.getJsonObject(STATE_FIELD);
            final TicketState state;
            if (so != null) {
                state = new TicketState(
                        so.getString(SESSION_FIELD),
                        so.getString(PRINCIPAL_FIELD),
                        Instant.ofEpochMilli(so.getJsonNumber(AUTHN_INSTANT_FIELD).longValueExact()),
                        so.getString(AUTHN_METHOD_FIELD));
                final JsonValue consent = so.get(CONSENTED_ATTRS_FIELD);
                if (consent instanceof JsonArray) {
                    final Set<String> idset = new HashSet<>();
                    for (final JsonValue id : (JsonArray) consent) {
                        if (id instanceof JsonString) {
                            idset.add(((JsonString) id).getString());
                        }
                    }
                    state.setConsentedAttributeIds(idset);
                }
            } else {
                state = null;
            }
            final T ticket = createTicket(to, key, service, expiry);
            ticket.setTicketState(state);
            return ticket;
        } catch (final JsonException e) {
            logger.error("Exception deserializing {}", value, e);
            throw new IOException("Exception deserializing ticket", e);
        }
    }

    /**
     * Create a ticket.
     * 
     * @param o JSON object
     * @param id ticket ID
     * @param service service that requested the ticket
     * @param expiry expiration instant
     * 
     * @return the newly created ticket
     */
    protected abstract T createTicket(
            @Nonnull final JsonObject o,
            @Nonnull final String id,
            @Nonnull final String service,
            @Nonnull final Instant expiry);

    /**
     * Serialize a ticket.
     * 
     * @param generator JSON generator
     * @param ticket ticket
     */
    protected abstract void serializeInternal(@Nonnull final JsonGenerator generator, @Nonnull final T ticket);
}

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

package net.shibboleth.idp.session;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link SPSession} serializers that handles data common to all such objects.
 */
@ThreadSafe
public abstract class AbstractSPSessionSerializer extends AbstractInitializableComponent
        implements StorageSerializer<SPSession> {

    /** Field name of service ID. */
    @Nonnull @NotEmpty private static final String SERVICE_ID_FIELD = "id";

    /** Field name of creation instant. */
    @Nonnull @NotEmpty private static final String CREATION_INSTANT_FIELD = "ts";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSPSessionSerializer.class);

    /** Milliseconds to subtract from record expiration to establish session expiration value. */
    @Nonnull private final Duration expirationOffset;
    
    /**
     * Constructor.
     * 
     * @param offset time to subtract from record expiration to establish session expiration value
     */
    protected AbstractSPSessionSerializer(@Nonnull final Duration offset) {
        expirationOffset = Constraint.isNotNull(offset, "Offset cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final SPSession instance) throws IOException {
        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = Json.createGenerator(sink);
            gen.writeStartObject()
                .write(SERVICE_ID_FIELD, instance.getId())
                .write(CREATION_INSTANT_FIELD, instance.getCreationInstant().toEpochMilli());
            
            doSerializeAdditional(instance, gen);
            
            gen.writeEnd().close();
            
            return sink.toString();
        } catch (final JsonException e) {
            log.error("Exception while serializing SPSession: {}", e.getMessage());
            throw new IOException("Exception while serializing SPSession", e);
        }
    }

    /** {@inheritDoc} */
    @Nonnull public SPSession deserialize(final long version, @Nonnull @NotEmpty final String context,
            @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value, @Nullable final Long expiration)
                    throws IOException {
        
        if (expiration == null) {
            throw new IOException("SPSession objects must have an expiration");
        }

        try {
            final JsonReader reader = Json.createReader(new StringReader(value));
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing SPSession");
            }
            final JsonObject obj = (JsonObject) st;
            
            final String serviceId = obj.getString(SERVICE_ID_FIELD);
            final Instant creation = Instant.ofEpochMilli(obj.getJsonNumber(CREATION_INSTANT_FIELD).longValueExact());

            return doDeserialize(obj, serviceId, creation, Instant.ofEpochMilli(expiration).minus(expirationOffset));
            
        } catch (final NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            log.error("Exception while parsing SPSession: {}", e.getMessage());
            throw new IOException("Found invalid data structure while parsing SPSession", e);
        }
    }

    /**
     * Override this method to handle serialization of additional data.
     * 
     * <p>The serialization "context" is the continuation of a JSON struct.</p>
     * 
     * @param instance object to serialize
     * @param generator JSON generator to write to
     */
    protected void doSerializeAdditional(@Nonnull final SPSession instance,
            @Nonnull final JsonGenerator generator) {
        
    }
    
    /**
     * Implement this method to return the appropriate type of object, populated with the basic
     * information supplied.
     * 
     * <p>The JSON object supplied is a structure that may contain additional data created by the
     * concrete subclass during serialization.</p>
     * 
     * @param obj JSON structure to parse
     * @param id the identifier of the service associated with this session
     * @param creation creation time of session
     * @param expiration expiration time of session
     * 
     * @return the newly constructed object
     * @throws IOException if an error occurs during deserialization
     */
    @Nonnull protected abstract SPSession doDeserialize(@Nonnull final JsonObject obj,
            @Nonnull @NotEmpty final String id, @Nonnull final Instant creation,
            @Nonnull final Instant expiration) throws IOException;
    
}
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link ServiceSession} serializers that handles data common to all such objects.
 */
@ThreadSafe
public abstract class AbstractServiceSessionSerializer implements StorageSerializer<ServiceSession> {

    /** Field name of service ID. */
    private static final String SERVICE_ID_FIELD = "id";

    /** Field name of creation instant. */
    private static final String CREATION_INSTANT_FIELD = "ts";

    /** Field name of Flow ID. */
    private static final String FLOW_ID_FIELD = "flow";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractServiceSessionSerializer.class);
    
    /** Constructor. */
    protected AbstractServiceSessionSerializer() {

    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final ServiceSession instance) throws IOException {
        
        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = Json.createGenerator(sink);
            gen.writeStartObject()
                .write(SERVICE_ID_FIELD, instance.getId())
                .write(CREATION_INSTANT_FIELD, instance.getCreationInstant())
                .write(FLOW_ID_FIELD, instance.getAuthenticationFlowId());
            
            doSerializeAdditional(gen);
            
            gen.writeEnd().close();
            
            return sink.toString();
        } catch (JsonException e) {
            log.error("Exception while serializing ServiceSession", e);
            throw new IOException("Exception while serializing ServiceSession", e);
        }
    }

    /** {@inheritDoc} */
    @Nonnull public ServiceSession deserialize(@Nonnull @NotEmpty final String value,
            @Nullable final String context, @Nullable final String key, @Nullable final Long expiration)
            throws IOException {
        
        if (expiration == null) {
            throw new IOException("ServiceSession objects must have an expiration");
        }

        try {
            JsonReader reader = Json.createReader(new StringReader(value));
            JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing ServiceSession");
            }
            JsonObject obj = (JsonObject) st;
            
            String serviceId = obj.getString(SERVICE_ID_FIELD);
            long creation = obj.getJsonNumber(CREATION_INSTANT_FIELD).longValueExact();
            String flowId = obj.getString(FLOW_ID_FIELD);

            return doDeserialize(obj, serviceId, flowId, creation, expiration);
            
        } catch (NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            log.error("Exception while parsing ServiceSession", e);
            throw new IOException("Found invalid data structure while parsing ServiceSession", e);
        }
    }

    /**
     * Override this method to handle serialization of additional data.
     * 
     * <p>The serialization "context" is the continuation of a JSON struct.</p>
     * 
     * @param generator JSON generator to write to
     */
    protected void doSerializeAdditional(@Nonnull final JsonGenerator generator) {
        
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
     * @param flowId authentication flow used to authenticate the principal to this service
     * @param creation creation time of session, in milliseconds since the epoch
     * @param expiration expiration time of session, in milliseconds since the epoch
     * 
     * @return the newly constructed object
     * @throws IOException if an error occurs during deserialization
     */
    @Nonnull protected abstract ServiceSession doDeserialize(@Nonnull final JsonObject obj,
            @Nonnull @NotEmpty final String id, @Nonnull @NotEmpty final String flowId,
            final long creation, final long expiration) throws IOException;
    
}
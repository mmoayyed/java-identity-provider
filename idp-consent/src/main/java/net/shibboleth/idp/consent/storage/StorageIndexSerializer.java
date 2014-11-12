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

package net.shibboleth.idp.consent.storage;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Storage serializer for map of {@link StorageIndex} objects keyed by the {@link StorageIndex} context. */
// TODO tests
public class StorageIndexSerializer extends AbstractInitializableComponent implements
        StorageSerializer<Map<String, StorageIndex>> {

    /** Field name of storage index context. */
    @Nonnull @NotEmpty private static final String CONTEXT_FIELD = "ctx";

    /** Field name of storage index keys. */
    @Nonnull @NotEmpty private static final String KEYS_FIELD = "keys";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageIndexSerializer.class);

    /** JSON generator factory. */
    @Nonnull private final JsonGeneratorFactory generatorFactory;

    /** JSON reader factory. */
    @Nonnull private final JsonReaderFactory readerFactory;

    /** Constructor. */
    public StorageIndexSerializer() {
        generatorFactory = Json.createGeneratorFactory(null);
        readerFactory = Json.createReaderFactory(null);
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Map<String, StorageIndex> instance) throws IOException {
        Constraint.isNotNull(instance, "Map of storage indexes cannot be null");

        // TODO what should be returned in this case ?

        if (instance.isEmpty()) {
            return "";
        }

        final StringWriter sink = new StringWriter(128);
        final JsonGenerator gen = generatorFactory.createGenerator(sink);

        gen.writeStartArray();
        for (final StorageIndex storageIndex : instance.values()) {
            gen.writeStartObject();
            gen.write(CONTEXT_FIELD, storageIndex.getContext());
            if (!storageIndex.getKeys().isEmpty()) {
                gen.writeStartArray(KEYS_FIELD);
                for (final String key : storageIndex.getKeys()) {
                    gen.write(key);
                }
                gen.writeEnd();
            }
            gen.writeEnd();
        }
        gen.writeEnd();
        gen.close();

        final String serialized = sink.toString();
        log.debug("Serialized '{}' as '{}", instance, serialized);
        return serialized;
    }

    /** {@inheritDoc} */
    public Map<String, StorageIndex>
            deserialize(final long version, @Nonnull @NotEmpty final String context,
                    @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value,
                    @Nullable final Long expiration) throws IOException {

        try (final JsonReader reader = readerFactory.createReader(new StringReader(value))) {
            final JsonStructure st = reader.read();

            if (!(st instanceof JsonArray)) {
                throw new IOException("Found invalid data structure while parsing storage index");
            }

            final Map<String, StorageIndex> storageIndexes = new LinkedHashMap<>();

            for (final JsonValue arrayValue : (JsonArray) st) {
                if (arrayValue.getValueType().equals(ValueType.OBJECT)) {
                    final JsonObject o = (JsonObject) arrayValue;
                    final StorageIndex storageIndex = new StorageIndex();
                    storageIndex.setContext(o.getString(CONTEXT_FIELD));
                    final JsonArray keysArray = o.getJsonArray(KEYS_FIELD);
                    if (keysArray != null) {
                        for (final JsonValue keyValue : keysArray) {
                            if (keyValue instanceof JsonString) {
                                storageIndex.getKeys().add(((JsonString) keyValue).getString());
                            }
                        }
                    }

                    storageIndexes.put(storageIndex.getContext(), storageIndex);
                }
            }

            log.debug("Deserialized context '{}' key '{}' value '{}' expiration '{}' as '{}", new Object[] {context,
                    key, value, expiration, storageIndexes,});
            return storageIndexes;
        } catch (final NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            log.error("Exception while parsing storage index", e);
            throw new IOException("Found invalid data structure while parsing storage index", e);
        }
    }

}

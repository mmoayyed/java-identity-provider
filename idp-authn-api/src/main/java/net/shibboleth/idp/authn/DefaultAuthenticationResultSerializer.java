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

package net.shibboleth.idp.authn;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.principal.GenericPrincipalSerializer;
import net.shibboleth.idp.authn.principal.PrincipalSerializer;
import net.shibboleth.idp.authn.principal.UsernamePrincipalSerializer;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Handles serialization of results, delegating handling of {@link Principal} objects to one or more
 * {@link PrincipalSerializer} plugins.
 */
public class DefaultAuthenticationResultSerializer extends AbstractInitializableComponent
        implements StorageSerializer<AuthenticationResult> {

    /** Field name of Flow ID. */
    @Nonnull @NotEmpty private static final String FLOW_ID_FIELD = "id";

    /** Field name of authentication instant. */
    @Nonnull @NotEmpty private static final String AUTHN_INSTANT_FIELD = "ts";

    /** Field name of principal array. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_ARRAY_FIELD = "princ";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DefaultAuthenticationResultSerializer.class);

    /** JSON generator factory. */
    @Nonnull private final JsonGeneratorFactory generatorFactory;

    /** JSON reader factory. */
    @Nonnull private final JsonReaderFactory readerFactory;

    /** Principal serializers. */
    @Nonnull @NonnullElements private Collection<PrincipalSerializer<String>> principalSerializers;

    /** Generic principal serializer for any unsupported principals. */
    @Nonnull private final GenericPrincipalSerializer genericSerializer;

    /** Constructor. */
    public DefaultAuthenticationResultSerializer() {
        generatorFactory = Json.createGeneratorFactory(null);
        readerFactory = Json.createReaderFactory(null);
        
        principalSerializers =
                Collections.<PrincipalSerializer<String>>singletonList(new UsernamePrincipalSerializer());
        
        genericSerializer = new GenericPrincipalSerializer();
    }

    /**
     * Set the principal serializers used for principals found in the {@link AuthenticationResult}.
     * 
     * @param serializers principal serializers to use
     */
    public void setPrincipalSerializers(
            @Nonnull @NonnullElements Collection<PrincipalSerializer<String>> serializers) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        principalSerializers = Lists.newArrayList(Collections2.filter(serializers, Predicates.notNull()));
    }

    /**
     * Returns the {@GenericPrincipalSerializer} used for any unsupported principals found
     * in the {@link AuthenticationResult}.
     * 
     * @return generic principal serializer
     */
    @Nonnull public GenericPrincipalSerializer getGenericPrincipalSerializer() {
        return genericSerializer;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final AuthenticationResult instance) throws IOException {

        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = generatorFactory.createGenerator(sink);
            gen.writeStartObject().write(FLOW_ID_FIELD, instance.getAuthenticationFlowId())
                    .write(AUTHN_INSTANT_FIELD, instance.getAuthenticationInstant())
                    .writeStartArray(PRINCIPAL_ARRAY_FIELD);

            for (Principal p : instance.getSubject().getPrincipals()) {
                boolean serialized = false;
                for (PrincipalSerializer<String> serializer : principalSerializers) {
                    if (serializer.supports(p)) {
                        final JsonReader reader = readerFactory.createReader(new StringReader(serializer.serialize(p)));
                        try {
                            gen.write(reader.readObject());
                        } finally {
                            reader.close();
                        }
                        serialized = true;
                    }
                }
                if (!serialized && genericSerializer.supports(p)) {
                    final JsonReader reader =
                            readerFactory.createReader(new StringReader(genericSerializer.serialize(p)));
                    try {
                        gen.write(reader.readObject());
                    } finally {
                        reader.close();
                    }
                }
            }

            // TODO handle custom creds

            gen.writeEnd().writeEnd().close();

            return sink.toString();
        } catch (JsonException e) {
            log.error("Exception while serializing AuthenticationResult", e);
            throw new IOException("Exception while serializing AuthenticationResult", e);
        }
    }

    /** {@inheritDoc} */
    @Nonnull public AuthenticationResult
            deserialize(final int version, @Nonnull @NotEmpty final String context,
                    @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value,
                    @Nullable final Long expiration) throws IOException {

        try {
            final JsonReader reader = readerFactory.createReader(new StringReader(value));
            JsonStructure st = null;
            try {
                st = reader.read();
            } finally {
                reader.close();
            }
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing AuthenticationResult");
            }
            final JsonObject obj = (JsonObject) st;

            final String flowId = obj.getString(FLOW_ID_FIELD);
            long authnInstant = obj.getJsonNumber(AUTHN_INSTANT_FIELD).longValueExact();
            final JsonArray principals = obj.getJsonArray(PRINCIPAL_ARRAY_FIELD);

            final AuthenticationResult result = new AuthenticationResult(flowId, new Subject());
            result.setAuthenticationInstant(authnInstant);
            result.setLastActivityInstant(expiration != null ? expiration : authnInstant);

            for (JsonValue p : principals) {
                if (p instanceof JsonObject) {
                    final String json = ((JsonObject) p).toString();
                    boolean deserialized = false;
                    for (PrincipalSerializer serializer : principalSerializers) {
                        if (serializer.supports(json)) {
                            final Principal principal = serializer.deserialize(json);
                            if (principal != null) {
                                result.getSubject().getPrincipals().add(principal);
                                deserialized = true;
                            }
                        }
                    }
                    if (!deserialized && genericSerializer.supports(json)) {
                        final Principal principal = genericSerializer.deserialize(json);
                        if (principal != null) {
                            result.getSubject().getPrincipals().add(principal);
                        }
                    }
                }
            }

            // TODO handle custom creds

            return result;

        } catch (NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            log.error("Exception while parsing AuthenticationResult", e);
            throw new IOException("Found invalid data structure while parsing AuthenticationResult", e);
        }
    }

}
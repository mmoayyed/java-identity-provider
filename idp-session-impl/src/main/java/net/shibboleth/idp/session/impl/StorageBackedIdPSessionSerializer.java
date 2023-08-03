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

package net.shibboleth.idp.session.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.session.AbstractIdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A serializer for instances of {@link StorageBackedIdPSession} designed in conjunction with the
 * {@link org.opensaml.storage.StorageService}-backed {@link net.shibboleth.idp.session.SessionManager} implementation.
 */
@ThreadSafe
public class StorageBackedIdPSessionSerializer extends AbstractInitializableComponent implements
        StorageSerializer<StorageBackedIdPSession> {

    /** Field name of creation instant. */
    @Nonnull @NotEmpty private static final String CREATION_INSTANT_FIELD = "ts";

    /** Field name of principal name. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_NAME_FIELD = "nam";

    /** Field name of IPv4 address. */
    @Nonnull @NotEmpty private static final String IPV4_ADDRESS_FIELD = "v4";

    /** Field name of IPv6 address. */
    @Nonnull @NotEmpty private static final String IPV6_ADDRESS_FIELD = "v6";

    /** Field name of Unknown address. */
    @Nonnull @NotEmpty private static final String UNK_ADDRESS_FIELD = "unk";

    /** Field name of flow ID array. */
    @Nonnull @NotEmpty private static final String FLOW_ID_ARRAY_FIELD = "flows";

    /** Field name of service ID array. */
    @Nonnull @NotEmpty private static final String SERVICE_ID_ARRAY_FIELD = "svcs";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageBackedIdPSessionSerializer.class);

    /** Back-reference to parent instance. */
    @Nonnull private final StorageBackedSessionManager sessionManager;

    /** Object instance to overwrite with deserialization method. */
    @Nullable private final StorageBackedIdPSession targetObject;

    /** JsonProvider to be used for session serialization. */
    @Nonnull private final JsonProvider jsonProvider;
    /**
     * Constructor.
     * 
     * @param manager parent SessionManager instance
     * @param target object to overwrite when deserializing instead of creating a new instance
     */
    public StorageBackedIdPSessionSerializer(@Nonnull final StorageBackedSessionManager manager,
            @Nullable final StorageBackedIdPSession target) {
        sessionManager = Constraint.isNotNull(manager, "SessionManager cannot be null");
        targetObject = target;
        final JsonProvider prov = JsonProvider.provider();
        assert prov!=null;
        jsonProvider = prov;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override @Nonnull @NotEmpty public String serialize(@Nonnull final StorageBackedIdPSession instance)
            throws IOException {

        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = jsonProvider.createGenerator(sink);
            gen.writeStartObject().write(CREATION_INSTANT_FIELD, instance.getCreationInstant().toEpochMilli())
                    .write(PRINCIPAL_NAME_FIELD, instance.getPrincipalName());

            if (instance.getAddress(AbstractIdPSession.AddressFamily.IPV4) != null) {
                gen.write(IPV4_ADDRESS_FIELD, instance.getAddress(AbstractIdPSession.AddressFamily.IPV4));
            }

            if (instance.getAddress(AbstractIdPSession.AddressFamily.IPV6) != null) {
                gen.write(IPV6_ADDRESS_FIELD, instance.getAddress(AbstractIdPSession.AddressFamily.IPV6));
            }

            if (instance.getAddress(AbstractIdPSession.AddressFamily.UNKNOWN) != null) {
                gen.write(UNK_ADDRESS_FIELD, instance.getAddress(AbstractIdPSession.AddressFamily.UNKNOWN));
            }
            
            final Set<AuthenticationResult> results = instance.getAuthenticationResults();
            if (!results.isEmpty()) {
                gen.writeStartArray(FLOW_ID_ARRAY_FIELD);
                for (final AuthenticationResult result : results) {
                    gen.write(result.getAuthenticationFlowId());
                }
                gen.writeEnd();
            }

            if (sessionManager.isTrackSPSessions()) {
                if (sessionManager.storageServiceMeetsThreshold()) {
                    final Set<SPSession> services = instance.getSPSessions();
                    if (!services.isEmpty()) {
                        gen.writeStartArray(SERVICE_ID_ARRAY_FIELD);
                        for (final SPSession service : services) {
                            gen.write(service.getId());
                        }
                        gen.writeEnd();
                    }
                } else {
                    log.debug("Unable to serialize SP session due to to storage service limitations");
                }
            }

            gen.writeEnd().close();
            final String val = sink.toString();
            assert val != null;
            return val;
        } catch (final JsonException e) {
            log.error("Exception while serializing IdPSession: {}", e.getMessage());
            throw new IOException("Exception while serializing IdPSession", e);
        }
    }

// Checkstyle: MethodLength OFF
    /** {@inheritDoc} */
    @Override
    @Nonnull public StorageBackedIdPSession deserialize(final long version,
            @Nonnull @NotEmpty final String context, @Nonnull @NotEmpty final String key,
            @Nonnull @NotEmpty final String value, @Nullable final Long expiration) throws IOException {

        if (expiration == null) {
            throw new IOException("IdPSession objects must have an expiration");
        }

        try {
            final JsonReader reader = jsonProvider.createReader(new StringReader(value));
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing IdPSession");
            }
            final JsonObject obj = (JsonObject) st;

            // Create new object if necessary.
            StorageBackedIdPSession objectToPopulate = targetObject;
            if (objectToPopulate == null) {
                final Instant creation =
                        Instant.ofEpochMilli(obj.getJsonNumber(CREATION_INSTANT_FIELD).longValueExact());
                final String principalName = obj.getString(PRINCIPAL_NAME_FIELD);
                assert principalName!=null && creation != null;
                objectToPopulate = new StorageBackedIdPSession(sessionManager, context, principalName, creation);
            }

            // Populate fields in-place, bypassing any storage interactions.
            objectToPopulate.setVersion(version);
            final Instant lastActivityInstant =
                    Instant.ofEpochMilli(expiration).minus(sessionManager.getSessionTimeout()).minus(
                            sessionManager.getSessionSlop());
            assert lastActivityInstant!=null;
            objectToPopulate.doSetLastActivityInstant(lastActivityInstant);
            if (obj.containsKey(IPV4_ADDRESS_FIELD)) {
                final String addr = obj.getString(IPV4_ADDRESS_FIELD);
                assert addr != null;
                objectToPopulate.doBindToAddress(addr);
            }
            if (obj.containsKey(IPV6_ADDRESS_FIELD)) {
                final String addr = obj.getString(IPV6_ADDRESS_FIELD);
                assert addr != null;
                objectToPopulate.doBindToAddress(addr);
            }
            if (obj.containsKey(UNK_ADDRESS_FIELD)) {
                final String addr = obj.getString(UNK_ADDRESS_FIELD);
                assert addr != null;
                objectToPopulate.doBindToAddress(addr);
            }

            objectToPopulate.getAuthenticationResultMap().clear();
            if (obj.containsKey(FLOW_ID_ARRAY_FIELD)) {
                final JsonArray flowIds = obj.getJsonArray(FLOW_ID_ARRAY_FIELD);
                if (flowIds != null) {
                    for (final JsonString flowId : flowIds.getValuesAs(JsonString.class)) {
                        // An absent mapping is used to signify the existence of a result not yet loaded.
                        objectToPopulate.getAuthenticationResultMap().put(flowId.getString(), Optional.empty());
                    }
                }
            }

            objectToPopulate.getSPSessionMap().clear();
            if (obj.containsKey(SERVICE_ID_ARRAY_FIELD)) {
                final JsonArray svcIds = obj.getJsonArray(SERVICE_ID_ARRAY_FIELD);
                if (svcIds != null) {
                    for (final JsonString svcId : svcIds.getValuesAs(JsonString.class)) {
                        // An absent mapping is used to signify the existence of a session not yet loaded.
                        objectToPopulate.getSPSessionMap().put(svcId.getString(), Optional.empty());
                    }
                }
            }

            return objectToPopulate;

        } catch (final NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            log.error("Exception while parsing IdPSession: {}", e.getMessage());
            throw new IOException("Found invalid data structure while parsing IdPSession", e);
        }
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON

}
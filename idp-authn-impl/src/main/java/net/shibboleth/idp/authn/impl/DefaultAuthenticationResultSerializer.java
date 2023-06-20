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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.security.auth.Subject;

import org.opensaml.security.x509.X509Support;
import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.principal.GenericPrincipalSerializer;
import net.shibboleth.idp.authn.principal.PrincipalSerializer;
import net.shibboleth.idp.authn.principal.PrincipalService;
import net.shibboleth.idp.authn.principal.PrincipalServiceManager;
import net.shibboleth.idp.authn.principal.impl.AuthenticationResultPrincipalSerializer;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.EncodingException;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

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

    /** Field name of public credentials array. */
    @Nonnull @NotEmpty private static final String PUB_CREDS_ARRAY_FIELD = "pub";

    /** Field name of X.509 certificates array. */
    @Nonnull @NotEmpty private static final String X509_CREDS_ARRAY_FIELD = "x509";

    /** Field name of private credentials array. */
    @Nonnull @NotEmpty private static final String PRIV_CREDS_ARRAY_FIELD = "priv";
    
    /** Field name of private credentials array. */
    @Nonnull @NotEmpty private static final String ADDTL_DATA_FIELD = "props";    

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DefaultAuthenticationResultSerializer.class);

    /** JSON generator factory. */
    @Nonnull private final JsonGeneratorFactory generatorFactory;

    /** JSON reader factory. */
    @Nonnull private final JsonReaderFactory readerFactory;

    /** Manager for principal services. */
    @Nonnull private final PrincipalServiceManager principalServiceManager;

    /** Principal serializers. */
    @Nonnull private Collection<PrincipalSerializer<String>> principalSerializers;

    /**
     * Specialized serializer for {@link net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal}
     * that requires a circular ref.
     */
    @Nonnull private final AuthenticationResultPrincipalSerializer authnResultPrincipalSerializer;
    
    /** Generic principal serializer for any unsupported principals. */
    @Nonnull private final GenericPrincipalSerializer genericSerializer;

    /**
     * Constructor.
     * 
     * <p>This is mostly left to facilitate tests that can live with essentially no real
     * serialization support.</p>
     *
     * @throws ComponentInitializationException if unable to instantiate internal defaults
     */
    public DefaultAuthenticationResultSerializer() throws ComponentInitializationException {
        generatorFactory = Json.createGeneratorFactory(null);
        readerFactory = Json.createReaderFactory(null);
        
        principalSerializers = CollectionSupport.emptyList();
        authnResultPrincipalSerializer = new AuthenticationResultPrincipalSerializer(this);
        principalServiceManager = new PrincipalServiceManager(null);
        genericSerializer = new GenericPrincipalSerializer();
        genericSerializer.initialize();
    }
        
    /** 
     * Constructor.
     * 
     * @param manager {@link PrincipalServiceManager} to use
     * @param defaultSerializer the default serializer to use
     * 
     * @since 4.1.0
     */
    public DefaultAuthenticationResultSerializer(@Nonnull final PrincipalServiceManager manager,
            @Nonnull final GenericPrincipalSerializer defaultSerializer) {
        generatorFactory = Json.createGeneratorFactory(null);
        readerFactory = Json.createReaderFactory(null);
        
        principalSerializers = CollectionSupport.emptyList();
        authnResultPrincipalSerializer = new AuthenticationResultPrincipalSerializer(this);
        principalServiceManager = Constraint.isNotNull(manager, "PrincipalServiceManager cannot be null");
        genericSerializer = Constraint.isNotNull(defaultSerializer, "Default serializer cannot be null");
    }

    /**
     * Returns the {@link GenericPrincipalSerializer} used for any unsupported principals found
     * in the {@link AuthenticationResult}.
     * 
     * @return generic principal serializer
     */
    @Nonnull public GenericPrincipalSerializer getGenericPrincipalSerializer() {
        return genericSerializer;
    }
    
    /** {@inheritDoc} */
    @Override
    public void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        authnResultPrincipalSerializer.initialize();
        
        final List<PrincipalSerializer<String>> serializers =
                principalServiceManager.all()
                    .stream()
                    .map(PrincipalService::getSerializer)
                    .collect(Collectors.toUnmodifiableList());
        
        if (serializers.isEmpty()) {
            principalSerializers = CollectionSupport.singletonList(authnResultPrincipalSerializer);
        } else {
            final List<PrincipalSerializer<String>> copy = new ArrayList<>(serializers);
            copy.add(authnResultPrincipalSerializer);
            principalSerializers = CollectionSupport.copyToList(copy);
        }
    }

// Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final AuthenticationResult instance) throws IOException {
        checkComponentActive();
        
        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = generatorFactory.createGenerator(sink);
            gen.writeStartObject().write(FLOW_ID_FIELD, instance.getAuthenticationFlowId())
                    .write(AUTHN_INSTANT_FIELD, instance.getAuthenticationInstant().toEpochMilli());
            
            final Map<String,String> addtlData = instance.getAdditionalData();
            if (!addtlData.isEmpty()) {
                gen.writeStartObject(ADDTL_DATA_FIELD);
                addtlData.forEach((k,v) -> gen.write(k, v));
                gen.writeEnd();
            }
            
            gen.writeStartArray(PRINCIPAL_ARRAY_FIELD);
            for (final Principal p : instance.getSubject().getPrincipals()) {
                assert p != null;
                serializePrincipal(gen, p);
            }
            gen.writeEnd();
            
            final Set<Principal> publicCreds = instance.getSubject().getPublicCredentials(Principal.class);
            if (publicCreds != null && !publicCreds.isEmpty()) {
                gen.writeStartArray(PUB_CREDS_ARRAY_FIELD);
                for (final Principal p : publicCreds) {
                    assert p != null;
                    serializePrincipal(gen, p);
                }
                gen.writeEnd();
            }

            final Set<Principal> privateCreds = instance.getSubject().getPrivateCredentials(Principal.class);
            if (privateCreds != null && !privateCreds.isEmpty()) {
                gen.writeStartArray(PRIV_CREDS_ARRAY_FIELD);
                for (final Principal p : privateCreds) {
                    assert p != null;
                    serializePrincipal(gen, p);
                }
                gen.writeEnd();
            }
            
            final Set<X509Certificate> x509Creds = instance.getSubject().getPublicCredentials(X509Certificate.class);
            if (x509Creds != null && !x509Creds.isEmpty()) {
                gen.writeStartArray(X509_CREDS_ARRAY_FIELD);
                for (final X509Certificate x : x509Creds) {
                    try {
                        gen.write(Base64Support.encode(x.getEncoded(), false));
                    } catch (final CertificateEncodingException|EncodingException e) {
                        log.warn("Unable to serialize X.509 certificate with subject: {}",
                                x.getSubjectDN().toString());
                    }
                }
                gen.writeEnd();
            }
            
            // TODO handle other creds

            gen.writeEnd().close();

            return sink.toString();
        } catch (final JsonException e) {
            throw new IOException("Exception while serializing AuthenticationResult", e);
        }
    }

    /** {@inheritDoc} */
    @Nonnull public AuthenticationResult deserialize(final long version, @Nonnull @NotEmpty final String context,
                    @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value,
                    @Nullable final Long expiration) throws IOException {
        checkComponentActive();
        
        try (final JsonReader reader = readerFactory.createReader(new StringReader(value))) {
            
            final JsonStructure st = reader.read();
            
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing AuthenticationResult");
            }
            final JsonObject obj = (JsonObject) st;

            final String flowId = obj.getString(FLOW_ID_FIELD);
            final long authnInstant = obj.getJsonNumber(AUTHN_INSTANT_FIELD).longValueExact();

            final AuthenticationResult result = new AuthenticationResult(flowId, new Subject());
            result.setAuthenticationInstant(Instant.ofEpochMilli(authnInstant));
            result.setLastActivityInstant(Instant.ofEpochMilli(expiration != null ? expiration : authnInstant));
            result.setPreviousResult(true);

            final JsonObject addtlData = obj.getJsonObject(ADDTL_DATA_FIELD);
            if (addtlData != null) {
                final Map<String,String> dataMap = result.getAdditionalData();
                addtlData.entrySet()
                    .stream()
                    .filter(e -> e.getValue().getValueType().equals(ValueType.STRING))
                    .forEach(e -> dataMap.put(e.getKey(), ((JsonString) e.getValue()).getString()));
            }
            
            final JsonArray principals = obj.getJsonArray(PRINCIPAL_ARRAY_FIELD);
            if (principals != null) {
                for (final JsonValue val : principals) {
                    assert val != null;
                    final Principal principal = deserializePrincipal(val);
                    if (principal != null) {
                        result.getSubject().getPrincipals().add(principal);
                    }
                }
            }

            final JsonArray publicCreds = obj.getJsonArray(PUB_CREDS_ARRAY_FIELD);
            if (publicCreds != null) {
                for (final JsonValue val : publicCreds) {
                    assert val != null;
                    final Principal principal = deserializePrincipal(val);
                    if (principal != null) {
                        result.getSubject().getPublicCredentials().add(principal);
                    }
                }
            }

            final JsonArray privateCreds = obj.getJsonArray(PRIV_CREDS_ARRAY_FIELD);
            if (privateCreds != null) {
                for (final JsonValue val : privateCreds) {
                    assert val != null;
                    final Principal principal = deserializePrincipal(val);
                    if (principal != null) {
                        result.getSubject().getPrivateCredentials().add(principal);
                    }
                }
            }
            
            final JsonArray x509Creds = obj.getJsonArray(X509_CREDS_ARRAY_FIELD);
            if (x509Creds != null) {
                for (final JsonValue val : x509Creds) {
                    if (val.getValueType() == ValueType.STRING) {
                        try {
                            final String valStr = val.toString();
                            assert valStr != null;
                            final X509Certificate cert = X509Support.decodeCertificate(valStr);
                            result.getSubject().getPublicCredentials().add(cert);
                        } catch (final CertificateException e) {
                            log.warn("Unable to parse certificate", e);
                        }
                    }
                }
            }

            // TODO handle other creds

            return result;

        } catch (final NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            throw new IOException("Found invalid data structure while parsing AuthenticationResult", e);
        }
    }
 // Checkstyle: CyclomaticComplexity|MethodLength ON

    /**
     * Attempt to serialize a principal with the registered and default serializers.
     *
     * @param generator the JSON context to write into
     * @param principal object to serialize
     * 
     * @throws IOException if serialization fails
     */
    private void serializePrincipal(@Nonnull final JsonGenerator generator, @Nonnull final Principal principal)
            throws IOException {

        final String serializedForm;
        
        // This is a special case because the serializer here is a dedicated one.
        if (authnResultPrincipalSerializer.supports(principal)) {
            serializedForm = authnResultPrincipalSerializer.serialize(principal);
        } else {
            // Otherwise we just obtain the instance by class, or try the generic one.
            final PrincipalService<?> principalService = principalServiceManager.byClass(principal.getClass());
            if (principalService != null) {
                serializedForm = principalService.getSerializer().serialize(principal);
            } else if (genericSerializer.supports(principal)) {
                serializedForm = genericSerializer.serialize(principal);
            } else {
                serializedForm = null;
            }
        }

        if (serializedForm != null) {
            try (final JsonReader reader = readerFactory.createReader(new StringReader(serializedForm))) {
                generator.write(reader.readObject());
            }
        }
    }

    /**
     * Attempt to deserialize a principal with the registered and default serializers.
     * 
     * @param jsonValue the JSON object to parse
     * 
     * @return the {@link Principal} recovered, or null
     * @throws IOException if an error occurs
     */
    @Nullable private Principal deserializePrincipal(@Nonnull final JsonValue jsonValue) throws IOException {
        if (jsonValue instanceof JsonObject) {
            final String json = ((JsonObject) jsonValue).toString();
            assert json != null;
            for (final PrincipalSerializer<? super String> serializer : principalSerializers) {
                if (serializer.supports(json)) {
                    return serializer.deserialize(json);
                }
            }
            if (genericSerializer.supports(json)) {
                return genericSerializer.deserialize(json);
            }
        }
        
        return null;
    }
    
}

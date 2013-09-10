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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.security.auth.Subject;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

/**
 * Handles serialization of results that carry only custom {@link Principal} objects of a simple
 * nature that can be reconstructed via a String-argument constructor.
 */
public class BaseAuthenticationResultSerializer implements StorageSerializer<AuthenticationResult> {

    /** Field name of Flow ID. */
    private static final String FLOW_ID_FIELD = "id";

    /** Field name of authentication instant. */
    private static final String AUTHN_INSTANT_FIELD = "ts";

    /** Field name of last activity time. */
    private static final String LAST_ACTIVITY_FIELD = "act";

    /** Field name of principal array. */
    private static final String PRINCIPAL_ARRAY_FIELD = "princ";

    /** Field name of principal type. */
    private static final String PRINCIPAL_TYPE_FIELD = "typ";

    /** Field name of principal name. */
    private static final String PRINCIPAL_NAME_FIELD = "nam";

    /** Field name of {@link UsernamePrincipal}. */
    private static final String USERNAME_FIELD = "U";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseAuthenticationResultSerializer.class);
        
    /** Shrinkage of long constants into symbolic numbers. */
    @Nonnull private BiMap<String,Integer> symbolics;
    
    /** A cache of Principal types that support string-based construction. */
    @Nonnull private final Set<Class<? extends Principal>> compatiblePrincipalTypes;
    
    /** Constructor. */
    public BaseAuthenticationResultSerializer() {
        symbolics = ImmutableBiMap.of();
        compatiblePrincipalTypes = Collections.synchronizedSet(new HashSet<Class<? extends Principal>>());
    }

    /**
     * Sets mappings of string constants to symbolic constants.
     * 
     * @param mappings  string to symbolic mappings
     */
    public void setSymbolics(@Nonnull @NonnullElements final Map<String,Integer> mappings) {
        symbolics = HashBiMap.create(Constraint.isNotNull(mappings, "Mappings cannot be null"));
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final AuthenticationResult instance) throws IOException {
        
        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = Json.createGenerator(sink);
            gen.writeStartObject()
                .write(FLOW_ID_FIELD, instance.getAuthenticationFlowId())
                .write(AUTHN_INSTANT_FIELD, instance.getAuthenticationInstant())
                .write(LAST_ACTIVITY_FIELD, instance.getLastActivityInstant())
                .writeStartArray(PRINCIPAL_ARRAY_FIELD);
            
            for (Principal p : instance.getSubject().getPrincipals()) {
                doSerializePrincipal(gen, p);
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
    @Nonnull public AuthenticationResult deserialize(@Nonnull @NotEmpty final String value,
            @Nullable final String context, @Nullable final String key, @Nullable final Long expiration)
            throws IOException {

        try {
            JsonReader reader = Json.createReader(new StringReader(value));
            JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing AuthenticationResult");
            }
            JsonObject obj = (JsonObject) st;
            
            String flowId = obj.getString(FLOW_ID_FIELD);
            long authnInstant = obj.getJsonNumber(AUTHN_INSTANT_FIELD).longValueExact();
            long activityTime = obj.getJsonNumber(LAST_ACTIVITY_FIELD).longValueExact();
            JsonArray principals = obj.getJsonArray(PRINCIPAL_ARRAY_FIELD);

            AuthenticationResult result = new AuthenticationResult(flowId, new Subject());
            result.setAuthenticationInstant(authnInstant);
            result.setLastActivityInstant(activityTime);
            
            for (JsonValue p : principals) {
                if (p instanceof JsonObject) {
                    doDeserializePrincipal((JsonObject) p, result.getSubject());
                }
            }
            
            // TODO handle custom creds
            
            return result;
            
        } catch (NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            log.error("Exception while parsing AuthenticationResult", e);
            throw new IOException("Found invalid data structure while parsing AuthenticationResult", e);
        }
    }

    /**
     * Override this method to handle serialization of additional Principal types, and invoke the base class
     * version for anything else.
     * 
     * <p>Anything serialized should be in a subobject created by and ended by this method.</p>
     * 
     * @param generator JSON generator to write to
     * @param principal the principal to serialize
     */
    protected void doSerializePrincipal(@Nonnull final JsonGenerator generator, @Nonnull final Principal principal) {
        
        if (principal instanceof UsernamePrincipal) {
            generator.writeStartObject()
                .write(USERNAME_FIELD, principal.getName())
                .writeEnd();
        } else if (isCompatible(principal.getClass())) {
            generator.writeStartObject();
            
            Integer symbol = symbolics.get(principal.getClass().getName());
            if (symbol != null) {
                generator.write(PRINCIPAL_TYPE_FIELD, symbol);
            } else {
                generator.write(PRINCIPAL_TYPE_FIELD, principal.getClass().getName());
            }
            
            symbol = symbolics.get(principal.getName());
            if (symbol != null) {
                generator.write(PRINCIPAL_NAME_FIELD, symbol);
            } else {
                generator.write(PRINCIPAL_NAME_FIELD, principal.getName());
            }
                
            generator.writeEnd();
        }
    }
    
    /**
     * Override this method to handle deserialization of additional Principal types, and invoke the base class
     * version for anything else.
     * 
     * @param obj JSON structure to parse
     * @param subject the subject to add any results to
     */
    protected void doDeserializePrincipal(@Nonnull final JsonObject obj, @Nonnull final Subject subject) {
        
        try {
            JsonString str = obj.getJsonString(USERNAME_FIELD);
            if (str != null) {
                String username = str.getString();
                if (!Strings.isNullOrEmpty(username)) {
                    subject.getPrincipals().add(new UsernamePrincipal(username));
                } else {
                    log.warn("Skipping null/empty UsernamePrincipal");
                }
            } else {
                JsonValue typefield = obj.get(PRINCIPAL_TYPE_FIELD);
                JsonValue namefield = obj.get(PRINCIPAL_NAME_FIELD);
                if (typefield != null && namefield != null) {
                    String type = desymbolize(typefield);
                    String name = desymbolize(namefield);
                    if (!Strings.isNullOrEmpty(type) && !Strings.isNullOrEmpty(name)) {
                        try {
                            Class<? extends Principal> pclass = Class.forName(type).asSubclass(Principal.class);
                            Constructor<? extends Principal> ctor = pclass.getConstructor(String.class);
                            subject.getPrincipals().add(ctor.newInstance(name));
                        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException
                                    | InstantiationException | IllegalAccessException | IllegalArgumentException
                                    | InvocationTargetException e) {
                            log.warn("Exception instantiating custom Principal type " + type + " with name " + name, e);
                        }
                    } else {
                        log.warn("Unparseable Principal type or name in structure");
                    }
                } else {
                    log.warn("Missing Principal type or name in structure");
                }
            }
        } catch (NullPointerException | ClassCastException | ArithmeticException e) {
            log.warn("Exception parsing Principal structure", e);
        }
    }
    
    /**
     * Map the field value to a string, either directly or via the symbolic map.
     * 
     * @param field the object field to examine
     * 
     * @return the resulting string, or null if invalid
     */
    private String desymbolize(@Nonnull final JsonValue field) {
       switch (field.getValueType()) {
           case STRING:
               return ((JsonString) field).getString();
           
           case NUMBER:
               return symbolics.inverse().get(((JsonNumber) field).intValueExact());
               
           default:
               return null;
       }
    }
    
    
    /**
     * Determines whether the specified Principal type can be handled by this class, through the presence
     * of a single-argument String-based constructor. 
     * 
     * @param principalType the type to check
     * @return  true iff the class supports a String-based constructor
     */
    private boolean isCompatible(@Nonnull final Class<? extends Principal> principalType) {
        
        if (compatiblePrincipalTypes.contains(principalType)) {
            return true;
        }
        
        try {
            principalType.getConstructor(String.class);
            compatiblePrincipalTypes.add(principalType);
            return true;
        } catch (NoSuchMethodException | SecurityException e) {
            log.warn("Unsupported Principal type will be omitted: {}", principalType.getClass().getName());
        }
        
        return false;
    }
}
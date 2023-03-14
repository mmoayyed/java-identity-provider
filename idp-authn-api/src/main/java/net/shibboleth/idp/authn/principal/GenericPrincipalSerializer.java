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

package net.shibboleth.idp.authn.principal;

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
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Principal serializer for arbitrary principal types.
 * 
 * @since 4.1.0
 */
@ThreadSafeAfterInit
public class GenericPrincipalSerializer extends AbstractPrincipalSerializer<String> {

    /** Field name of principal type. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_TYPE_FIELD = "typ";

    /** Field name of principal name. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_NAME_FIELD = "nam";

    /** Pattern used to determine if input is supported. */
    @SuppressWarnings("null")
    @Nonnull private static final Pattern JSON_PATTERN = Pattern.compile("^\\{\"typ\":.*,\"nam\":.*\\}$");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(GenericPrincipalSerializer.class);

    /** Shrinkage of long constants into symbolic numbers. */
    @Nonnull @NonnullElements private BiMap<String,Integer> symbolics;
    
    /** A cache of Principal types that support string-based construction. */
    @Nonnull @NonnullElements private final Set<Class<? extends Principal>> compatiblePrincipalTypes;

    /**
     * Constructor.
     */
    public GenericPrincipalSerializer() {
        final BiMap<String,Integer> s = ImmutableBiMap.of();
        assert s!=null;
        symbolics = s;
        final Set<Class<? extends Principal>> cpt = Collections.synchronizedSet(new HashSet<Class<? extends Principal>>());
        assert cpt!=null;
        compatiblePrincipalTypes = cpt;
    }

    /**
     * Sets mappings of string constants to symbolic constants.
     * 
     * @param mappings  string to symbolic mappings
     */
    public void setSymbolics(@Nonnull @NonnullElements final Map<String,Integer> mappings) {
        checkSetterPreconditions();
        final BiMap<String,Integer> s = HashBiMap.create(Constraint.isNotNull(mappings, "Mappings cannot be null"));
        assert s!=null;
        symbolics = s;
    }
        
    /** {@inheritDoc} */
    public boolean supports(@Nonnull final Principal principal) {
        final Class<? extends Principal> principalType = principal.getClass();
        if (compatiblePrincipalTypes.contains(principalType)) {
            return true;
        }
        
        try {
            principalType.getConstructor(String.class);
            compatiblePrincipalTypes.add(principalType);
            return true;
        } catch (final NoSuchMethodException | SecurityException e) {
            log.warn("Unsupported Principal type will be omitted: {}", principalType.getName());
        }
        
        return false;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        checkComponentActive();
        final StringWriter sink = new StringWriter(32);
        
        try (final JsonGenerator gen = getJsonGenerator(sink)) {
            gen.writeStartObject();
            
            Integer symbol = symbolics.get(principal.getClass().getName());
            if (symbol != null) {
                gen.write(PRINCIPAL_TYPE_FIELD, symbol);
            } else {
                gen.write(PRINCIPAL_TYPE_FIELD, principal.getClass().getName());
            }
            
            symbol = symbolics.get(principal.getName());
            if (symbol != null) {
                gen.write(PRINCIPAL_NAME_FIELD, symbol);
            } else {
                gen.write(PRINCIPAL_NAME_FIELD, principal.getName());
            }
                
            gen.writeEnd();
        }
        final String result = sink.toString();
        assert result != null;
        return result;
    }

    /** {@inheritDoc} */
    public boolean supports(@Nonnull @NotEmpty final String value) {
        return JSON_PATTERN.matcher(value).matches();
    }

    /** {@inheritDoc} */
    @Nullable public Principal deserialize(@Nonnull @NotEmpty final String value) throws IOException {
        checkComponentActive();
        try (final JsonReader reader = getJsonReader(new StringReader(value))) {
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing a generic principal");
            }
            final JsonObject obj = (JsonObject) st;
            final JsonValue typefield = obj.get(PRINCIPAL_TYPE_FIELD);
            final JsonValue namefield = obj.get(PRINCIPAL_NAME_FIELD);
            if (typefield != null && namefield != null) {
                final String type = desymbolize(typefield);
                final String name = desymbolize(namefield);
                if (!Strings.isNullOrEmpty(type) && !Strings.isNullOrEmpty(name)) {
                    try {
                        final Class<? extends Principal> pclass = Class.forName(type).asSubclass(Principal.class);
                        final Constructor<? extends Principal> ctor = pclass.getConstructor(String.class);
                        return ctor.newInstance(name);
                    } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException
                                | InstantiationException | IllegalAccessException | IllegalArgumentException
                                | InvocationTargetException e) {
                        log.warn("Exception instantiating custom Principal type {} with name {}", type, name, e);
                    }
                } else {
                    log.warn("Unparseable Principal type or name in structure");
                }
            } else {
                log.warn("Missing Principal type or name in structure");
            }
            return null;
        } catch (final JsonException e) {
            throw new IOException("Found invalid data structure while parsing a generic principal", e);
        }
    }

    
    /**
     * Map a field value to a string, either directly or via the symbolic map.
     * 
     * @param field the object field to examine
     * 
     * @return the resulting string, or null if invalid
     */
    @Nullable protected String desymbolize(@Nonnull final JsonValue field) {
       switch (field.getValueType()) {
           case STRING:
               return ((JsonString) field).getString();
           
           case NUMBER:
               return symbolics.inverse().get(((JsonNumber) field).intValueExact());
               
           default:
               return null;
       }
    }
    
}
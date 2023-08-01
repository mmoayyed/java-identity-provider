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

package net.shibboleth.idp.authn.principal.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.slf4j.Logger;

import com.google.common.base.Strings;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.authn.principal.AbstractPrincipalSerializer;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Principal serializer for {@link IdPAttributePrincipal}.
 */
@ThreadSafe
public class IdPAttributePrincipalSerializer extends AbstractPrincipalSerializer<String> {

    /** Field name of principal name. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_NAME_FIELD = "IDPATTR";

    /** Field name of principal entry. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_ENTRY_FIELD = "VALS";

    /** Field name of type code of an {@link EmptyAttributeValue}. */
    @Nonnull @NotEmpty private static final String EMPTY_VALUE_FIELD = "EMPTY";

    /** Field name of type code of an {@link StringAttributeValue}. */
    @Nonnull @NotEmpty private static final String STRING_VALUE_FIELD = "STR";

    /** Field name of type code of an {@link ScopedStringAttributeValue}. */
    @Nonnull @NotEmpty private static final String SCOPED_VALUE_FIELD = "SCO";

    /** Pattern used to determine if input is supported. */
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\{\"IDPATTR\":.*,\"VALS\":.*\\}$");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IdPAttributePrincipalSerializer.class);
    
    /** JSON object bulder factory. */
    @Nonnull private final JsonBuilderFactory objectBuilderFactory;

    /** Constructor. */
    public IdPAttributePrincipalSerializer() {
        final JsonBuilderFactory factory = Json.createBuilderFactory(null);
        assert factory != null;
        objectBuilderFactory = factory;
    }
    
    /** {@inheritDoc} */
    public boolean supports(@Nonnull final Principal principal) {
        return principal instanceof IdPAttributePrincipal;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        final StringWriter sink = new StringWriter(128);
        try (final JsonGenerator gen = getJsonGenerator(sink)) {
            gen.writeStartObject()
               .write(PRINCIPAL_NAME_FIELD, principal.getName());        
            
            final IdPAttribute attribute = ((IdPAttributePrincipal) principal).getAttribute();
            final JsonArrayBuilder arrayBuilder = getJsonArrayBuilder();
            for (final IdPAttributeValue value : attribute.getValues()) {
                assert value != null;
                final JsonObject obj = serializeValue(value);
                if (obj != null) {
                    arrayBuilder.add(obj);
                } else {
                    log.warn("Skipping unsupported attribute value type ({})", value.getClass());
                }
            }
            gen.write(PRINCIPAL_ENTRY_FIELD, arrayBuilder.build());
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
    @Nullable public IdPAttributePrincipal deserialize(@Nonnull @NotEmpty final String value) throws IOException {
        
        try (final JsonReader reader = getJsonReader(new StringReader(value))) {
            
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing IdPAttributePrincipal");
            }
            
            final JsonObject obj = (JsonObject) st;
            final JsonString str = obj.getJsonString(PRINCIPAL_NAME_FIELD);
            final JsonArray vals = obj.getJsonArray(PRINCIPAL_ENTRY_FIELD);
            final String nativeString;
            if (str != null) {
                nativeString = str.getString();
            } else {
                nativeString = null;
            }
            if (nativeString != null && !Strings.isNullOrEmpty(nativeString ) && vals != null) {
                final IdPAttribute attribute = new IdPAttribute(nativeString);
                final List<IdPAttributeValue> values = new ArrayList<>();
                
                for (final JsonValue entry : vals) {
                    if (entry instanceof JsonObject) {
                        final IdPAttributeValue attrValue = deserializeValue((JsonObject) entry);
                        if (attrValue != null) {
                            values.add(attrValue);
                        } else {
                            log.warn("Skipping unsupported attribute value serialization");
                        }
                    } else {
                        log.warn("Skipping non-object attribute value array entry");
                    }
                }
                
                attribute.setValues(values);
                return new IdPAttributePrincipal(attribute);
            }
            log.warn("Skipping IdPAttributePrincipal missing attribute name or values");
            
            return null;
        } catch (final JsonException e) {
            throw new IOException("Found invalid data structure while parsing IdPAttributePrincipal", e);
        }
    }

    /**
     * Serialize an attribute value and produce a {@link JsonObject}.
     * 
     * <p>Override this method to support additional value types.</p>
     * 
     * @param value the attribute value to serialize
     * 
     * @return the object
     */
    @Nullable protected JsonObject serializeValue(@Nonnull final IdPAttributeValue value) {
        final JsonObjectBuilder builder = getJsonObjectBuilder();
        
        if (value instanceof EmptyAttributeValue) {
            final EmptyAttributeValue eVal = (EmptyAttributeValue) value;
            if (eVal.getValue().equals(EmptyType.NULL_VALUE)) {
                // Null
                builder.add(EMPTY_VALUE_FIELD, 0);
            } else {
                // Zero-Length
                builder.add(EMPTY_VALUE_FIELD, 1);
            }
            return builder.build();
        } else if (value instanceof ScopedStringAttributeValue) {
            final JsonArrayBuilder arrayBuilder = getJsonArrayBuilder();
            arrayBuilder.add(((ScopedStringAttributeValue) value).getValue());
            arrayBuilder.add(((ScopedStringAttributeValue) value).getScope());
            builder.add(SCOPED_VALUE_FIELD, arrayBuilder);
            return builder.build();
        } else if (value instanceof StringAttributeValue) {
            builder.add(STRING_VALUE_FIELD, ((StringAttributeValue) value).getValue());
            return builder.build();
        }  
        
        return null;
    }
    
    /**
     * Deserialize an attribute value from a {@link JsonObject}.
     * 
     * <p>Override this method to support additional value types.</p>
     * 
     * @param object object to deserialize
     * 
     * @return the attribute value, or null
     */
    @Nullable protected IdPAttributeValue deserializeValue(@Nonnull final JsonObject object) {
        
        if (object.containsKey(EMPTY_VALUE_FIELD)) {
            final JsonValue value = object.get(EMPTY_VALUE_FIELD);
            if (value instanceof JsonNumber) {
                if (((JsonNumber) value).intValueExact() == 0) {
                    return EmptyAttributeValue.NULL;
                }
                return EmptyAttributeValue.ZERO_LENGTH;
            }
        } else if (object.containsKey(STRING_VALUE_FIELD)) {
            final JsonValue value = object.get(STRING_VALUE_FIELD);
            if (value instanceof JsonString) {
                return new StringAttributeValue(((JsonString) value).getString());
            }
        } else if (object.containsKey(SCOPED_VALUE_FIELD)) {
            final JsonValue value = object.get(SCOPED_VALUE_FIELD);
            if (value instanceof JsonArray && ((JsonArray) value).size() == 2) {
                return new ScopedStringAttributeValue(((JsonString) ((JsonArray) value).get(0)).getString(),
                        ((JsonString) ((JsonArray) value).get(1)).getString());
            }
        }
        
        return null;
    }

    /**
     * Get a {@link JsonObjectBuilder} in a thread-safe manner.
     * 
     * @return  an object builder
     */
    @Nonnull private synchronized JsonObjectBuilder getJsonObjectBuilder() {
        final JsonObjectBuilder result = objectBuilderFactory.createObjectBuilder();
        assert result != null;
        return result;
    }

    /**
     * Get a {@link JsonArrayBuilder} in a thread-safe manner.
     * 
     * @return  an array builder
     */
    @Nonnull private synchronized JsonArrayBuilder getJsonArrayBuilder() {
        final JsonArrayBuilder result = objectBuilderFactory.createArrayBuilder();
        assert result != null;
        return result;
    }
    
}

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

package net.shibboleth.idp.authn.principal.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerator;
import javax.security.auth.x500.X500Principal;

import net.shibboleth.idp.authn.principal.AbstractPrincipalSerializer;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.DecodingException;
import net.shibboleth.shared.codec.EncodingException;

/**
 * Principal serializer for {@link X500Principal}.
 * 
 * @since 4.1.0
 */
@ThreadSafe
public class X500PrincipalSerializer extends AbstractPrincipalSerializer<String> {

    /** Field name of X.500 name. */
    @Nonnull @NotEmpty private static final String X500_NAME_FIELD = "X500";

    /** Pattern used to determine if input is supported. */
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\{\"X500\":.*\\}$");
    
    /** {@inheritDoc} */
    @Override
    public boolean supports(@Nonnull final Principal principal) {
        return principal instanceof X500Principal;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        
        final X500Principal x500Principal = (X500Principal) principal;

        final String name;
        try {
            final byte[] encoded = x500Principal.getEncoded();
            assert encoded != null;
            name = Base64Support.encode(encoded, false);
        } catch (final EncodingException e) {
            throw new IOException(e);
        }
        
        final StringWriter sink = new StringWriter(32);
        
        try (final JsonGenerator gen = getJsonGenerator(sink)) {
            gen.writeStartObject().write(X500_NAME_FIELD, name).writeEnd();
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
    @Nullable public X500Principal deserialize(@Nonnull @NotEmpty final String value)
            throws IOException {
        
        try (final JsonReader reader = getJsonReader(new StringReader(value))) {
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing X500Principal");
            }
            
            final JsonValue jsonValue = ((JsonObject) st).get(X500_NAME_FIELD);
            if (jsonValue != null && ValueType.STRING.equals(jsonValue.getValueType())) {
                final String nativeString = ((JsonString) jsonValue).getString();
                assert nativeString != null;
                return new X500Principal(Base64Support.decode(nativeString));
            }
            
            throw new IOException("Serialized X500Principal missing name field");
        } catch (final JsonException | DecodingException | IllegalArgumentException e) {
            throw new IOException("Found invalid data while parsing X500Principal", e);
        }
    }    

}
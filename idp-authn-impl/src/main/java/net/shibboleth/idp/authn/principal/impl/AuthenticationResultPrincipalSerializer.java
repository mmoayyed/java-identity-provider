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
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.principal.AbstractPrincipalSerializer;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Principal serializer for {@link AuthenticationResultPrincipal}.
 */
@ThreadSafe
public class AuthenticationResultPrincipalSerializer extends AbstractPrincipalSerializer<String> {

    /** Field name of principal name. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_NAME_FIELD = "AUTHRES";

    /** Pattern used to determine if input is supported. */
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\{\"AUTHRES\":.*\\}$");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AuthenticationResultPrincipalSerializer.class);

    /** Circular reference back to the parent serializer. */
    @Nonnull private final StorageSerializer<AuthenticationResult> resultSerializer;
    
    /**
     * Constructor.
     * 
     * @param serializer serializer for the nested {@link AuthenticationResult} object
     */
    public AuthenticationResultPrincipalSerializer(
            @Nonnull final StorageSerializer<AuthenticationResult> serializer) {
        resultSerializer = Constraint.isNotNull(serializer, "AuthenticationResult serializer cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean supports(@Nonnull final Principal principal) {
        return principal instanceof AuthenticationResultPrincipal;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        final StringWriter sink = new StringWriter(128);
        try (final JsonGenerator gen = getJsonGenerator(sink)) {
            gen.writeStartObject();   
            
            final AuthenticationResult result = ((AuthenticationResultPrincipal) principal).getAuthenticationResult();
            
            gen.write(PRINCIPAL_NAME_FIELD, resultSerializer.serialize(result));
            
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
    @Nullable public AuthenticationResultPrincipal deserialize(@Nonnull @NotEmpty final String value)
            throws IOException {
        
        try (final JsonReader reader = getJsonReader(new StringReader(value))) {
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing AuthenticationResultPrincipal");
            }
            final JsonObject obj = (JsonObject) st;
            final JsonValue str = obj.get(PRINCIPAL_NAME_FIELD);
            if (str != null && str instanceof JsonString) {
                final String nativeString = ((JsonString) str).getString();
                assert nativeString != null;
                return new AuthenticationResultPrincipal(
                        resultSerializer.deserialize(1, "context", "key", nativeString, null));
            }
            log.warn("Skipping non-string principal value");
            
            return null;
        } catch (final JsonException e) {
            throw new IOException("Found invalid data structure while parsing AuthenticationResultPrincipal", e);
        }
    }    
}

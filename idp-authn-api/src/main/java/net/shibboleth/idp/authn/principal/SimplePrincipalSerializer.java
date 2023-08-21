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

package net.shibboleth.idp.authn.principal;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
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
import jakarta.json.stream.JsonGenerator;

import com.google.common.base.Strings;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Principal serializer for string-based principals that serialize to a simple JSON structure.
 * 
 * @param <T> principal type
 * 
 * @since 4.1.0
 */
@ThreadSafe
public class SimplePrincipalSerializer<T extends Principal> extends AbstractPrincipalSerializer<String> {

    /** Principal type. */
    @Nonnull private final Class<T> principalType;

    /** Constructor. */
    @Nonnull private final Constructor<T> ctor;
    
    /** Field name. */
    @Nonnull @NotEmpty private final String fieldName;

    /** Pattern used to determine if input is supported. */
    @Nonnull private final Pattern jsonPattern;

    /**
     * Constructor.
     * 
     * @param claz principal type
     * @param name field name of JSON structure
     * 
     * @throws SecurityException if the constructor cannot be accessed 
     * @throws NoSuchMethodException if the constructor does not exist
     */
    public SimplePrincipalSerializer(@Nonnull @ParameterName(name="claz") final Class<T> claz,
            @Nonnull @NotEmpty @ParameterName(name="name") final String name)
                    throws NoSuchMethodException, SecurityException {
        
        final Class<T> typeClaz = principalType = Constraint.isNotNull(claz, "Principal type cannot be null");
        final Constructor<T> constructor = typeClaz.getConstructor(String.class);
        assert constructor!=null;
        ctor = constructor;
        fieldName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Field name cannot be empty or null");
        final Pattern pattern = Pattern.compile("^\\{\"" + fieldName + "\":.*\\}$");
        assert pattern != null;
        jsonPattern = pattern;
    }
    
    /** {@inheritDoc} */
    public boolean supports(@Nonnull final Principal principal) {
        return principalType.isInstance(principal);
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        final StringWriter sink = new StringWriter(32);
        try (final JsonGenerator gen = getJsonGenerator(sink)) {
            gen.writeStartObject()
                .write(fieldName, getName(principal))
                .writeEnd();
        }
        final String result = sink.toString();
        assert result != null;
        return result;
    }
    
    /**
     * Return the appropriate value to serialize from the input object.
     * 
     * @param principal input object
     * 
     * @return the value to serialize.
     * 
     * @throws IOException if an error occurs
     */
    @Nonnull @NotEmpty protected String getName(@Nonnull final Principal principal) throws IOException {
        final String result = principal.getName();
        assert result != null;
        return result;
    }

    /** {@inheritDoc} */
    public boolean supports(@Nonnull @NotEmpty final String value) {
        return jsonPattern.matcher(value).matches();
    }

    /** {@inheritDoc} */
    @Nullable public T deserialize(@Nonnull @NotEmpty final String value) throws IOException {
        
        try (final JsonReader reader = getJsonReader(new StringReader(value))) {
            
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing " + principalType.getSimpleName());
            }
            
            final JsonString str = ((JsonObject) st).getJsonString(fieldName);
            if (str != null) {
                final String name = getName(str.getString());
                if (!Strings.isNullOrEmpty(name)) {
                    return ctor.newInstance(name);
                }
            }
            return null;
        } catch (final JsonException e) {
            throw new IOException("Found invalid data structure while parsing " + principalType.getSimpleName(), e);
        } catch (final ReflectiveOperationException e) {
            throw new IOException("Unable to reflectively create " + principalType.getSimpleName(), e);
        }
    }

    /**
     * Return the appropriate value to create the {@link Principal} around based on the serialized form.
     * 
     * @param serializedName the value in the serialization.
     * 
     * @return the transformed value
     * 
     * @throws IOException if an error occurs 
     */
    @Nullable protected String getName(@Nullable final String serializedName) throws IOException {
        return serializedName;
    }
    
}
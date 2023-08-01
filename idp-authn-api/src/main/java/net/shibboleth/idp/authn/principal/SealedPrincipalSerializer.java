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
import java.security.Principal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.base.Strings;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.DataSealerException;

/**
 * Principal serializer that encrypts/decrypts the data when serializing.
 * 
 * @param <T> principal type
 * 
 * @since 4.1.0
 */
public class SealedPrincipalSerializer<T extends Principal> extends SimplePrincipalSerializer<T> {

    /** Field name of password. */
    @Nonnull @NotEmpty private static final String PASSWORD_FIELD = "PW";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SealedPrincipalSerializer.class);

    /** Data sealer. */
    @Nullable private DataSealer sealer;
    
    /**
     * Constructor.
     *
     * @param claz principal type
     * @param name field name of JSON structure
     * 
     * @throws SecurityException if the constructor cannot be accessed 
     * @throws NoSuchMethodException if the constructor does not exist
     */
    public SealedPrincipalSerializer(@Nonnull @ParameterName(name="claz") final Class<T> claz,
            @Nonnull @NotEmpty @ParameterName(name="name") final String name)
                    throws NoSuchMethodException, SecurityException {
        super(claz, name);
    }

    /**
     * Set the {@link DataSealer} to use.
     * 
     * @param theSealer encrypting component to use
     */
    public void setDataSealer(@Nullable final DataSealer theSealer) {
        checkSetterPreconditions();
        sealer = theSealer;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supports(@Nonnull final Principal principal) {
        checkComponentActive();
        if (!super.supports(principal)) {
            return false;
        } else if (sealer == null) {
            log.error("No DataSealer was provided, unable to support serialization");
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supports(@Nonnull @NotEmpty final String value) {
        checkComponentActive();
        if (!super.supports(value)) {
            return false;
        } else if (sealer == null) {
            log.error("No DataSealer was provided, unable to support deserialization");
            return false;
        }
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected String getName(@Nonnull final Principal principal) throws IOException {
        checkComponentActive();
        try {
            if (sealer == null) {
                throw new IOException("No DataSealer was provided, unable to support serialization");
            }
            assert sealer != null;
            return sealer.wrap(super.getName(principal));
        } catch (final DataSealerException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected String getName(@Nullable final String serializedName) throws IOException {
        checkComponentActive();
        if (!Strings.isNullOrEmpty(serializedName)) {
            try {
                if (sealer == null) {
                    throw new IOException("No DataSealer was provided, unable to support serialization");
                }
                assert sealer != null;
                assert serializedName != null;
                return sealer.unwrap(serializedName);
            } catch (final DataSealerException e) {
                throw new IOException(e);
            }
        }
        
        return null;
    }
    
}
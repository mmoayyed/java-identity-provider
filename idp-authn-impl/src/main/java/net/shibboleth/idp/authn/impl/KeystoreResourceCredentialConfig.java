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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.resource.Resource;

import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.KeyStoreCredentialReader;
import org.ldaptive.ssl.KeyStoreSSLContextInitializer;
import org.ldaptive.ssl.SSLContextInitializer;

/**
 * Implementation of {@link CredentialConfig} that loads keystore and truststore data using a {@link Resource}.
 */
public class KeystoreResourceCredentialConfig implements CredentialConfig {

    /** Handles loading keystores. */
    @Nonnull private final KeyStoreCredentialReader keyStoreReader = new KeyStoreCredentialReader();

    /** Truststore resource. */
    @Nullable private Resource truststore;

    /** Password for truststore. */
    @Nullable private String truststorePassword;

    /** Type of truststore. */
    @Nullable private String truststoreType;

    /** Truststore aliases to use. */
    @Nullable private String[] truststoreAliases;

    /** Keystore resource. */
    @Nullable private Resource keystore;

    /** Password for keystore. */
    @Nullable private String keystorePassword;

    /** Type of keystore. */
    @Nullable private String keystoreType;

    /** Keystore aliases to use. */
    @Nullable private String[] keystoreAliases;

    /**
     * Set the truststore resource.
     * 
     * @param resource the truststore resource
     */
    public void setTruststore(@Nonnull @NotEmpty final Resource resource) {
        truststore = Constraint.isNotNull(resource, "Truststore resource cannot be null");
    }

    /**
     * Set the truststore password.
     * 
     * @param password the truststore password
     */
    public void setTruststorePassword(@Nonnull @NotEmpty final String password) {
        truststorePassword = Constraint.isNotNull(password, "Truststore password cannot be null");
    }

    /**
     * Set the truststore type.
     * 
     * @param type the truststore type
     */
    public void setTruststoreType(@Nonnull @NotEmpty final String type) {
        truststoreType =
                Constraint.isNotNull(StringSupport.trimOrNull(type), "Truststore type cannot be null or empty");
    }

    /**
     * Set the truststore aliases.
     * 
     * @param aliases the truststore aliases
     */
    public void setTruststoreAliases(@Nonnull @NotEmpty final String[] aliases) {
        truststoreAliases = Constraint.isNotNull(aliases, "Truststore aliases cannot be null or empty");
    }

    /**
     * Set the keystore resource.
     * 
     * @param resource the keystore resource
     */
    public void setKeystore(@Nonnull @NotEmpty final Resource resource) {
        keystore = Constraint.isNotNull(resource, "Keystore resource cannot be null");
    }

    /**
     * Set the keystore password.
     * 
     * @param password the keystore password
     */
    public void setKeystorePassword(@Nonnull @NotEmpty final String password) {
        keystorePassword = Constraint.isNotNull(password, "Keystore password cannot be null");
    }

    /**
     * Set the keystore type.
     * 
     * @param type the keystore type
     */
    public void setKeystoreType(@Nonnull @NotEmpty final String type) {
        keystoreType = Constraint.isNotNull(StringSupport.trimOrNull(type), "Keystore type cannot be null or empty");
    }

    /**
     * Set the keystore aliases.
     * 
     * @param aliases the keystore aliases
     */
    public void setKeystoreAliases(@Nonnull @NotEmpty final String[] aliases) {
        keystoreAliases = Constraint.isNotNull(aliases, "Keystore aliases cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Override public SSLContextInitializer createSSLContextInitializer() throws GeneralSecurityException {
        final KeyStoreSSLContextInitializer sslInit = new KeyStoreSSLContextInitializer();
        try {
            if (truststore != null) {
                sslInit.setTrustKeystore(keyStoreReader.read(truststore.getInputStream(), truststorePassword,
                        truststoreType));
                sslInit.setTrustAliases(truststoreAliases);
            }
            if (keystore != null) {
                sslInit.setAuthenticationKeystore(keyStoreReader.read(keystore.getInputStream(),
                        keystorePassword, keystoreType));
                sslInit.setAuthenticationPassword(keystorePassword != null ? keystorePassword.toCharArray() : null);
                sslInit.setAuthenticationAliases(keystoreAliases);
            }
        } catch (final IOException e) {
            throw new GeneralSecurityException(e);
        }
        return sslInit;
    }
}

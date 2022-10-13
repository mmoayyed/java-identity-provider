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

package net.shibboleth.idp.profile.config;

import java.time.Duration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.security.IdentifierGenerationStrategy;

import org.opensaml.xmlsec.DecryptionConfiguration;
import org.opensaml.xmlsec.EncryptionConfiguration;
import org.opensaml.xmlsec.SignatureSigningConfiguration;
import org.opensaml.xmlsec.SignatureValidationConfiguration;

/**
 * Basic implementation of {@link XMLSecurityConfiguration} interface.
 * 
 * @since 5.0.0
 */
public class BasicXMLSecurityConfiguration extends BasicSecurityConfiguration implements XMLSecurityConfiguration {

    /** Configuration used when validating protocol message signatures. */
    @Nullable private SignatureValidationConfiguration sigValidateConfig;

    /** Configuration used when generating protocol message signatures. */
    @Nullable private SignatureSigningConfiguration sigSigningConfig;

    /** Configuration used when decrypting protocol message information. */
    @Nullable private DecryptionConfiguration decryptConfig;

    /** Configuration used when encrypting protocol message information. */
    @Nullable private EncryptionConfiguration encryptConfig;

    /**
     * Default constructor.
     */
    public BasicXMLSecurityConfiguration() {
        
    }

    /**
     * Constructor.
     * 
     * @param skew the clock skew, must be greater than 0
     * @param generator the identifier generator, must not be null
     */
    public BasicXMLSecurityConfiguration(@Nonnull final Duration skew, @Nonnull final IdentifierGenerationStrategy generator) {
        super(skew, generator);
    }

    /** {@inheritDoc} */
    @Nullable public SignatureValidationConfiguration getSignatureValidationConfiguration() {
        return sigValidateConfig;
    }

    /**
     * Set the configuration used when validating protocol message signatures.
     * 
     * @param config configuration used when validating protocol message signatures, or null
     */
    public void setSignatureValidationConfiguration(@Nullable final SignatureValidationConfiguration config) {
        sigValidateConfig = config;
    }

    /** {@inheritDoc} */
    @Nullable public SignatureSigningConfiguration getSignatureSigningConfiguration() {
        return sigSigningConfig;
    }

    /**
     * Set the configuration used when generating protocol message signatures.
     * 
     * @param config configuration used when generating protocol message signatures, or null
     */
    public void setSignatureSigningConfiguration(@Nullable final SignatureSigningConfiguration config) {
        sigSigningConfig = config;
    }

    /** {@inheritDoc} */
    @Nullable public DecryptionConfiguration getDecryptionConfiguration() {
        return decryptConfig;
    }

    /**
     * Set the configuration used when decrypting protocol message information.
     * 
     * @param config configuration used when decrypting protocol message information, or null
     */
    public void setDecryptionConfiguration(@Nullable final DecryptionConfiguration config) {
        decryptConfig = config;
    }

    /** {@inheritDoc} */
    @Nullable public EncryptionConfiguration getEncryptionConfiguration() {
        return encryptConfig;
    }

    /**
     * Set the configuration used when encrypting protocol message information.
     * 
     * @param config configuration used when encrypting protocol message information, or null
     */
    public void setEncryptionConfiguration(@Nullable final EncryptionConfiguration config) {
        encryptConfig = config;
    }

}
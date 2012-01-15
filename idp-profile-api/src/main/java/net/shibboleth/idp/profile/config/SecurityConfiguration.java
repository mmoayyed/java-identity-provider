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

import java.util.concurrent.TimeUnit;

import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;

import org.opensaml.xml.security.DecryptionConfiguration;
import org.opensaml.xml.security.EncryptionConfiguration;
import org.opensaml.xml.security.SignatureSigningConfiguration;
import org.opensaml.xml.security.SignatureValidationConfiguration;

/** Configuration for request/response security operations. */
public class SecurityConfiguration {

    /** Acceptable clock skew expressed in milliseconds. */
    private final long clockSkew;

    /** Generator used to generate various secure IDs (e.g., message identifiers). */
    private final IdentifierGenerationStrategy idGenerator;

    /** Configuration used when validating protocol message signatures. */
    private SignatureValidationConfiguration sigValidateConfig;

    /** Configuration used when generating protocol message signatures. */
    private SignatureSigningConfiguration sigSigningConfig;

    /** Configuration used when decrypting protocol message information. */
    private DecryptionConfiguration decryptConfig;

    /** Configuration used when encrypting protocol message information. */
    private EncryptionConfiguration encryptConfig;

    /**
     * Constructor. Initializes the clock skew to 5 minutes and the identifier generator to
     * {@link SecureRandomIdentifierGenerator} using the SHA1PRNG algorithm.
     */
    public SecurityConfiguration() {
        clockSkew = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
        idGenerator = new RandomIdentifierGenerationStrategy();
    }

    /**
     * Constructor.
     * 
     * @param skew the clock skew, must be greater than 0
     * @param generator the identifier generator, must not be null
     */
    public SecurityConfiguration(int skew, IdentifierGenerationStrategy generator) {
        clockSkew = (int) Assert.isGreaterThan(0, skew, "Clock skew must be greater than 0");
        idGenerator = Assert.isNotNull(generator, "Identifier generator can not be null");
    }

    /**
     * Gets the acceptable clock skew expressed in milliseconds.
     * 
     * @return acceptable clock skew expressed in milliseconds
     */
    public long getClockSkew() {
        return clockSkew;
    }

    /**
     * Gets the generator used to generate secure identifiers.
     * 
     * @return generator used to generate secure identifiers, never null
     */
    public IdentifierGenerationStrategy getIdGenerator() {
        return idGenerator;
    }

    /**
     * Gets the configuration used when validating protocol message signatures.
     * 
     * @return configuration used when validating protocol message signatures, may be null
     */
    public SignatureValidationConfiguration getSignatureValidationConfiguration() {
        return sigValidateConfig;
    }

    /**
     * Sets the configuration used when validating protocol message signatures.
     * 
     * @param config configuration used when validating protocol message signatures, may be null
     */
    public void setSignatureValidationConfiguration(SignatureValidationConfiguration config) {
        sigValidateConfig = config;
    }

    /**
     * Gets the configuration used when generating protocol message signatures.
     * 
     * @return configuration used when generating protocol message signatures, may be null
     */
    public SignatureSigningConfiguration getSignatureSigningConfiguration() {
        return sigSigningConfig;
    }

    /**
     * Sets the configuration used when generating protocol message signatures.
     * 
     * @param config configuration used when generating protocol message signatures, may be null
     */
    public void setSignatureSigningConfiguration(SignatureSigningConfiguration config) {
        sigSigningConfig = config;
    }

    /**
     * Gets the configuration used when decrypting protocol message information.
     * 
     * @return configuration used when decrypting protocol message information, may be null
     */
    public DecryptionConfiguration getDecryptionConfiguration() {
        return decryptConfig;
    }

    /**
     * Sets the configuration used when decrypting protocol message information.
     * 
     * @param config configuration used when decrypting protocol message information, never null
     */
    public void setDecryptionConfiguration(DecryptionConfiguration config) {
        decryptConfig = config;
    }

    /**
     * Gets the configuration used when encrypting protocol message information.
     * 
     * @return configuration used when encrypting protocol message information, may be null
     */
    public EncryptionConfiguration getEncryptionConfiguration() {
        return encryptConfig;
    }

    /**
     * Sets the configuration used when encrypting protocol message information.
     * 
     * @param config configuration used when encrypting protocol message information, may be null
     */
    public void setEncryptionConfiguration(EncryptionConfiguration config) {
        encryptConfig = config;
    }
}
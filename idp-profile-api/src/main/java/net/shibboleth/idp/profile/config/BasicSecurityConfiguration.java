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

import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;

import org.opensaml.security.httpclient.HttpClientSecurityConfiguration;
import org.opensaml.security.x509.tls.ClientTLSValidationConfiguration;

/**
 * Basic implementation of {@link SecurityConfiguration} interface.
 * 
 * @since 5.0.0
 */
public class BasicSecurityConfiguration implements SecurityConfiguration {

    /** Acceptable clock skew. */
    @Nonnull private final Duration clockSkew;

    /** Generator used to generate various secure IDs (e.g., message identifiers). */
    @Nonnull private final IdentifierGenerationStrategy idGenerator;

    /** Configuration used when validating client TLS X509Credentials. */
    @Nullable private ClientTLSValidationConfiguration clientTLSConfig;

    /** Configuration used when executing HttpClient requests. */
    @Nullable private HttpClientSecurityConfiguration httpClientConfig;
    
    /**
     * Constructor.
     * 
     * Initializes the clock skew to 5 minutes and the identifier generator to {@link ProviderType#SECURE}.
     */
    public BasicSecurityConfiguration() {
        clockSkew = Duration.ofMinutes(5);
        idGenerator = IdentifierGenerationStrategy.getInstance(ProviderType.SECURE);
    }

    /**
     * Constructor.
     * 
     * @param skew the clock skew, must be greater than 0
     * @param generator the identifier generator, must not be null
     */
    public BasicSecurityConfiguration(@Nonnull final Duration skew,
            @Nonnull final IdentifierGenerationStrategy generator) {
        Constraint.isNotNull(skew, "Clock skew cannot be null");
        Constraint.isFalse(skew.isNegative() || skew.isZero(), "Clock skew must be greater than 0");
        
        clockSkew = skew;
        idGenerator = Constraint.isNotNull(generator, "Identifier generator cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull public Duration getClockSkew() {
        return clockSkew;
    }

    /** {@inheritDoc} */
    @Nonnull public IdentifierGenerationStrategy getIdGenerator() {
        return idGenerator;
    }

    /** {@inheritDoc} */
    @Nullable public ClientTLSValidationConfiguration getClientTLSValidationConfiguration() {
        return clientTLSConfig;
    }

    /**
     * Set the configuration used when validating client TLS X509Credentials.
     * 
     * @param config configuration used when validating client TLS X509Credentials, or null
     */
    public void setClientTLSValidationConfiguration(@Nullable final ClientTLSValidationConfiguration config) {
        clientTLSConfig = config;
    }
    
    /** {@inheritDoc} */
    @Nullable public HttpClientSecurityConfiguration getHttpClientSecurityConfiguration() {
        return httpClientConfig;
    }

    /**
     * Set the configuration used when executing HttpClient requests.
     * 
     * @param config configuration used when executing HttpClient requests, or null
     */
    public void setHttpClientSecurityConfiguration(@Nullable final HttpClientSecurityConfiguration config) {
        httpClientConfig = config;
    }
    
}
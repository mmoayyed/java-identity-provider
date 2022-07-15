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

package net.shibboleth.idp.saml.nameid.impl;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.opensaml.saml.common.SAMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;

/**
 * Generates transients using a {@link DataSealer} to encrypt the result into a recoverable value,
 * for use with stateless clustering.
 */
public class CryptoTransientIdGenerationStrategy extends AbstractIdentifiableInitializableComponent
        implements TransientIdGenerationStrategy {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CryptoTransientIdGenerationStrategy.class);

    /** Object used to protect and encrypt the data. */
    @NonnullAfterInit private DataSealer dataSealer;

    /** Length tokens are valid. */
    @Nonnull private Duration idLifetime;

    /** Constructor. */
    public CryptoTransientIdGenerationStrategy() {
        idLifetime = Duration.ofHours(4);
    }

    /**
     * Set the data sealer to use.
     * 
     * @param sealer object used to protect and encrypt the data
     */
    public void setDataSealer(@Nonnull final DataSealer sealer) {
        checkSetterPreconditions();
        dataSealer = Constraint.isNotNull(sealer, "DataSealer cannot be null");
    }
    
    /**
     * Get the time ids are valid.
     * 
     * @return  time ids are valid
     */
    @Nonnull public Duration getIdLifetime() {
        return idLifetime;
    }

    /**
     * Set the time ids are valid.
     * 
     * @param lifetime time ids are valid
     */
    public void setIdLifetime(@Nonnull final Duration lifetime) {
        checkSetterPreconditions();
        Constraint.isNotNull(lifetime, "Lifetime cannot be null");
        Constraint.isFalse(lifetime.isNegative() || lifetime.isZero(), "Lifetime must be positive");
        
        idLifetime = lifetime;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == dataSealer) {
            throw new ComponentInitializationException("DataSealer cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String generate(@Nonnull @NotEmpty final String relyingPartyId,
            @Nonnull @NotEmpty final String principalName) throws SAMLException {
        checkComponentActive();
        final StringBuilder principalTokenIdBuilder = new StringBuilder();
        principalTokenIdBuilder.append(relyingPartyId).append("!").append(principalName);

        try {
            return dataSealer.wrap(principalTokenIdBuilder.toString(), Instant.now().plus(idLifetime));
        } catch (final DataSealerException e) {
            throw new SAMLException("Exception wrapping principal identifier", e);
        }
    }

}
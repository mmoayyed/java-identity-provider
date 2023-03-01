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

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.DataSealerException;

/**
 * Generates transients using a {@link DataSealer} to encrypt the result into a recoverable value,
 * for use with stateless clustering.
 */
public class CryptoTransientIdGenerationStrategy extends AbstractIdentifiableInitializableComponent
        implements TransientIdGenerationStrategy {

    /** Object used to protect and encrypt the data. */
    @NonnullAfterInit private DataSealer dataSealer;

    /** Length tokens are valid. */
    @Nonnull private Duration idLifetime;

    /** Constructor. */
    public CryptoTransientIdGenerationStrategy() {
        final Duration fourHours = Duration.ofHours(4);
        assert fourHours!=null;
        idLifetime = fourHours;
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
            final String result = principalTokenIdBuilder.toString();
            assert result!=null;
            return dataSealer.wrap(result, Instant.now().plus(idLifetime));
        } catch (final DataSealerException e) {
            throw new SAMLException("Exception wrapping principal identifier", e);
        }
    }

}
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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.opensaml.saml.common.SAMLException;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.Positive;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;
import net.shibboleth.shared.security.RandomIdentifierParameterSpec;

/**
 * Generates transients using a {@link StorageService} to manage the reverse mappings.
 * 
 * <p>The identifier itself is the record key, and the value combines the principal name with the
 * identifier of the recipient.</p>
 */
public class StoredTransientIdGenerationStrategy extends AbstractIdentifiableInitializableComponent
        implements TransientIdGenerationStrategy {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StoredTransientIdGenerationStrategy.class);

    /** Store used to map identifiers to principals. */
    @NonnullAfterInit private StorageService idStore;

    /** Generator of random, hex-encoded, identifiers. */
    @NonnullAfterInit private IdentifierGenerationStrategy idGenerator;

    /** Size, in bytes, of the identifier. */
    private int idSize;

    /** Length identifiers are valid. */
    @Nonnull private Duration idLifetime;

    /** Constructor. */
    public StoredTransientIdGenerationStrategy() {
        idSize = 16;
        final Duration fourHours = Duration.ofHours(4);
        assert fourHours!=null;
        idLifetime = fourHours;
    }

    /**
     * Set the ID store we should use.
     * 
     * @param store the store to use.
     */
    public void setIdStore(@Nonnull final StorageService store) {
        checkSetterPreconditions();
        idStore = Constraint.isNotNull(store, "StorageService cannot be null");
    }

    /**
     * Set the ID generator we should use.
     * 
     * @param generator identifier generation strategy to use
     */
    public void setIdGenerator(@Nonnull final IdentifierGenerationStrategy generator) {
        checkSetterPreconditions();
        idGenerator = Constraint.isNotNull(generator, "IdentifierGenerationStrategy cannot be null");
    }
    
    /**
     * Get the size, in bytes, of the id.
     * 
     * @return  id size, in bytes
     */
    @Positive public int getIdSize() {
        return idSize;
    }
    
    /**
     * Set the size, in bytes, of the id.
     * 
     * @param size size, in bytes, of the id
     */
    public void setIdSize(@Positive final int size) {
        checkSetterPreconditions();
        idSize = (int) Constraint.isGreaterThan(0, size, "ID size must be positive");
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
        Constraint.isNotNull(lifetime, "ID lifetime cannot be null");
        Constraint.isFalse(lifetime.isNegative() || lifetime.isZero(), "ID lifetime must be greater than 0");
        
        idLifetime = lifetime;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == idStore) {
            throw new ComponentInitializationException("StorageService cannot be null");
        }

        if (idGenerator == null) {
            try {
                idGenerator = IdentifierGenerationStrategy.getInstance(ProviderType.RANDOM,
                        new RandomIdentifierParameterSpec(null, idSize, null));
            } catch (final InvalidAlgorithmParameterException|NoSuchAlgorithmException e) {
                throw new ComponentInitializationException(e);
            }
        }
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String generate(@Nonnull @NotEmpty final String relyingPartyId,
            @Nonnull @NotEmpty final String principalName) throws SAMLException {
        checkComponentActive();
        try {
            final String principalTokenId = new TransientIdParameters(relyingPartyId, principalName).encode();
    
            // This code used to store the entries keyed by the ID *and* the value, which I think
            // was used to prevent generation of multiple IDs if the resolver runs multiple times.
            // This is the source of the current V2 bug that causes the same transient to be reused
            // for the same SP within the TTL window. If we need to prevent multiple resolutions, I
            // suggest we do that by storing transactional state for resolver plugins in the context
            // tree. But in practice, I'm not sure it matters much how many times this runs, that's
            // the point of a transient. So this version never reads the store, it just writes to it.
    
            final String id = idGenerator.generateIdentifier();
    
            log.debug("Creating new transient ID '{}'", id);
    
            final Instant expiration = Instant.now().plus(idLifetime);
    
            int collisions = 0;
            while (collisions < 5) {
                if (idStore.create(TransientIdParameters.CONTEXT, id, principalTokenId, expiration.toEpochMilli())) {
                    return id;
                }
                ++collisions;
            }
        
            throw new SAMLException("Exceeded allowable number of collisions");
        } catch (final IOException e) {
            throw new SAMLException(e);
        }
    }

}
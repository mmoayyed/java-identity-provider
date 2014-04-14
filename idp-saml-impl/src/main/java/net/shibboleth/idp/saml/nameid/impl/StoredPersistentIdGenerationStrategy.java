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
import java.sql.Timestamp;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.nameid.PersistentIdEntry;
import net.shibboleth.idp.saml.nameid.PersistentIdStore;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.ProfileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages persistent IDs via a {@link PersistentIdStore}, generating them either randomly or via a
 * {@link ComputedPersistentIdGenerationStrategy} (for compatibility with existing data).
 */
public class StoredPersistentIdGenerationStrategy extends AbstractIdentifiableInitializableComponent
        implements PersistentIdGenerationStrategy {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StoredPersistentIdGenerationStrategy.class);

    /** Persistent identifier data store. */
    @NonnullAfterInit private PersistentIdStore pidStore;

    /** Optional generator of computed ID values. */
    @Nullable private ComputedPersistentIdGenerationStrategy computedIdStrategy;
    
    /**
     * Get the {@link DataSource} used to communicate with the database.
     * 
     * @return the {@link DataSource}.
     */
    @NonnullAfterInit public PersistentIdStore getIDStore() {
        return pidStore;
    }

    /**
     * Set the {@link PersistentIdStore} used to store the IDs.
     * 
     * @param store the ID store to use
     */
    public void setIDStore(@Nonnull final PersistentIdStore store) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        pidStore = Constraint.isNotNull(store, "PersistentIdStore cannot be null");
    }

    /**
     * Set a strategy to use to compute IDs for the first time.
     * 
     * @param strategy  computed ID strategy
     */
    public void setComputedIdStrategy(@Nullable final ComputedPersistentIdGenerationStrategy strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        computedIdStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (null == pidStore) {
            throw new ComponentInitializationException("PersistentIdStore cannot be null");
        }
    }

    /** {@inheritDoc} */
    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String generate(@Nonnull @NotEmpty final String assertingPartyId,
            @Nonnull @NotEmpty final String relyingPartyId, @Nonnull @NotEmpty final String principalName,
            @Nonnull @NotEmpty final String sourceId) throws ProfileException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        try {
            log.debug("Checking for existing, active, stored ID for principal '{}'", principalName);
            PersistentIdEntry idEntry = pidStore.getActiveEntry(assertingPartyId, relyingPartyId, sourceId);
            if (idEntry == null) {
                log.debug("No existing, active, stored ID, creating a new one for principal '{}'", principalName);
                idEntry = createPersistentId(principalName, assertingPartyId, relyingPartyId, sourceId);
                pidStore.store(idEntry);
                log.debug("Created stored ID '{}'", idEntry);
            } else {
                log.debug("Located existing stored ID {}", idEntry);
            }
    
            final String pid = StringSupport.trimOrNull(idEntry.getPersistentId());
            if (null == pid) {
                log.debug("Returned persistent ID was empty");
                throw new ProfileException("Returned persistent ID was empty");
            }
    
            return pid;
        } catch (final IOException e) {
            log.debug("ID storage error retrieving persistent identifier", e);
            throw new ProfileException("ID storage error retrieving persistent identifier", e);
        }
    }
    

    /**
     * Create a persistent ID that is unique for a given local/peer/localId tuple.
     * 
     * <p>If an ID has never been issued, and a legacy strategy is supplied, then an ID is created using that strategy.
     * This is to ensure compatability with IDs created by that strategy when switching to this one.</p>
     * 
     * <p>If an ID has been issued to the given tuple than a new, random type 4 UUID is generated.</p>
     * 
     * @param principalName principal name of the user to whom the persistent ID belongs
     * @param localEntityId ID of the local entity associated with the persistent ID
     * @param peerEntityId ID of the peer entity associated with the persistent ID
     * @param localId underlying unique ID of the subject the persistent ID represents
     * 
     * @return the created identifier
     * 
     * @throws IOException if there is a problem communication with the database
     * @throws ProfileException if there is a problem with generation
     */
    @Nonnull protected PersistentIdEntry createPersistentId(@Nonnull @NotEmpty String principalName,
            @Nonnull @NotEmpty String localEntityId, @Nonnull @NotEmpty String peerEntityId,
            @Nonnull @NotEmpty String localId) throws IOException, ProfileException {
        
        final PersistentIdEntry entry = new PersistentIdEntry();
        entry.setIssuerEntityId(Constraint.isNotNull(StringSupport.trimOrNull(localEntityId),
                "Attribute Issuer entity Id must not be null"));
        entry.setRecipientEntityId(Constraint.isNotNull(StringSupport.trimOrNull(peerEntityId),
                "Attribute Recipient entity Id must not be null"));
        entry.setPrincipalName(Constraint.isNotNull(StringSupport.trimOrNull(principalName),
                "Principal must not be null"));
        entry.setSourceId(localId);

        final int numberOfExistingEntries = pidStore.getCount(entry.getIssuerEntityId(),
                entry.getRecipientEntityId(), entry.getSourceId());

        String persistentId;
        if (numberOfExistingEntries == 0 && null != computedIdStrategy) {
            persistentId = computedIdStrategy.generate(localEntityId, peerEntityId, principalName, localId);
        } else {
            persistentId = UUID.randomUUID().toString();
        }

        while (!pidStore.isAvailable(persistentId)) {
            log.debug("Generated persistent ID was already assigned to another user, regenerating");
            persistentId = UUID.randomUUID().toString();
        }

        entry.setPersistentId(persistentId);
        entry.setCreationTime(new Timestamp(System.currentTimeMillis()));

        return entry;
    }
    
}
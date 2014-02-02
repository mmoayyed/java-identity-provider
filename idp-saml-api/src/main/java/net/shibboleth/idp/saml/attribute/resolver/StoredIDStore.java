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

package net.shibboleth.idp.saml.attribute.resolver;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.joda.time.DateTime;

/**
 * Represents as persistent, store of identifiers.
 * 
 */
public interface  StoredIDStore {

    /**
     * Gets the number of persistent ID entries for a (principal, peer, local) tuple.
     * 
     * @param localEntity entity ID of the ID issuer
     * @param peerEntity entity ID of the peer the ID is for
     * @param localId local ID part of the persistent ID
     * 
     * @return the number of identifiers
     * 
     * @throws StoredIDException thrown if there is a problem
     */
    public int getNumberOfPersistentIdEntries(@Nonnull @NotEmpty String localEntity,
            @Nonnull @NotEmpty String peerEntity, @Nonnull @NotEmpty String localId) throws StoredIDException;

    /**
     * Gets all the persistent ID entries for a (principal, peer, local) tuple.
     * 
     * @param localId local ID part of the persistent ID
     * @param peerEntity entity ID of the peer the ID is for
     * @param localEntity entity ID of the ID issuer
     * 
     * @return the active identifier
     * 
     * @throws StoredIDException thrown if there is a problem
     */
    @Nonnull public List<PersistentIdEntry> getPersistentIdEntries(@Nonnull @NotEmpty String localEntity,
            @Nonnull @NotEmpty String peerEntity, @Nonnull @NotEmpty String localId) throws StoredIDException;

    /**
     * Gets the persistent ID entry for the given ID.
     * 
     * @param persistentId the persistent ID
     * 
     * @return the ID entry for the given ID or null if none exists
     * 
     * @throws StoredIDException thrown if there is a problem
     */
    @Nullable public PersistentIdEntry getActivePersistentIdEntry(String persistentId) throws StoredIDException;

    /**
     * Gets the currently active identifier entry for a (principal, peer, local) tuple.
     * 
     * @param localId local ID part of the persistent ID
     * 
     * @param peerEntity entity ID of the peer the ID is for
     * 
     * @param localEntity entity ID of the ID issuer
     * 
     * @return the active identifier
     * 
     * @throws StoredIDException thrown if there is a problem
     */
    @Nonnull public PersistentIdEntry getActivePersistentIdEntry(@Nonnull @NotEmpty String localEntity,
            @Nonnull @NotEmpty String peerEntity, @Nonnull @NotEmpty String localId) throws StoredIDException;


    /**
     * Stores a persistent ID entry.
     * 
     * @param entry entry to persist
     * 
     * @throws StoredIDException thrown is there is a problem
     */
    public void storePersistentIdEntry(@Nonnull PersistentIdEntry entry) throws StoredIDException;
    
    /**
     * Deactivates a given persistent ID.
     * 
     * @param persistentId ID to deactivate
     * @param deactivation deactivation time, if null the current time is used
     * 
     * @throws StoredIDException thrown if there is a problem communication with the database
     */
    public void deactivatePersistentId(@NotEmpty String persistentId, @Nullable DateTime deactivation)
            throws StoredIDException;

}
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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * A data connector that generates persistent identifiers in one of two ways. The generated attribute has an ID of
 * <tt>peristentId</tt> and contains a single {@link String} value.
 * 
 * If a salt is supplied at construction time the generated IDs will be the Base64-encoded SHA-1 hash of the user's
 * principal name, the peer entity ID, and the salt. If the salt is not provided we fall straight into generating UUIDs.
 * <em>NOTE</em> that neither or both must be supplied.
 * 
 * If a {@link DataSource} is supplied the IDs are created and managed as described by {@link StoredIDStore}.
 */
public class StoredIDDataConnector extends BaseComputedIDDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StoredIDDataConnector.class);

    /** The {@link DataSource} used to communicate with the database. */
    private DataSource dataSource;

    /** SQL query timeout in seconds. */
    private int queryTimeout;

    /** Persistent identifier data store. */
    private StoredIDStore pidStore;

    /**
     * Gets the {@link DataSource} used to communicate with the database.
     * 
     * @return the {@link DataSource}.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the {@link DataSource} used to communicate with the database.
     * 
     * @param source the {@link DataSource}.
     */
    public void setDataSource(DataSource source) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        dataSource = source;
    }

    /**
     * Gets the data store used to manage stored IDs.
     * 
     * @return data store used to manage stored IDs
     */
    public StoredIDStore getStoredIDStore() {
        return pidStore;
    }

    /**
     * Get the SQL query timeout.
     * 
     * @return the timeout in seconds
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * Set the SQL query timeout.
     * 
     * @param timeout the timeout to set in seconds
     */
    public void setQueryTimeout(int timeout) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        queryTimeout = timeout;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == dataSource) {
            throw new ComponentInitializationException("StoredIdConnector " + getId()
                    + ": No database connection provided");
        }

        if (null != getSalt() && getSalt().length < 16) {
            throw new ComponentInitializationException("StoredIDDataConnector definition '" + getId()
                    + "': If provided, the salt must be at least 16 bytes in size");
        }

        StoredIDStore store = new StoredIDStore();
        store.setDataSource(dataSource);
        store.setQueryTimeout(queryTimeout);
        store.initialize();
        pidStore = store;
    }

    /**
     * Creates a persistent ID that is unique for a given local/peer/localId tuple.
     * 
     * If an ID has never been issued for to the given tuple then an ID is created by taking a SHA-1 hash of the peer's
     * entity ID, the local ID, and a salt. This is to ensure compatability with IDs created by the now deprecated
     * {@link ComputedIDDataConnector}.
     * 
     * If an ID has been issued to the given tuple than a new, random type 4 UUID is generated as the persistent ID.
     * 
     * @param resolutionContext
     * 
     * @param principalName principal name of the user to whom the persistent ID belongs
     * @param localEntityId ID of the local entity associated with the persistent ID
     * @param peerEntityId ID of the peer entity associated with the persistent ID
     * @param localId principal the the persistent ID represents
     * 
     * @return the created identifier
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     * @throws ResolutionException if there is a problem with has generation
     */
    protected PersistentIdEntry createPersistentId(String principalName, String localEntityId, String peerEntityId,
            String localId) throws SQLException, ResolutionException {
        PersistentIdEntry entry = new PersistentIdEntry();
        entry.setAttributeIssuerId(localEntityId);
        entry.setPeerEntityId(peerEntityId);
        entry.setPrincipalName(principalName);
        entry.setLocalId(localId);

        String persistentId;
        int numberOfExistingEntries =
                pidStore.getNumberOfPersistentIdEntries(entry.getAttributeIssuerId(), entry.getAttributeConsumerId(),
                        entry.getLocalId());

        if (numberOfExistingEntries == 0 && null != getSalt()) {
            persistentId = generateComputedId(peerEntityId, localId);
        } else {
            persistentId = UUID.randomUUID().toString();
        }

        while (pidStore.getPersistentIdEntry(persistentId, false) != null) {
            log.debug("Generated persistent ID was already assigned to another user, regenerating");
            persistentId = UUID.randomUUID().toString();
        }

        entry.setPersistentId(persistentId);

        entry.setCreationTime(new Timestamp(System.currentTimeMillis()));

        return entry;
    }

    /**
     * Gets the persistent ID stored in the database. If one does not exist it is created.
     * 
     * @param principalName principal name of the user to whom the persistent ID belongs
     * @param localEntityId ID of the local entity associated with the persistent ID
     * @param spEntityId ID of the peer entity associated with the persistent ID
     * @param localId principal the the persistent ID represents
     * 
     * @return persistent ID
     * 
     * @throws ResolutionException thrown if there is a problem retrieving or storing the persistent ID
     */
    protected String getStoredId(String principalName, String localEntityId, String spEntityId, String localId)
            throws ResolutionException {
        PersistentIdEntry idEntry;
        try {
            log.debug("Checking for existing, active, stored ID for principal '{}'", principalName);
            idEntry = pidStore.getActivePersistentIdEntry(localEntityId, spEntityId, localId);
            if (idEntry == null) {
                log.debug("No existing, active, stored ID, creating a new one for principal '{}'", principalName);
                idEntry = createPersistentId(principalName, localEntityId, spEntityId, localId);
                pidStore.storePersistentIdEntry(idEntry);
                log.debug("Created stored ID '{}'", idEntry);
            } else {
                log.debug("Located existing stored ID {}", idEntry);
            }

            return idEntry.getPersistentId();
        } catch (SQLException e) {
            log.debug("Database error retrieving persistent identifier", e);
            throw new ResolutionException("Database error retrieving persistent identifier", e);
        }
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Map<String, Attribute>> doDataConnectorResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            log.warn("Attribute definition '{}' no attribute recipient context provided ", getId());
            return Optional.absent();
        }

        final String principal = StringSupport.trimOrNull(attributeRecipientContext.getPrincipal());
        boolean returnAbsent = false;

        if (null == principal) {
            log.warn("StoredID '{}' : No principal available, skipping ID creation", getId());
            returnAbsent = true;
        }

        final String localId = StringSupport.trimOrNull(resolveSourceAttribute(resolutionContext));
        if (null == localId) {
            // We did the logging in the helper method
            returnAbsent = true;
        }

        final String attributeIssuerID = StringSupport.trimOrNull(attributeRecipientContext.getAttributeIssuerID());
        if (null == attributeIssuerID) {
            log.warn("StoredID '{}' : Could not get attribute issuer ID, skipping ID creation", getId());
            returnAbsent = true;
        }

        final String attributeRecipientID =
                StringSupport.trimOrNull(attributeRecipientContext.getAttributeRecipientID());
        if (null == attributeRecipientID) {
            log.warn("StoredID '{}' : Could not get attribute recipient ID, skipping ID creation", getId());
            returnAbsent = true;
        }

        if (returnAbsent) {
            return Optional.absent();
        }

        return encodeAsAttribute(getStoredId(principal, attributeIssuerID, attributeRecipientID, localId));
    }
}

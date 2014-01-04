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
import javax.annotation.Nullable;
import javax.sql.DataSource;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private StoredIDStore pidStore = new StoredIDStore();

    /**
     * Gets the {@link DataSource} used to communicate with the database.
     * 
     * @return the {@link DataSource}.
     */
    @Nullable @NonnullAfterInit public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the {@link DataSource} used to communicate with the database.
     * 
     * @param source the {@link DataSource}.
     */
    public void setDataSource(@Nullable DataSource source) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        dataSource = source;
    }

    /**
     * Gets the data store used to manage stored IDs.
     * 
     * @return data store used to manage stored IDs
     */
    @NonnullAfterInit public StoredIDStore getStoredIDStore() {
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
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == dataSource) {
            throw new ComponentInitializationException(getLogPrefix() + " No database connection provided");
        }

        if (null != getSalt() && getSalt().length < 16) {
            throw new ComponentInitializationException(getLogPrefix()
                    + " If provided, the salt must be at least 16 bytes in size");
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
    @Nonnull protected PersistentIdEntry createPersistentId(@Nonnull @NotEmpty String principalName,
            @Nonnull @NotEmpty String localEntityId, @Nonnull @NotEmpty String peerEntityId,
            @Nonnull @NotEmpty String localId) throws SQLException, ResolutionException {
        final PersistentIdEntry entry = new PersistentIdEntry();
        entry.setAttributeIssuerId(Constraint.isNotNull(StringSupport.trimOrNull(localEntityId),
                "Attribute Issuer entity Id must not be null"));
        entry.setPeerEntityId(Constraint.isNotNull(StringSupport.trimOrNull(peerEntityId),
                "Attribute Recipient entity Id must not be null"));
        entry.setPrincipalName(Constraint.isNotNull(StringSupport.trimOrNull(principalName),
                "Principal must not be null"));
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
            log.debug("{} Generated persistent ID was already assigned to another user, regenerating", getLogPrefix());
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
    @Nonnull @NotEmpty protected String getStoredId(@Nonnull @NotEmpty String principalName,
            @Nonnull @NotEmpty String localEntityId, @Nonnull @NotEmpty String spEntityId,
            @Nonnull @NotEmpty String localId) throws ResolutionException {
        PersistentIdEntry idEntry;
        try {
            log.debug("{} Checking for existing, active, stored ID for principal '{}'", getLogPrefix(), principalName);
            idEntry = pidStore.getActivePersistentIdEntry(localEntityId, spEntityId, localId);
            if (idEntry == null) {
                log.debug("{} No existing, active, stored ID, creating a new one for principal '{}'", getLogPrefix(),
                        principalName);
                idEntry = createPersistentId(principalName, localEntityId, spEntityId, localId);
                pidStore.storePersistentIdEntry(idEntry);
                log.debug("{} Created stored ID '{}'", getLogPrefix(), idEntry);
            } else {
                log.debug("{} Located existing stored ID {}", getLogPrefix(), idEntry);
            }

            final String pid = StringSupport.trimOrNull(idEntry.getPersistentId());
            if (null == pid) {
                log.debug("{} Returned persistent ID was empty", getLogPrefix());
                throw new ResolutionException(getLogPrefix() + " Returned persistent ID was empty");
            }

            return pid;
        } catch (SQLException e) {
            log.debug("{} Database error retrieving persistent identifier", getLogPrefix(), e);
            throw new ResolutionException("Database error retrieving persistent identifier", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Map<String, IdPAttribute> doDataConnectorResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final String principal = StringSupport.trimOrNull(resolutionContext.getPrincipal());

        if (null == principal) {
            log.warn("{} No principal available, skipping ID creation", getLogPrefix());
            return null;
        }

        final String localId = StringSupport.trimOrNull(resolveSourceAttribute(resolutionContext));
        if (null == localId) {
            // We did the logging in the helper method
            return null;
        }

        final String attributeIssuerID = StringSupport.trimOrNull(resolutionContext.getAttributeIssuerID());
        if (null == attributeIssuerID) {
            log.warn("{} Could not get attribute issuer ID, skipping ID creation", getLogPrefix());
            return null;
        }

        final String attributeRecipientID =
                StringSupport.trimOrNull(resolutionContext.getAttributeRecipientID());
        if (null == attributeRecipientID) {
            log.warn("{} Could not get attribute recipient ID, skipping ID creation", getLogPrefix());
            return null;
        }

        return encodeAsAttribute(getStoredId(principal, attributeIssuerID, attributeRecipientID, localId));
    }
}

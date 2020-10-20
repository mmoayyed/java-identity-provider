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

package net.shibboleth.idp.attribute.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import net.shibboleth.idp.attribute.DurablePairwiseIdStore;
import net.shibboleth.idp.attribute.PairwiseId;
import net.shibboleth.idp.attribute.PairwiseIdStore;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC-based storage management for pairwise IDs.
 * 
 * <p>The general DDL for the database, which is unchanged for compatibility, is:</p>
 * 
 * <pre>
 * CREATE TABLE shibpid (
 *      localEntity VARCHAR(255) NOT NULL,
 *      peerEntity VARCHAR(255) NOT NULL,
 *      persistentId VARCHAR(50) NOT NULL,
 *      principalName VARCHAR(50) NOT NULL,
 *      localId VARCHAR(50) NOT NULL,
 *      peerProvidedId VARCHAR(50) NULL,
 *      creationDate TIMESTAMP NOT NULL,
 *      deactivationDate TIMESTAMP NULL,
 *      PRIMARY KEY (localEntity, peerEntity, persistentId)
 *     );</pre>.
 *    
 * <p>The first three columns should be defined as the primary key of the table, and the other columns
 * should be indexed.</p>
 * 
 * @since 4.0.0
 */
public class JDBCPairwiseIdStore extends AbstractInitializableComponent implements DurablePairwiseIdStore {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(JDBCPairwiseIdStore.class);
    
    /** JDBC data source for retrieving connections. */
    @NonnullAfterInit private DataSource dataSource;

    /** Timeout of SQL queries. */
    @Nonnull private Duration queryTimeout;

    /** Number of times to retry a transaction if it rolls back. */
    @NonNegative private int transactionRetry;
    
    /** Error messages that signal a transaction should be retried. */
    @Nonnull @NonnullElements private Collection<String> retryableErrors;

    /** Whether to fail if the database cannot be verified.  */
    private boolean verifyDatabase;
    
    /** Name of the database table. */
    @Nonnull @NotEmpty private String tableName;

    /** Name of the issuer entityID column. */
    @Nonnull @NotEmpty private String issuerColumn;

    /** Name of the recipient entityID column. */
    @Nonnull @NotEmpty private String recipientColumn;

    /** Name of the principal name column. */
    @Nonnull @NotEmpty private String principalNameColumn;

    /** Name of the source ID column. */
    @Nonnull @NotEmpty private String sourceIdColumn;

    /** Name of the persistent ID column. */
    @Nonnull @NotEmpty private String persistentIdColumn;

    /** Name of recipient-attached alias column. */
    @Nonnull @NotEmpty private String peerProvidedIdColumn;

    /** Name of the creation time column. */
    @Nonnull @NotEmpty private String creationTimeColumn;

    /** Name of the deactivation time column. */
    @Nonnull @NotEmpty private String deactivationTimeColumn;

    /** Parameterized select query for lookup by issued value. */
    @NonnullAfterInit private String getByIssuedSelectSQL;

    /** Parameterized select query for lookup by source ID. */
    @NonnullAfterInit private String getBySourceSelectSQL;

    /** Parameterized insert statement used to insert a new record. */
    @NonnullAfterInit private String insertSQL;

    /** Parameterized update statement used to deactivate an ID. */
    @NonnullAfterInit private String deactivateSQL;

    /** Parameterized update statement used to attach an alias to an ID. */
    @NonnullAfterInit private String attachSQL;
    
    /** Parameterized delete statement used to clear dummy rows after verification. */
    @NonnullAfterInit private String deleteSQL;
    
    /** Optional hook for obtaining initial values from a primary store, usually a computed algorithm. */
    @Nullable private PairwiseIdStore initialValueStore;

    /** Constructor. */
    public JDBCPairwiseIdStore() {
        transactionRetry = 3;
        retryableErrors = List.of("23000", "23505");
        queryTimeout = Duration.ofSeconds(5);
        verifyDatabase = true;
        
        tableName = "shibpid";
        issuerColumn = "localEntity";
        recipientColumn = "peerEntity";
        principalNameColumn = "principalName";
        sourceIdColumn = "localId";
        persistentIdColumn = "persistentId";
        peerProvidedIdColumn = "peerProvidedId";
        creationTimeColumn = "creationDate";
        deactivationTimeColumn = "deactivationDate";
    }
    
    /**
     * Get the source datasource used to communicate with the database.
     * 
     * @return the data source;
     */
    @NonnullAfterInit public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Get the source datasource used to communicate with the database.
     * 
     * @param source the data source;
     */
    public void setDataSource(@Nonnull final DataSource source) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        dataSource = Constraint.isNotNull(source, "DataSource cannot be null");
    }

    /**
     * Get the SQL query timeout.
     * 
     * @return the timeout
     */
    @Nonnull public Duration getQueryTimeout() {
        return queryTimeout;
    }
    
    /**
     * Set the SQL query timeout. Defaults to 5s.
     * 
     * @param timeout the timeout to set
     */
    public void setQueryTimeout(@Nonnull final Duration timeout) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(timeout, "Timeout cannot be null");
        Constraint.isFalse(timeout.isNegative(), "Timeout cannot be negative");
        
        queryTimeout = timeout;
    }

    /**
     * Get the number of retries to attempt for a failed transaction.
     * 
     * @return number of retries
     */
    public int getTransactionRetries() {
        return transactionRetry;
    }
    
    /**
     * Set the number of retries to attempt for a failed transaction. Defaults to 3.
     * 
     * @param retries the number of retries
     */
    public void setTransactionRetries(@NonNegative final int retries) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        transactionRetry = Constraint.isGreaterThanOrEqual(0, retries, "Retries must be greater than or equal to 0");
    }

    /**
     * Get the error messages to check for classifying a driver error as retryable, generally indicating
     * a lock violation or duplicate insert that signifies a broken database.
     * 
     * @return retryable messages
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<String> getRetryableErrors() {
        return retryableErrors;
    }
    
    /**
     * Set the error messages to check for classifying a driver error as retryable, generally indicating
     * a lock violation or duplicate insert that signifies a broken database.
     * 
     * @param errors retryable messages
     */
    public void setRetryableErrors(@Nullable @NonnullElements final Collection<String> errors) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        retryableErrors = List.copyOf(StringSupport.normalizeStringCollection(errors));
    }
    
    /**
     * Get whether to allow startup if the database cannot be verified.
     * 
     * @return whether to allow startup if the database cannot be verified
     */
    public boolean getVerifyDatabase() {
        return verifyDatabase;
    }
    
    /**
     * Set whether to allow startup if the database cannot be verified.
     * 
     * <p>Verification consists not only of a liveness check, but the successful insertion of
     * a dummy row, a failure to insert a duplicate, and then deletion of the row.</p>
     * 
     * @param flag flag to set
     */
    public void setVerifyDatabase(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        verifyDatabase = flag;
    }
    
    /**
     * Gets the table name.
     * 
     * @return table name
     * 
     * @since 4.1.0
     */
    @Nonnull @NotEmpty public String getTableName() {
        return tableName;
    }

    /**
     * Set the table name.
     * 
     * @param name table name
     */
    public void setTableName(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        tableName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Table name cannot be null or empty");
    }

    /**
     * Set the name of the issuer entityID column.
     * 
     * @param name name of issuer column
     */
    public void setLocalEntityColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        issuerColumn = Constraint.isNotNull(StringSupport.trimOrNull(name), "Column name cannot be null or empty");
    }

    /**
     * Set the name of the recipient entityID column.
     * 
     * @param name name of recipient column
     */
    public void setPeerEntityColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        recipientColumn = Constraint.isNotNull(StringSupport.trimOrNull(name), "Column name cannot be null or empty");
    }

    /**
     * Set the name of the principal name column.
     * 
     * @param name name of principal name column
     */
    public void setPrincipalNameColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        principalNameColumn = Constraint.isNotNull(StringSupport.trimOrNull(name),
                "Column name cannot be null or empty");
    }

    /**
     * Set the name of the source ID column.
     * 
     * @param name name of source ID column
     */
    public void setSourceIdColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        sourceIdColumn = Constraint.isNotNull(StringSupport.trimOrNull(name), "Column name cannot be null or empty");
    }

    /**
     * Set the name of the persistent ID column.
     * 
     * @param name name of the persistent ID column
     */
    public void setPersistentIdColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        persistentIdColumn = Constraint.isNotNull(StringSupport.trimOrNull(name),
                "Column name cannot be null or empty");
    }

    /**
     * Set the name of the peer-provided ID column.
     * 
     * @param name name of peer-provided ID column
     */
    public void setPeerProvidedIdColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        peerProvidedIdColumn = Constraint.isNotNull(StringSupport.trimOrNull(name),
                "Column name cannot be null or empty");
    }

    /**
     * Set the name of the creation time column.
     * 
     * @param name name of creation time column
     */
    public void setCreateTimeColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        creationTimeColumn = Constraint.isNotNull(StringSupport.trimOrNull(name),
                "Column name cannot be null or empty");
    }

    /**
     * Set the name of the deactivation time column.
     * 
     * @param name name of deactivation time column
     */
    public void setDeactivationTimeColumn(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        deactivationTimeColumn = Constraint.isNotNull(StringSupport.trimOrNull(name),
                "Column name cannot be null or empty");
    }

    /**
     * Set the SELECT statement used to lookup records by issued value.
     * 
     * @param sql statement text, which must contain three parameters (NameQualifier, SPNameQualifier, value)
     */
    public void setGetByIssuedSelectSQL(@Nonnull @NotEmpty final String sql) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        getByIssuedSelectSQL = Constraint.isNotNull(StringSupport.trimOrNull(sql),
                "SQL statement cannot be null or empty");
    }

    /**
     * Set the SELECT statement used to lookup records by source ID.
     * 
     * @param sql statement text, which must contain six parameters
     * (NameQualifier, SPNameQualifier, source ID, NameQualifier, SPNameQualifier, source ID)
     */
    public void setGetBySourceSelectSQL(@Nonnull @NotEmpty final String sql) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        getBySourceSelectSQL = Constraint.isNotNull(StringSupport.trimOrNull(sql),
                "SQL statement cannot be null or empty");
    }

    /**
     * Set the INSERT statement used to insert new records.
     * 
     * @param sql statement text, which must contain 8 parameters
     *  (NameQualifier, SPNameQualifier, value, principal, source ID, SPProvidedID, creation time, deactivation time)
     */
    public void setInsertSQL(@Nonnull @NotEmpty final String sql) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        insertSQL = Constraint.isNotNull(StringSupport.trimOrNull(sql), "SQL statement cannot be null or empty");
    }

    /**
     * Set the UPDATE statement used to deactivate issued values.
     * 
     * @param sql statement text, which must contain four parameters
     *  (deactivation TS, NameQualifier, SPNameQualifier, value)
     */
    public void setDeactivateSQL(@Nonnull @NotEmpty final String sql) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        deactivateSQL = Constraint.isNotNull(StringSupport.trimOrNull(sql), "SQL statement cannot be null or empty");
    }

    /**
     * Set the UPDATE statement used to attach an SPProvidedID to an issued value.
     * 
     * @param sql statement text, which must contain four parameters
     *  (SPProvidedID, NameQualifier, SPNameQualifier, value)
     */
    public void setAttachSQL(@Nonnull @NotEmpty final String sql) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        attachSQL = Constraint.isNotNull(StringSupport.trimOrNull(sql), "SQL statement cannot be null or empty");
    }

    /**
     * Set the DELETE statement used to clear dummy row(s) created during verification.
     * 
     * @param sql statement text, which must contain one parameter (NameQualifier)
     */
    public void setDeleteSQL(@Nonnull @NotEmpty final String sql) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        deleteSQL = Constraint.isNotNull(StringSupport.trimOrNull(sql), "SQL statement cannot be null or empty");
    }
        
    /**
     * Get a store to use to produce the first value for a given issuer/recipient pair.
     * 
     * @return initial value source
     */
    @Nullable public PairwiseIdStore getInitialValueStore() {
        return initialValueStore;
    }

    /**
     * Set a store to use to produce the first value for a given issuer/recipient pair.
     * 
     * <p>This is typically used to draw the "first" (often only) value for a given pairwise
     * relationship from an algorithm instead of a random value requiring storage to know.</p>
     * 
     * @param store initial value source
     */
    public void setInitialValueStore(@Nullable final PairwiseIdStore store) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        initialValueStore = store;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (null == dataSource) {
            throw new ComponentInitializationException("DataSource cannot be null");
        }
        
        if (getByIssuedSelectSQL == null) {
            getByIssuedSelectSQL = "SELECT * FROM " + tableName + " WHERE " + issuerColumn + "= ? AND "
                    + recipientColumn + "= ? AND " + persistentIdColumn + "= ?";
        }
        
        if (getBySourceSelectSQL == null) {
            getBySourceSelectSQL = "SELECT * FROM " + tableName + " WHERE " + issuerColumn + "= ? AND "
                    + recipientColumn + "= ? AND " + sourceIdColumn + "= ? "
                    + "AND (" + deactivationTimeColumn + " IS NULL OR "
                    + deactivationTimeColumn + " = (SELECT MAX(" + deactivationTimeColumn
                    + ") FROM " + tableName + " WHERE " + issuerColumn + "= ? AND "
                    + recipientColumn + "= ? AND " + sourceIdColumn + "= ?)) ORDER BY "
                    + creationTimeColumn + " DESC";
        }
        
        if (insertSQL == null) {
            insertSQL = "INSERT INTO " + tableName + " ("
                    + issuerColumn + ", "
                    + recipientColumn + ", "
                    + persistentIdColumn + ", "
                    + principalNameColumn + ", "
                    + sourceIdColumn + ", "
                    + peerProvidedIdColumn + ", "
                    + creationTimeColumn + ", "
                    + deactivationTimeColumn
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        }
                
        if (deactivateSQL == null) {
            deactivateSQL = "UPDATE " + tableName + " SET " + deactivationTimeColumn + "= ? WHERE "
                    + issuerColumn + "= ? AND " + recipientColumn + "= ? AND " + persistentIdColumn + "= ?";
        }

        if (attachSQL == null) {
            attachSQL = "UPDATE " + tableName + " SET " + peerProvidedIdColumn + "= ? WHERE "
                    + issuerColumn + "= ? AND " + recipientColumn + "= ? AND " + persistentIdColumn + "= ?";
        }
        
        if (deleteSQL == null) {
            deleteSQL = "DELETE FROM " + tableName + " WHERE " + issuerColumn + "= ?";
        }
        
        try {
            verifyDatabase();
            log.info("DataSource successfully verified");
        } catch (final SQLException e) {
            if (verifyDatabase) {
                log.error("Exception verifying database", e);
                throw new ComponentInitializationException(
                        "The database was not reachable or was not defined with an appropriate table + primary key");
            }
            log.warn("The database was not reachable or was not defined with an appropriate table + primary key", e);
        }
    }

    // Checkstyle: MethodLength|CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Nullable public PairwiseId getBySourceValue(@Nonnull final PairwiseId pid, final boolean allowCreate)
            throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(pid, "Input PairwiseId object cannot be null");
        Constraint.isNotEmpty(pid.getIssuerEntityID(), "Issuer entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getRecipientEntityID(), "Recipient entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getPrincipalName(), "Principal name cannot be null or empty");
        Constraint.isNotEmpty(pid.getSourceSystemId(), "Source system ID cannot be null or empty");
        
        log.debug("Obtaining pairwise ID for source ID: {}", pid.getSourceSystemId());

        log.trace("Prepared statement: {}", getBySourceSelectSQL);
        log.trace("Setting prepared statement parameter {}: {}", 1, pid.getIssuerEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 2, pid.getRecipientEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 3, pid.getSourceSystemId());
        log.trace("Setting prepared statement parameter {}: {}", 4, pid.getIssuerEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 5, pid.getRecipientEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 6, pid.getSourceSystemId());

        int retries = transactionRetry;
        while (true) {
            try (final Connection dbConn = getConnection(false)) {
                final PreparedStatement statement = dbConn.prepareStatement(getBySourceSelectSQL);
                statement.setQueryTimeout((int) queryTimeout.toSeconds());
                statement.setString(1, pid.getIssuerEntityID());
                statement.setString(2, pid.getRecipientEntityID());
                statement.setString(3, pid.getSourceSystemId());
                statement.setString(4, pid.getIssuerEntityID());
                statement.setString(5, pid.getRecipientEntityID());
                statement.setString(6, pid.getSourceSystemId());
        
                log.debug("Getting active and/or last inactive pairwise ID entry");
                final List<PairwiseId> entries = buildIdentifierEntries(statement.executeQuery());
                if (entries != null && entries.size() > 0 && (entries.get(0).getDeactivationTime() == null
                        || entries.get(0).getDeactivationTime().isAfter(Instant.now()))) {
                    dbConn.commit();
                    log.debug("Returning existing active pairwise ID: {}", entries.get(0).getPairwiseId());
                    return entries.get(0);
                } else if (!allowCreate) {
                    dbConn.commit();
                    log.debug("No existing pairwise ID and creation is not permitted by caller");
                    return null;
                }

                pid.setCreationTime(Instant.now());
                
                // Circumvent final modifier on parameter.
                PairwiseId retValue = pid;

                if ((entries == null || entries.size() == 0) && initialValueStore != null) {
                    log.debug("Issuing new pairwise ID using initial value store");
                    retValue = initialValueStore.getBySourceValue(pid, allowCreate);
                } else {
                    log.debug("Issuing new random pairwise ID");
                    retValue.setPairwiseId(UUID.randomUUID().toString());
                    if (entries != null && entries.size() > 0) {
                        retValue.setPeerProvidedId(entries.get(0).getPeerProvidedId());
                    }
                }
                store(retValue, dbConn);
                dbConn.commit();
                return retValue;
            } catch (final SQLException e) {
                boolean retry = false;
                for (final String msg : retryableErrors) {
                    if (e.getSQLState() != null && e.getSQLState().contains(msg)) {
                        log.warn("Caught retryable SQL exception", e);
                        retry = true;
                        break;
                    }
                }
                
                if (retry) {
                    if (--retries < 0) {
                        log.warn("Error retryable, but retry limit exceeded");
                        throw new IOException(e);
                    }
                    log.info("Retrying pairwise ID lookup/create operation");
                } else {
                    throw new IOException(e);
                }
            }
        }
    }
// Checkstyle: MethodLength|CyclomaticComplexity ON
    
    /** {@inheritDoc} */
    @Nullable public PairwiseId getByIssuedValue(@Nonnull final PairwiseId pid) throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(pid, "Input PairwiseId object cannot be null");
        Constraint.isNotEmpty(pid.getIssuerEntityID(), "Issuer entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getRecipientEntityID(), "Recipient entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getPairwiseId(), "Pairwise ID cannot be null or empty");
    
        log.debug("Selecting previously issued pairwise ID entry", getByIssuedSelectSQL);
    
        log.trace("Prepared statement: {}", getByIssuedSelectSQL);
        log.trace("Setting prepared statement parameter {}: {}", 1, pid.getIssuerEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 2, pid.getRecipientEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 3, pid.getPairwiseId());
    
        try (final Connection dbConn = getConnection(true)) {
            final PreparedStatement statement = dbConn.prepareStatement(getByIssuedSelectSQL);
            statement.setQueryTimeout((int) queryTimeout.toSeconds());
    
            statement.setString(1, pid.getIssuerEntityID());
            statement.setString(2, pid.getRecipientEntityID());
            statement.setString(3, pid.getPairwiseId());
    
            final List<PairwiseId> entries = buildIdentifierEntries(statement.executeQuery());
    
            if (entries == null || entries.size() == 0) {
                return null;
            }
    
            if (entries.size() > 1) {
                log.error("More than one record found for a single persistent ID value");
            }
            
            if (entries.get(0).getDeactivationTime() != null &&
                    !entries.get(0).getDeactivationTime().isAfter(Instant.now())) {
                return null;
            }
    
            return entries.get(0);
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    public void deactivate(@Nonnull final PairwiseId pid) throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(pid, "Input PairwiseId object cannot be null");
        Constraint.isNotEmpty(pid.getIssuerEntityID(), "Issuer entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getRecipientEntityID(), "Recipient entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getPairwiseId(), "Pairwise ID cannot be null or empty");
        
        final Timestamp deactivationTime;
        if (pid.getDeactivationTime() == null) {
            deactivationTime = new Timestamp(System.currentTimeMillis());
        } else {
            deactivationTime = new Timestamp(pid.getDeactivationTime().toEpochMilli());
        }

        log.debug("Deactivating pairwise ID {} as of {}", pid.getPairwiseId(), deactivationTime);

        log.trace("Prepared statement: {}", deactivateSQL);
        log.trace("Setting prepared statement parameter {}: {}", 1, deactivationTime);
        log.trace("Setting prepared statement parameter {}: {}", 2, pid.getIssuerEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 3, pid.getRecipientEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 4, pid.getPairwiseId());
        
        try (final Connection dbConn = getConnection(true)) {
            final PreparedStatement statement = dbConn.prepareStatement(deactivateSQL);
            statement.setQueryTimeout((int) queryTimeout.toSeconds());
            statement.setTimestamp(1, deactivationTime);
            statement.setString(2, pid.getIssuerEntityID());
            statement.setString(3, pid.getRecipientEntityID());
            statement.setString(4, pid.getPairwiseId());
            final int rowCount = statement.executeUpdate();
            if (rowCount != 1) {
                log.warn("Unexpected result, statement affected {} rows", rowCount);
            }
            
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    public void attach(@Nonnull final PairwiseId pid) throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isNotNull(pid, "Input PairwiseId object cannot be null");
        Constraint.isNotEmpty(pid.getIssuerEntityID(), "Issuer entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getRecipientEntityID(), "Recipient entityID cannot be null or empty");
        Constraint.isNotEmpty(pid.getPairwiseId(), "Pairwise ID cannot be null or empty");
        Constraint.isNotEmpty(pid.getPeerProvidedId(), "Peer-provided ID cannot be null or empty");

        log.debug("Attaching peer-provided ID {} to pairwise id {}", pid.getPeerProvidedId(), pid.getPairwiseId());

        log.trace("Prepared statement: {}", attachSQL);
        log.trace("Setting prepared statement parameter {}: {}", 1, pid.getPeerProvidedId());
        log.trace("Setting prepared statement parameter {}: {}", 2, pid.getIssuerEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 3, pid.getRecipientEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 4, pid.getPairwiseId());
        
        try (final Connection dbConn = getConnection(true)) {
            final PreparedStatement statement = dbConn.prepareStatement(attachSQL);
            statement.setQueryTimeout((int) queryTimeout.toSeconds());
            statement.setString(1, pid.getPeerProvidedId());
            statement.setString(2, pid.getIssuerEntityID());
            statement.setString(3, pid.getRecipientEntityID());
            statement.setString(4, pid.getPairwiseId());
            final int rowCount = statement.executeUpdate();
            if (rowCount != 1) {
                log.warn("Unexpected result, statement affected {} rows", rowCount);
            }
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }
    
// Checkstyle: MethodLength|CyclomaticComplexity ON
    
    /**
     * Store a record containing the values from the input object.
     * 
     * @param entry new object to store
     * @param dbConn connection to obtain a statement from.
     * 
     * @throws SQLException if an error occurs
     */
    void store(@Nonnull final PairwiseId entry, @Nonnull final Connection dbConn) throws SQLException {
        
        log.debug("Storing new pairwise ID entry");
        
        if (StringSupport.trimOrNull(entry.getIssuerEntityID()) == null
                || StringSupport.trimOrNull(entry.getRecipientEntityID()) == null
                || StringSupport.trimOrNull(entry.getPairwiseId()) == null
                || StringSupport.trimOrNull(entry.getPrincipalName()) == null
                || StringSupport.trimOrNull(entry.getSourceSystemId()) == null
                || entry.getCreationTime() == null) {
            throw new SQLException("Required field was empty/null, store operation not possible");
        }
        
        log.trace("Prepared statement: {}", insertSQL);
        log.trace("Setting prepared statement parameter {}: {}", 1, entry.getIssuerEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 2, entry.getRecipientEntityID());
        log.trace("Setting prepared statement parameter {}: {}", 3, entry.getPairwiseId());
        log.trace("Setting prepared statement parameter {}: {}", 4, entry.getPrincipalName());
        log.trace("Setting prepared statement parameter {}: {}", 5, entry.getSourceSystemId());
        log.trace("Setting prepared statement parameter {}: {}", 6, entry.getPeerProvidedId());
        log.trace("Setting prepared statement parameter {}: {}", 7, entry.getCreationTime());
        log.trace("Setting prepared statement parameter {}: {}", 8, entry.getDeactivationTime());
        
        final PreparedStatement statement = dbConn.prepareStatement(insertSQL);
        statement.setQueryTimeout((int) queryTimeout.toSeconds());
    
        statement.setString(1, entry.getIssuerEntityID());
        statement.setString(2, entry.getRecipientEntityID());
        statement.setString(3, entry.getPairwiseId());
        statement.setString(4, entry.getPrincipalName());
        statement.setString(5, entry.getSourceSystemId());
        if (entry.getPeerProvidedId() != null) {
            statement.setString(6, entry.getPeerProvidedId());
        } else {
            statement.setNull(6, Types.VARCHAR);
        }
        statement.setTimestamp(7, new Timestamp(entry.getCreationTime().toEpochMilli()));
        if (entry.getDeactivationTime() != null) {
            statement.setTimestamp(8, new Timestamp(entry.getDeactivationTime().toEpochMilli()));
        } else {
            statement.setNull(8, Types.TIMESTAMP);
        }
    
        statement.executeUpdate();
    }

    /**
     * Obtain a connection from the data source.
     * 
     * <p>The caller must close the connection.</p>
     * 
     * @param autoCommit auto-commit setting to apply to the connection
     * 
     * @return a fresh connection
     * @throws SQLException if an error occurs
     */
    @Nonnull private Connection getConnection(final boolean autoCommit) throws SQLException {
        final Connection conn = dataSource.getConnection();
        conn.setAutoCommit(autoCommit);
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        return conn;
    }
    
    /**
     * Check the database and the presence of a uniqueness constraint.
     * 
     * @throws SQLException if the database cannot be verified 
     */
    private void verifyDatabase() throws SQLException {
        
        final String uuid = UUID.randomUUID().toString();
        
        final PairwiseId newEntry = new PairwiseId();
        newEntry.setIssuerEntityID("http://dummy.com/idp/" + uuid);
        newEntry.setRecipientEntityID("http://dummy.com/sp/" + uuid);
        newEntry.setSourceSystemId("dummy");
        newEntry.setPrincipalName("dummy");
        newEntry.setCreationTime(Instant.now());
        newEntry.setPairwiseId(uuid);
        
        try (final Connection conn = getConnection(true)) {
            store(newEntry, conn);
        }

        boolean keyMissing = false;
        try (final Connection conn = getConnection(true)) {
            store(newEntry, conn);
            keyMissing = true;
        } catch (final SQLException e) {
            if (e.getSQLState() != null && !retryableErrors.contains(e.getSQLState())) {
                log.warn("Duplicate insert failed as required with SQL State '{}', ensure this value is "
                        + "configured as a retryable error", e.getSQLState());
            }
        }

        try (final Connection conn = getConnection(true)) {
            final PreparedStatement statement = conn.prepareStatement(deleteSQL);
            statement.setQueryTimeout((int) queryTimeout.toSeconds());
            statement.setString(1, "http://dummy.com/idp/" + uuid);
            statement.executeUpdate();
        }
        
        if (keyMissing) {
            throw new SQLException("Duplicate insertion succeeded, primary key missing from table");
        }
    }
    
    /**
     * Build a list of {@link PairwiseId} objects from a result set.
     * 
     * @param resultSet the result set
     * 
     * @return list of {@link PairwiseId} objects
     * 
     * @throws SQLException thrown if there is a problem reading the information from the database
     */
    @Nonnull @NonnullElements @Live private List<PairwiseId> buildIdentifierEntries(
            @Nonnull final ResultSet resultSet) throws SQLException {
        
        final ArrayList<PairwiseId> entries = new ArrayList<>();
    
        while (resultSet.next()) {
            final PairwiseId entry = new PairwiseId();
            entry.setIssuerEntityID(resultSet.getString(issuerColumn));
            entry.setRecipientEntityID(resultSet.getString(recipientColumn));
            entry.setPrincipalName(resultSet.getString(principalNameColumn));
            entry.setPairwiseId(resultSet.getString(persistentIdColumn));
            entry.setSourceSystemId(resultSet.getString(sourceIdColumn));
            entry.setPeerProvidedId(resultSet.getString(peerProvidedIdColumn));
            Timestamp ts = resultSet.getTimestamp(creationTimeColumn);
            if (ts != null) {
                entry.setCreationTime(Instant.ofEpochMilli(ts.getTime()));
            }
            ts = resultSet.getTimestamp(deactivationTimeColumn);
            if (ts != null) {
                entry.setDeactivationTime(Instant.ofEpochMilli(ts.getTime()));
            }
            entries.add(entry);
    
            log.trace("Entry {} added to results", entry.toString());
        }
    
        return entries;
    }
    
}
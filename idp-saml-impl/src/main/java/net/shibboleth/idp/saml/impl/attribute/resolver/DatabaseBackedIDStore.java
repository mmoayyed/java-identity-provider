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

package net.shibboleth.idp.saml.impl.attribute.resolver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import net.shibboleth.idp.saml.attribute.resolver.PersistentIdEntry;
import net.shibboleth.idp.saml.attribute.resolver.StoredIDException;
import net.shibboleth.idp.saml.attribute.resolver.StoredIDStore;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents as persistent, database-backed, store of identifiers.
 * 
 * The DDL for the database is
 * 
 * <tt>CREATE TABLE shibpid {localEntity VARCHAR NOT NULL, peerEntity VARCHAR NOT NULL, principalName \\
 *     VARCHAR NOT NULL, localId VARCHAR NOT NULL, persistentId VARCHAR NOT NULL, peerProvidedId \\
 *     VARCHAR, creationDate TIMESTAMP NOT NULL, deactivationDate TIMESTAMP}</tt> .
 */
public class DatabaseBackedIDStore extends AbstractInitializableComponent implements StoredIDStore {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(DatabaseBackedIDStore.class);

    /** JDBC data source for retrieving connections. */
    private DataSource dataSource;

    /** Timeout of SQL queries in seconds. */
    private int queryTimeout;

    /** Name of the database table. */
    private final String table = "shibpid";

    /** Name of the local entity ID column. */
    private final String localEntityColumn = "localEntity";

    /** Name of the peer entity ID name column. */
    private final String peerEntityColumn = "peerEntity";

    /** Name of the principal name column. */
    private final String principalNameColumn = "principalName";

    /** Name of the local ID column. */
    private final String localIdColumn = "localId";

    /** Name of the persistent ID column. */
    private final String persistentIdColumn = "persistentId";

    /** ID, provided by peer, associated with the persistent ID. */
    private final String peerProvidedIdColumn = "peerProvidedId";

    /** Name of the creation time column. */
    private final String createTimeColumn = "creationDate";

    /** Name of the deactivation time column. */
    private final String deactivationTimeColumn = "deactivationDate";

    /** Partial select query for ID entries. */
    private final String idEntrySelectSQL = "SELECT * FROM " + table + " WHERE ";

    /** SQL used to deactivate an ID. */
    private final String deactivateIdSQL = "UPDATE " + table + " SET " + deactivationTimeColumn + "= ? WHERE "
            + persistentIdColumn + "= ?";

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
    public void setDataSource(@Nullable DataSource source) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        dataSource = source;
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
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == dataSource) {
            throw new ComponentInitializationException(getLogPrefix() + " No database connection provided");
        }
    }

    /** {@inheritDoc} */
    @Override public int getNumberOfPersistentIdEntries(@Nonnull @NotEmpty String localEntity,
            @Nonnull @NotEmpty String peerEntity, @Nonnull @NotEmpty String localId) throws StoredIDException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT");
        sqlBuilder.append(" count(").append(persistentIdColumn).append(")");
        sqlBuilder.append(" FROM ").append(table).append(" WHERE ");
        sqlBuilder.append(localEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ");
        sqlBuilder.append(peerEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ");
        sqlBuilder.append(localIdColumn).append(" = ?");

        final String sql = sqlBuilder.toString();
        Connection dbConn = null;

        try {
            dbConn = dataSource.getConnection();
            log.debug("{} Selecting number of persistent ID entries based on prepared sql statement: {}",
                    getLogPrefix(), sql);
            PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout(queryTimeout);

            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, localEntity);
            statement.setString(1, localEntity);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 2, peerEntity);
            statement.setString(2, peerEntity);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 3, localId);
            statement.setString(3, localId);

            ResultSet rs = statement.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e1) {
            throw new StoredIDException(e1);
        } finally {
            try {
                if (dbConn != null && !dbConn.isClosed()) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("Error closing database connection", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull public List<PersistentIdEntry> getPersistentIdEntries(@Nonnull @NotEmpty String localEntity,
            @Nonnull @NotEmpty String peerEntity, @Nonnull @NotEmpty String localId) throws StoredIDException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        StringBuilder sqlBuilder = new StringBuilder(idEntrySelectSQL);
        sqlBuilder.append(localEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(peerEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(localIdColumn).append(" = ?");
        String sql = sqlBuilder.toString();

        log.debug("{} Selecting all persistent ID entries based on prepared sql statement: {}", getLogPrefix(), sql);

        Connection dbConn = null;
        try {
            dbConn = dataSource.getConnection();
            PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout(queryTimeout);

            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, localEntity);
            statement.setString(1, localEntity);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 2, peerEntity);
            statement.setString(2, peerEntity);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 3, localId);
            statement.setString(3, localId);

            return buildIdentifierEntries(statement.executeQuery());
        } catch (SQLException e1) {
            throw new StoredIDException(e1);
        } finally {
            try {
                if (dbConn != null && !dbConn.isClosed()) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("{} Error closing database connection {}", getLogPrefix(), e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable public PersistentIdEntry getActivePersistentIdEntry(String persistentId)
            throws StoredIDException {
        return getPersistentIdEntry(persistentId, true);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isPersistentIdAvailable(@Nonnull @NotEmpty final String persistentId) throws StoredIDException {
        return getPersistentIdEntry(persistentId, false) == null;
    }

    /**
     * Gets the persistent ID entry for the given ID.
     * 
     * @param persistentId the persistent ID
     * @param onlyActiveId true if only an active ID should be returned, false if a deactivated ID may be returned
     * 
     * @return the ID entry for the given ID or null if none exists
     * 
     * @throws StoredIDException thrown if there is a problem communication with the database
     */
    @Nullable protected PersistentIdEntry getPersistentIdEntry(@Nonnull @NotEmpty String persistentId, 
            boolean onlyActiveId) throws StoredIDException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        StringBuilder sqlBuilder = new StringBuilder(idEntrySelectSQL);
        sqlBuilder.append(persistentIdColumn).append(" = ?");
        if (onlyActiveId) {
            sqlBuilder.append(" AND ").append(deactivationTimeColumn).append(" IS NULL");
        }
        String sql = sqlBuilder.toString();

        log.debug("{} Selecting persistent ID entry based on prepared sql statement: {}", getLogPrefix(), sql);

        Connection dbConn = null;
        try {
            dbConn = dataSource.getConnection();
            PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout(queryTimeout);

            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, persistentId);
            statement.setString(1, persistentId);

            List<PersistentIdEntry> entries = buildIdentifierEntries(statement.executeQuery());

            if (entries == null || entries.size() == 0) {
                return null;
            }

            if (entries.size() > 1) {
                log.warn("{} More than one identifier found, only the first will be used", getLogPrefix());
            }

            return entries.get(0);
        } catch (SQLException e1) {
            throw new StoredIDException(e1);
        } finally {
            try {
                if (dbConn != null && !dbConn.isClosed()) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("{} Error closing database connection {}", getLogPrefix(), e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull public PersistentIdEntry getActivePersistentIdEntry(@Nonnull @NotEmpty String localEntity,
            @Nonnull @NotEmpty String peerEntity, @Nonnull @NotEmpty String localId) throws StoredIDException {
        StringBuilder sqlBuilder = new StringBuilder(idEntrySelectSQL);
        sqlBuilder.append(localEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(peerEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(localIdColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(deactivationTimeColumn).append(" IS NULL");
        String sql = sqlBuilder.toString();

        log.debug("{} Selecting active persistent ID entry based on prepared sql statement: {}", getLogPrefix(), sql);
        Connection dbConn = null;
        try {
            dbConn = dataSource.getConnection();
            PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout(queryTimeout);

            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, localEntity);
            statement.setString(1, localEntity);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 2, peerEntity);
            statement.setString(2, peerEntity);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 3, localId);
            statement.setString(3, localId);

            log.debug("{} Getting active persistent Id entries.", getLogPrefix());
            List<PersistentIdEntry> entries = buildIdentifierEntries(statement.executeQuery());

            if (entries == null || entries.size() == 0) {
                return null;
            }

            if (entries.size() > 1) {
                log.warn("{} More than one active identifier, only the first will be used", getLogPrefix());
            }

            return entries.get(0);
        } catch (SQLException e1) {
            throw new StoredIDException(e1);
        } finally {
            try {
                if (dbConn != null && !dbConn.isClosed()) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("{} Error closing database connection: {}", getLogPrefix(), e);
            }
        }
    }

    /**
     * Check that the entry meets the constraints imposed by the SQL DDL.
     * 
     * localEntity VARCHAR NOT NULL, <br/>
     * peerEntity VARCHAR NOT NULL, <br/>
     * principalName VARCHAR NOT NULL, <br/>
     * localId VARCHAR NOT NULL, <br/>
     * persistentId VARCHAR NOT NULL, <br/>
     * 
     * @param entry what to look at
     * @throws SQLException if we go against the constraint.
     */
    protected void validatePersistentIdEntry(@Nonnull PersistentIdEntry entry) throws SQLException {
        boolean doThrow = false;

        if (null == entry.getAttributeIssuerId()) {
            log.warn("{} Entry {} has null attribute Issuer Id", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getAttributeConsumerId()) {
            log.warn("{} Entry {} has null attribute consumer Id", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getPrincipalName()) {
            log.warn("{} Entry {} has null principalName", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getLocalId()) {
            log.warn("{} Entry {} has null localId", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getPersistentId()) {
            log.warn("{} Entry {} has null persistentId", getLogPrefix(), entry);
            doThrow = true;
        }

        if (doThrow) {
            throw new SQLException();
        }
    }

    /**
     * Code to build the INSERT SQL required by storePersistentIdEntry.
     * 
     * @return the SQL statement
     */
    @Nonnull private String getInsertSql() {
        final StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
        sqlBuilder.append(table).append(" (");
        sqlBuilder.append(localEntityColumn).append(", ");
        sqlBuilder.append(peerEntityColumn).append(", ");
        sqlBuilder.append(principalNameColumn).append(", ");
        sqlBuilder.append(localIdColumn).append(", ");
        sqlBuilder.append(persistentIdColumn).append(", ");
        sqlBuilder.append(peerProvidedIdColumn).append(", ");
        sqlBuilder.append(createTimeColumn);
        sqlBuilder.append(") VALUES (?, ?, ?, ?, ?, ?, ?)");

        return sqlBuilder.toString();
    }

    /** {@inheritDoc} */
    @Override public void storePersistentIdEntry(@Nonnull PersistentIdEntry entry) throws StoredIDException {

        try {
            validatePersistentIdEntry(entry);
        } catch (SQLException e2) {
           throw new StoredIDException(e2);
        }

        final String sql = getInsertSql();

        Connection dbConn = null;
        try {
            dbConn = dataSource.getConnection();
            log.debug("{} Storing persistent ID entry based on prepared sql statement: {}", getLogPrefix(), sql);
            PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout(queryTimeout);

            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, 
                        entry.getAttributeIssuerId());
            statement.setString(1, entry.getAttributeIssuerId());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 2,
                    entry.getAttributeConsumerId());
            statement.setString(2, entry.getAttributeConsumerId());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 3, entry.getPrincipalName());
            statement.setString(3, entry.getPrincipalName());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 4, entry.getLocalId());
            statement.setString(4, entry.getLocalId());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 5, entry.getPersistentId());
            statement.setString(5, entry.getPersistentId());

            if (entry.getPeerProvidedId() == null) {
                log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 6, Types.VARCHAR);
                statement.setNull(6, Types.VARCHAR);
            } else {
                log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 6,
                        entry.getPeerProvidedId());
                statement.setString(6, entry.getPeerProvidedId());
            }
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 7, timestamp.toString());
            statement.setTimestamp(7, timestamp);

            log.debug(statement.toString());

            statement.executeUpdate();
        } catch (SQLException e1) {
            throw new StoredIDException(e1);
        } finally {
            try {
                if (dbConn != null && !dbConn.isClosed()) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("{} Error closing database connection: {}", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void deactivatePersistentId(@NotEmpty String persistentId, @Nullable DateTime deactivation)
            throws StoredIDException {
        final Timestamp deactivationTime;
        if (deactivation == null) {
            deactivationTime = new Timestamp(System.currentTimeMillis());
        } else {
            deactivationTime = new Timestamp(deactivation.getMillis());
        }

        Connection dbConn = null;
        try {
            dbConn = dataSource.getConnection();
            log.debug("Deactivating persistent id {} as of {}", persistentId, deactivationTime.toString());
            PreparedStatement statement = dbConn.prepareStatement(deactivateIdSQL);
            statement.setQueryTimeout(queryTimeout);
            statement.setTimestamp(1, deactivationTime);
            statement.setString(2, persistentId);
            statement.executeUpdate();
        } catch (SQLException e1) {
            throw new StoredIDException(e1);
        } finally {
            try {
                if (dbConn != null && !dbConn.isClosed()) {
                    dbConn.close();
                }
            } catch (SQLException e) {
                log.error("{} Error closing database connection: {}", getLogPrefix(), e);
            }
        }
    }

    /**
     * Builds a list of {@link PersistentIdEntry}s from a result set.
     * 
     * @param resultSet the result set
     * 
     * @return list of {@link PersistentIdEntry}s
     * 
     * @throws SQLException thrown if there is a problem reading the information from the database
     */
    @Nonnull protected List<PersistentIdEntry> buildIdentifierEntries(ResultSet resultSet) throws SQLException {
        ArrayList<PersistentIdEntry> entries = new ArrayList<PersistentIdEntry>();

        PersistentIdEntry entry;
        while (resultSet.next()) {
            entry = new PersistentIdEntry();
            entry.setAttributeIssuerId(resultSet.getString(localEntityColumn));
            entry.setPeerEntityId(resultSet.getString(peerEntityColumn));
            entry.setPrincipalName(resultSet.getString(principalNameColumn));
            entry.setPersistentId(resultSet.getString(persistentIdColumn));
            entry.setLocalId(resultSet.getString(localIdColumn));
            entry.setPeerProvidedId(resultSet.getString(peerProvidedIdColumn));
            entry.setCreationTime(resultSet.getTimestamp(createTimeColumn));
            entry.setDeactivationTime(resultSet.getTimestamp(deactivationTimeColumn));
            entries.add(entry);

            log.trace("{} Entry {} added to results", getLogPrefix(), entry.toString());
        }

        return entries;
    }

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Stored Id Store:"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        return "Stored Id Store:";
    }
}
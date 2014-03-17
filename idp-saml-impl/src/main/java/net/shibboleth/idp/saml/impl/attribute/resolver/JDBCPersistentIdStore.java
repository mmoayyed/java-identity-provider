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

import java.io.IOException;
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

import net.shibboleth.idp.saml.nameid.PersistentIdEntry;
import net.shibboleth.idp.saml.nameid.PersistentIdStore;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC-based storage management for SAML persistent IDs.
 * 
 * The DDL for the database is
 * 
 * <tt>CREATE TABLE shibpid {localEntity VARCHAR NOT NULL, peerEntity VARCHAR NOT NULL, principalName \\
 *     VARCHAR NOT NULL, localId VARCHAR NOT NULL, persistentId VARCHAR NOT NULL, peerProvidedId \\
 *     VARCHAR, creationDate TIMESTAMP NOT NULL, deactivationDate TIMESTAMP}</tt> .
 */
public class JDBCPersistentIdStore extends AbstractInitializableComponent implements PersistentIdStore {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(JDBCPersistentIdStore.class);

    /** JDBC data source for retrieving connections. */
    @NonnullAfterInit private DataSource dataSource;

    /** Timeout of SQL queries in milliseconds. */
    @Duration @NonNegative private long queryTimeout;

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
    public void setDataSource(@Nonnull final DataSource source) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        dataSource = Constraint.isNotNull(source, "DataSource cannot be null");
    }

    /**
     * Get the SQL query timeout.
     * 
     * @return the timeout in milliseconds
     */
    @NonNegative public long getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * Set the SQL query timeout.
     * 
     * @param timeout the timeout to set in milliseconds
     */
    public void setQueryTimeout(@Duration @NonNegative final long timeout) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        queryTimeout = Constraint.isGreaterThanOrEqual(0, timeout, "Timeout must be greater than or equal to 0");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (null == dataSource) {
            throw new ComponentInitializationException(getLogPrefix() + " No database connection provided");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable(@Nonnull @NotEmpty final String persistentId) throws IOException {
        return getPersistentIdEntry(persistentId,false) == null;
    }

    /** {@inheritDoc} */
    @Override public void store(@Nonnull PersistentIdEntry entry) throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
    
        try {
            validatePersistentIdEntry(entry);
        } catch (SQLException e2) {
           throw new IOException(e2);
        }
    
        final String sql = getInsertSql();
    
        try (final Connection dbConn = dataSource.getConnection()) {
            log.debug("{} Storing persistent ID entry based on prepared sql statement: {}", getLogPrefix(), sql);
            final PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout((int) (queryTimeout / 1000));
    
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, 
                        entry.getIssuerEntityId());
            statement.setString(1, entry.getIssuerEntityId());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 2,
                    entry.getRecipientEntityId());
            statement.setString(2, entry.getRecipientEntityId());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 3, entry.getPrincipalName());
            statement.setString(3, entry.getPrincipalName());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 4, entry.getSourceId());
            statement.setString(4, entry.getSourceId());
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
            final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 7, timestamp.toString());
            statement.setTimestamp(7, timestamp);
    
            log.debug(statement.toString());
    
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getCount(@Nonnull @NotEmpty final String issuer, @Nonnull @NotEmpty final String recipient,
            @Nonnull @NotEmpty final String sourceId) throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT");
        sqlBuilder.append(" count(").append(persistentIdColumn).append(")");
        sqlBuilder.append(" FROM ").append(table).append(" WHERE ");
        sqlBuilder.append(localEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ");
        sqlBuilder.append(peerEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ");
        sqlBuilder.append(localIdColumn).append(" = ?");

        final String sql = sqlBuilder.toString();
        try (final Connection dbConn = dataSource.getConnection()) {
            log.debug("{} Selecting number of persistent ID entries based on prepared sql statement: {}",
                    getLogPrefix(), sql);
            final PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout((int) (queryTimeout / 1000));

            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, issuer);
            statement.setString(1, issuer);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 2, recipient);
            statement.setString(2, recipient);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 3, sourceId);
            statement.setString(3, sourceId);

            final ResultSet rs = statement.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public PersistentIdEntry getActiveEntry(@Nonnull @NotEmpty final String persistentId) throws IOException {
        return getPersistentIdEntry(persistentId, true);
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable public PersistentIdEntry getActiveEntry(@Nonnull @NotEmpty final String issuer,
            @Nonnull @NotEmpty final String recipient, @Nonnull @NotEmpty final String sourceId) throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final StringBuilder sqlBuilder = new StringBuilder(idEntrySelectSQL);
        sqlBuilder.append(localEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(peerEntityColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(localIdColumn).append(" = ?");
        sqlBuilder.append(" AND ").append(deactivationTimeColumn).append(" IS NULL");
        final String sql = sqlBuilder.toString();
    
        log.debug("{} Selecting active persistent ID entry based on prepared sql statement: {}", getLogPrefix(), sql);
        
        try (final Connection dbConn = dataSource.getConnection()) {
            final PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout((int) (queryTimeout / 1000));
    
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, issuer);
            statement.setString(1, issuer);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 2, recipient);
            statement.setString(2, recipient);
            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 3, sourceId);
            statement.setString(3, sourceId);
    
            log.debug("{} Getting active persistent Id entries.", getLogPrefix());
            final List<PersistentIdEntry> entries = buildIdentifierEntries(statement.executeQuery());
    
            if (entries == null || entries.size() == 0) {
                return null;
            }
    
            if (entries.size() > 1) {
                log.warn("{} More than one active identifier, only the first will be used", getLogPrefix());
            }
    
            return entries.get(0);
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void deactivate(@NotEmpty String persistentId, @Nullable DateTime deactivation)
            throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final Timestamp deactivationTime;
        if (deactivation == null) {
            deactivationTime = new Timestamp(System.currentTimeMillis());
        } else {
            deactivationTime = new Timestamp(deactivation.getMillis());
        }
    
        try (final Connection dbConn = dataSource.getConnection()) {
            log.debug("Deactivating persistent id {} as of {}", persistentId, deactivationTime.toString());
            final PreparedStatement statement = dbConn.prepareStatement(deactivateIdSQL);
            statement.setQueryTimeout((int) (queryTimeout / 1000));
            statement.setTimestamp(1, deactivationTime);
            statement.setString(2, persistentId);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the entry for the given ID.
     * 
     * @param persistentId the persistent ID
     * @param onlyActiveId true if only an active ID should be returned, false if a deactivated ID may be returned
     * 
     * @return the ID entry for the given ID or null if none exists
     * 
     * @throws IOException thrown if there is a problem communication with the database
     */
    @Nullable private PersistentIdEntry getPersistentIdEntry(@Nonnull @NotEmpty final String persistentId, 
            boolean onlyActiveId) throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final StringBuilder sqlBuilder = new StringBuilder(idEntrySelectSQL);
        sqlBuilder.append(persistentIdColumn).append(" = ?");
        if (onlyActiveId) {
            sqlBuilder.append(" AND ").append(deactivationTimeColumn).append(" IS NULL");
        }
        final String sql = sqlBuilder.toString();

        log.debug("{} Selecting persistent ID entry based on prepared sql statement: {}", getLogPrefix(), sql);

        try (final Connection dbConn = dataSource.getConnection()) {
            final PreparedStatement statement = dbConn.prepareStatement(sql);
            statement.setQueryTimeout((int) (queryTimeout / 1000));

            log.debug("{} Setting prepared statement parameter {}: {}", getLogPrefix(), 1, persistentId);
            statement.setString(1, persistentId);

            final List<PersistentIdEntry> entries = buildIdentifierEntries(statement.executeQuery());

            if (entries == null || entries.size() == 0) {
                return null;
            }

            if (entries.size() > 1) {
                log.warn("{} More than one identifier found, only the first will be used", getLogPrefix());
            }

            return entries.get(0);
        } catch (final SQLException e) {
            throw new IOException(e);
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
     * 
     * @throws SQLException if we go against the constraint.
     */
    protected void validatePersistentIdEntry(@Nonnull PersistentIdEntry entry) throws SQLException {
        boolean doThrow = false;

        if (null == entry.getIssuerEntityId()) {
            log.warn("{} Entry {} has null issuer id", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getRecipientEntityId()) {
            log.warn("{} Entry {} has null recipient id", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getPrincipalName()) {
            log.warn("{} Entry {} has null principal name", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getSourceId()) {
            log.warn("{} Entry {} has null source id", getLogPrefix(), entry);
            doThrow = true;
        }

        if (null == entry.getPersistentId()) {
            log.warn("{} Entry {} has null persistent id", getLogPrefix(), entry);
            doThrow = true;
        }

        if (doThrow) {
            throw new SQLException("Entry is not consistent with database constraints");
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

    /**
     * Builds a list of {@link PersistentIdEntry}s from a result set.
     * 
     * @param resultSet the result set
     * 
     * @return list of {@link PersistentIdEntry}s
     * 
     * @throws SQLException thrown if there is a problem reading the information from the database
     */
    @Nonnull private List<PersistentIdEntry> buildIdentifierEntries(ResultSet resultSet) throws SQLException {
        final ArrayList<PersistentIdEntry> entries = new ArrayList<>();

        while (resultSet.next()) {
            PersistentIdEntry entry = new PersistentIdEntry();
            entry.setIssuerEntityId(resultSet.getString(localEntityColumn));
            entry.setRecipientEntityId(resultSet.getString(peerEntityColumn));
            entry.setPrincipalName(resultSet.getString(principalNameColumn));
            entry.setPersistentId(resultSet.getString(persistentIdColumn));
            entry.setSourceId(resultSet.getString(localIdColumn));
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
    @Nonnull @NotEmpty private String getLogPrefix() {
        return "Stored Id Store:";
    }
    
}
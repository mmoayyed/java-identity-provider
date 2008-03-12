/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents as persistent, database-backed, store of identifiers.
 * 
 * The DDL for the database is
 * <tt>CREATE TABLE shibpid {localEntity VARCHAR NOT NULL, peerEntity VARCHAR NOT NULL, principalName VARCHAR NOT NULL, localId VARCHAR NOT NULL, persistentId VARCHAR NOT NULL, peerProvidedId VARCHAR, creationDate TIMESTAMP NOT NULL, deactivationDate TIMESTAMP}</tt>.
 */
public class StoredIDStore {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StoredIDStore.class);

    /** JDBC data source for retrieving connections. */
    private DataSource dataSource;

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
    private final String deactivateIdSQL = "UPDATE " + table + " SET " + deactivationTimeColumn + "='%s' WHERE "
            + persistentIdColumn + "='%s'";

    /**
     * Constructor.
     * 
     * @param source datasouce used to communicate with the database
     */
    public StoredIDStore(DataSource source) {
        dataSource = source;
    }

    /**
     * Gets all the number persistent ID entries for a (principal, peer, local) tuple.
     * 
     * @param localId local ID part of the persistent ID
     * @param peerEntity entity ID of the peer the ID is for
     * @param localEntity entity ID of the ID issuer
     * 
     * @return the active identifier
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public int getNumberOfPersistentIdEntries(String localEntity, String peerEntity, String localId)
            throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT");
        sqlBuilder.append(" count(").append(persistentIdColumn).append(")");
        sqlBuilder.append(" FROM ").append(table).append(" WHERE ");
        sqlBuilder.append(localEntityColumn).append(" = '").append(localEntity).append("'");
        sqlBuilder.append(" AND ");
        sqlBuilder.append(peerEntityColumn).append(" = '").append(peerEntity).append("'");
        sqlBuilder.append(" AND ");
        sqlBuilder.append(localIdColumn).append(" = '").append(localId).append("'");

        String sql = sqlBuilder.toString();
        Connection dbConn = null;
        try {
            log.debug("Selecting number of persistent ID entry based on SQL query: {}", sql);
            dbConn = dataSource.getConnection();
            Statement stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            rs.next();
            return rs.getInt(1);
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

    /**
     * Gets all the persistent ID entries for a (principal, peer, local) tuple.
     * 
     * @param localId local ID part of the persistent ID
     * @param peerEntity entity ID of the peer the ID is for
     * @param localEntity entity ID of the ID issuer
     * 
     * @return the active identifier
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public List<PersistentIdEntry> getPersistentIdEntries(String localEntity, String peerEntity, String localId)
            throws SQLException {
        StringBuilder whereClauseBuilder = new StringBuilder();
        whereClauseBuilder.append(localEntityColumn).append(" = '").append(localEntity).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(peerEntityColumn).append(" = '").append(peerEntity).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(localIdColumn).append(" = '").append(localId).append("'");

        return getIdentifierEntries(whereClauseBuilder.toString());
    }

    /**
     * Gets the persistent ID entry for the given ID.
     * 
     * @param persistentId the persistent ID
     * 
     * @return the ID entry for the given ID
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public PersistentIdEntry getActivePersistentIdEntry(String persistentId) throws SQLException {
        StringBuilder whereClauseBuilder = new StringBuilder();
        whereClauseBuilder.append(persistentIdColumn).append(" = '").append(persistentId).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(deactivationTimeColumn).append(" IS NULL");

        List<PersistentIdEntry> entries = getIdentifierEntries(whereClauseBuilder.toString());

        if (entries == null || entries.size() == 0) {
            return null;
        }

        if (entries.size() > 1) {
            log.warn("More than one active identifier, only the first will be used");
        }

        return entries.get(0);
    }

    /**
     * Gets the currently active identifier entry for a (principal, peer, local) tuple.
     * 
     * @param localId local ID part of the persistent ID
     * @param peerEntity entity ID of the peer the ID is for
     * @param localEntity entity ID of the ID issuer
     * 
     * @return the active identifier
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public PersistentIdEntry getActivePersistentIdEntry(String localEntity, String peerEntity, String localId)
            throws SQLException {
        StringBuilder whereClauseBuilder = new StringBuilder();
        whereClauseBuilder.append(localEntityColumn).append(" = '").append(localEntity).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(peerEntityColumn).append(" = '").append(peerEntity).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(localIdColumn).append(" = '").append(localId).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(deactivationTimeColumn).append(" IS NULL");

        List<PersistentIdEntry> entries = getIdentifierEntries(whereClauseBuilder.toString());

        if (entries == null || entries.size() == 0) {
            return null;
        }

        if (entries.size() > 1) {
            log.warn("More than one active identifier, only the first will be used");
        }

        return entries.get(0);
    }

    /**
     * Gets the list of deactivated IDs for a given (principal, peer, local) tuple.
     * 
     * @param localId local component of the Id
     * @param peerEntity entity ID of the peer the ID is for
     * @param localEntity entity ID of the ID issuer
     * 
     * @return list of deactivated identifiers
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public List<PersistentIdEntry> getDeactivatedPersistentIdEntries(String localEntity, String peerEntity,
            String localId) throws SQLException {
        StringBuilder whereClauseBuilder = new StringBuilder();
        whereClauseBuilder.append(localEntityColumn).append(" = '").append(localEntity).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(peerEntityColumn).append(" = '").append(peerEntity).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(localIdColumn).append(" = '").append(localId).append("'");
        whereClauseBuilder.append(" AND ");
        whereClauseBuilder.append(deactivationTimeColumn).append(" IS NOT NULL");

        List<PersistentIdEntry> entries = getIdentifierEntries(whereClauseBuilder.toString());

        if (entries == null || entries.size() == 0) {
            return null;
        }

        return entries;
    }

    /**
     * Stores a persistent ID entry into the database.
     * 
     * @param entry entry to persist
     * 
     * @throws SQLException thrown is there is a problem writing to the database
     */
    public void storePersistentIdEntry(PersistentIdEntry entry) throws SQLException {

        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
        sqlBuilder.append(table);

        sqlBuilder.append(" (");
        sqlBuilder.append(localEntityColumn).append(", ");
        sqlBuilder.append(peerEntityColumn).append(", ");
        sqlBuilder.append(principalNameColumn).append(", ");
        sqlBuilder.append(localIdColumn).append(", ");
        sqlBuilder.append(persistentIdColumn).append(", ");
        sqlBuilder.append(peerProvidedIdColumn).append(", ");
        sqlBuilder.append(createTimeColumn);
        sqlBuilder.append(") VALUES (?, ?, ?, ?, ?, ?, ?);");

        String sql = sqlBuilder.toString();

        Connection dbConn = null;
        try {
            dbConn = dataSource.getConnection();
            PreparedStatement statement = dbConn.prepareStatement(sql);

            statement.setString(1, entry.getLocalEntityId());
            statement.setString(2, entry.getPeerEntityId());
            statement.setString(3, entry.getPrincipalName());
            statement.setString(4, entry.getLocalId());
            statement.setString(5, entry.getPersistentId());
            if (entry.getPeerProvidedId() == null) {
                statement.setNull(6, Types.NULL);
            } else {
                statement.setString(6, entry.getPeerProvidedId());
            }
            statement.setTimestamp(7, new Timestamp(System.currentTimeMillis()));

            log.debug("Inserting newly created persistent id {} for principal {}", entry.getPersistentId(), entry
                    .getPrincipalName());

            log.trace("Executing SQL statement {}", sql);
            statement.executeUpdate();
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

    /**
     * Deactivates a given persistent ID.
     * 
     * @param persistentId ID to deactivate
     * @param deactivation deactivation time, if null the current time is used
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public void deactivatePersistentId(String persistentId, Timestamp deactivation) throws SQLException {
        String deactivationTime;
        if (deactivation == null) {
            deactivationTime = new Timestamp(System.currentTimeMillis()).toString();
        } else {
            deactivationTime = deactivation.toString();
        }

        Connection dbConn = null;
        try {
            log.debug("Deactivating persistent id {} as of {}", persistentId, deactivationTime);
            dbConn = dataSource.getConnection();
            Statement stmt = dbConn.createStatement();
            stmt.execute(String.format(deactivateIdSQL, deactivationTime, persistentId));
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

    /**
     * Gets a list of {@link PersistentIdEntry}s based on the given SQL where clause.
     * 
     * @param whereClause selection criteria
     * 
     * @return resultant list of {@link PersistentIdEntry}s
     * 
     * @throws SQLException thrown if there is a problem communicating with the database
     */
    protected List<PersistentIdEntry> getIdentifierEntries(String whereClause) throws SQLException {
        Connection dbConn = null;
        List<PersistentIdEntry> entries;
        String query = idEntrySelectSQL + whereClause;

        try {
            log.debug("Selecting persistent ID entry based on SQL query: {}", query);
            dbConn = dataSource.getConnection();
            Statement stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            entries = buildIdentifierEntries(rs);
            log.debug("{} persitent ID entries retrieved", entries.size());
            return entries;
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

    /**
     * Builds a list of {@link PersistentIdEntry}s from a result set.
     * 
     * @param resultSet the result set
     * 
     * @return list of {@link PersistentIdEntry}s
     * 
     * @throws SQLException thrown if there is a problem reading the information from the database
     */
    protected List<PersistentIdEntry> buildIdentifierEntries(ResultSet resultSet) throws SQLException {
        ArrayList<PersistentIdEntry> entries = new ArrayList<PersistentIdEntry>();

        PersistentIdEntry entry;
        while (resultSet.next()) {
            entry = new PersistentIdEntry();
            entry.setLocalEntityId(resultSet.getString(localEntityColumn));
            entry.setPeerEntityId(resultSet.getString(peerEntityColumn));
            entry.setPrincipalName(resultSet.getString(principalNameColumn));
            entry.setPersistentId(resultSet.getString(persistentIdColumn));
            entry.setLocalId(resultSet.getString(localIdColumn));
            entry.setPeerProvidedId(resultSet.getString(peerProvidedIdColumn));
            entry.setCreationTime(resultSet.getTimestamp(createTimeColumn));
            entry.setDeactivationTime(resultSet.getTimestamp(deactivationTimeColumn));
            entries.add(entry);
        }

        return entries;
    }

    /** Data object representing a persistent identifier entry in the database. */
    public class PersistentIdEntry implements Serializable {

        /** Serial version UID . */
        private static final long serialVersionUID = -8711779466442306767L;

        /** ID of the entity that issued that identifier. */
        private String localEntityId;

        /** ID of the entity to which the identifier was issued. */
        private String peerEntityId;

        /** Name of the principal represented by the identifier. */
        private String principalName;

        /** Local component portion of the persistent ID entry. */
        private String localId;

        /** The persistent identifier. */
        private String persistentId;

        /** ID, associated with the persistent identifier, provided by the peer. */
        private String peerProvidedId;

        /** Time the identifier was created. */
        private Timestamp creationTime;

        /** Time the identifier was deactivated. */
        private Timestamp deactivationTime;

        /** Constructor. */
        public PersistentIdEntry() {
        }

        /**
         * Gets the ID of the entity that issued the identifier.
         * 
         * @return ID of the entity that issued the identifier
         */
        public String getLocalEntityId() {
            return localEntityId;
        }

        /**
         * Sets the ID of the entity that issued the identifier.
         * 
         * @param id ID of the entity that issued the identifier
         */
        public void setLocalEntityId(String id) {
            localEntityId = id;
        }

        /**
         * Gets the ID of the entity to which the identifier was issued.
         * 
         * @return ID of the entity to which the identifier was issued
         */
        public String getPeerEntityId() {
            return peerEntityId;
        }

        /**
         * Sets the ID of the entity to which the identifier was issued.
         * 
         * @param id ID of the entity to which the identifier was issued
         */
        public void setPeerEntityId(String id) {
            peerEntityId = id;
        }

        /**
         * Gets the name of the principal the identifier represents.
         * 
         * @return name of the principal the identifier represents
         */
        public String getPrincipalName() {
            return principalName;
        }

        /**
         * Sets the name of the principal the identifier represents.
         * 
         * @param name name of the principal the identifier represents
         */
        public void setPrincipalName(String name) {
            principalName = name;
        }

        /**
         * Gets the local ID component of the persistent identifier.
         * 
         * @return local ID component of the persistent identifier
         */
        public String getLocalId() {
            return localId;
        }

        /**
         * Sets the local ID component of the persistent identifier.
         * 
         * @param id local ID component of the persistent identifier
         */
        public void setLocalId(String id) {
            localId = id;
        }

        /**
         * Gets the persistent identifier.
         * 
         * @return the persistent identifier
         */
        public String getPersistentId() {
            return persistentId;
        }

        /**
         * Set the persistent identifier.
         * 
         * @param id the persistent identifier
         */
        public void setPersistentId(String id) {
            persistentId = id;
        }

        /**
         * Gets the ID, provided by the peer, associated with this ID.
         * 
         * @return ID, provided by the peer, associated with this ID
         */
        public String getPeerProvidedId() {
            return peerProvidedId;
        }

        /**
         * Sets the ID, provided by the peer, associated with this ID.
         * 
         * @param id ID, provided by the peer, associated with this ID
         */
        public void setPeerProvidedId(String id) {
            peerProvidedId = id;
        }

        /**
         * Gets the time the identifier was created.
         * 
         * @return time the identifier was created
         */
        public Timestamp getCreationTime() {
            return creationTime;
        }

        /**
         * Sets the time the identifier was created.
         * 
         * @param time time the identifier was created
         */
        public void setCreationTime(Timestamp time) {
            creationTime = time;
        }

        /**
         * Gets the time the identifier was deactivated.
         * 
         * @return time the identifier was deactivated
         */
        public Timestamp getDeactivationTime() {
            return deactivationTime;
        }

        /**
         * Sets the time the identifier was deactivated.
         * 
         * @param time the time the identifier was deactivated
         */
        public void setDeactivationTime(Timestamp time) {
            this.deactivationTime = time;
        }
    }
}
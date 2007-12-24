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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents as persistent, database-backed, store of identifiers.
 * 
 * The DDL for the database is
 * <tt>CREATE TABLE shibpid {princpal VARCHAR NOT NULL, peerEntity VARCHAR NOT NULL, localEntity VARCHAR NOT NULL, persistentId VARCHAR NOT NULL, creationDate TIMESTAMP NOT NULL, deactivationDate TIMESTAMP}</tt>.
 * 
 * An index should be created on the <tt>persistentId</tt> and <tt>deactivationDate</tt> column as this is used to
 * query for principal names associated with the a given persistent identifier.
 * 
 * An index should be created on the <tt>principal</tt>, <tt>peerEntity</tt>, <tt>localEntity</tt>, and
 * <tt>deactivationDate</tt> columns as these are used to query for the persistent ID.
 */
public class PersistentIDStore {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PersistentIDStore.class);

    /** JDBC data source for retrieving connections. */
    private DataSource dataSource;

    /** Name of the database table. */
    private final String table = "shibpid";

    /** Name of the principal name column. */
    private final String principalColumn = "princpal";

    /** Name of the peer entity ID name column. */
    private final String peerEntityColumn = "peerEntity";

    /** Name of the local entity ID column. */
    private final String localEntityColumn = "localEntity";

    /** Name of the persistent ID column. */
    private final String persistentIdColumn = "persistentId";

    /** Name of the creation time column. */
    private final String createTimeColumn = "creationDate";

    /** Name of the deactivation time column. */
    private final String deactivationTimeColumn = "deactivationDate";

    /** Partial select query for ID entris. */
    private final String idEntrySelectSQL = "SELECT * FROM shibpid WHERE ";

    /** SQL used to deactivate an ID. */
    private final String deactivateIdSQL = "UPDATE shibidp SET " + deactivationTimeColumn + "='%s' WHERE "
            + persistentIdColumn + "='%s'";

    /**
     * Constructor.
     * 
     * @param source datasouce used to communicate with the database
     */
    public PersistentIDStore(DataSource source) {
        dataSource = source;
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
        String whereClause = String.format("persistentId = '%s' AND deactivationDate IS NULL", persistentId);
        List<PersistentIdEntry> entries = getIdentifierEntries(whereClause);

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
     * @param principal principal the the persistent ID represents
     * @param peer entity ID of the peer the ID is for
     * @param local entity ID of the ID issuer
     * 
     * @return the active identifier
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public PersistentIdEntry getActivePersistentIdEntry(String principal, String peer, String local)
            throws SQLException {
        String whereClause = String.format(
                "princiaplName = '%s' AND peerEntity = '%s' AND localEntity = '%s' AND deactivationDate IS NULL",
                principal, peer, local);
        List<PersistentIdEntry> entries = getIdentifierEntries(whereClause);

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
     * @param principal principal the the persistent IDs represent
     * @param peer entity ID of the peer the ID is for
     * @param local entity ID of the ID issuer
     * 
     * @return list of deactivated identifiers
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public List<PersistentIdEntry> getDeactivatedPersistentIdEntries(String principal, String peer, String local)
            throws SQLException {
        String whereClause = String.format(
                "princiaplName = '%s' AND peerEntity = '%s' AND localEntity = '%s' AND deactivationDate IS NOT NULL",
                principal, peer, local);
        List<PersistentIdEntry> entries = getIdentifierEntries(whereClause);

        if (entries == null || entries.size() == 0) {
            return null;
        }

        return entries;
    }

    /**
     * Creates a peristent ID for the given (principal, peer entity, local entity) tuple.
     * 
     * @param principal principal the the persistent ID represents
     * @param peer entity ID of the peer the ID is for
     * @param local entity ID of the ID issuer
     * 
     * @return the created identifier
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public String createPersisnentId(String principal, String peer, String local) throws SQLException {
        String persistentId = UUID.randomUUID().toString();

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(table);
        sql.append(" ('princpal', 'peerEntity', 'localEntity', 'persistentId', 'creationDate') VALUES ()");
        sql.append("'");
        sql.append(principal);
        sql.append("',");
        sql.append("'");
        sql.append(peer);
        sql.append("',");
        sql.append("'");
        sql.append(local);
        sql.append("',");
        sql.append("'");
        sql.append(persistentId);
        sql.append("',");
        sql.append("'");
        sql.append(new Timestamp(System.currentTimeMillis()).toString());
        sql.append("')");

        Connection dbConn = null;
        try {
            log.debug("Inserting newly created persistent id {} for principal {}", persistentId, principal);
            dbConn = dataSource.getConnection();
            Statement stmt = dbConn.createStatement();
            stmt.execute(sql.toString());
            return persistentId;
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
        List<PersistentIdEntry> entires;
        String query = idEntrySelectSQL + whereClause;

        try {
            log.debug("Selecting persistent ID entry based on SQL query: {}", query);
            dbConn = dataSource.getConnection();
            Statement stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            entires = buildIdentifierEntries(rs);
            log.debug("{} persitent ID entries retrieved", entires.size());
            return entires;
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

        String persistentId;
        String peerEntity;
        String localEntity;
        String principalName;
        Timestamp creationTime;
        Timestamp deactivationTime;
        PersistentIdEntry entry;
        while (resultSet.next()) {
            persistentId = resultSet.getString(persistentIdColumn);
            peerEntity = resultSet.getString(peerEntityColumn);
            localEntity = resultSet.getString(localEntityColumn);
            principalName = resultSet.getString(principalColumn);
            creationTime = resultSet.getTimestamp(createTimeColumn);
            deactivationTime = resultSet.getTimestamp(deactivationTimeColumn);

            entry = new PersistentIdEntry(persistentId, peerEntity, localEntity, principalName, creationTime,
                    deactivationTime);
            entries.add(entry);
        }

        return entries;
    }

    /** Data object representing a persistent identifier entry in the database. */
    public class PersistentIdEntry {

        /** The persistent identifier. */
        private String persistentId;

        /** ID of the entity to which the identifier was issued. */
        private String peerEntity;

        /** ID of the entity that issued that identifier. */
        private String localEntity;

        /** Name of the principal represented by the identifier. */
        private String principalName;

        /** Time the identifier was created. */
        private DateTime creationTime;

        /** Time the idenifier was deactived. */
        private DateTime deactivationTime;

        /**
         * Constructor.
         * 
         * @param persistentId the persistent identifier
         * @param peerEntity ID of the entity to which the identifier was issued
         * @param localEntity ID of the entity that issued the identifier
         * @param principalName name of the principal represented by the identifier
         * @param creationTime time the identifier was created
         * @param deactivationTime time the identifier was deactived
         */
        public PersistentIdEntry(String persistentId, String peerEntity, String localEntity, String principalName,
                Timestamp creationTime, Timestamp deactivationTime) {
            this.persistentId = persistentId;
            this.peerEntity = peerEntity;
            this.localEntity = localEntity;
            this.principalName = principalName;
            this.creationTime = new DateTime(creationTime.getTime());

            if (deactivationTime != null) {
                this.deactivationTime = new DateTime(deactivationTime.getTime());
            }
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
         * Gets the ID of the entity to which the identifier was issued.
         * 
         * @return ID of the entity to which the identifier was issued
         */
        public String getPeerEntity() {
            return peerEntity;
        }

        /**
         * Gets the ID of the entity that issued the identifier.
         * 
         * @return ID of the entity that issued the identifier
         */
        public String getLocalEntity() {
            return localEntity;
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
         * Gets the time the identifier was created.
         * 
         * @return time the identifier was created
         */
        public DateTime getCreationTime() {
            return creationTime;
        }

        /**
         * Gets the time the identifier was deactived.
         * 
         * @return time the identifier was deactived
         */
        public DateTime getDeactivationTime() {
            return deactivationTime;
        }

    }
}
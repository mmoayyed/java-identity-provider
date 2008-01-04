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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents as persistent, database-backed, store of identifiers.
 * 
 * The DDL for the database is
 * <tt>CREATE TABLE shibpid {localEntity VARCHAR NOT NULL, peerEntity VARCHAR NOT NULL, localId VARCHAR NOT NULL, persistentId VARCHAR NOT NULL, peerProvidedId VARCHAR, creationDate TIMESTAMP NOT NULL, deactivationDate TIMESTAMP}</tt>.
 * 
 * An index should be created on the <tt>persistentId</tt> and <tt>deactivationDate</tt> column as this is used to
 * query for principal names associated with the a given persistent identifier.
 * 
 * An index should be created on the <tt>principal</tt>, <tt>peerEntity</tt>, <tt>localEntity</tt>, and
 * <tt>deactivationDate</tt> columns as these are used to query for the persistent ID.
 */
public class StoredIDStore {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StoredIDStore.class);

    /** JDBC data source for retrieving connections. */
    private DataSource dataSource;

    /** Name of the database table. */
    private final String table = "shibpid";

    /** Name of the local ID column. */
    private final String localIdColumn = "localId";

    /** Name of the peer entity ID name column. */
    private final String peerEntityColumn = "peerEntity";

    /** Name of the local entity ID column. */
    private final String localEntityColumn = "localEntity";

    /** Name of the persistent ID column. */
    private final String persistentIdColumn = "persistentId";

    /** ID, provided by peer, associated with the persistent ID. */
    private final String peerProvidedIdColumn = "peerProvidedId";

    /** Name of the creation time column. */
    private final String createTimeColumn = "creationDate";

    /** Name of the deactivation time column. */
    private final String deactivationTimeColumn = "deactivationDate";

    /** Partial select query for ID entries. */
    private final String idEntrySelectSQL = "SELECT * FROM shibpid WHERE ";

    /** SQL used to deactivate an ID. */
    private final String deactivateIdSQL = "UPDATE shibidp SET " + deactivationTimeColumn + "='%s' WHERE "
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
            return rs.getInt(0);
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
        sqlBuilder.append("'").append(localEntityColumn).append("', ");
        sqlBuilder.append("'").append(peerEntityColumn).append("', ");
        sqlBuilder.append("'").append(localIdColumn).append("', ");
        sqlBuilder.append("'").append(persistentIdColumn).append("', ");
        sqlBuilder.append("'").append(peerProvidedIdColumn).append("', ");
        sqlBuilder.append("'").append(peerProvidedIdColumn).append("'");
        sqlBuilder.append(") VALUES (");

        sqlBuilder.append("'").append(entry.getPersistentId()).append("', ");
        sqlBuilder.append("'").append(entry.getLocalEntity()).append("', ");
        sqlBuilder.append("'").append(entry.getPeerEntity()).append("', ");
        sqlBuilder.append("'").append(entry.getPersistentId()).append("', ");
        if (entry.getPeerProvidedId() == null) {
            sqlBuilder.append(" NULL,");
        } else {
            sqlBuilder.append("'").append(entry.getPeerProvidedId()).append("', ");
        }
        sqlBuilder.append("'").append(new Timestamp(System.currentTimeMillis()).toString()).append("')");

        Connection dbConn = null;
        try {
            log.debug("Inserting newly created persistent id {} for principal {}", entry.getPersistentId(), entry
                    .getPrincipalName());
            dbConn = dataSource.getConnection();
            Statement stmt = dbConn.createStatement();

            String sql = sqlBuilder.toString();
            log.trace("Executing SQL statement {}", sql);
            stmt.execute(sql);
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
     * Creates a persistent ID that is unique for a given local/peer/localId tuple.
     * 
     * If an ID has never been issued for to the given tuple then an ID is created by taking a SHA-1 hash of the peer's
     * entity ID, the local ID, and a salt. This is to ensure compatability with IDs created by the now deprecated
     * {@link ComputedIDDataConnector}.
     * 
     * If an ID has been issued to the given tuple than a new, random type 4 UUID is generated as the persistent ID.
     * 
     * @param localEntity entity ID of the ID issuer
     * @param peerEntity entity ID of the peer the ID is for
     * @param localId principal the the persistent ID represents
     * @param salt salt used when computing a persistent ID via SHA-1 hash
     * 
     * @return the created identifier
     * 
     * @throws SQLException thrown if there is a problem communication with the database
     */
    public String createPersisnentId(String localEntity, String peerEntity, String localId, byte[] salt)
            throws SQLException {
        int numberOfExistingEntries = getNumberOfPersistentIdEntries(localEntity, peerEntity, localId);
        
        if (numberOfExistingEntries == 0) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(peerEntity.getBytes());
                md.update((byte) '!');
                md.update(localId.getBytes());
                md.update((byte) '!');
                md.digest(salt);

                return Base64.encodeBytes(md.digest());
            } catch (NoSuchAlgorithmException e) {
                log.error("JVM error, SHA-1 is not supported, unable to compute ID");
                throw new SQLException("SHA-1 is not supported, unable to compute ID");
            }
        } else {
            return UUID.randomUUID().toString();
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
        String peerProvidedId;
        Timestamp creationTime;
        Timestamp deactivationTime;
        PersistentIdEntry entry;
        while (resultSet.next()) {
            persistentId = resultSet.getString(persistentIdColumn);
            peerEntity = resultSet.getString(peerEntityColumn);
            localEntity = resultSet.getString(localEntityColumn);
            principalName = resultSet.getString(localIdColumn);
            peerProvidedId = resultSet.getString(peerProvidedIdColumn);
            creationTime = resultSet.getTimestamp(createTimeColumn);
            deactivationTime = resultSet.getTimestamp(deactivationTimeColumn);

            entry = new PersistentIdEntry(persistentId, peerEntity, localEntity, principalName, peerProvidedId,
                    creationTime, deactivationTime);
            entries.add(entry);
        }

        return entries;
    }

    /** Data object representing a persistent identifier entry in the database. */
    public class PersistentIdEntry implements Serializable {

        /** Serial version UID . */
        private static final long serialVersionUID = -8711779466442306767L;

        /** The persistent identifier. */
        private String persistentId;

        /** ID of the entity to which the identifier was issued. */
        private String peerEntity;

        /** ID, associated with the persistent identifier, provided by the peer. */
        private String peerProvidedId;

        /** ID of the entity that issued that identifier. */
        private String localEntity;

        /** Name of the principal represented by the identifier. */
        private String principalName;

        /** Time the identifier was created. */
        private DateTime creationTime;

        /** Time the identifier was deactivated. */
        private DateTime deactivationTime;

        /**
         * Constructor.
         * 
         * @param persistentId the persistent identifier
         * @param peerEntity ID of the entity to which the identifier was issued
         * @param localEntity ID of the entity that issued the identifier
         * @param principalName name of the principal represented by the identifier
         * @param peerProvidedId ID, associated with the persistent identifier, provided by the peer
         * @param creationTime time the identifier was created
         * @param deactivationTime time the identifier was deactivated
         */
        public PersistentIdEntry(String persistentId, String peerEntity, String localEntity, String principalName,
                String peerProvidedId, Timestamp creationTime, Timestamp deactivationTime) {
            this.persistentId = persistentId;
            this.peerEntity = peerEntity;
            this.localEntity = localEntity;
            this.principalName = principalName;
            this.peerProvidedId = peerProvidedId;
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
         * Gets the time the identifier was deactivated.
         * 
         * @return time the identifier was deactivated
         */
        public DateTime getDeactivationTime() {
            return deactivationTime;
        }

        /**
         * Gets the ID, provided by the peer, associated with this ID.
         * 
         * @return ID, provided by the peer, associated with this ID
         */
        public String getPeerProvidedId() {
            return peerProvidedId;
        }
    }
}
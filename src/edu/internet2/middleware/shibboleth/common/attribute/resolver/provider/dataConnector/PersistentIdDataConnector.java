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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;

/**
 * A data connector that generates persistent identifiers in one of two ways. The generated attribute has an ID of
 * <tt>peristentId</tt> and contains a single {@link String} value.
 * 
 * If a salt is supplied at construction time the generated IDs will be the Base64-encoded SHA-1 hash of the user's
 * principal name, the peer entity ID, and the salt.
 * 
 * If a {@link DataSource} is provided at construction IDs are looked up in a database. IF the ID does not exist a
 * {@link UUID} is generated, stored in the database, and used as the ID. The DDL for the database is
 * <tt>CREATE TABLE shibpid {princpal VARCHAR NOT NULL, peerEntity VARCHAR NOT NULL, localEntity VARCHAR NOT NULL, persistentId VARCHAR NOT NULL, creationDate TIMESTAMP NOT NULL, deactivationDate TIMESTAMP}</tt>.
 * An index should be created on the <tt>principa</tt>, <tt>peerEntity</tt>, <tt>localEntity</tt>, and
 * <tt>deactivationDate</tt> columns as these are used to query for the persistent ID. Specifically, the query used to
 * lookup the persistent identifier is
 * <tt>SELECT persistentId FROM shibpid WHERE princiaplName = ? AND peerEntity = ? AND localEntity = ? AND deactivationDate IS NULL</tt>
 */
public class PersistentIdDataConnector extends BaseDataConnector {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PersistentIdDataConnector.class);

    /** JDBC data source for retrieving connections. */
    private DataSource dataSource;

    /** SQL used to lookup the persistent ID. */
    private String lookupIdSQL = "SELECT persistentId FROM shibpid WHERE princiaplName = '%s' AND peerEntity = '%s' AND localEntity = '%s' AND deactivationDate IS NULL";

    /** SQL used to store the persistent ID. */
    private String insertIdSQL = "INSERT INTO shibpid ('princpal', 'peerEntity', 'localEntity', 'persistentId', 'creationDate') VALUES ('%s', '%s', '%s', '%s', '%s')";

    /** Hashing salt. */
    private byte[] salt;

    /**
     * Constructor.
     * 
     * @param source datasouce used to communicate with the database
     */
    public PersistentIdDataConnector(DataSource source) {
        dataSource = source;
    }

    /**
     * Constructor.
     * 
     * @param newSalt salt used when generating IDs by hashing
     */
    public PersistentIdDataConnector(byte[] newSalt) {
        salt = newSalt;
    }

    /** {@inheritDoc} */
    protected BaseAttribute<String> doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        String persistentId = null;

        BasicAttribute<String> attribute = new BasicAttribute<String>();
        attribute.setId(getId());
        attribute.getValues().add(persistentId);
        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        if (salt.length == 0) {
            throw new AttributeResolutionException("No hash salt provided");
        }
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        String persistentId = null;
        if(salt != null){
            persistentId = generateHashedId(resolutionContext);
        }else{
            persistentId = getStoredId(resolutionContext);
        }
        
        BasicAttribute<String> attribute = new BasicAttribute<String>();
        attribute.setId("persistentId");
        attribute.getValues().add(persistentId);
        
        Map<String, BaseAttribute> attributes = new HashMap<String, BaseAttribute>();
        attributes.put(attribute.getId(), attribute);
        return attributes;
    }

    /**
     * Generates a persitent ID by hash.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return generated ID
     * 
     * @throws AttributeResolutionException thrown if SHA-1 is not supportd by the VM
     */
    protected String generateHashedId(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(resolutionContext.getAttributeRequestContext().getInboundMessageIssuer().getBytes());
            md.update((byte) '!');
            md.update(resolutionContext.getAttributeRequestContext().getPrincipalName().getBytes());
            md.update((byte) '!');
            return Base64.encodeBytes(md.digest(salt));
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to load SHA-1 hash algorithm.");
            throw new AttributeResolutionException("Unable to computer persistent identifier", e);
        }
    }

    /**
     * Gets the persistent ID stored in the database. If one does not exist it is created.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return persistent ID
     * 
     * @throws AttributeResolutionException thrown if there is a problem retrieving or storing the persistent ID
     */
    protected String getStoredId(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        SAMLProfileRequestContext requestCtx = resolutionContext.getAttributeRequestContext();
        Connection dbConnection = null;
        try {
            String persistentId = null;
            String idQuery = String.format(lookupIdSQL, requestCtx.getPrincipalName(), requestCtx
                    .getInboundMessageIssuer(), requestCtx.getLocalEntityId());
            dbConnection = dataSource.getConnection();
            Statement statement = dbConnection.createStatement();
            ResultSet results = statement.executeQuery(idQuery);
            if (results.first()) {
                persistentId = results.getString("persistentId");
            } else {
                persistentId = createAndPersistId(dbConnection, requestCtx);
            }

            return persistentId;
        } catch (SQLException e) {
            log.error("Error looking up persistent ID", e);
            throw new AttributeResolutionException("Error looking up persistent ID", e);
        } finally {
            try {
                if (dbConnection != null && !dbConnection.isClosed()) {
                    dbConnection.close();
                }
            } catch (SQLException e) {
                log.error("Error closing database connection", e);
            }
        }
    }

    /**
     * Creates a UUID based persistent identifer and stores it in the database.
     * 
     * @param dbConnection connection used to communication with the database
     * @param requestCtx current request context
     * 
     * @return generated persistent ID
     * 
     * @throws AttributeResolutionException thrown if the ID can not be stored in the database
     */
    protected String createAndPersistId(Connection dbConnection, SAMLProfileRequestContext requestCtx)
            throws AttributeResolutionException {
        String persistentId = UUID.randomUUID().toString();

        String now = new Timestamp(System.currentTimeMillis()).toString();
        String sql = String.format(insertIdSQL, requestCtx.getPrincipalName(), requestCtx.getInboundMessageIssuer(),
                requestCtx.getLocalEntityId(), persistentId, now);

        try {
            Statement statement = dbConnection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("Error storing persistent ID", e);
            throw new AttributeResolutionException("Error storing persistent ID", e);
        }

        return persistentId;
    }
}
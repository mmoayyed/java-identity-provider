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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.PersistentIDStore.PersistentIdEntry;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;

/**
 * A data connector that generates persistent identifiers in one of two ways. The generated attribute has an ID of
 * <tt>peristentId</tt> and contains a single {@link String} value.
 * 
 * If a salt is supplied at construction time the generated IDs will be the Base64-encoded SHA-1 hash of the user's
 * principal name, the peer entity ID, and the salt.
 * 
 * If a {@link DataSource} is supplied the IDs are created and managed as described by {@link PersistentIDStore}.
 */
public class PersistentIdDataConnector extends BaseDataConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PersistentIdDataConnector.class);

    /** Persistent identifier data store. */
    private PersistentIDStore pidStore;

    /** Hashing salt. */
    private byte[] salt;

    /**
     * Constructor.
     * 
     * @param source datasouce used to communicate with the database
     */
    public PersistentIdDataConnector(DataSource source) {
        pidStore = new PersistentIDStore(source);
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
        if (salt != null) {
            persistentId = generateHashedId(resolutionContext);
        } else {
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
     * Generates a persistent ID by hash.
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

        try {
            PersistentIdEntry idEntry = pidStore.getActivePersistentIdEntry(requestCtx.getPrincipalName(), requestCtx
                    .getInboundMessageIssuer(), requestCtx.getLocalEntityId());
            if (idEntry != null) {
                return idEntry.getPersistentId();
            } else {
                return pidStore.createPersisnentId(requestCtx.getPrincipalName(), requestCtx.getInboundMessageIssuer(),
                        requestCtx.getLocalEntityId());
            }
        } catch (SQLException e) {
            log.error("Database error retrieving persistent identifier", e);
            throw new AttributeResolutionException("Database error retrieving persistent identifier", e);
        }
    }
}
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector;

import java.sql.SQLException;

import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.StoredIDDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.StoredIDStore;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.StoredIDStore.PersistentIdEntry;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;

/**
 * A principal connector that resolved ID created by {@link StoredIDPrincipalConnector}s into principals.
 */
public class StoredIDPrincipalConnector extends BasePrincipalConnector {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StoredIDPrincipalConnector.class);

    /** ID store that manages the stored IDs. */
    private StoredIDStore pidStore;

    /**
     * Constructor.
     * 
     * @param idProducer data connector that produced the stored ID.
     */
    public StoredIDPrincipalConnector(StoredIDDataConnector idProducer) {
        if (idProducer == null) {
            throw new IllegalArgumentException("ID producing data connector may not be null");
        }
        pidStore = idProducer.getStoredIDStore();

    }

    /** {@inheritDoc} */
    public String resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        SAMLProfileRequestContext requestContext = resolutionContext.getAttributeRequestContext();

        String persistentId;
        if (requestContext.getSubjectNameIdentifier() instanceof NameIdentifier) {
            persistentId = ((NameIdentifier) requestContext.getSubjectNameIdentifier()).getNameIdentifier();
        } else if (requestContext.getSubjectNameIdentifier() instanceof NameID) {
            persistentId = ((NameID) requestContext.getSubjectNameIdentifier()).getValue();
        } else {
            throw new AttributeResolutionException("Subject name identifier is not of a supported type");
        }

        try {
            PersistentIdEntry pidEntry = pidStore.getActivePersistentIdEntry(persistentId);
            if(pidEntry != null){
                return pidEntry.getPrincipalName();
            }else{
                return null;
            }
        } catch (SQLException e) {
            log.error("Error retrieving persistent ID from database", e);
            throw new AttributeResolutionException("Error retrieving persistent ID from database", e);
        }
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        if(pidStore == null){
            throw new AttributeResolutionException("Persistent ID store was null");
        }
        
        try{
            pidStore.getPersistentIdEntry("test", false);
        }catch(SQLException e){
            throw new AttributeResolutionException("Persistent ID store can not perform persistent ID search", e);
        }
    }
}
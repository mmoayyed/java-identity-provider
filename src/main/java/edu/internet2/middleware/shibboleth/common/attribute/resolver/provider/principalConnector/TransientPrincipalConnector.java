/*
 * Copyright 2007 University Corporation for Advanced Internet Development, Inc.
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

import org.opensaml.common.SAMLObject;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.NameID;
import org.opensaml.util.storage.StorageService;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.TransientIdEntry;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;

/**
 * A principal connector that attempts to look up a name identifier within a store.
 */
public class TransientPrincipalConnector extends BasePrincipalConnector {

    /** Store used to map transient identifier tokens to principal names. */
    private StorageService<String, TransientIdEntry> identifierStore;

    /** Storage partition in which IDs are stored. */
    private String partition;

    /**
     * Constructor.
     * 
     * @param store the backing store used to map transient identifier tokens to principal names
     */
    public TransientPrincipalConnector(StorageService<String, TransientIdEntry> store) {
        if (store == null) {
            throw new IllegalArgumentException("Identifier store may not be null");
        }
        identifierStore = store;
        partition = "transientId";
    }

    /** {@inheritDoc} */
    public String resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        SAMLProfileRequestContext requestContext = resolutionContext.getAttributeRequestContext();

        String transientId = null;
        SAMLObject subjectId = requestContext.getSubjectNameIdentifier();
        if (subjectId instanceof NameIdentifier) {
            NameIdentifier nameId = (NameIdentifier) requestContext.getSubjectNameIdentifier();
            if (nameId != null) {
                transientId = nameId.getNameIdentifier();
            }
        } else if (requestContext.getSubjectNameIdentifier() instanceof NameID) {
            NameID nameId = (NameID) requestContext.getSubjectNameIdentifier();
            if (nameId != null) {
                transientId = nameId.getValue();
            }
        } else {
            throw new AttributeResolutionException("Subject name identifier is not of a supported type");
        }

        if (transientId == null) {
            throw new AttributeResolutionException("Invalid subject name identifier");
        }

        TransientIdEntry idToken = identifierStore.get(partition, transientId);
        if (idToken == null || idToken.isExpired()) {
            throw new AttributeResolutionException("No information associated with transient identifier: "
                    + transientId);
        }

        if (!idToken.getRelyingPartyId().equals(requestContext.getInboundMessageIssuer())) {
            throw new AttributeResolutionException("Transient identifier was issued to " + idToken.getRelyingPartyId()
                    + " but is being used by " + requestContext.getInboundMessageIssuer());
        }

        return idToken.getPrincipalName();
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }
}
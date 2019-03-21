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

package net.shibboleth.idp.attribute;

import java.time.Instant;

import javax.annotation.Nullable;

/**
 * Object representing a pairwise/targeted identifier entry as a storage input/output.
 * 
 * @since 4.0.0
 */
public class PairwiseId {

    /** ID of the entity that issued that identifier. */
    @Nullable private String issuerEntityID;

    /** ID of the entity to which the identifier was issued. */
    @Nullable private String recipientEntityID;

    /** Name of the principal represented by the identifier. */
    @Nullable private String principalName;

    /** Underlying unique key/ID of the principal. */
    @Nullable private String sourceSystemId;

    /** The identifier. */
    @Nullable private String pairwiseId;

    /** A secondary identifier attached to the record by the recipient. */
    @Nullable private String peerProvidedId;

    /** Time the identifier was created. */
    @Nullable private Instant creationTime;

    /** Time the identifier was deactivated. */
    @Nullable private Instant deactivationTime;

    /**
     * Get the ID of the entity that issued the identifier.
     * 
     * @return ID of the entity that issued the identifier
     */
    @Nullable public String getIssuerEntityID() {
        return issuerEntityID;
    }

    /**
     * Set the ID of the entity that issued the identifier.
     * 
     * @param id ID of the entity that issued the identifier
     */
    public void setIssuerEntityID(@Nullable final String id) {
        issuerEntityID = id;
    }

    /**
     * Get the ID of the entity to which the identifier was issued.
     * 
     * @return ID of the entity to which the identifier was issued
     */
    @Nullable public String getRecipientEntityID() {
        return recipientEntityID;
    }

    /**
     * Set the ID of the entity to which the identifier was issued.
     * 
     * @param id ID of the entity to which the identifier was issued
     */
    public void setRecipientEntityID(@Nullable final String id) {
        recipientEntityID = id;
    }

    /**
     * Get the name of the principal the identifier represents.
     * 
     * @return name of the principal the identifier represents
     */
    @Nullable public String getPrincipalName() {
        return principalName;
    }

    /**
     * Set the name of the principal the identifier represents.
     * 
     * @param name name of the principal the identifier represents
     */
    public void setPrincipalName(@Nullable final String name) {
        principalName = name;
    }

    /**
     * Get the underlying unique key/ID from the source IDM system, which may be more
     * stable than the "name".
     * 
     * @return the principal's unique key or ID
     */
    @Nullable public String getSourceSystemId() {
        return sourceSystemId;
    }

    /**
     * Set the underlying unique key/ID from the source IDM system, which may be more
     * stable than the "name".
     * 
     * @param id principal's unique key or ID
     */
    public void setSourceSystemId(@Nullable final String id) {
        sourceSystemId = id;
    }

    /**
     * Get the pairwise identifier.
     * 
     * @return the pairwise identifier
     */
    @Nullable public String getPairwiseId() {
        return pairwiseId;
    }

    /**
     * Set the pairwise identifier.
     * 
     * @param id the pairwise identifier
     */
    public void setPairwiseId(@Nullable final String id) {
        pairwiseId = id;
    }

    /**
     * Get the alias, provided by the recipient, attached to this ID.
     * 
     * @return alias, provided by the recipient, associated with this ID
     */
    @Nullable public String getPeerProvidedId() {
        return peerProvidedId;
    }

    /**
     * Set the alias, provided by the recipient, attached to this ID.
     * 
     * @param id alias, provided by the recipient, attached to this ID
     */
    public void setPeerProvidedId(@Nullable final String id) {
        peerProvidedId = id;
    }

    /**
     * Get the time the identifier was created.
     * 
     * @return time the identifier was created
     */
    @Nullable public Instant getCreationTime() {
        return creationTime;
    }

    /**
     * Set the time the identifier was created.
     * 
     * @param time time the identifier was created
     */
    public void setCreationTime(@Nullable final Instant time) {
        creationTime = time;
    }

    /**
     * Get the time the identifier was deactivated.
     * 
     * @return time the identifier was deactivated
     */
    @Nullable public Instant getDeactivationTime() {
        return deactivationTime;
    }

    /**
     * Set the time the identifier was deactivated.
     * 
     * @param time the time the identifier was deactivated
     */
    public void setDeactivationTime(@Nullable final Instant time) {
        deactivationTime = time;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder stringForm = new StringBuilder("PairwiseId {");
        stringForm.append("pairwiseId:").append(pairwiseId).append(", ");
        stringForm.append("issuerEntityID:").append(issuerEntityID).append(", ");
        stringForm.append("recipientEntityID:").append(recipientEntityID).append(", ");
        stringForm.append("sourceSystemId:").append(sourceSystemId).append(", ");
        stringForm.append("principalName:").append(principalName).append(", ");
        stringForm.append("peerProvidedId:").append(peerProvidedId).append(", ");
        stringForm.append("creationTime:").append(creationTime).append(", ");
        stringForm.append("deactivationTime:").append(deactivationTime);
        stringForm.append("}");
        return stringForm.toString();
    }
    
}
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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import java.io.Serializable;
import java.sql.Timestamp;

/** Data object representing a persistent identifier entry in the persisten storage. */
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

    /** {@inheritDoc} */
    public String toString() {
        StringBuilder stringForm = new StringBuilder("PersistentIdEntry{");
        stringForm.append("persistentId:").append(persistentId).append(", ");
        stringForm.append("localEntityId:").append(localEntityId).append(", ");
        stringForm.append("peerEntityId:").append(peerEntityId).append(", ");
        stringForm.append("localId:").append(localId).append(", ");
        stringForm.append("principalName:").append(principalName).append(", ");
        stringForm.append("peerProvidedId:").append(peerProvidedId).append(", ");
        stringForm.append("creationTime:").append(creationTime).append(", ");
        stringForm.append("deactivationTime:").append(deactivationTime).append(", ");
        stringForm.append("}");
        return stringForm.toString();
    }
}
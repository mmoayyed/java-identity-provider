/*
z * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.consent.entities;

/**
 *
 */
public class RelyingParty {
    private long id;

    private String entityId;

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object object) {
        if (object instanceof RelyingParty) {
            return this.entityId == ((RelyingParty) object).getEntityId();
        }
        return false;
    }

    /**
     * @return Returns the entityId.
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * @return Returns the id.
     */
    public long getId() {
        return id;
    }

    /**
     * @param entityId The entityId to set.
     */
    public void setEntityId(final String entityId) {
        this.entityId = entityId;
    }

    /**
     * @param id The id to set.
     */
    public void setId(final long id) {
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "RelyingParty [id=" + id + ", entityId=" + entityId + "]";
    }
}

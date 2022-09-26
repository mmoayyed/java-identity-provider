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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.saml.metadata.resolver.DetectDuplicateEntityIDs;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/**
 * Object representing a query for metadata.
 * 
 * <p>This abstracts the parameters used to populate the context tree.</p>
 */
@ThreadSafe
public class MetadataQueryRequest {

    /** An entityID. */
    @Nullable @NotEmpty private String entityID;
    
    /** Protocol identifier for query. */
    @Nullable private String protocol;
    
    /** The strategy for duplicate entityID detection. */
    @Nullable private DetectDuplicateEntityIDs detectDuplicateEntityIDs;

    /**
     * Constructor.
     */
    public MetadataQueryRequest() {
        
    }

    /**
     * Get the entityID to query on.
     * 
     * @return entityID for query
     */
    @Nullable @NotEmpty public String getEntityID() {
        return entityID;
    }
    
    /**
     * Set the entityID to query on.
     * 
     * @param id entityID for query
     */
    public void setEntityID(@Nullable @NotEmpty final String id) {
        entityID = StringSupport.trimOrNull(id);
    }

    /**
     * Get the protocol to query on.
     * 
     * @return protocol for query
     */
    @Nullable @NotEmpty public String getProtocol() {
        return protocol;
    }
    
    /**
     * Set the protocol to query on.
     * 
     * @param prot protocol for query
     */
    public void setProtocol(@Nullable @NotEmpty final String prot) {
        protocol = StringSupport.trimOrNull(prot);
    }

    /**
     * Get the strategy for duplicate entityID detection.
     * 
     * @return strategy for duplicate entityID detection
     */
    @Nullable public DetectDuplicateEntityIDs getDetectDuplicateEntityIDs() {
        return detectDuplicateEntityIDs;
    }

    /**
     * Set the strategy for duplicate entityID detection.
     * 
     * @param strategy the strategy for duplicate entityID detection
     */
    public void setDetectDuplicateEntityIDs(@Nullable final DetectDuplicateEntityIDs strategy) {
        detectDuplicateEntityIDs = strategy;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("entityID", entityID)
            .add("protocol", protocol)
            .add("detectDuplicateEntityIDs", detectDuplicateEntityIDs)
            .toString();
    }
    
}
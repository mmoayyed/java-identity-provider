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

package net.shibboleth.idp.saml.profile.config;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Interface for outbound SAML artifact configuration.
 * 
 * <p>While sufficient for known SAML 1 and 2 artifact types, the interface
 * may be extended if necessary to carry type-specific additions.</p>
 */
public class BasicSAMLArtifactConfiguration implements SAMLArtifactConfiguration {

    /** The artifact type code. */
    @Nullable private Integer artifactType;
    
    /** The artifact resolution URL. */
    @Nullable private String artifactResolutionURL;
    
    /** The artifact resolution index. */
    @Nullable private Integer artifactResolutionIndex;
    
    /** {@inheritDoc} */
    @Nullable public Integer getArtifactType() {
        return artifactType;
    }
    
    /**
     * Set the type code of the artifact to use.
     * 
     * @param type  type code of artifact
     */
    public void setArtifactType(@Nullable final Integer type) {
        artifactType = type;
    }
    
    /** {@inheritDoc} */
    @Nullable public String getArtifactResolutionServiceURL() {
        return artifactResolutionURL;
    }
    
    /**
     * Set the location, as a URL, of the issuer's resolution service endpoint.
     * 
     * @param url   location of the resolution service endpoint
     */
    public void setArtifactResolutionServiceURL(@Nullable final String url) {
        artifactResolutionURL = StringSupport.trimOrNull(url);
    }
    
    /** {@inheritDoc} */
    @Nullable public Integer getArtifactResolutionServiceIndex() {
        return artifactResolutionIndex;
    }
    
    /**
     * Set the index of the issuer's resolution service endpoint, corresponding to its metadata.
     * 
     * @param index index of resolution service endpoint
     */
    public void setArtifactResolutionServiceIndex(@Nullable Integer index) {
        artifactResolutionIndex = index;
    }
    
}
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

package edu.internet2.middleware.shibboleth.common.attribute.filtering;

import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;

import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * A {@link FilterContext} with access to SAML specific information.
 */
public interface SAMLFilterContext extends FilterContext {

    /**
     * Gets the metadata provider that may be used to look up entity information.
     * 
     * @return metadata provider that may be used to look up entity information
     */
    public MetadataProvider getMetadataProvicer();
    
    /**
     * Gets information about the relying party or attribute requester.
     * 
     * @return information about the relying party
     */
    public RelyingPartyConfiguration getRelyingPartyConfig();
    
    /**
     * Gets the metadata for the relying party or attribute requester.
     * 
     * @return metadata for the relying party or attribute requester
     */
    public EntityDescriptor getRequesterMetadata();
}
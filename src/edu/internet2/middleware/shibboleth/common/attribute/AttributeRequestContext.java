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

package edu.internet2.middleware.shibboleth.common.attribute;

import java.util.Set;

import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * Contextual information for requesting attributes from an attribute authority.
 */
public interface AttributeRequestContext {
    
    /**
     * Gets the principal name (userid) of the user the attributes in this context describe.
     * 
     * @return principal name of the user the attributes in this context describe
     */
    public String getPrincipalName();
    
    /**
     * Gets the method used to authenticate the principal to the attribute requester.
     * 
     * @return method used to authenticate the principal to the attribute requester
     */
    public String getPrincipalAuthenticationMethod();
    
    /**
     * Gets the configuration for the relying party.
     * 
     * @return configuration for the relying party
     */
    public RelyingPartyConfiguration getRelyingPartyConfiguration();

    /**
     * Gets the ID of the requester of the attributes.
     * 
     * @return requester of the attributes
     */
    public String getAttributeRequester();
    
    /**
     * Gets the ID of the issuer of the attributes.
     * 
     * @return ID of the issuer of the attributes
     */
    public String getAttributeIssuer();
    
    /**
     * Gets the set of attributes, identified by their ID, that should be resolved.
     * 
     * @return set of attributes that should be resolved
     */
    public Set<String> getRequestedAttributes();
}
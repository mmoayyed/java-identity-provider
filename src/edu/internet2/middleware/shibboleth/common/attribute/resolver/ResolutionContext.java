/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver;

import java.util.Map;

import javax.servlet.ServletRequest;

import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.metadata.provider.MetadataProvider;

/**
 * A context for a resolution request.  This provides access to the information about the subject the attributes describe,
 * the requester of the attributes, the prodocuer of the attributes, and the attributes, attribute definitions, and data connectors 
 * already resolved.
 * 
 * All information in this object is read-only to the resolution plugins it is given to.
 */
public interface ResolutionContext {
   
    /**
     * Gets the principal name (userid) of the user the attributes in this context describe.
     * 
     * @return principal name of the user the attributes in this context describe
     */
    public String getPrincipalName();
    
    /**
     * Gets the subject the attributes in this context describe.
     * 
     * @return subject the attributes in this context describe
     */
    public NameID getSubject();
    
    /**
     * Gets the requester of the attributes.
     * 
     * @return requester of the attributes
     */
    public String getAttributeRequester();
    
    /**
     * Gets the request from the client.  The request and all related objects are read-only.
     * 
     * @return request from the client
     */
    public ServletRequest getRequest();
    
    /**
     * Gets the data connectors that have already been resolved, indexed by the connector's ID.
     * 
     * @return data connectors that have already been resolved
     */
    public Map<String, DataConnector> getResolvedDataConnectors();
    
    /**
     * Gets the attribute definitions that have already been resolved, indexed by the definition's ID.
     * 
     * @return attribute definitions that have already been resolved
     */
    public Map<String, AttributeDefinition> getResolvedAttributeDefinitions();
    
    /**
     * Gets the metadata provider that may be used to lookup information about entities.
     * 
     * @return metadata provider that may be used to lookup information about entities
     */
    public MetadataProvider getMetadataProvider();
}
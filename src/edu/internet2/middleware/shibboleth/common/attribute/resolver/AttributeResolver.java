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

import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.metadata.provider.MetadataProvider;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;

/**
 * The service that resolves the attribtues for a particular subject.
 * 
 * "Raw" attributes are gathered by the registered {@link DataConnector}s while the {@link AttributeDefinition}s
 * refine the raw attributes or create attributes of their own. Connectors and definitions may depend on each other so
 * implementations must use a directed dependency graph when performing the resolution.
 */
public interface AttributeResolver {

    /**
     * Creates a resolution context for the given user and attribute requester.
     * 
     * @param principal the principal whose attributes will be resolved
     * @param attributeRequester the party requesting the attributes
     * @param request the request from the client
     * 
     * @return resolution context containing the principal name for subject
     */
    public ResolutionContext createResolutionContext(String principal, String attributeRequester, ServletRequest request);

    /**
     * Creates a resolution context for the given user and attribute requester. The subject's nameID is resolved into a
     * system specific principal name.
     * 
     * @param subject the subject whose attributes will be resolved
     * @param attributeRequester the party requesting the attributes
     * @param request the request from the client
     * 
     * @return resolution context containing the principal name for subject
     * 
     * @throws AttributeResolutionException thrown if the NameID can not be resolved into a principal name
     */
    public ResolutionContext createResolutionContext(NameID subject, String attributeRequester, ServletRequest request)
            throws AttributeResolutionException;

    /**
     * Gets all the attributes for a given subject. While an initial attribute producer is given this does not mean
     * every returned attribute is from that producer. The producer may return information that can be used by data
     * connectors to contact other producers and retrieve attributes from them.
     * 
     * @param attributes list of attributes to resolve or null to resolve all attributes
     * @param resolutionContext the attribute resolution context to use to resolve attributes
     * 
     * @return the attributes describing the subject
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes for the subject
     */
	public Set<Attribute> resolveAttributes(Set<String> attributes, ResolutionContext resolutionContext) throws AttributeResolutionException;
    
    /**
     * Gets the list of principal connectors used to convert {@link NameID}s into userids.
     * 
     * @return connectors used to convert {@link NameID}s into userids
     */
    public Collection<PrincipalConnector> getPrincipalConnectors();

    /**
     * Gets the list of data connectors used to fetch the raw attributes for a subject.
     * 
     * @return data connectors used to fetch the raw attributes for a subject
     */
    public Collection<DataConnector> getDataConnectors();

    /**
     * Gets the list of attribute definitions that will produce the attributes for a subject.
     * 
     * @return attribute definitions that will produce the attributes for a subject
     */
    public Collection<AttributeDefinition> getAttributeDefinitions();

    /**
     * Gets the metadata provider that may be used to lookup information about entities.
     * 
     * @return metadata provider that may be used to lookup information about entities
     */
    public MetadataProvider getMetadataProvider();

    /**
     * Sets the metadata provider that may be used to lookup information about entities.
     * 
     * @param metadataProvider metadata provider that may be used to lookup information about entities
     */
    public void setMetadataProvider(MetadataProvider metadataProvider);

}
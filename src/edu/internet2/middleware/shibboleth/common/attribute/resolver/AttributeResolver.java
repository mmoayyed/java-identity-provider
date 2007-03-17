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
import java.util.Set;

import org.opensaml.saml2.core.NameID;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;

/**
 * The service that resolves the attribtues for a particular subject.
 */
public interface AttributeResolver {

    /**
     * Creates a resolution context for the given user and attribute requester.
     * 
     * @param principal the principal whose attributes will be resolved
     * @param attributeRequester the party requesting the attributes
     * 
     * @return resolution context containing the principal name for subject
     */
    public ResolutionContext createResolutionContext(String principal, String attributeRequester);

    /**
     * Creates a resolution context for the given user and attribute requester. The subject's nameID is resolved into a
     * system specific principal name.
     * 
     * @param subject the subject whose attributes will be resolved
     * @param attributeRequester the party requesting the attributes
     * 
     * @return resolution context containing the principal name for subject
     * 
     * @throws AttributeResolutionException thrown if the NameID can not be resolved into a principal name
     */
    public ResolutionContext createResolutionContext(NameID subject, String attributeRequester)
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
    public Map<String, Attribute> resolveAttributes(Set<String> attributes, ResolutionContext resolutionContext)
            throws AttributeResolutionException;

    /**
     * Check that the Attribute Resolver is in a valid state and ready to begin receiving resolution requests.
     * 
     * @throws AttributeResolutionException if resolver is in an invalid state
     */
    public void validate() throws AttributeResolutionException;
    // TODO: does it really make sense to throw an AttributeResolutionException? Perhaps a ConfigurationException?

}
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

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.profile.ProfileMessageContext;

/**
 * The service that resolves the attributes for a particular subject.
 * 
 * @param <RequestContextType> the type of attribute request context used by the resolver.
 */
public interface AttributeResolver<RequestContextType extends ProfileMessageContext> {

    /**
     * Gets all the attributes for a given subject. While an initial attribute producer is given this does not mean
     * every returned attribute is from that producer. The producer may return information that can be used by data
     * connectors to contact other producers and retrieve attributes from them.
     * 
     * @param resolutionContext the attribute resolution context to use to resolve attributes
     * 
     * @return the attributes describing the subject
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes for the subject
     */
    public Map<String, BaseAttribute> resolveAttributes(RequestContextType resolutionContext)
            throws AttributeResolutionException;

    /**
     * Check that the Attribute Resolver is in a valid state and ready to begin receiving resolution requests.
     * 
     * @throws AttributeResolutionException if resolver is in an invalid state
     */
    public void validate() throws AttributeResolutionException;
}
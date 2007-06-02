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

import java.util.Map;

/**
 * Interface for an attribute authority. An attribute authority will pull attribute information for the principal
 * specified in the request context. If no principal is specified it is assumed that the authority implementation will
 * be able to derive the principal from other data within the request context.
 * 
 * @param <ContextType> contextual information expected by the attribute authority
 */
public interface AttributeAuthority<ContextType extends AttributeRequestContext> {

    /**
     * Gets the attributes for the principal identified in the request.
     * 
     * @param requestContext contextual information for the attribute request
     * 
     * @return the request attributes keyed by the attributes' IDs.
     * 
     * @throws AttributeRequestException thrown if there is a problem retrieving the attributes
     */
    public Map<String, BaseAttribute> getAttributes(ContextType requestContext)
            throws AttributeRequestException;
}
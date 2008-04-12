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

import java.util.Collection;
import java.util.Map;

/**
 * Base interface for attribute resolution requests.
 */
public interface AttributeRequestContext {

    /**
     * Gets the collection of IDs for the attributes being requested by the relying party.
     * 
     * @return collection of IDs for the attributes being requested by the relying party
     */
    public Collection<String> getRequestedAttributesIds();

    /**
     * Sets the collection of IDs for the attributes being requested by the relying party.
     * 
     * @param ids collection of IDs for the attributes being requested by the relying party
     */
    public void setRequestedAttributes(Collection<String> ids);

    /**
     * Gets the retrieved attributes.
     * 
     * @return retrieved attributes
     */
    public Map<String, BaseAttribute> getAttributes();

    /**
     * Sets the retrieved attributes.
     * 
     * @param attributes retrieved attributes
     */
    public void setAttributes(Map<String, BaseAttribute> attributes);
    
    /**
     * Gets the method used to authenticate the principal.
     * 
     * @return method used to authenticate the principal
     */
    public String getPrincipalAuthenticationMethod();

    /**
     * Gets the principal name of the subject of the request.
     * 
     * @return principal name of the subject of the request
     */
    public String getPrincipalName();
    
    /**
     * Sets the method used to authenticate the principal.
     * 
     * @param method method used to authenticate the principal
     */
    public void setPrincipalAuthenticationMethod(String method);

    /**
     * Sets the principal name of the subject of the request.
     * 
     * @param name principal name of the subject of the request
     */
    public void setPrincipalName(String name);
}
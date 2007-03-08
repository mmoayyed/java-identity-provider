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

import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;

/**
 * A context for an attribute filtering request. This provides access to the information about the subject the
 * attributes describe, the requester of the attributes, and the attributes.
 * 
 * All information in this object is read-only to the resolution plugins it is given to.
 */
public interface FilterContext {

    /**
     * Gets the principal name (userid) of the user the attributes in this context describe.
     * 
     * @return principal name of the user the attributes in this context describe
     */
    public String getPrincipalName();

    /**
     * Gets the requester of the attributes.
     * 
     * @return requester of the attributes
     */
    public String getAttributeRequester();
    
    /**
     * Get the producer of the attributes.
     * 
     * @return producer of the attributes
     */
    public String getAttributeProducer();

    /**
     * Gets the attributes indexed by the attributes ID.
     * 
     * @return unfiltered attributes
     */
    public Map<String, Attribute> getAttributes();
}
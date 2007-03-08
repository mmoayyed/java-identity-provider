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

package edu.internet2.middleware.shibboleth.common.attribute.filtering;

import java.util.Map;
import edu.internet2.middleware.shibboleth.common.attribute.Attribute;

/**
 * The engine that applies attribute acceptance policies to a collection of attributes.
 */
public interface FilteringEngine {

    /**
     * Processes the given collection of attributes using the effective attribute acceptance policy for the identity
     * provider identified by its entity ID. Processing filters out attributes, or attribute values, that will not be
     * accepted from the identity provider and may optionally transform the given attributes (e.g. changing the format
     * of values, concatinating values).
     * 
     * @param filterContext contextual information for filtering attributes
     * 
     * @return the filtered attributes, attribute ID is the key, attribute object is the value
     * 
     * @throws FilteringException thrown if there is a problem retrieving or applying the attribute acceptance policy
     */
    public Map<String, Attribute> filterAttributes(FilterContext filterContext) throws FilteringException;
}
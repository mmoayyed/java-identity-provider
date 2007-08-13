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

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.profile.ProfileMessageContext;

/**
 * The engine that applies attribute acceptance policies to a collection of attributes.
 * 
 * @param <ContextType> type of request context expected by this filtering engine
 */
public interface AttributeFilteringEngine<ContextType extends ProfileMessageContext> {

    /**
     * Filters values for the given attribute, removing or allowing attributes per some implementation specific policy.
     * This filtering process may remove attributes with no values but must not add attributes or attribute values.
     * 
     * @param attributes attributes to be filtered
     * @param context attribute request context
     * 
     * @return the filtered attributes, attribute ID is the key, attribute object is the value
     * 
     * @throws AttributeFilteringException thrown if there is a problem retrieving or applying the attribute acceptance
     *             policy
     */
    public Map<String, BaseAttribute> filterAttributes(Map<String, BaseAttribute> attributes, ContextType context)
            throws AttributeFilteringException;
}
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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;

/**
 * Contextual information for performing attribute filtering.
 */
public class ShibbolethFilteringContext {

    /** The attribute request. */
    private SAMLProfileRequestContext attributeRequestContext;

    /** Attributes being filtered. */
    private Map<String, BaseAttribute> unfilteredAttributes;

    /** Retained values for a given attribute. */
    private Map<String, Collection> retainedValues;

    /**
     * Constructor.
     * 
     * @param attributes unfiltered attribute set
     * @param context attribute request context
     */
    public ShibbolethFilteringContext(Map<String, BaseAttribute> attributes, SAMLProfileRequestContext context) {
        attributeRequestContext = context;
        unfilteredAttributes = attributes;
        retainedValues = new HashMap<String, Collection>();
    }

    /**
     * Gets the context for the attribute request.
     * 
     * @return context for the attribute request
     */
    public SAMLProfileRequestContext getAttributeRequestContext() {
        return attributeRequestContext;
    }

    /**
     * Gets the attributes being filtered.
     * 
     * @return attributes being filtered
     */
    public Map<String, BaseAttribute> getUnfilteredAttributes() {
        return unfilteredAttributes;
    }

    /**
     * Gets the values, for the given attribute, that have no yet been filtered out.
     * 
     * @param attributeId attribute to retrieve the values for
     * @param prepopulate whether to pre-populate the retained value list from the unfiltered value list if there is
     *            currently no set of values retained for the given attribute
     * 
     * @return attribute values not yet filtered out, never null
     */
    public Collection getRetainedValues(String attributeId, boolean prepopulate) {
        Collection attributeValues = null;
        if (!retainedValues.containsKey(attributeId) && prepopulate) {
            if (prepopulate) {
                BaseAttribute attribute = unfilteredAttributes.get(attributeId);
                if (attribute != null) {
                    attributeValues = attribute.getValues();
                }

                retainedValues.put(attributeId, attributeValues);
            }
        } else {
            attributeValues = retainedValues.get(attributeId);
        }

        if (attributeValues == null) {
            attributeValues = new TreeSet<Object>();
        }
        return attributeValues;
    }
}
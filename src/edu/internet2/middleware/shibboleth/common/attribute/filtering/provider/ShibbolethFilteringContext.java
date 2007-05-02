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

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;

/**
 * Contextual information for performing attribute filtering.
 */
public class ShibbolethFilteringContext {

    /** The attribute request. */
    private ShibbolethAttributeRequestContext attributeRequestContext;

    /** Attributes being filtered. */
    private Map<String, Attribute> unfilteredAttributes;

    /** Retained values for a given attribute. */
    private Map<String, SortedSet> retainedValues;

    /**
     * Constructor.
     * 
     * @param attributes unfiltered attribute set
     * @param context attribute request context
     */
    public ShibbolethFilteringContext(Map<String, Attribute> attributes,
            ShibbolethAttributeRequestContext context) {
        attributeRequestContext = context;
        unfilteredAttributes = attributes;
    }

    /**
     * Gets the context for the attribute request.
     * 
     * @return context for the attribute request
     */
    public ShibbolethAttributeRequestContext getAttributeRequestContext() {
        return attributeRequestContext;
    }

    /**
     * Gets the attributes being filtered.
     * 
     * @return attributes being filtered
     */
    public Map<String, Attribute> getUnfilteredAttributes() {
        return unfilteredAttributes;
    }

    /**
     * Gets the values, for the given attribute, that have no yet been filtered out.
     * 
     * @param attributeId attribute to retreive the values for
     * 
     * @return attribtue values not yet filtered out, never null
     */
    public SortedSet getRetainedValues(String attributeId) {
        SortedSet attributeValues;
        if (!retainedValues.containsKey(attributeId)) {
            Attribute attribute = unfilteredAttributes.get(attributeId);
            if (attribute != null) {
                attributeValues = attribute.getValues();
            } else {
                attributeValues = new TreeSet<Object>();
            }

            retainedValues.put(attributeId, attributeValues);
        } else {
            attributeValues = retainedValues.get(attributeId);
        }

        return attributeValues;
    }
}
/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.filtering;

import java.util.Collection;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazyList;

/** A simple, mock implementation of {@link AttributeValueMatcher}. */
public class MockAttributeValueMatcher implements AttributeValueMatcher {

    /** ID of the attribute to which this matcher applies. */
    private String matchingAttribute;

    /** Values, of the attribute, considered to match this matcher. */
    private Collection matchingValues;

    /**
     * Sets the ID of the attribute to which this matcher applies.
     * 
     * @param id ID of the attribute to which this matcher applies
     */
    public void setMatchingAttribute(String id) {
        matchingAttribute = StringSupport.trimOrNull(id);
        Assert.isNotNull(matchingAttribute, "attribute ID can not be null or empty");
    }

    /**
     * Sets the values, of the attribute, considered to match this matcher. If null then all attribute values are
     * considered to be matching.
     * 
     * @param values values, of the attribute, considered to match this matcher
     */
    public void setMatchingValues(Collection values) {
        matchingValues = values;
    }

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(Attribute<?> attribute, AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        if (!ObjectSupport.equals(attribute.getId(), matchingAttribute)) {
            return null;
        }

        if (matchingValues == null) {
            return attribute.getValues();
        }

        LazyList<Object> values = new LazyList<Object>();
        for (Object value : attribute.getValues()) {
            if (matchingValues.contains(value)) {
                values.add(value);
            }
        }

        return values;
    }
}
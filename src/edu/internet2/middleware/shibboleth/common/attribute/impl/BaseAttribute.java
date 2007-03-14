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

package edu.internet2.middleware.shibboleth.common.attribute.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;

/**
 * Basic {@Attribute} implementation.
 * 
 * @param <ValueType> value type
 */
public class BaseAttribute<ValueType> extends Attribute<ValueType> implements Cloneable {

    /** ID of this attribute. */
    private String id;

    /** Map of attribute encoders for this attribute, keyed off of category. */
    private Map<String, AttributeEncoder> encoders;

    /** Set of values for this attribute. */
    private SortedSet<ValueType> values;

    /** Comparator for this attribute. */
    private Comparator<ValueType> comparator;

    /** Constructor. */
    public BaseAttribute() {
        encoders = new HashMap<String, AttributeEncoder>();
        values = new TreeSet<ValueType>();
    }

    /** {@inheritDoc} */
    public Map<String, AttributeEncoder> getEncoders() {
        return encoders;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Set id of this attribute.
     * 
     * @param newID new ID
     */
    public void setId(String newID) {
        id = newID;
    }

    /** {@inheritDoc} */
    public Comparator<ValueType> getValueComparator() {
        return comparator;
    }

    /**
     * Set value comparator for this attribute.
     * 
     * @param newComparator new value comparator
     */
    public void setValueComparator(Comparator<ValueType> newComparator) {
        comparator = newComparator;
    }

    /** {@inheritDoc} */
    public SortedSet<ValueType> getValues() {
        return values;
    }

    /**
     * Replace the current set of values with the given set.
     * 
     * @param newValues new values to replace existing ones
     */
    public void setValues(SortedSet<ValueType> newValues) {
        values = newValues;
    }

    /** {@inheritDoc} */
    public BaseAttribute<ValueType> clone() {
        BaseAttribute<ValueType> newAttribute = new BaseAttribute<ValueType>();

        newAttribute.setId(this.getId());

        newAttribute.setValueComparator(this.getValueComparator());

        for (ValueType value : this.getValues()) {
            newAttribute.getValues().add(value);
        }

        for (String category : getEncoders().keySet()) {
            newAttribute.getEncoders().put(category, getEncoders().get(category));
        }

        return newAttribute;
    }

    /** {@inheritDoc} */
    public AttributeEncoder getEncoderByCategory(String category) {
        return encoders.get(category);
    }

}
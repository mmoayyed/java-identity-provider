/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.attribute;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazyList;
import org.opensaml.util.collections.LazyMap;

/**
 * Each attribute represents one piece of information about a user and has associated encoders used to turn that
 * information in to protocol-specific formats.
 * 
 * Instances of {@link Attribute} are compared using their IDs. That is, two attributes are considered the same if they
 * have the same ID, regardless of whether their display names, descriptions, values, or encoders are the same.
 * 
 * @param <ValueType> the object type of the values for this attribute
 */
@ThreadSafe
public class Attribute<ValueType> implements Comparable<Attribute> {

    /** ID of this attribute. */
    private final String id;

    /** Localized human intelligible attribute names. */
    private Map<Locale, String> displayNames;

    /** Localized human readable descriptions of attribute. */
    private Map<Locale, String> displayDescriptions;

    /** Values for this attribute. */
    private Collection<ValueType> values;

    /** Encoders that may be used to encode this attribute. */
    private List<AttributeEncoder<?>> encoders;

    /**
     * Constructor.
     * 
     * @param attributeId unique identifier of the attribute
     */
    public Attribute(final String attributeId) {
        id = StringSupport.trimOrNull(attributeId);
        Assert.isNotNull(id, "Attribute ID may not be null");

        values = new LazyList<ValueType>();
        displayNames = new LazyMap<Locale, String>();
        displayDescriptions = new LazyMap<Locale, String>();
        encoders = new LazyList<AttributeEncoder<?>>();
    }

    /**
     * Gets the localized human readable description of attribute.
     * 
     * @return human readable description of attribute, never null
     */
    public Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    /**
     * Gets the localized human readable name of the attribute.
     * 
     * @return human readable name of the attribute, never null
     */
    public Map<Locale, String> getDisplayNames() {
        return displayNames;
    }

    /**
     * Gets the list of attribute encoders usable with this attribute.
     * 
     * @return attribute encoders usable with this attribute, never null
     */
    public List<AttributeEncoder<?>> getEncoders() {
        return encoders;
    }

    /**
     * Gets the unique ID of the attribute. This ID need not be at all related to any protocol-specific attribute
     * identifiers.
     * 
     * @return unique ID of the attribute
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the unordered list of values of the attribute.
     * 
     * @return values of the attribute, never null
     */
    public Collection<ValueType> getValues() {
        return values;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ObjectSupport.hashCode(id);
        result = prime * result + ObjectSupport.hashCode(values);
        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Attribute)) {
            return false;
        }

        Attribute other = (Attribute) obj;
        return ObjectSupport.equals(id, other.getId()) && values.equals(other.getValues());
    }

    /** {@inheritDoc} */
    public int compareTo(Attribute other) {
        return getId().compareTo(other.getId());
    }

    /** {@inheritDoc} */
    public String toString() {
        return "Attribute:" + getId();
    }
}
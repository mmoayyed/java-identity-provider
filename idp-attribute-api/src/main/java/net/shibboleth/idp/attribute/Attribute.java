/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazyList;
import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;

/**
 * Each attribute represents one piece of information about a user and has associated encoders used to turn that
 * information in to protocol-specific formats.
 * 
 * Instances of {@link Attribute} are compared using their IDs. That is, two attributes are considered the same if they
 * have the same ID, regardless of whether their display names, descriptions, values, or encoders are the same.
 * 
 * @param <ValueType> the object type of the values for this attribute
 */
@NotThreadSafe
public class Attribute<ValueType> implements Comparable<Attribute>, Cloneable {

    /** ID of this attribute. */
    private final String id;

    /** Localized human intelligible attribute names. */
    private Map<Locale, String> displayNames;

    /** Localized human readable descriptions of attribute. */
    private Map<Locale, String> displayDescriptions;

    /** Values for this attribute. */
    private Collection<ValueType> values;

    /** Encoders that may be used to encode this attribute. */
    private Set<AttributeEncoder<?>> encoders;

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
        encoders = new LazySet<AttributeEncoder<?>>();
    }

    /**
     * Gets the unique ID of the attribute. This ID need not be related to any protocol-specific attribute identifiers.
     * 
     * @return unique ID of the attribute
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the unmodifiable collection of localized human readable name of the attribute. This collection never
     * contains entries whose key or value are null.
     * 
     * @return human readable name of the attribute, never null
     */
    public Map<Locale, String> getDisplayNames() {
        return Collections.unmodifiableMap(displayNames);
    }

    /**
     * Replaces the existing display names for this attribute with the given ones. If the given collection is null the
     * existing display names are cleared and no new ones are added.
     * 
     * @param newNames the new names for this attribute, may be null
     */
    public void setDisplayNames(final Map<Locale, String> newNames) {
        displayNames.clear();

        if (newNames == null) {
            return;
        }

        for (Entry<Locale, String> entry : newNames.entrySet()) {
            addDisplayName(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds a display name to this attribute.
     * 
     * @param locale local of the display name, never null
     * @param name the display name, never null or empty
     * 
     * @return the value that was replaced for the given locale or null if no value was replaced
     */
    public String addDisplayName(final Locale locale, final String name) {
        Assert.isNotNull(locale, "Display name locale can not be null");

        final String trimmedName = StringSupport.trimOrNull(name);
        Assert.isNotNull(trimmedName, "Display name can not be null or empty");

        return displayNames.put(locale, trimmedName);
    }

    /**
     * Removes the display name for the given locale.
     * 
     * @param locale locale whose display name will be removed, may be null
     * 
     * @return the removed value or null if no value existed for the given locale
     */
    public String removeDisplayName(final Locale locale) {
        if (locale == null) {
            return null;
        }

        return displayNames.remove(locale);
    }

    /**
     * Gets the unmodifiable localized human readable description of attribute. This collection never contains entries
     * whose key or value are null.
     * 
     * @return human readable description of attribute, never null
     */
    public Map<Locale, String> getDisplayDescriptions() {
        return Collections.unmodifiableMap(displayDescriptions);
    }

    /**
     * Replaces the existing display descriptions for this attribute with the given ones. If the given collection is
     * null the existing display descriptions are cleared and no new ones are added.
     * 
     * @param newDescriptions the new descriptions for this attribute, may be null
     */
    public void setDisplayDescriptions(final Map<Locale, String> newDescriptions) {
        displayDescriptions.clear();

        if (newDescriptions == null) {
            return;
        }

        for (Entry<Locale, String> entry : newDescriptions.entrySet()) {
            addDisplayDescription(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds a display description to this attribute.
     * 
     * @param locale local of the display name, never null
     * @param description the display description, never null or empty
     * 
     * @return the value that was replaced for the given locale or null if no value was replaced
     */
    public String addDisplayDescription(final Locale locale, final String description) {
        Assert.isNotNull(locale, "Display description locale can not be null");

        final String trimmedDescription = StringSupport.trimOrNull(description);
        Assert.isNotNull(trimmedDescription, "Display description can not be null or empty");

        return displayDescriptions.put(locale, trimmedDescription);
    }

    /**
     * Removes the display description for the given locale.
     * 
     * @param locale locale whose display description will be removed, may be null
     * 
     * @return the removed value or null if no value existed for the given locale
     */
    public String removeDisplayDescription(final Locale locale) {
        if (locale == null) {
            return null;
        }

        return displayDescriptions.remove(locale);
    }

    /**
     * Gets the unordered, unmodifiable collection of values of the attribute. This collection never contains null
     * values.
     * 
     * @return values of the attribute, never null
     */
    public Collection<ValueType> getValues() {
        return Collections.unmodifiableCollection(values);
    }

    /**
     * Replaces the existing values for this attribute with the given values. If the given collection is null the
     * existing values are cleared and no new values are added.
     * 
     * @param newValues the new values for this attribute, may be null
     */
    public void setValues(final Collection<ValueType> newValues) {
        CollectionSupport.nonNullReplace(newValues, values);
    }

    /**
     * Adds a value to this attribute.
     * 
     * @param value value to be added, may be null but null values are discarded
     * 
     * @return true if a value was added, false otherwise
     */
    public boolean addValue(final ValueType value) {
        return CollectionSupport.nonNullAdd(values, value);
    }

    /**
     * Removes a value from this attribute.
     * 
     * @param value value to be removed, may be null
     * 
     * @return true if a value was removed, false otherwise
     */
    public boolean removeValue(final Object value) {
        return CollectionSupport.nonNullRemove(values, value);
    }

    /**
     * Gets the unmodifiable list of attribute encoders usable with this attribute. This collection never contains null
     * values.
     * 
     * @return attribute encoders usable with this attribute, never null
     */
    public Set<AttributeEncoder<?>> getEncoders() {
        return Collections.unmodifiableSet(encoders);
    }

    /**
     * Replaces the existing encoders for this attribute with the given encoders. If the given collection is null the
     * existing encoders are cleared and no new ones are added.
     * 
     * @param newEncoders the new encoders for this attribute, may be null
     */
    public void setEncoders(final Set<AttributeEncoder<?>> newEncoders) {
        CollectionSupport.nonNullReplace(newEncoders, encoders);
    }

    /**
     * Adds an encoder for this attribute.
     * 
     * @param encoder the encode to be added, may be null but null values will be discarded
     * 
     * @return true if an encoder was added, false otherwise
     */
    public boolean addEncoder(final AttributeEncoder<?> encoder) {
        return CollectionSupport.nonNullAdd(encoders, encoder);
    }

    /**
     * Removes an encoder from this attribute.
     * 
     * @param encoder encoder to be removed, may be null
     * 
     * @return true if an encoder was removed, false otherwise
     */
    public boolean removeEncoder(final AttributeEncoder<?> encoder) {
        return CollectionSupport.nonNullRemove(encoders, encoder);
    }

    /** {@inheritDoc} */
    public int compareTo(final Attribute other) {
        return getId().compareTo(other.getId());
    }

    /**
     * Clones an attribute. The clone will contains defensive copies of this objects display descriptions and names,
     * encoders, and values.  The elements of each collection, however, are not themselves cloned.
     * 
     * {@inheritDoc}
     */
    public Attribute clone() {
        try {
            Attribute clone = (Attribute) super.clone();
            clone.setDisplayDescriptions(getDisplayDescriptions());
            clone.setDisplayNames(getDisplayNames());
            clone.setEncoders(getEncoders());
            clone.setValues(getValues());
            return (Attribute) super.clone();
        } catch (CloneNotSupportedException e) {
            // nothing to do, all Attribute's must support clone
            return null;
        }
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
    public boolean equals(final Object obj) {
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
        return ObjectSupport.equals(id, other.getId());
    }

    /** {@inheritDoc} */
    public String toString() {
        return "Attribute:" + getId();
    }
}
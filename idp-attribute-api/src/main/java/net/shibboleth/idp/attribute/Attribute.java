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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.Constraints;
import com.google.common.collect.ImmutableMap;

/**
 * Each attribute represents one piece of information about a user and has associated encoders used to turn that
 * information in to protocol-specific formats.
 * 
 * Instances of {@link Attribute} are compared using their IDs. That is, two attributes are considered the same if they
 * have the same ID, regardless of whether their display names, descriptions, values, or encoders are the same.
 */
@NotThreadSafe
public class Attribute implements Comparable<Attribute>, Cloneable {

    /** ID of this attribute. */
    private final String id;

    /** Localized human intelligible attribute names. */
    private Map<Locale, String> displayNames;

    /** Localized human readable descriptions of attribute. */
    private Map<Locale, String> displayDescriptions;

    /** Values for this attribute. */
    private Set<AttributeValue> values;

    /** Encoders that may be used to encode this attribute. */
    private Set<AttributeEncoder<?>> encoders;

    /**
     * Constructor.
     * 
     * @param attributeId unique identifier of the attribute
     */
    public Attribute(@Nonnull @NotEmpty final String attributeId) {
        id = Assert.isNotNull(StringSupport.trimOrNull(attributeId), "Attribute ID may not be null");

        displayNames = Collections.emptyMap();
        displayDescriptions = Collections.emptyMap();

        values = Constraints.constrainedSet(new HashSet<AttributeValue>(), Constraints.notNull());
        encoders = Constraints.constrainedSet(new HashSet<AttributeEncoder<?>>(), Constraints.notNull());
    }

    /**
     * Gets the unique ID of the attribute. This ID need not be related to any protocol-specific attribute identifiers.
     * 
     * @return unique ID of the attribute
     */
    @Nonnull @NotEmpty public String getId() {
        return id;
    }

    /**
     * Gets the localized human readable name of the attribute.
     * 
     * @return human readable name of the attribute
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<Locale, String> getDisplayNames() {
        return displayNames;
    }

    /**
     * Replaces the existing display names for this attribute with the given ones.
     * 
     * @param newNames the new names for this attribute
     */
    public void setDisplayNames(@Nullable @NullableElements final Map<Locale, String> newNames) {
        HashMap<Locale, String> checkedNames = new HashMap<Locale, String>();
        String trimmedName;
        for (Entry<Locale, String> entry : newNames.entrySet()) {
            trimmedName = StringSupport.trimOrNull(entry.getValue());
            if (trimmedName != null) {
                checkedNames.put(entry.getKey(), trimmedName);
            }
        }

        displayNames = ImmutableMap.copyOf(checkedNames);
    }

    /**
     * Gets the localized human readable description of attribute.
     * 
     * @return human readable description of attribute
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    /**
     * Replaces the existing display descriptions for this attribute with the given ones.
     * 
     * @param newDescriptions the new descriptions for this attribute
     */
    public void setDisplayDescriptions(@Nullable @NullableElements final Map<Locale, String> newDescriptions) {
        HashMap<Locale, String> checkedDescriptions = new HashMap<Locale, String>();
        String trimmedDescription;
        for (Entry<Locale, String> entry : newDescriptions.entrySet()) {
            trimmedDescription = StringSupport.trimOrNull(entry.getValue());
            if (trimmedDescription != null) {
                checkedDescriptions.put(entry.getKey(), trimmedDescription);
            }
        }

        displayDescriptions = ImmutableMap.copyOf(checkedDescriptions);
    }

    /**
     * Gets the unordered, unmodifiable collection of values of the attribute.
     * 
     * @return values of the attribute
     */
    @Nonnull @NonnullElements public Set<AttributeValue> getValues() {
        return values;
    }

    /**
     * Replaces the existing values for this attribute with the given values.
     * 
     * @param newValues the new values for this attribute
     */
    public void setValues(@Nullable @NullableElements final Collection<AttributeValue> newValues) {
        Set<AttributeValue> checkedValues =
                Constraints.constrainedSet(new HashSet<AttributeValue>(), Constraints.notNull());
        CollectionSupport.addIf(checkedValues, newValues, Predicates.<AttributeValue> notNull());
        values = checkedValues;
    }

    /**
     * Gets the list of attribute encoders usable with this attribute.
     * 
     * @return attribute encoders usable with this attribute
     */
    @Nonnull @NonnullElements public Set<AttributeEncoder<?>> getEncoders() {
        return encoders;
    }

    /**
     * Replaces the existing encoders for this attribute with the given encoders.
     * 
     * @param newEncoders the new encoders for this attribute
     */
    public void setEncoders(@Nullable @NullableElements final Collection<AttributeEncoder<?>> newEncoders) {
        Set<AttributeEncoder<?>> checkedEncoders =
                Constraints.constrainedSet(new HashSet<AttributeEncoder<?>>(), Constraints.notNull());
        CollectionSupport.addIf(checkedEncoders, newEncoders, Predicates.notNull());
        encoders = checkedEncoders;
    }

    /** {@inheritDoc} */
    public int compareTo(final Attribute other) {
        return getId().compareTo(other.getId());
    }

    /**
     * Clones an attribute. The clone will contains defensive copies of this objects display descriptions and names,
     * encoders, and values. The elements of each collection, however, are not themselves cloned.
     * 
     * {@inheritDoc}
     */
    @Nonnull public Attribute clone() {
        try {
            Attribute clone = (Attribute) super.clone();
            clone.setDisplayDescriptions(getDisplayDescriptions());
            clone.setDisplayNames(getDisplayNames());
            clone.setEncoders(getEncoders());
            // TODO(lajoie): should we clone the values?
            clone.setValues(getValues());
            return clone;
        } catch (CloneNotSupportedException e) {
            // nothing to do, all Attribute's must support clone
            return null;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(id, values);
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
        return Objects.equal(id, other.getId());
    }

    /** {@inheritDoc} */
    @Nonnull public String toString() {
        return Objects.toStringHelper(this).add("id", getId()).add("displayNames", displayNames)
                .add("displayDescriptions", displayDescriptions).add("encoders", encoders).add("values", values)
                .toString();
    }
}
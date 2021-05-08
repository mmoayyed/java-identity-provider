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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Each attribute represents one piece of information about a user and has associated encoders used to turn that
 * information in to protocol-specific formats.
 * 
 * Instances of {@link IdPAttribute} are compared using their IDs. That is, two attributes are considered the same if
 * they have the same ID, regardless of whether their display names, descriptions, values, or encoders are the same.
 */
@NotThreadSafe
public class IdPAttribute implements Comparable<IdPAttribute>, Cloneable {
    
    /** Value for testing illegal name. */
    private static final Predicate<String> SPACE_CONTAINING = Pattern.compile("\\S*").asMatchPredicate();

    /** Logger - static. */
    @Nonnull private static final Logger LOG = LoggerFactory.getLogger(IdPAttribute.class);
    
    /** Helper Function for map manipulation. */
    @Nonnull private static Function<Entry<Locale, String>, Entry<Locale, String>> filterEntry =
            new Function<>() {
        public Entry<Locale, String> apply(final Entry<Locale, String> t) {
            return new Entry<>() {
                private String val = Constraint.isNotNull(StringSupport.trimOrNull(t.getValue()),
                        "Values must not be null");
                private Locale key = Constraint.isNotNull(t.getKey(), "Locale must not be null");
                public Locale getKey() {
                    return key;
                }

                public String getValue() {
                    return val;
                }

                public String setValue(final String value) {
                    throw new ConstraintViolationException("Unmodifable map modified");
                }
            };
        };
    };

    /** ID of this attribute. */
    @Nonnull private final String id;

    /** Localized human intelligible attribute names. */
    @Nonnull private Map<Locale, String> displayNames;

    /** Localized human readable descriptions of attribute. */
    @Nonnull private Map<Locale, String> displayDescriptions;

    /** Values for this attribute. */
    @Nonnull private List<IdPAttributeValue> values;

    /**
     * Constructor.
     * 
     * @param attributeId unique identifier of the attribute
     */
    public IdPAttribute(@Nonnull @NotEmpty @ParameterName(name="attributeId") final String attributeId) {

        id = Constraint.isNotNull(StringSupport.trimOrNull(attributeId), "Attribute ID may not be null");
        Constraint.isFalse(isInvalidId(id), "Attribute ID must not have spaces");
        
        if (isDeprecatedId(id)) {
            // Issue a deprecation warning once but log more in debug and trace to help fixing
            DeprecationSupport.warnOnce(ObjectType.CONFIGURATION,
                    "IdPAttribute",
                    "IdPAttribute id with special characters (\'\"%{})", null);
            LOG.debug("{} - deprecated character in attribute name", id);
            LOG.trace("Stack", new Exception("Stack Trace, not a thrown exception:"));
        }
        displayNames = Collections.emptyMap();
        displayDescriptions = Collections.emptyMap();

        values = Collections.emptyList();
    }

    /** Centralized method to police deprecated Identifiers.
     * @param id what to test
     * @return whether the name is currently deprecated. 
     */
    public static boolean isDeprecatedId(@Nonnull @NotEmpty final String id) {
        return id.indexOf('\'') >= 0 ||
                id.indexOf('%') >= 0 ||
                id.indexOf('{') >= 0 ||
                id.indexOf('}') >= 0;
    }
    
    /** Centralized method to police invalid Identifiers.
     * @param id what to test
     * @return whether the name is disallowed. 
     */
    public static boolean isInvalidId(@Nullable final String id) {
       return null == StringSupport.trimOrNull(id) || !SPACE_CONTAINING.test(id);
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
     * @deprecated These values should be calculated at point of use
     * 
     * @return human readable name of the attribute
     */
    @Deprecated(forRemoval = true, since = "4.2") @Nonnull @NonnullElements @Unmodifiable
    public Map<Locale, String> getDisplayNames() {
        return displayNames;
    }

    /**
     * Process input to {@link #setDisplayNames(Map)} and {{@link #setDisplayDescriptions(Map)} to strip out null input,
     * null keys, and null values.
     * 
     * @param inputMap the input map.
     * @return the unmodifiable, non null-containing output.
     */
    @SuppressWarnings("unchecked")
    @Nonnull @Unmodifiable private final Map<Locale, String> checkedNamesFrom(@Nonnull @NonnullElements
            final Map<Locale, String> inputMap) {
        
        return Map.ofEntries(inputMap.
                entrySet().
                stream().
                map(filterEntry).
                toArray(Map.Entry[]::new));
    }

    /**
     * Replaces the existing display names for this attribute with the given ones.
     * @deprecated These values should be calculated at point of use
     * 
     * @param newNames the new names for this attribute
     */
    @Deprecated(forRemoval = true, since = "4.2")
    public void setDisplayNames(@Nonnull @NonnullElements final Map<Locale, String> newNames) {
        DeprecationSupport.warnOnce(ObjectType.METHOD, "setDisplayNames", null, null);
        displayNames = checkedNamesFrom(
                Constraint.isNotNull(newNames, "Display Names should not be null"));
    }

    /**
     * Gets the localized human readable description of attribute.
     * @deprecated These values should be calculated at point of use
     * 
     * @return human readable description of attribute
     */
    @Deprecated(forRemoval = true, since = "4.2")
    @Nonnull @NonnullElements @Unmodifiable public Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    /**
     * Replaces the existing display descriptions for this attribute with the given ones.
     * @deprecated These values should be calculated at point of use
     * 
     * @param newDescriptions the new descriptions for this attribute
     */
    @Deprecated(forRemoval = true, since = "4.2")
    public void setDisplayDescriptions(@Nonnull @NonnullElements final Map<Locale, String> newDescriptions) {
        DeprecationSupport.warnOnce(ObjectType.METHOD, "setDisplayDescriptions", null, null);
        displayDescriptions = checkedNamesFrom(
                Constraint.isNotNull(newDescriptions, "Display Descriptions should not be null"));
    }

    /**
     * Get the unmodifiable ordered collection of values of the attribute.
     * 
     * @return values of the attribute
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<IdPAttributeValue> getValues() {
        return values;
    }

    /**
     * Replaces the existing values for this attribute with the given values.
     * 
     * @param newValues the new values for this attribute
     */
    public void setValues(@Nullable @NullableElements final Collection<IdPAttributeValue> newValues) {
        if (newValues != null) {
            if (!(newValues instanceof List)) {
                DeprecationSupport.warnOnce(ObjectType.METHOD, "Passing a Collection to IdpAttribute#setValues()",
                        null, "List");
            }
            values = newValues.stream().
                     map(e -> e==null? new EmptyAttributeValue(EmptyType.NULL_VALUE) :e).
                     collect(Collectors.toUnmodifiableList());
        } else {
            values = Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final IdPAttribute other) {
        return getId().compareTo(other.getId());
    }

    /**
     * Clones an attribute. The clone will contains defensive copies of this objects display descriptions and names,
     * encoders, and values. The elements of each collection, however, are not themselves cloned.
     * 
     * {@inheritDoc}
     */
    @Override
    @Nonnull public IdPAttribute clone() throws CloneNotSupportedException {
        final IdPAttribute clone = (IdPAttribute) super.clone();
        clone.setDisplayDescriptions(getDisplayDescriptions());
        clone.setDisplayNames(getDisplayNames());
        clone.setValues(getValues());
        return clone;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(id, values);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof IdPAttribute)) {
            return false;
        }

        final IdPAttribute other = (IdPAttribute) obj;
        return java.util.Objects.equals(id, other.getId());
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public String toString() {
        return MoreObjects.toStringHelper(this).add("id", getId()).add("displayNames", displayNames)
                .add("displayDescriptions", displayDescriptions).add("values", values)
                .toString();
    }
    
}

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

package net.shibboleth.idp.attribute.filtering;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.collect.Constraints;
import com.google.common.collect.MapConstraints;

/** Context used to collect data as attributes are filtered. */
@NotThreadSafe
public final class AttributeFilterContext extends BaseContext {

    /** Attributes which are to be filtered. */
    private Map<String, Attribute> prefilteredAttributes;

    /** Values, for a given attribute, that are permitted to be released. */
    private Map<String, Set<AttributeValue>> permittedValues;

    /** Values, for a given attribute, that are not permitted to be released. */
    private Map<String, Set<AttributeValue>> deniedValues;

    /** Attributes which have been filtered. */
    private Map<String, Attribute> filteredAttributes;

    /** Constructor. */
    public AttributeFilterContext() {
        prefilteredAttributes =
                MapConstraints.constrainedMap(new HashMap<String, Attribute>(), MapConstraints.notNull());
        permittedValues =
                MapConstraints.constrainedMap(new HashMap<String, Set<AttributeValue>>(), MapConstraints.notNull());
        deniedValues =
                MapConstraints.constrainedMap(new HashMap<String, Set<AttributeValue>>(), MapConstraints.notNull());
        filteredAttributes = MapConstraints.constrainedMap(new HashMap<String, Attribute>(), MapConstraints.notNull());
    }

    /**
     * Gets the collection of attributes that are to be filtered, indexed by attribute ID.
     * 
     * @return attributes to be filtered
     */
    @Nonnull @NonnullElements public Map<String, Attribute> getPrefilteredAttributes() {
        return prefilteredAttributes;
    }

    /**
     * Sets the attributes which are to be filtered.
     * 
     * @param attributes attributes which are to be filtered
     */
    public void setPrefilteredAttributes(@Nullable @NullableElements final Collection<Attribute> attributes) {
        Map<String, Attribute> checkedAttributes =
                MapConstraints.constrainedMap(new HashMap<String, Attribute>(), MapConstraints.notNull());

        if (attributes != null) {
            for (Attribute attribute : attributes) {
                if (attribute != null) {
                    checkedAttributes.put(attribute.getId(), attribute);
                }
            }
        }

        prefilteredAttributes = checkedAttributes;
    }

    /**
     * Gets the collection of attribute values, indexed by ID, that are permitted to be released.
     * 
     * @return collection of attribute values, indexed by ID, that are permitted to be released,
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, Set<AttributeValue>> getPermittedAttributeValues() {
        return Collections.unmodifiableMap(permittedValues);
    }

    /**
     * Adds a collection of attribute values that are permitted to be released. Attempting to add values for an
     * attribute that is not a member of {@link #getPrefilteredAttributes()} will result in an
     * {@link IllegalArgumentException}. Attempting to add an attribute value that is not a member of
     * {@link Attribute#getValues()} will result in an {@link IllegalArgumentException}.
     * 
     * @param attributeId ID of the attribute whose values are permitted to be released
     * @param attributeValues values for the attribute that are permitted to be released
     */
    public void addPermittedAttributeValues(@Nonnull @NotEmpty String attributeId,
            @Nullable @NullableElements Collection<? extends AttributeValue> attributeValues) {
        String trimmedAttributeId =
                Constraint.isNotNull(StringSupport.trimOrNull(attributeId), "Attribute ID can not be null or empty");
        Constraint.isTrue(prefilteredAttributes.containsKey(trimmedAttributeId), "no attribute with ID "
                + trimmedAttributeId + " exists in the pre-filtered attribute set");

        if (attributeValues == null || attributeValues.isEmpty()) {
            return;
        }

        Set<AttributeValue> permittedAttributeValues = permittedValues.get(trimmedAttributeId);
        if (permittedAttributeValues == null) {
            permittedAttributeValues = Constraints.constrainedSet(new HashSet<AttributeValue>(), Constraints.notNull());
            permittedValues.put(trimmedAttributeId, permittedAttributeValues);
        }

        for (AttributeValue value : attributeValues) {
            if (value != null) {
                if (!prefilteredAttributes.get(trimmedAttributeId).getValues().contains(value)) {
                    throw new IllegalArgumentException("permitted value is not a current value of attribute "
                            + trimmedAttributeId);
                }

                if (!permittedAttributeValues.contains(value)) {
                    permittedAttributeValues.add(value);
                }
            }
        }
    }

    /**
     * Gets the unmodifiable collection of attribute values, indexed by ID, that are not permitted to be released.
     * 
     * @return collection of attribute values, indexed by ID, that are not permitted to be released
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, Set<AttributeValue>> getDeniedAttributeValues() {
        return Collections.unmodifiableMap(deniedValues);
    }

    /**
     * Adds a collection of attribute values that are not permitted to be released. Attempting to add values for an
     * attribute that is not a member of {@link #getPrefilteredAttributes()} will result in an
     * {@link IllegalArgumentException}. Attempting to add an attribute value that is not a member of
     * {@link Attribute#getValues()} will result in an {@link IllegalArgumentException}.
     * 
     * @param attributeId ID of the attribute whose values are not permitted to be released
     * @param attributeValues values for the attribute that are not permitted to be released
     */
    public void addDeniedAttributeValues(@Nonnull @NotEmpty String attributeId,
            @Nullable @NullableElements Collection<? extends AttributeValue> attributeValues) {
        String trimmedAttributeId =
                Constraint.isNotNull(StringSupport.trimOrNull(attributeId), "Attribute ID can not be null or empty");
        Constraint.isTrue(prefilteredAttributes.containsKey(trimmedAttributeId), "No attribute with ID "
                + trimmedAttributeId + " exists in the pre-filtered attribute set");

        if (attributeValues == null || attributeValues.isEmpty()) {
            return;
        }

        Set<AttributeValue> deniedAttributeValues = deniedValues.get(trimmedAttributeId);
        if (deniedAttributeValues == null) {
            deniedAttributeValues = Constraints.constrainedSet(new HashSet<AttributeValue>(), Constraints.notNull());
            deniedValues.put(trimmedAttributeId, deniedAttributeValues);
        }

        for (AttributeValue value : attributeValues) {
            if (value != null) {
                if (!prefilteredAttributes.get(trimmedAttributeId).getValues().contains(value)) {
                    throw new IllegalArgumentException("denied value is not a current value of attribute "
                            + trimmedAttributeId);
                }

                if (!deniedAttributeValues.contains(value)) {
                    deniedAttributeValues.add(value);
                }
            }
        }
    }

    /**
     * Gets the collection of attributes, indexed by ID, left after the filtering process has run.
     * 
     * @return attributes left after the filtering process has run
     */
    @Nonnull @NonnullElements public Map<String, Attribute> getFilteredAttributes() {
        return filteredAttributes;
    }

    /**
     * Sets the attributes that have been filtered.
     * 
     * @param attributes attributes that have been filtered
     */
    public void setFilteredAttributes(@Nullable @NullableElements final Collection<Attribute> attributes) {
        Map<String, Attribute> checkedAttributes =
                MapConstraints.constrainedMap(new HashMap<String, Attribute>(), MapConstraints.notNull());

        if (attributes != null) {
            for (Attribute attribute : attributes) {
                if (attribute != null) {
                    checkedAttributes.put(attribute.getId(), attribute);
                }
            }
        }

        filteredAttributes = checkedAttributes;
    }
}
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

package net.shibboleth.idp.attribute.filtering;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazyList;
import org.opensaml.util.collections.LazyMap;

/** Context used to collect data as attributes are filtered. */
@NotThreadSafe
public final class AttributeFilterContext implements Subcontext {

    /** Context which acts as the owner or parent of this context. */
    private final SubcontextContainer parentContext;

    /** Attributes which are to be filtered. */
    private Map<String, Attribute<?>> prefilteredAttributes;

    /** Values, for a given attribute, that are permitted to be released. */
    private Map<String, Collection<?>> permittedValues;

    /** Values, for a given attribute, that are not permitted to be released. */
    private Map<String, Collection<?>> deniedValues;

    /** Attributes which have been filtered. */
    private Map<String, Attribute<?>> filteredAttributes;

    /**
     * Constructor.
     * 
     * @param parent the parent of this context
     */
    public AttributeFilterContext(final SubcontextContainer parent) {
        if (parent != null) {
            parentContext = parent;
            parent.addSubcontext(this);
        } else {
            parentContext = null;
        }

        prefilteredAttributes = new LazyMap<String, Attribute<?>>();
        permittedValues = new LazyMap<String, Collection<?>>();
        deniedValues = new LazyMap<String, Collection<?>>();
        filteredAttributes = new LazyMap<String, Attribute<?>>();
    }

    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return parentContext;
    }

    /**
     * Gets the unmodifiable collection of attributes that are to be filtered, indexed by attribute ID. The returned
     * collection is never null nor does it contain any null keys or values.
     * 
     * @return attributes to be filtered, never null
     */
    public Map<String, Attribute<?>> getPrefilteredAttributes() {
        return Collections.unmodifiableMap(prefilteredAttributes);
    }

    /**
     * Sets the attributes which are to be filtered.
     * 
     * @param attributes attributes which are to be filtered, may be or contain null
     */
    public void setPrefilteredAttributes(final Collection<Attribute<?>> attributes) {
        prefilteredAttributes.clear();

        if (attributes != null) {
            for (Attribute<?> attribute : attributes) {
                addPrefilteredAttribute(attribute);
            }
        }
    }

    /**
     * Adds an attribute to be filtered.
     * 
     * @param attribute attribute to be filtered, may be null
     * 
     * @return the attribute replaced by the newly added attribute, or null if no attribute was replaced
     */
    public Attribute<?> addPrefilteredAttribute(final Attribute<?> attribute) {
        if (attribute == null) {
            return null;
        }

        return prefilteredAttributes.put(attribute.getId(), attribute);
    }

    /**
     * Removes an attribute to be filtered.
     * 
     * @param attributeId ID of the attribute to be removed
     * 
     * @return the attribute that was removed or null if no attribute with the given identifier was to be filtered
     */
    public Attribute<?> removePrefilteredAttribute(final String attributeId) {
        final String trimmedId = StringSupport.trimOrNull(attributeId);
        if (trimmedId == null) {
            return null;
        }

        return prefilteredAttributes.remove(trimmedId);
    }

    /**
     * Gets the unmodifiable collection of attribute values, indexed by ID, that are permitted to be released.
     * 
     * @return collection of attribute values, indexed by ID, that are permitted to be released, never null
     */
    public Map<String, Collection<?>> getPermittedAttributeValues() {
        return Collections.unmodifiableMap(permittedValues);
    }

    /**
     * Adds a collection of attribute values that are permitted to be released. Attempting to add values for an
     * attribute that is not a member of {@link #getPrefilteredAttributes()} will result in an
     * {@link IllegalArgumentException}. Attempting to add an attribute value that is not a member of
     * {@link Attribute#getValues()} will result in an {@link IllegalArgumentException}.
     * 
     * @param attributeId ID of the attribute whose values are permitted to be released, can not be null or empty
     * @param attributeValues values for the attribute that are permitted to be released, may be null or empty
     */
    public void addPermittedAttributeValues(String attributeId, Collection<?> attributeValues) {
        String trimmedAttributeId = StringSupport.trimOrNull(attributeId);
        Assert.isNotNull(trimmedAttributeId, "Attribute ID can not be null or empty");
        Assert.isTrue(prefilteredAttributes.containsKey(trimmedAttributeId), "no attribute with ID "
                + trimmedAttributeId + " exsists in the pre-filtered attribute set");

        if (attributeValues == null || attributeValues.isEmpty()) {
            return;
        }

        Collection permittedAttributeValues = permittedValues.get(trimmedAttributeId);
        if (permittedAttributeValues == null) {
            permittedAttributeValues = new LazyList();
            permittedValues.put(trimmedAttributeId, permittedAttributeValues);
        }

        for (Object value : attributeValues) {
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
     * @return collection of attribute values, indexed by ID, that are not permitted to be released, never null
     */
    public Map<String, Collection<?>> getDeniedAttributeValues() {
        return Collections.unmodifiableMap(deniedValues);
    }

    /**
     * Adds a collection of attribute values that are not permitted to be released. Attempting to add values for an
     * attribute that is not a member of {@link #getPrefilteredAttributes()} will result in an
     * {@link IllegalArgumentException}. Attempting to add an attribute value that is not a member of
     * {@link Attribute#getValues()} will result in an {@link IllegalArgumentException}.
     * 
     * @param attributeId ID of the attribute whose values are not permitted to be released, can not be null or empty
     * @param attributeValues values for the attribute that are not permitted to be released, may be null or empty
     */
    public void addDeniedAttributeValues(String attributeId, Collection<?> attributeValues) {
        String trimmedAttributeId = StringSupport.trimOrNull(attributeId);
        Assert.isNotNull(trimmedAttributeId, "Attribute ID can not be null or empty");
        Assert.isTrue(prefilteredAttributes.containsKey(trimmedAttributeId), "no attribute with ID "
                + trimmedAttributeId + " exsists in the pre-filtered attribute set");

        if (attributeValues == null || attributeValues.isEmpty()) {
            return;
        }

        Collection deniedAttributeValues = deniedValues.get(trimmedAttributeId);
        if (deniedAttributeValues == null) {
            deniedAttributeValues = new LazyList();
            deniedValues.put(trimmedAttributeId, deniedAttributeValues);
        }

        for (Object value : attributeValues) {
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
     * Gets the unmodifiable collection of attributes, indexed by ID, left after the filtering process has run. The
     * returned collection is never null nor does it contain any null keys or values.
     * 
     * @return attributes left after the filtering process has run, never null
     */
    public Map<String, Attribute<?>> getFilteredAttributes() {
        return Collections.unmodifiableMap(filteredAttributes);
    }

    /**
     * Sets the attributes that have been filtered.
     * 
     * @param attributes attributes that have been filtered, may be or contain null
     */
    public void setFilteredAttributes(final Collection<Attribute<?>> attributes) {
        filteredAttributes.clear();

        if (attributes != null) {
            for (Attribute<?> attribute : attributes) {
                addFilteredAttribute(attribute);
            }
        }
    }

    /**
     * Adds an attribute that has been filtered.
     * 
     * @param attribute attribute that has been filtered, may be null
     * 
     * @return the attribute replaced by the newly added attribute, or null if no attribute was replaced
     */
    public Attribute<?> addFilteredAttribute(final Attribute<?> attribute) {
        if (attribute == null) {
            return null;
        }

        return filteredAttributes.put(attribute.getId(), attribute);
    }

    /**
     * Removes an attribute that has been filtered.
     * 
     * @param attributeId ID of the attribute to be removed
     * 
     * @return the attribute that was removed or null if no attribute with the given identifier had been filtered
     */
    public Attribute<?> removeFilteredAttribute(final String attributeId) {
        final String trimmedId = StringSupport.trimOrNull(attributeId);
        if (trimmedId == null) {
            return null;
        }

        return filteredAttributes.remove(trimmedId);
    }
}
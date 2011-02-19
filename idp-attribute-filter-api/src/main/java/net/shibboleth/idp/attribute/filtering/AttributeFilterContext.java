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
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazyMap;

/** Context used to collect data as attributes are filtered. */
@NotThreadSafe
public final class AttributeFilterContext implements Subcontext {

    /** Context which acts as the owner or parent of this context. */
    private final SubcontextContainer parentContext;

    /** Attributes which are to be filtered. */
    private Map<String, Attribute<?>> prefilteredAttributes;

    /** Attributes which have been filtered. */
    private Map<String, Attribute<?>> filteredAttributes;

    /**
     * Constructor.
     * 
     * @param parent the parent of this context
     */
    public AttributeFilterContext(final SubcontextContainer parent) {
        parentContext = parent;
        parent.addSubcontext(this);

        prefilteredAttributes = new LazyMap<String, Attribute<?>>();
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
    public Attribute<?> removeFilteredAttributes(final String attributeId) {
        final String trimmedId = StringSupport.trimOrNull(attributeId);
        if (trimmedId == null) {
            return null;
        }

        return filteredAttributes.remove(trimmedId);
    }
}
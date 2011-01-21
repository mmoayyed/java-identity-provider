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

package net.shibboleth.idp.attribute.resolver;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncoder;

import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for attribute definition resolver plugins. */
@ThreadSafe
public abstract class BaseAttributeDefinition extends BaseResolverPlugin<Attribute<?>> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseAttributeDefinition.class);

    /** Whether this attribute definition is only a dependency and thus its values should never be released. */
    private boolean dependencyOnly;

    /** Attribute encoders associated with this definition. */
    private LazySet<AttributeEncoder> encoders;

    /** Localized human intelligible attribute name. */
    private Map<Locale, String> displayNames;

    /** Localized human readable description of attribute. */
    private Map<Locale, String> displayDescriptions;

    /**
     * Constructor.
     * 
     * @param definitionId unique identifier for this attribute definition
     */
    public BaseAttributeDefinition(final String definitionId) {
        super(definitionId);

        dependencyOnly = false;
        encoders = new LazySet<AttributeEncoder>();
        displayNames = new LazyMap<Locale, String>();
        displayDescriptions = new LazyMap<Locale, String>();
    }

    /**
     * Gets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @return true if this attribute is only used as a dependency, false otherwise
     */
    public boolean isDependencyOnly() {
        return dependencyOnly;
    }

    /**
     * Sets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @param isDependencyOnly whether this attribute definition is only a dependency
     */
    public void setDependencyOnly(final boolean isDependencyOnly) {
        dependencyOnly = isDependencyOnly;
    }

    /**
     * Gets the localized human readable descriptions of attribute.
     * 
     * @return human readable descriptions of attribute, never null
     */
    public Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    /**
     * Sets the localized human readable descriptions of attribute.
     * 
     * @param descriptions localized human readable descriptions of attribute
     */
    public void setDisplayDescriptions(final Map<Locale, String> descriptions) {
        final LazyMap<Locale, String> newDescriptions = new LazyMap<Locale, String>();

        if (descriptions != null) {
            for (Entry<Locale, String> description : descriptions.entrySet()) {
                if (description.getKey() != null || description.getValue() != null) {
                    newDescriptions.put(description.getKey(), description.getValue());
                }
            }
        }

        displayDescriptions = newDescriptions;
    }

    /**
     * Gets the localized human readable names of the attribute.
     * 
     * @return human readable names of the attribute
     */
    public Map<Locale, String> getDisplayNames() {
        return displayNames;
    }

    /**
     * Sets the localized human readable names of the attribute.
     * 
     * @param names localized human readable names of the attribute
     */
    public void setDisplayNames(final Map<Locale, String> names) {
        final LazyMap<Locale, String> newNames = new LazyMap<Locale, String>();
        if (names != null && !names.isEmpty()) {
            newNames.putAll(names);
        }

        displayNames = newNames;
    }

    /**
     * Gets the unmodifiable encoders used to encode the values of this attribute in to protocol specific formats. The
     * returned collection is never null nor contains any null.
     * 
     * @return encoders used to encode the values of this attribute in to protocol specific formats, never null
     */
    public Set<AttributeEncoder> getAttributeEncoders() {
        return Collections.unmodifiableSet(encoders);
    }

    /**
     * Sets the encoders used to encode the values of this attribute in to protocol specific formats.
     * 
     * @param attributeEncoders encoders used to encode the values of this attribute in to protocol specific formats
     */
    public void setAttributeEncoders(final List<AttributeEncoder> attributeEncoders) {
        encoders.clear();

        if (attributeEncoders != null) {
            for (AttributeEncoder<?> encoder : attributeEncoders) {
                addAttributeEncoder(encoder);
            }
        }
    }

    /**
     * Adds an attribute encoder to this definition.
     * 
     * @param attributeEncoder encoder to be added, may be null
     * 
     * @return true if the addition changed the encoders for this definition, false otherwise
     */
    public boolean addAttributeEncoder(final AttributeEncoder<?> attributeEncoder) {
        if (attributeEncoder == null || encoders.contains(attributeEncoder)) {
            return false;
        }

        return encoders.add(attributeEncoder);
    }

    /**
     * Removes an attribute encoder from this definition.
     * 
     * @param attributeEncoder encoder to be removed, may be null
     * 
     * @return true if the removal changed the encoders for this definition, false otherwise
     */
    public boolean removeAttributeEndoer(final AttributeEncoder<?> attributeEncoder) {
        if (attributeEncoder == null) {
            return false;
        }

        return encoders.remove(attributeEncoder);
    }

    /**
     * {@inheritDoc}
     * 
     * This method delegates the actual resolution of the attribute's values to the
     * {@link #doResolve(AttributeResolutionContext)} method. Afterwards this method will attach the registered display
     * names, descriptions, and encoders to the resultant attribute.
     */
    protected Attribute<?> doResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        final Attribute resolvedAttribute = doAttributeResolution(resolutionContext);

        if (resolvedAttribute == null) {
            log.error("{} produced a null attribute, this is not allowed", getId());
            throw new AttributeResolutionException(getId() + " produced a null attribute");
        }

        resolvedAttribute.getDisplayNames().putAll(getDisplayNames());
        resolvedAttribute.getDisplayDescriptions().putAll(getDisplayDescriptions());
        resolvedAttribute.getEncoders().addAll(getAttributeEncoders());

        return resolvedAttribute;
    }

    /**
     * Creates and populates the values for the resolved attribute. Implementations should *not* set, or otherwise
     * manage, the resolved attribute's display name, description or encoders. Nor should the resultant attribute be
     * recorded in the given resolution context.
     * 
     * @param resolutionContext current attribute resolution context
     * 
     * @return resolved attribute
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving and creating the attribute
     */
    protected abstract Attribute<?> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException;
}
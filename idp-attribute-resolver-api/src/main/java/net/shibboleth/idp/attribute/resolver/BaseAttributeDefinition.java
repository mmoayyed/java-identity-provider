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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncoder;

import org.opensaml.util.collections.LazyList;
import org.opensaml.util.collections.LazyMap;
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
    private LazyList<AttributeEncoder> encoders;

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
        encoders = new LazyList<AttributeEncoder>();
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
        LazyMap<Locale, String> newDescriptions = new LazyMap<Locale, String>();
        if (descriptions != null && !descriptions.isEmpty()) {
            newDescriptions.putAll(descriptions);
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
        LazyMap<Locale, String> newNames = new LazyMap<Locale, String>();
        if (names != null && !names.isEmpty()) {
            newNames.putAll(names);
        }

        displayNames = newNames;
    }

    /**
     * Gets the encoders used to encode the values of this attribute in to protocol specific formats.
     * 
     * @return encoders used to encode the values of this attribute in to protocol specific formats, never null
     */
    public List<AttributeEncoder> getAttributeEncoders() {
        return encoders;
    }

    /**
     * Sets the encoders used to encode the values of this attribute in to protocol specific formats.
     * 
     * @param attributeEncoders encoders used to encode the values of this attribute in to protocol specific formats
     */
    public void setAttributeEncoders(final LazyList<AttributeEncoder> attributeEncoders) {
        LazyList<AttributeEncoder> newEncoders = new LazyList<AttributeEncoder>();
        if (attributeEncoders != null && !attributeEncoders.isEmpty()) {
            newEncoders.addAll(attributeEncoders);
        }

        encoders = newEncoders;
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
        Attribute resolvedAttribute = doAttributeResolution(resolutionContext);

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
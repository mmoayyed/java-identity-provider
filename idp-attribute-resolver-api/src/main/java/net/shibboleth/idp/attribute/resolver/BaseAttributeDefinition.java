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

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;

/** Base class for attribute definition resolver plugins. */
@ThreadSafe
public abstract class BaseAttributeDefinition extends BaseResolverPlugin<Attribute<?>> {

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
     * Gets the unmodifiable localized human readable descriptions of attribute. The returned collection is never null
     * nor does it contain any null keys or values.
     * 
     * @return human readable descriptions of attribute, never null
     */
    public Map<Locale, String> getDisplayDescriptions() {
        return Collections.unmodifiableMap(displayDescriptions);
    }

    /**
     * Sets the localized human readable descriptions of attribute.
     * 
     * @param descriptions localized human readable descriptions of attribute
     */
    public void setDisplayDescriptions(final Map<Locale, String> descriptions) {
        displayDescriptions.clear();

        if (descriptions != null) {
            for (Entry<Locale, String> description : descriptions.entrySet()) {
                addDisplayDescription(description.getKey(), description.getValue());
            }
        }
    }

    /**
     * Adds a display description to this definition.
     * 
     * @param locale local of the description, never null
     * @param description description, never null or empty
     * 
     * @return the description previously associated with the given locale or null if there was no previous description
     */
    public String addDisplayDescription(final Locale locale, final String description) {
        Assert.isNotNull(locale, "display description locale may not be null");

        String trimmedDescription = StringSupport.trimOrNull(description);
        Assert.isNotNull(trimmedDescription, "display description may not be null or empty");

        return displayDescriptions.put(locale, trimmedDescription);
    }

    /**
     * Removes a display description from this definition.
     * 
     * @param locale local of the description, may be null
     * 
     * @return the description associated with the given locale or null if there was no description associated with the
     *         locale
     */
    public String removeDisplayDescription(final Locale locale) {
        if (locale == null) {
            return null;
        }

        return displayDescriptions.remove(locale);
    }

    /**
     * Gets the unmodifiable localized human readable names of the attribute. The returned collection is never null nor
     * does it contain any null keys or values.
     * 
     * @return human readable names of the attribute
     */
    public Map<Locale, String> getDisplayNames() {
        return Collections.unmodifiableMap(displayNames);
    }

    /**
     * Sets the localized human readable names of the attribute.
     * 
     * @param names localized human readable names of the attribute
     */
    public void setDisplayNames(final Map<Locale, String> names) {
        displayNames.clear();

        if (names != null) {
            for (Entry<Locale, String> name : names.entrySet()) {
                addDisplayName(name.getKey(), name.getValue());
            }
        }
    }

    /**
     * Adds a display name to this definition.
     * 
     * @param locale locale of the name, never null
     * @param name the display name, never null or empty
     * 
     * @return the name previously associated with the given locale or null if there was no previous name
     */
    public String addDisplayName(final Locale locale, final String name) {
        Assert.isNotNull(locale, "display name locale may not be null");

        String trimmedName = StringSupport.trimOrNull(name);
        Assert.isNotNull(trimmedName, "display name may not be null or empty");

        return displayNames.put(locale, trimmedName);
    }

    /**
     * Removes a display name to this definition.
     * 
     * @param locale locale of the name, may be null
     * 
     * @return the name previously associated with the given locale or null if there was no previous name
     */
    public String removeDisplayName(final Locale locale) {
        if (locale == null) {
            return null;
        }

        return displayNames.remove(locale);
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
        CollectionSupport.nonNullReplace(attributeEncoders, encoders);
    }

    /**
     * Adds an attribute encoder to this definition.
     * 
     * @param attributeEncoder encoder to be added, may be null
     * 
     * @return true if the addition changed the encoders for this definition, false otherwise
     */
    public boolean addAttributeEncoder(final AttributeEncoder<?> attributeEncoder) {
        return CollectionSupport.nonNullAdd(encoders, attributeEncoder);
    }

    /**
     * Removes an attribute encoder from this definition.
     * 
     * @param attributeEncoder encoder to be removed, may be null
     * 
     * @return true if the removal changed the encoders for this definition, false otherwise
     */
    public boolean removeAttributeEndoer(final AttributeEncoder<?> attributeEncoder) {
        return CollectionSupport.nonNullRemove(encoders, attributeEncoder);
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
            return null;
        }

        resolvedAttribute.setDisplayDescriptions(getDisplayDescriptions());
        resolvedAttribute.setDisplayNames(getDisplayNames());
        resolvedAttribute.setEncoders(getAttributeEncoders());

        return resolvedAttribute;
    }

    /**
     * Creates and populates the values for the resolved attribute. Implementations should <strong>not</strong> set, or
     * otherwise manage, the resolved attribute's display name, description or encoders. Nor should the resultant
     * attribute be recorded in the given resolution context.
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
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncoder;

import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.DestructableComponent;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.opensaml.util.component.ValidatableComponent;

/** Base class for attribute definition resolver plugins. */
@ThreadSafe
public abstract class BaseAttributeDefinition extends BaseResolverPlugin<Attribute<?>> {

    /** Whether this attribute definition is only a dependency and thus its values should never be released. */
    private boolean dependencyOnly;

    /** Attribute encoders associated with this definition. */
    private Set<AttributeEncoder> encoders = Collections.emptySet();

    /** Localized human intelligible attribute name. */
    private Map<Locale, String> displayNames = Collections.emptyMap();

    /** Localized human readable description of attribute. */
    private Map<Locale, String> displayDescriptions = Collections.emptyMap();

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
    public synchronized void setDependencyOnly(final boolean isDependencyOnly) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, dependency only status can not be changed.");
        }

        dependencyOnly = isDependencyOnly;
    }

    /**
     * Gets the unmodifiable localized human readable descriptions of attribute. The returned collection is never null
     * nor does it contain any null keys or values.
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
    public synchronized void setDisplayDescriptions(final Map<Locale, String> descriptions) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, display descriptions can not be changed.");
        }

        HashMap<Locale, String> checkedDescriptions = new HashMap<Locale, String>();

        String trimmedDescription;
        for (Locale locale : descriptions.keySet()) {
            if (locale == null) {
                continue;
            }

            trimmedDescription = StringSupport.trimOrNull(descriptions.get(locale));
            if (trimmedDescription != null) {
                checkedDescriptions.put(locale, trimmedDescription);
            }
        }

        if (checkedDescriptions.isEmpty()) {
            displayDescriptions = Collections.emptyMap();
        } else {
            displayDescriptions = Collections.unmodifiableMap(descriptions);
        }
    }

    /**
     * Gets the unmodifiable localized human readable names of the attribute. The returned collection is never null nor
     * does it contain any null keys or values.
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
    public synchronized void setDisplayNames(final Map<Locale, String> names) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, display names can not be changed.");
        }

        HashMap<Locale, String> checkedNames = new HashMap<Locale, String>();

        String trimmedName;
        for (Locale locale : names.keySet()) {
            if (locale == null) {
                continue;
            }

            trimmedName = StringSupport.trimOrNull(names.get(locale));
            if (trimmedName != null) {
                checkedNames.put(locale, trimmedName);
            }
        }

        if (checkedNames.isEmpty()) {
            displayNames = Collections.emptyMap();
        } else {
            displayNames = Collections.unmodifiableMap(checkedNames);
        }
    }

    /**
     * Gets the unmodifiable encoders used to encode the values of this attribute in to protocol specific formats. The
     * returned collection is never null nor contains any null.
     * 
     * @return encoders used to encode the values of this attribute in to protocol specific formats, never null
     */
    public Set<AttributeEncoder> getAttributeEncoders() {
        return encoders;
    }

    /**
     * Sets the encoders used to encode the values of this attribute in to protocol specific formats.
     * 
     * @param attributeEncoders encoders used to encode the values of this attribute in to protocol specific formats
     */
    public void setAttributeEncoders(final List<AttributeEncoder> attributeEncoders) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, attribute encoders can not be changed.");
        }

        HashSet<AttributeEncoder> checkedEncoders =
                CollectionSupport.addNonNull(attributeEncoders, new HashSet<AttributeEncoder>());
        if (checkedEncoders.isEmpty()) {
            encoders = Collections.emptySet();
        } else {
            encoders = Collections.unmodifiableSet(checkedEncoders);
        }
    }

    /** {@inheritDoc} */
    public synchronized void destroy() {

        for (AttributeEncoder encoder : encoders) {
            if (encoder instanceof DestructableComponent) {
                ((DestructableComponent) encoder).destroy();
            }
        }

        encoders = Collections.emptySet();
        displayDescriptions = Collections.emptyMap();
        displayNames = Collections.emptyMap();

        super.destroy();
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        super.validate();

        for (AttributeEncoder encoder : encoders) {
            if (encoder instanceof ValidatableComponent) {
                ((ValidatableComponent) encoder).validate();
            }
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        for (AttributeEncoder encoder : encoders) {
            if (encoder instanceof InitializableComponent) {
                ((InitializableComponent) encoder).initialize();
            }
        }
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
        if (!isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has not been initialized and can not yet be used.");
        }

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
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

package edu.internet2.middleware.shibboleth.idp.attribute.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;

/** A context which carries and collects information through an attribute resolution. */
@NotThreadSafe
public class AttributeResolutionContext implements Subcontext {

    /** Context which acts as the owner or parent of this context. */
    private final SubcontextContainer parentContext;

    /** Attributes that have been requested to be resolved. */
    private Set<Attribute<?>> requestedAttributes;

    /** Plugins that be resolved. */
    private Map<String, BaseResolverPlugin<?>> resolvedPlugins;

    /** Attributes which have been resolved. */
    private Map<String, Attribute<?>> resolvedAttributes;

    /**
     * Constructor.
     * 
     * @param parent the parent of this context
     */
    public AttributeResolutionContext(final SubcontextContainer parent) {
        parentContext = parent;
        parent.addSubcontext(this);

        requestedAttributes = new LazySet<Attribute<?>>();
        resolvedPlugins = new LazyMap<String, BaseResolverPlugin<?>>();
        resolvedAttributes = new LazyMap<String, Attribute<?>>();
    }

    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return parentContext;
    }

    /**
     * Gets the set of attributes requested to be resolved.
     * 
     * @return set of attributes requested to be resolved, never null
     */
    public Set<Attribute<?>> getRequestedAttributes() {
        return requestedAttributes;
    }

    /**
     * Sets the set of attributes requested to be resolved.
     * 
     * @param attributes attributes requested to be resolved
     */
    public void setRequestedAttributes(final Set<Attribute<?>> attributes) {
        LazySet<Attribute<?>> newAttributes = new LazySet<Attribute<?>>();
        if (attributes != null && !attributes.isEmpty()) {
            newAttributes.addAll(attributes);
        }

        requestedAttributes = newAttributes;
    }

    /**
     * Gets the resolver plugins that have been resolved. Each plugin is wrapped in proxy that caches the plugin's
     * response so subsequent {@link BaseResolverPlugin#resolve(AttributeResolutionContext)} calls return the same
     * results as the initial request.
     * 
     * @return resolver plugins that have been resolved
     */
    public Map<String, BaseResolverPlugin<?>> getResolvedPlugins() {
        return resolvedPlugins;
    }

    /**
     * Gets the resolved plugin with the given ID.
     * 
     * @param <T> type of the plugin
     * @param pluginId ID of the plugin
     * 
     * @return the plugin or null if the plugin ID was null/empty or the plugin has not yet been resolved
     */
    public <T extends BaseResolverPlugin<?>> T getResolvedPlugin(String pluginId) {
        String trimmedId = StringSupport.trimOrNull(pluginId);
        if (trimmedId == null) {
            return null;
        }

        return (T) resolvedPlugins.get(trimmedId);
    }

    /**
     * Gets the attributes that have been resolved, indexed by attribute ID.
     * 
     * @return attributes that have been resolved, never null
     */
    public Map<String, Attribute<?>> getResolvedAttributes() {
        return resolvedAttributes;
    }

    /**
     * Gets a resolved attribute.
     * 
     * @param attributeId the ID of the attribute
     * 
     * @return the attribute or null if the ID is null/empty or the attribute has not be resolved
     */
    public Attribute getResolvedAttribute(String attributeId) {
        String trimmedId = StringSupport.trimOrNull(attributeId);
        if (trimmedId == null) {
            return null;
        }

        return resolvedAttributes.get(trimmedId);
    }

    public void addResolvedAttribute(final Attribute<?> attribute) throws AttributeResolutionException {
        final String attributeId = attribute.getId();

        if (!resolvedAttributes.containsKey(attributeId)) {
            resolvedAttributes.put(attributeId, attribute);
        }

        Attribute<?> existingAttribute = resolvedAttributes.get(attributeId);

        if (attribute.getDisplayDescriptions() != null
                && ObjectSupport.equals(attribute.getDisplayDescriptions(), existingAttribute.getDisplayDescriptions())) {
            throw new AttributeResolutionException("Two " + attributeId
                    + " attributes were resolved with different display descriptions");
        }

        if (attribute.getDisplayNames() != null
                && ObjectSupport.equals(attribute.getDisplayNames(), existingAttribute.getDisplayNames())) {
            throw new AttributeResolutionException("Two " + attributeId
                    + " attributes were resolved with different display names");
        }

        if (attribute.getEncoders() != null
                && ObjectSupport.equals(attribute.getEncoders(), existingAttribute.getEncoders())) {
            throw new AttributeResolutionException("Two " + attributeId
                    + " attributes were resolved with different encoders");
        }

        Collection existingValues = existingAttribute.getValues();
        existingValues.addAll(attribute.getValues());
    }
}
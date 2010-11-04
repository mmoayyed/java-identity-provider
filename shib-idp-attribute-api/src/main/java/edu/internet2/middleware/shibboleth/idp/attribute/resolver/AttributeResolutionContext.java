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

import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;

/** A context which carries and collects information through an attribute resolution. */
@NotThreadSafe
public class AttributeResolutionContext implements Subcontext {

    /** Context which acts as the owner or parent of this context. */
    private final SubcontextContainer parentContext;

    /** Attributes that have been requested to be resolved. */
    private Set<Attribute<?>> requestAttributes;

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

        requestAttributes = new LazySet<Attribute<?>>();
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
    public Set<Attribute<?>> getRequestAttributes() {
        return requestAttributes;
    }

    /**
     * Sets the set of attributes requested to be resolved.
     * 
     * @param attributes attributes requested to be resolved
     */
    public void setRequestAttributes(final Set<Attribute<?>> attributes) {
        LazySet<Attribute<?>> newAttributes = new LazySet<Attribute<?>>();
        if (attributes != null && !attributes.isEmpty()) {
            newAttributes.addAll(attributes);
        }

        requestAttributes = newAttributes;
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
     * Gets the attributes that have been resolved, indexed by attribute ID.
     * 
     * @return attributes that have been resolved, never null
     */
    public Map<String, Attribute<?>> getResolvedAttributes() {
        return resolvedAttributes;
    }
}
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.collections.LazySet;

/** A context which carries and collects information through an attribute resolution. */
@NotThreadSafe
public class AttributeResolutionContext implements Subcontext {

    /** Context which acts as the owner or parent of this context. */
    private final SubcontextContainer parentContext;

    /** Attributes that have been requested to be resolved. */
    private Set<Attribute<?>> requestedAttributes;

    /** Attributes which were resolved and released by the attribute resolver. */
    private Map<String, Attribute<?>> resolvedAttributes;

    /** Attribute definitions that have been resolved and the resultant attribute. */
    private final Map<String, ResolvedAttributeDefinition> resolvedAttributeDefinitions;

    /** Data connectors that have been resolved and the resultant attributes. */
    private final Map<String, ResolvedDataConnector> resolvedDataConnectors;

    /**
     * Constructor.
     * 
     * @param parent the parent of this context
     */
    public AttributeResolutionContext(final SubcontextContainer parent) {
        parentContext = parent;

        if (parent != null) {
            parent.addSubcontext(this);
        }

        requestedAttributes = new LazySet<Attribute<?>>();
        resolvedAttributes = new LazyMap<String, Attribute<?>>();
        resolvedAttributeDefinitions = new LazyMap<String, ResolvedAttributeDefinition>();
        resolvedDataConnectors = new LazyMap<String, ResolvedDataConnector>();
    }

    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return parentContext;
    }

    /**
     * Gets the unmodifiable set of attributes requested to be resolved.
     * 
     * @return set of attributes requested to be resolved, never null nor containing null elements
     */
    public Set<Attribute<?>> getRequestedAttributes() {
        return Collections.unmodifiableSet(requestedAttributes);
    }

    /**
     * Sets the set of attributes requested to be resolved.
     * 
     * @param attributes attributes requested to be resolved
     */
    public void setRequestedAttributes(final Set<Attribute<?>> attributes) {
        CollectionSupport.nonNullReplace(attributes, requestedAttributes);
    }

    /**
     * Adds an attribute that is requested to be resolved.
     * 
     * @param attribute attribute to be added to the requested list, may be null
     * 
     * @return true if attribute was added, false otherwise (because the attribute was null or already existed in the
     *         requested attribute set)
     */
    public boolean addRequestedAttribute(final Attribute<?> attribute) {
        return CollectionSupport.nonNullAdd(requestedAttributes, attribute);
    }

    /**
     * Removes an attribute that is requested to be resolved.
     * 
     * @param attribute attribute to be removed from the requested list, may be null
     * 
     * @return true if attribute was removed, false otherwise (because the attribute was null or did not exist in the
     *         requested attribute set)
     */
    public boolean removeRequestedAttribute(final Attribute<?> attribute) {
        return CollectionSupport.nonNullRemove(requestedAttributes, attribute);
    }

    /**
     * Gets the unmodifiable collection of resolved attributes. The returned collection is never null nor does it
     * contain any null keys or values.
     * 
     * @return unmodifiable set of resolved attributes
     */
    public Map<String, Attribute<?>> getResolvedAttributes() {
        return Collections.unmodifiableMap(resolvedAttributes);
    }

    /**
     * Sets the set of resolved attributes.
     * 
     * @param attributes set of resolved attributes, may be null, empty or contain null values
     */
    public void setResolvedAttributes(final Collection<Attribute<?>> attributes) {
        resolvedAttributes.clear();

        if (attributes != null) {
            for (Attribute<?> attribute : attributes) {
                addResolvedAttribute(attribute);
            }
        }
    }

    /**
     * Adds a resolved attribute.
     * 
     * @param attribute attribute to be added, may be null
     * 
     * @return that {@link Attribute} that was replaced with the given attribute, null otherwise
     */
    public Attribute<?> addResolvedAttribute(final Attribute<?> attribute) {
        if (attribute == null) {
            return null;
        }

        return resolvedAttributes.put(attribute.getId(), attribute);
    }

    /**
     * Removes a resolved attribute.
     * 
     * @param attributeId ID of the attribute to be removed, may be null
     * 
     * @return the {@link Attribute} that was removed, null otherwise
     */
    public Attribute<?> removeResolvedAttribute(final String attributeId) {
        if (attributeId == null) {
            return null;
        }

        return resolvedAttributes.remove(attributeId);
    }

    /**
     * Gets the resolved attribute definitions that been recorded. The returned collection is never null nor does it
     * contain any null keys or values.
     * 
     * @return resolved attribute definitions that been recorded
     */
    public Map<String, ResolvedAttributeDefinition> getResolvedAttributeDefinitions() {
        return Collections.unmodifiableMap(resolvedAttributeDefinitions);
    }

    /**
     * Gets the resolved attribute definition.
     * 
     * @param definitionId the ID of the attribute definition, may be null/empty
     * 
     * @return the resolved attribute definition, may be null if the given ID was null/empty or the attribute definition
     *         has not yet be resolved
     */
    public ResolvedAttributeDefinition getResolvedAttributeDefinition(final String definitionId) {
        final String trimmedId = StringSupport.trimOrNull(definitionId);
        if (trimmedId == null) {
            return null;
        }

        return resolvedAttributeDefinitions.get(trimmedId);
    }

    /**
     * Records the results of an attribute definition resolution.
     * 
     * @param definition the resolved attribute definition, must not be null
     * @param attribute the attribute produced by the given attribute definition, may be null
     * 
     * @throws AttributeResolutionException thrown if a result of a resolution for the given attribute definition have
     *             already been recorded
     */
    public void recordAttributeDefinitionResolution(final BaseAttributeDefinition definition,
            final Attribute<?> attribute) throws AttributeResolutionException {
        Assert.isNotNull(definition, "Resolved attribute definition may not be null");

        if (resolvedAttributeDefinitions.containsKey(definition.getId())) {
            throw new AttributeResolutionException("The resolution of attribute definition " + definition.getId()
                    + " has already been recorded");
        }

        final ResolvedAttributeDefinition wrapper = new ResolvedAttributeDefinition(definition, attribute);
        resolvedAttributeDefinitions.put(definition.getId(), wrapper);
    }

    /**
     * Gets the resolved data connectors that been recorded. The returned collection is never null nor does it contain
     * any null keys or values.
     * 
     * @return resolved data connectors that been recorded
     */
    public Map<String, ResolvedDataConnector> getResolvedDataConnectors() {
        return Collections.unmodifiableMap(resolvedDataConnectors);
    }

    /**
     * Gets the resolved data connector.
     * 
     * @param connectorId the ID of the data connector, may be null/empty
     * 
     * @return the resolved data connector, may be null if the given ID was null/empty or the data connector has not yet
     *         be resolved
     */
    public ResolvedDataConnector getResolvedDataConnector(final String connectorId) {
        final String trimmedId = StringSupport.trimOrNull(connectorId);
        if (trimmedId == null) {
            return null;
        }

        return resolvedDataConnectors.get(trimmedId);
    }

    /**
     * Records the results of an data connector resolution.
     * 
     * @param connector the resolved data connector, must not be null
     * @param attributes the attribute produced by the given data connector, may be null
     * 
     * @throws AttributeResolutionException thrown if a result of a resolution for the given data connector has already
     *             been recorded
     */
    public void recordDataConnectorResolution(final BaseDataConnector connector,
            final Map<String, Attribute<?>> attributes) throws AttributeResolutionException {
        Assert.isNotNull(connector, "Resolved data connector may not be null");

        if (resolvedDataConnectors.containsKey(connector.getId())) {
            throw new AttributeResolutionException("The resolution of data connector " + connector.getId()
                    + " has already been recorded");
        }

        final ResolvedDataConnector wrapper = new ResolvedDataConnector(connector, attributes);
        resolvedDataConnectors.put(connector.getId(), wrapper);
    }
}
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.collection.TransformedInputMapBuilder;
import net.shibboleth.utilities.java.support.component.AbstractDestrucableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.TrimOrNullStringFunction;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

//TODO(lajoie) perf metrics
//TODO(lajoie) need to deal with thread safety issue where attribute definitions/data connectors might change in the midst of a resolution

/** A component that resolves the attributes for a particular subject. */
@ThreadSafe
public class AttributeResolver extends AbstractDestrucableIdentifiableInitializableComponent implements
        ValidatableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolver.class);

    /** Attribute definitions defined for this resolver. */
    private Map<String, BaseAttributeDefinition> attributeDefinitions;

    /** Data connectors defined for this resolver. */
    private Map<String, BaseDataConnector> dataConnectors;

    /**
     * Constructor.
     * 
     * @param resolverId ID of this resolver
     */
    public AttributeResolver(@Nonnull @NotEmpty String resolverId) {
        setId(resolverId);
        attributeDefinitions = Collections.emptyMap();
        dataConnectors = Collections.emptyMap();
    }

    /**
     * Gets the collection of attribute definitions for this resolver.
     * 
     * @return attribute definitions loaded in to this resolver
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, BaseAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    /**
     * Sets the collection of attribute definitions for this resolver.
     * 
     * @param definitions definition to set
     */
    public synchronized void setAttributeDefinition(
            @Nullable @NullableElements final Collection<BaseAttributeDefinition> definitions) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        TransformedInputMapBuilder<String, BaseAttributeDefinition> mapBuilder =
                new TransformedInputMapBuilder<String, BaseAttributeDefinition>()
                        .keyPreprocessor(TrimOrNullStringFunction.INSTANCE);

        if (definitions != null) {
            for (BaseAttributeDefinition definition : definitions) {
                if (definition != null) {
                    mapBuilder.put(definition.getId(), definition);
                }
            }
        }

        attributeDefinitions = mapBuilder.buildImmutableMap();
    }

    /**
     * Gets the unmodifiable collection of data connectors for this resolver.
     * 
     * @return data connectors loaded in to this resolver
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, BaseDataConnector> getDataConnectors() {
        return dataConnectors;
    }

    /**
     * Sets the collection of data connectors for this resolver.
     * 
     * @param connectors connectors to set
     */
    public synchronized void setDataConnectors(
            @Nullable @NullableElements final Collection<BaseDataConnector> connectors) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        TransformedInputMapBuilder<String, BaseDataConnector> mapBuilder =
                new TransformedInputMapBuilder<String, BaseDataConnector>()
                        .keyPreprocessor(TrimOrNullStringFunction.INSTANCE);

        if (connectors != null) {
            for (BaseDataConnector connector : connectors) {
                if (connector != null) {
                    mapBuilder.put(connector.getId(), connector);
                }
            }
        }

        dataConnectors = mapBuilder.buildImmutableMap();
    }

    /**
     * This method checks if each registered data connector and attribute definition is valid (via
     * {@link BaseResolverPlugin#validate()} and checks to see if there are any loops in the dependency for all
     * registered plugins.
     * 
     * {@inheritDoc}
     */
    public void validate() throws ComponentValidationException {
        ifNotInitializedThrowUninitializedComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        final LazyList<String> invalidDataConnectors = new LazyList<String>();
        for (BaseDataConnector plugin : dataConnectors.values()) {
            log.debug("Attribute resolver {}: checking if data connector {} is valid", getId(), plugin.getId());
            validateDataConnector(plugin, invalidDataConnectors);
        }

        final LazyList<String> invalidAttributeDefinitions = new LazyList<String>();
        for (BaseAttributeDefinition plugin : attributeDefinitions.values()) {
            log.debug("Attribute resolver {}: checking if attribute definition {} is valid", getId(), plugin.getId());
            try {
                plugin.validate();
                log.debug("Attribute resolver {}: attribute definition {} is valid", getId(), plugin.getId());
            } catch (ComponentValidationException e) {
                log.warn("Attribute resolver {}: attribute definition {} is not valid", new Object[] {this.getId(),
                        plugin.getId(), e,});
                invalidAttributeDefinitions.add(plugin.getId());
            }
        }

        if (!invalidDataConnectors.isEmpty() || !invalidAttributeDefinitions.isEmpty()) {
            throw new ComponentValidationException("Attribute resolver " + getId()
                    + ": the following attribute definitions were invalid ["
                    + StringSupport.listToStringValue(invalidAttributeDefinitions, ", ")
                    + "] and the following data connectors were invalid ["
                    + StringSupport.listToStringValue(invalidDataConnectors, ", ") + "]");
        }
    }

    /**
     * Resolves the attribute for the give request. Note, if attributes are requested,
     * {@link AttributeResolutionContext#getRequestedAttributes()}, the resolver will <strong>not</strong> fail if they
     * can not be resolved. This information serves only as a hint to the resolver to, potentially, optimize the
     * resolution of attributes.
     * 
     * @param resolutionContext the attribute resolution context that identifies the request subject and accumulates the
     *            resolved attributes
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes for the subject
     */
    public void resolveAttributes(@Nonnull final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        assert resolutionContext != null : "Attribute resolution context can not be null";

        ifNotInitializedThrowUninitializedComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        log.debug("Attribute Resolver {}: initiating attribute resolution", getId());

        if (attributeDefinitions.size() == 0) {
            log.debug("Attribute Resolver {}: no attribute definition available, no attributes were resolved", getId());
            return;
        }

        final Collection<String> attributeIds = getToBeResolvedAttributes(resolutionContext);
        log.debug("Attribute Resolver {}: attempting to resolve the following attribute definitions {}", getId(),
                attributeIds);

        for (String attributeId : attributeIds) {
            resolveAttributeDefinition(attributeId, resolutionContext);
        }

        log.debug("Attribute Resolver {}: finalizing resolved attributes", getId());
        finalizeResolvedAttributes(resolutionContext);

        log.debug("Attribute Resolver {}: final resolved attribute collection: {}", getId(), resolutionContext
                .getResolvedAttributes().keySet());

        return;
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        for (BaseResolverPlugin plugin : attributeDefinitions.values()) {
            plugin.destroy();
        }

        for (BaseResolverPlugin plugin : dataConnectors.values()) {
            plugin.destroy();
        }

        attributeDefinitions = Collections.emptyMap();
        dataConnectors = Collections.emptyMap();
    }

    /**
     * Gets the list of attributes, identified by IDs, that should be resolved. If the
     * {@link AttributeResolutionContext#getRequestedAttributes()} is not empty then those attributes are the ones to be
     * resolved, otherwise all registered attribute definitions are to be resolved.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return list of attributes, identified by IDs, that should be resolved
     */
    @Nonnull @NonnullElements protected Collection<String> getToBeResolvedAttributes(
            @Nonnull final AttributeResolutionContext resolutionContext) {
        assert resolutionContext != null : "Attribute resolution context can not be null";

        final Collection<String> attributeIds = new LazyList<String>();
        for (Attribute requestedAttribute : resolutionContext.getRequestedAttributes()) {
            attributeIds.add(requestedAttribute.getId());
        }

        // if no attributes requested, then resolve everything
        if (attributeIds.isEmpty()) {
            attributeIds.addAll(attributeDefinitions.keySet());
        }

        return attributeIds;
    }

    /**
     * Resolve the {@link BaseAttributeDefinition} which has the specified ID.
     * 
     * The results of the resolution are stored in the given {@link AttributeResolutionContext}.
     * 
     * @param attributeId id of the attribute definition to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @throws AttributeResolutionException if unable to resolve the requested attribute definition
     */
    protected void resolveAttributeDefinition(@Nonnull final String attributeId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        assert attributeId != null : "Attribute ID can not be null";
        assert resolutionContext != null : "Attribute resolution context can not be null";

        log.debug("Attribute Resolver {}: beginning to resolve attribute definition {}", getId(), attributeId);

        if (resolutionContext.getResolvedAttributeDefinitions().containsKey(attributeId)) {
            log.debug("Attribute Resolver {}: attribute definition {} was already resolved, nothing to do", getId(),
                    attributeId);
            return;
        }

        final BaseAttributeDefinition definition = attributeDefinitions.get(attributeId);
        if (definition == null) {
            log.debug("Attribute Resolver {}: no attribute definition was registered with ID {}, nothing to do",
                    getId(), attributeId);
            return;
        }

        resolveDependencies(definition, resolutionContext);

        Optional<Attribute> resolvedAttribute = Optional.absent();

        try {
            log.debug("Attribute Resolver {}: resolving attribute definition {}", getId(), attributeId);
            resolvedAttribute = definition.resolve(resolutionContext);
        } catch (AttributeResolutionException e) {
            if (definition.isPropagateResolutionExceptions()) {
                log.debug("Attribute Resolver {}: attribute definition {} produced the following"
                        + " error but was configured not to propogate it.", new Object[] {getId(), attributeId, e,});
            } else {
                throw e;
            }
        }

        if (!resolvedAttribute.isPresent()) {
            log.debug("Attribute Resolver {}: attribute definition {} produced no attribute", getId(), attributeId);
        } else {
            log.debug("Attribute Resolver {}: attribute definition {} produced an attribute with {} values",
                    new Object[] {getId(), attributeId, resolvedAttribute.get().getValues().size()});
        }

        resolutionContext.recordAttributeDefinitionResolution(definition, resolvedAttribute);
    }

    /**
     * Resolve the {@link DataConnector} which has the specified ID.
     * 
     * The results of the resolution are stored in the given {@link AttributeResolutionContext}.
     * 
     * @param connectorId id of the data connector to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @throws AttributeResolutionException if unable to resolve the requested connector
     */
    protected void resolveDataConnector(@Nonnull final String connectorId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        assert connectorId != null : "Data connector ID can not be null";
        assert resolutionContext != null : "Attribute resolution context can not be null";

        log.debug("Attribute Resolver {}: beginning to resolve data connector {}", getId(), connectorId);

        if (resolutionContext.getResolvedDataConnectors().containsKey(connectorId)) {
            log.debug("Attribute Resolver {}: data connector {} was already resolved, nothing to do", getId(),
                    connectorId);
            return;
        }

        final BaseDataConnector connector = dataConnectors.get(connectorId);
        if (connector == null) {
            log.debug("Attribute Resolver {}: no data connector was registered with ID {}, nothing to do", getId(),
                    connectorId);
            return;
        }

        resolveDependencies(connector, resolutionContext);

        Optional<Map<String, Attribute>> resolvedAttributes = Optional.absent();
        try {
            log.debug("Attribute Resolver {}: resolving data connector {}", getId(), connectorId);
            resolvedAttributes = connector.resolve(resolutionContext);
        } catch (AttributeResolutionException e) {
            final Optional<String> failoverDataConnectorId = connector.getFailoverDataConnectorId();
            if (failoverDataConnectorId.isPresent()) {
                log.debug(
                        "Attribute Resolver {}: data connector {} failed to resolve, invoking failover data connector {}.  Reason for the failure was: {}",
                        new Object[] {getId(), connectorId, failoverDataConnectorId.get(), e});
                resolveDataConnector(failoverDataConnectorId.get(), resolutionContext);
                return;
            } else {
                if (connector.isPropagateResolutionExceptions()) {
                    log.debug("Attribute Resolver {}: data connector {} produced the"
                            + " following error but was configured not to propogate it.", new Object[] {getId(),
                            connectorId, e,});
                } else {
                    throw e;
                }
            }
        }

        if (resolvedAttributes.isPresent()) {
            log.debug("Attribute Resolver {}: data connector {} resolved the following attributes {}", new Object[] {
                    getId(), connectorId, resolvedAttributes.get().keySet(),});
        } else {
            log.debug("Attribute Resolver {}: data connector {} produced no attributes", getId(), connectorId);
        }
        resolutionContext.recordDataConnectorResolution(connector, resolvedAttributes);
    }

    /**
     * Resolves all the dependencies for a given plugin.
     * 
     * @param plugin plugin whose dependencies should be resolved
     * @param resolutionContext current resolution context
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving a dependency
     */
    protected void resolveDependencies(@Nonnull final BaseResolverPlugin<?> plugin,
            @Nonnull final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        assert plugin != null : "Plugin dependency can not be null";
        assert resolutionContext != null : "Attribute resolution context can not be null";

        if (plugin.getDependencies().isEmpty()) {
            return;
        }

        log.debug("Attribute Resolver {}: resolving dependencies for {}", getId(), plugin.getId());

        String pluginId;
        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            pluginId = dependency.getDependencyPluginId();
            if (attributeDefinitions.containsKey(pluginId)) {
                resolveAttributeDefinition(pluginId, resolutionContext);
            } else if (dataConnectors.containsKey(pluginId)) {
                resolveDataConnector(pluginId, resolutionContext);
            } else {
                throw new AttributeResolutionException("Plugin " + plugin.getId() + " contains a depedency on plugin "
                        + pluginId + " which does not exist.");
            }
        }

        log.debug("Attribute Resolver {}: finished resolving dependencies for {}", getId(), plugin.getId());
    }

    /**
     * Finalizes the set of resolved attributes and places them in the {@link AttributeResolutionContext}. The result of
     * each {@link BaseAttributeDefinition} resolution is inspected. If the result is not null, a dependency-only
     * attribute, or an attribute that contains no values then it becomes part of the final set of resolved attributes.
     * 
     * @param resolutionContext current resolution context
     */
    protected void finalizeResolvedAttributes(@Nonnull final AttributeResolutionContext resolutionContext) {
        assert resolutionContext != null : "Attribute resolution context can not be null";

        final LazySet<Attribute> resolvedAttributes = new LazySet<Attribute>();

        Optional<Attribute> resolvedAttribute;
        for (ResolvedAttributeDefinition definition : resolutionContext.getResolvedAttributeDefinitions().values()) {
            resolvedAttribute = definition.getResolvedAttribute();

            // remove nulls
            if (!resolvedAttribute.isPresent()) {
                log.debug("Attribute Resolver {}: removing result of attribute definition {}, it's null", getId(),
                        definition.getId());
                continue;
            }

            // remove dependency-only attributes
            if (definition.isDependencyOnly()) {
                log.debug("Attribute Resolver {}: removing result of attribute definition {},"
                        + " it's marked as depdency only", getId(), definition.getId());
                continue;
            }

            // remove any nulls or duplicate attribute values
            cleanResolvedAttributeValues(resolvedAttribute.get());

            // remove value-less attributes
            if (resolvedAttribute.get().getValues().size() == 0) {
                log.debug("Attribute Resolver {}: removing result of attribute definition {},"
                        + " it's attribute contains no values", getId(), definition.getId());
                continue;
            }

            resolvedAttributes.add(resolvedAttribute.get());
        }

        resolutionContext.setResolvedAttributes(resolvedAttributes);
    }

    /**
     * Cleans the values of the given attribute. Currently this entails removal of any nulls or duplicate values.
     * 
     * @param attribute attribute whose values will be cleaned
     */
    protected void cleanResolvedAttributeValues(@Nonnull final Attribute attribute) {
        assert attribute != null : "Attribute can not be null";

        final Collection<?> values = attribute.getValues();
        if (values.isEmpty()) {
            return;
        }

        // TODO(lajoie) this possibly changes the type of value collection for the attribute, should it?
        final LazySet cleanedValues = new LazySet<Object>();
        for (Object value : values) {
            if (value != null) {
                cleanedValues.add(value);
            }
        }

        attribute.setValues(cleanedValues);
    }

    /**
     * Validates the given data connector.
     * 
     * @param connector connector to valid
     * @param invalidDataConnectors data connectors which have already been validated
     * 
     * @return whether the given data connector is valid
     */
    protected boolean validateDataConnector(@Nonnull BaseDataConnector connector,
            @Nonnull LazyList<String> invalidDataConnectors) {
        try {
            connector.validate();
            log.debug("Attribute resolver {}: data connector {} is valid", getId(), connector.getId());
            return true;
        } catch (ComponentValidationException e) {
            if (connector.getFailoverDataConnectorId() != null) {
                if (invalidDataConnectors.contains(connector.getFailoverDataConnectorId())) {
                    log.warn("Attribute resolver {}: data connector {} is not valid for the following reason"
                            + " and failover data connector {} has already been found to be inavlid", new Object[] {
                            getId(), connector.getId(), connector.getFailoverDataConnectorId(), e,});
                    invalidDataConnectors.add(connector.getId());
                    return false;
                } else {
                    log.warn("Attribute resolver {}: data connector {} is not valid for the following reason,"
                            + " checking if failover data connector {} is valid",
                            new Object[] {getId(), connector.getId(), connector.getFailoverDataConnectorId(), e,});
                    return validateDataConnector(dataConnectors.get(connector.getFailoverDataConnectorId()),
                            invalidDataConnectors);
                }
            }

            log.warn("Attribute resolver {}: data connector {} is not valid and has not failover connector",
                    new Object[] {this.getId(), connector.getId(), e,});
            invalidDataConnectors.add(connector.getId());
            return false;
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        HashSet<String> dependencyVerifiedPlugins = new HashSet<String>();

        for (BaseDataConnector plugin : dataConnectors.values()) {
            log.debug("Attribute resolver {}: checking if data connector {} is has a cirucular depdency", getId(),
                    plugin.getId());
            checkPlugInDependencies(plugin.getId(), plugin, dependencyVerifiedPlugins);
        }

        for (BaseAttributeDefinition plugin : attributeDefinitions.values()) {
            log.debug("Attribute resolver {}: checking if attribute definition {} has a circular depdency", getId(),
                    plugin.getId());
            checkPlugInDependencies(plugin.getId(), plugin, dependencyVerifiedPlugins);
        }
    }

    /**
     * Checks to ensure that there are no circular dependencies or dependencies on non-existent plugins.
     * 
     * @param circularCheckPluginId the ID of the plugin currently being checked for circular dependencies
     * @param plugin current plugin, in the dependency tree of the plugin being checked, that we're currently looking at
     * @param checkedPlugins IDs of plugins that have already been checked and known to be good
     * 
     * @throws ComponentInitializationException thrown if there is a dependency loop
     */
    protected void checkPlugInDependencies(final String circularCheckPluginId, final BaseResolverPlugin<?> plugin,
            final Set<String> checkedPlugins) throws ComponentInitializationException {
        final String pluginId = plugin.getId();

        BaseResolverPlugin<?> dependencyPlugin;
        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            if (checkedPlugins.contains(pluginId)) {
                continue;
            }

            if (circularCheckPluginId.equals(dependency.getDependencyPluginId())) {
                throw new ComponentInitializationException("Plugin " + circularCheckPluginId + " and plugin "
                        + dependency.getDependencyPluginId() + " have a circular dependecy on each other.");
            }

            dependencyPlugin = dataConnectors.get(dependency.getDependencyPluginId());
            if (dependencyPlugin == null) {
                dependencyPlugin = attributeDefinitions.get(dependency.getDependencyPluginId());
            }
            if (dependencyPlugin == null) {
                throw new ComponentInitializationException("Plugin " + plugin.getId() + " has a dependency on plugin "
                        + dependency.getDependencyPluginId() + " which does not exist");
            }

            checkPlugInDependencies(circularCheckPluginId, dependencyPlugin, checkedPlugins);
            checkedPlugins.add(pluginId);
        }
    }
}
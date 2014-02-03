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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.PrincipalConnectorDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolvedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPlugin;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.service.AbstractServiceableComponent;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

//TODO(lajoie) perf metrics

/**
 * A component that resolves the attributes for a particular subject.
 * 
 * <em>Note Well</em>This class is about <em>attribute resolution</em>, that is to say the summoning up of attributes in
 * response to the exigies of the provided context. It does <em>not</em> implement
 * {@link net.shibboleth.utilities.java.support.resolver.Resolver} which in about summoning up bits of generic data from
 * the configuration (usually the metadata) in response to specific
 * {@link net.shibboleth.utilities.java.support.resolver.Criterion}s. <br>
 * The implementation also implements {@link PrincipalConnectorDefinition} in support of the deprecated
 * &lt;PrincipalConnector&gt;
 * */
@ThreadSafe
public class AttributeResolverImpl extends AbstractServiceableComponent<AttributeResolver> implements
        AttributeResolver, PrincipalConnectorDefinition<SubjectCanonicalizationContext> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeResolverImpl.class);

    /** Attribute definitions defined for this resolver. */
    @Nonnull private final Map<String, AttributeDefinition> attributeDefinitions;

    /** Data connectors defined for this resolver. */
    @Nonnull private final Map<String, DataConnector> dataConnectors;

    /** cache for the log prefix - to save multiple recalculations. */
    @NonnullAfterInit private final String logPrefix;

    /** The Principal mapper. */
    @Nullable private final PrincipalConnectorDefinition<SubjectCanonicalizationContext> principalConnector;

    /**
     * Constructor.
     * 
     * @param resolverId ID of this resolver
     * @param definitions attribute definitions loaded in to this resolver
     * @param connectors data connectors loaded in to this resolver
     * @param principalResolver code to resolve the principal
     */
    public AttributeResolverImpl(@Nonnull @NotEmpty String resolverId,
            @Nullable @NullableElements Collection<AttributeDefinition> definitions,
            @Nullable @NullableElements Collection<DataConnector> connectors,
            @Nullable PrincipalConnectorDefinition<SubjectCanonicalizationContext> principalResolver) {
        setId(resolverId);

        logPrefix = new StringBuilder("Attribute Resolver '").append(getId()).append("':").toString();

        Map<String, AttributeDefinition> checkedDefinitions;
        if (definitions != null) {
            checkedDefinitions = new HashMap<>(definitions.size());
            for (AttributeDefinition definition : definitions) {
                if (definition != null) {
                    if (checkedDefinitions.containsKey(definition.getId())) {
                        throw new IllegalArgumentException(logPrefix + " duplicate Attribute Definition with id "
                                + definition.getId());
                    }
                    checkedDefinitions.put(definition.getId(), definition);
                }
            }
        } else {
            checkedDefinitions = Collections.emptyMap();
        }
        attributeDefinitions = ImmutableMap.copyOf(checkedDefinitions);

        Map<String, DataConnector> checkedConnectors;
        if (connectors != null) {
            checkedConnectors = new HashMap<>(connectors.size());
            for (DataConnector connector : connectors) {
                if (connector != null) {
                    if (checkedConnectors.containsKey(connector.getId())) {
                        throw new IllegalArgumentException(logPrefix + " duplicate Data Connector Definition with id "
                                + connector.getId());
                    }
                    checkedConnectors.put(connector.getId(), connector);
                }
            }
        } else {
            checkedConnectors = Collections.emptyMap();
        }
        dataConnectors = ImmutableMap.copyOf(checkedConnectors);

        principalConnector = principalResolver;
    }

    /**
     * Gets the collection of attribute definitions for this resolver.
     * 
     * @return attribute definitions loaded in to this resolver
     */
    @Override @Nonnull @NonnullElements @Unmodifiable public Map<String, AttributeDefinition> 
            getAttributeDefinitions() {
        return attributeDefinitions;
    }

    /**
     * Gets the unmodifiable collection of data connectors for this resolver.
     * 
     * @return data connectors loaded in to this resolver
     */
    @Override @Nonnull @NonnullElements @Unmodifiable public Map<String, DataConnector> getDataConnectors() {
        return dataConnectors;
    }

    /**
     * This method checks if each registered data connector and attribute definition is valid (via
     * {@link ResolverPlugin#validate()} and checks to see if there are any loops in the dependency for all registered
     * plugins.
     * 
     * {@inheritDoc}
     */
    @Override public void validate() throws ComponentValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final LazyList<String> invalidDataConnectors = new LazyList<String>();
        for (DataConnector plugin : dataConnectors.values()) {
            log.debug("{} checking if data connector {} is valid", logPrefix, plugin.getId());
            if (!validateDataConnector(plugin, invalidDataConnectors)) {
                invalidDataConnectors.add(plugin.getId());
            }
        }

        final LazyList<String> invalidAttributeDefinitions = new LazyList<String>();
        for (AttributeDefinition plugin : attributeDefinitions.values()) {
            log.debug("{} checking if attribute definition {} is valid", logPrefix, plugin.getId());
            try {
                plugin.validate();
                log.debug("{} attribute definition {} is valid", logPrefix, plugin.getId());
            } catch (ComponentValidationException e) {
                log.warn("{} attribute definition {} is not valid", new Object[] {logPrefix, plugin.getId(), e,});
                invalidAttributeDefinitions.add(plugin.getId());
            }
        }

        if (!invalidDataConnectors.isEmpty() || !invalidAttributeDefinitions.isEmpty()) {
            throw new ComponentValidationException(logPrefix + " the following attribute definitions were invalid ["
                    + StringSupport.listToStringValue(invalidAttributeDefinitions, ", ")
                    + "] and the following data connectors were invalid ["
                    + StringSupport.listToStringValue(invalidDataConnectors, ", ") + "]");
        }
    }

    /** {@inheritDoc} */
    @Override protected void doDestroy() {
        for (ResolverPlugin plugin : attributeDefinitions.values()) {
            plugin.destroy();
        }

        for (ResolverPlugin plugin : dataConnectors.values()) {
            plugin.destroy();
        }
    }

    /**
     * Resolves the attribute for the give request. Note, if attributes are requested,
     * {@link AttributeResolutionContext#getRequestedIdPAttributes()}, the resolver will <strong>not</strong> fail if
     * they can not be resolved. This information serves only as a hint to the resolver to, potentially, optimize the
     * resolution of attributes.
     * 
     * @param resolutionContext the attribute resolution context that identifies the request subject and accumulates the
     *            resolved attributes
     * 
     * @throws ResolutionException thrown if there is a problem resolving the attributes for the subject
     */
    @Override public void resolveAttributes(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");

        log.debug("{} initiating attribute resolution", logPrefix);

        if (attributeDefinitions.size() == 0) {
            log.debug("{} no attribute definition available, no attributes were resolved", logPrefix);
            return;
        }

        final Collection<String> attributeIds = getToBeResolvedAttributeIds(resolutionContext);
        log.debug("{} attempting to resolve the following attribute definitions {}", logPrefix, attributeIds);

        // Create work context to hold intermediate results.
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, true);

        for (String attributeId : attributeIds) {
            resolveAttributeDefinition(attributeId, resolutionContext);
        }

        log.debug("{} finalizing resolved attributes", logPrefix);
        finalizeResolvedAttributes(resolutionContext);

        resolutionContext.removeSubcontext(workContext);

        log.debug("{} final resolved attribute collection: {}", logPrefix, resolutionContext.getResolvedIdPAttributes()
                .keySet());
    }

    /**
     * Gets the list of attributes, identified by IDs, that should be resolved. If the
     * {@link AttributeResolutionContext#getRequestedIdPAttributes()} is not empty then those attributes are the ones to
     * be resolved, otherwise all registered attribute definitions are to be resolved.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return list of attributes, identified by IDs, that should be resolved
     */
    @Nonnull @NonnullElements protected Collection<String> getToBeResolvedAttributeIds(
            @Nonnull final AttributeResolutionContext resolutionContext) {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");

        // if no attributes requested, then resolve everything
        if (resolutionContext.getRequestedIdPAttributeNames().isEmpty()) {
            final Collection<String> attributeIds = new LazyList<String>();
            attributeIds.addAll(attributeDefinitions.keySet());
            return attributeIds;
        } else {
            return resolutionContext.getRequestedIdPAttributeNames();
        }

    }

    /**
     * Resolve the {@link AttributeDefinition} which has the specified ID.
     * 
     * The results of the resolution are stored in the given {@link AttributeResolutionContext}.
     * 
     * @param attributeId id of the attribute definition to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @throws ResolutionException if unable to resolve the requested attribute definition
     */
    protected void resolveAttributeDefinition(@Nonnull final String attributeId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(attributeId, "Attribute ID can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        log.trace("{} beginning to resolve attribute definition {}", logPrefix, attributeId);

        if (workContext.getResolvedIdPAttributeDefinitions().containsKey(attributeId)) {
            log.trace("{} attribute definition {} was already resolved, nothing to do", logPrefix, attributeId);
            return;
        }

        final AttributeDefinition definition = attributeDefinitions.get(attributeId);
        if (definition == null) {
            log.debug("{} no attribute definition was registered with ID {}, nothing to do", logPrefix, attributeId);
            return;
        }

        resolveDependencies(definition, resolutionContext);

        log.trace("{} resolving attribute definition {}", logPrefix, attributeId);
        final IdPAttribute resolvedAttribute = definition.resolve(resolutionContext);

        if (null == resolvedAttribute) {
            log.warn("{} attribute definition {} produced no attribute", logPrefix, attributeId);
        } else {
            log.debug("{} attribute definition {} produced an attribute with {} values", new Object[] {logPrefix,
                    attributeId, resolvedAttribute.getValues().size(),});
        }

        workContext.recordAttributeDefinitionResolution(definition, resolvedAttribute);
    }

    /**
     * Resolve the {@link DataConnector} which has the specified ID.
     * 
     * The results of the resolution are stored in the given {@link AttributeResolutionContext}.
     * 
     * @param connectorId id of the data connector to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @throws ResolutionException if unable to resolve the requested connector
     */
    protected void resolveDataConnector(@Nonnull final String connectorId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(connectorId, "Data connector ID can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        if (workContext.getResolvedDataConnectors().containsKey(connectorId)) {
            log.trace("{} data connector {} was already resolved, nothing to do", logPrefix, connectorId);
            return;
        }

        final DataConnector connector = dataConnectors.get(connectorId);
        if (connector == null) {
            log.debug("{} no data connector was registered with ID {}, nothing to do", logPrefix, connectorId);
            return;
        }

        resolveDependencies(connector, resolutionContext);
        Map<String, IdPAttribute> resolvedAttributes;
        try {
            log.debug("{} resolving data connector {}", logPrefix, connectorId);
            resolvedAttributes = connector.resolve(resolutionContext);
        } catch (ResolutionException e) {
            final String failoverDataConnectorId = connector.getFailoverDataConnectorId();
            if (null != failoverDataConnectorId) {
                log.debug("{} data connector {} failed to resolve, invoking failover data"
                        + " connector {}.  Reason for the failure was: {}", new Object[] {logPrefix, connectorId,
                        failoverDataConnectorId, e,});
                resolveDataConnector(failoverDataConnectorId, resolutionContext);
                return;
            } else {
                // Pass it on. Do not look at propagateException because this is handled in the
                // connector code logic.
                throw e;
            }
        }

        if (null != resolvedAttributes) {
            log.debug("{} data connector {} resolved the following attributes {}", new Object[] {logPrefix,
                    connectorId, resolvedAttributes.keySet(),});
        } else {
            log.debug("{} data connector {} produced no attributes", logPrefix, connectorId);
        }
        workContext.recordDataConnectorResolution(connector, resolvedAttributes);
    }

    /**
     * Resolves all the dependencies for a given plugin.
     * 
     * @param plugin plugin whose dependencies should be resolved
     * @param resolutionContext current resolution context
     * 
     * @throws ResolutionException thrown if there is a problem resolving a dependency
     */
    protected void resolveDependencies(@Nonnull final ResolverPlugin<?> plugin,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(plugin, "Plugin dependency can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");

        if (plugin.getDependencies().isEmpty()) {
            return;
        }

        log.debug("{} resolving dependencies for {}", logPrefix, plugin.getId());

        String pluginId;
        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            pluginId = dependency.getDependencyPluginId();
            if (attributeDefinitions.containsKey(pluginId)) {
                resolveAttributeDefinition(pluginId, resolutionContext);
            } else if (dataConnectors.containsKey(pluginId)) {
                resolveDataConnector(pluginId, resolutionContext);
            } else {
                // This will not happen for as long as we test this in initialization
                throw new ResolutionException("Plugin " + plugin.getId() + " contains a depedency on plugin "
                        + pluginId + " which does not exist.");
            }
        }

        log.debug("{} finished resolving dependencies for {}", logPrefix, plugin.getId());
    }

    /**
     * Finalizes the set of resolved attributes and places them in the {@link AttributeResolutionContext}. The result of
     * each {@link AttributeDefinition} resolution is inspected. If the result is not null, a dependency-only attribute,
     * or an attribute that contains no values then it becomes part of the final set of resolved attributes.
     * 
     * @param resolutionContext current resolution context
     */
    protected void finalizeResolvedAttributes(@Nonnull final AttributeResolutionContext resolutionContext) {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        final LazySet<IdPAttribute> resolvedAttributes = new LazySet<IdPAttribute>();

        IdPAttribute resolvedAttribute;
        for (ResolvedAttributeDefinition definition : workContext.getResolvedIdPAttributeDefinitions().values()) {
            resolvedAttribute = definition.getResolvedAttribute();

            // remove nulls
            if (null == resolvedAttribute) {
                log.debug("{} removing result of attribute definition {}, it is null", logPrefix, definition.getId());
                continue;
            }

            // remove dependency-only attributes
            if (definition.isDependencyOnly()) {
                log.debug("{} removing result of attribute definition {}, it is marked as depdency only", logPrefix,
                        definition.getId());
                continue;
            }

            // remove value-less attributes
            if (resolvedAttribute.getValues().size() == 0) {
                log.debug("{} removing result of attribute definition {}, its attribute contains no values", logPrefix,
                        definition.getId());
                continue;
            }

            resolvedAttributes.add(resolvedAttribute);
        }

        resolutionContext.setResolvedIdPAttributes(resolvedAttributes);
    }

    /**
     * Validates the given data connector.
     * 
     * @param connector connector to valid
     * @param invalidDataConnectors data connectors which have already been validated
     * 
     * @return whether the given data connector is valid
     */
    protected boolean validateDataConnector(@Nonnull DataConnector connector,
            @Nonnull LazyList<String> invalidDataConnectors) {
        Constraint.isNotNull(connector, "To-be-validated connector can no be null");
        Constraint.isNotNull(invalidDataConnectors, "List of invalid data connectors cannot be null");

        final String failoverId = connector.getFailoverDataConnectorId();
        if (null != failoverId) {
            if (!dataConnectors.containsKey(failoverId)) {
                log.warn("{} failover data connector {} for {} cannot be found", new Object[] {logPrefix, failoverId,
                        connector.getId(),});
                return false;
            }
        }

        boolean returnValue;
        try {
            connector.validate();
            log.debug("{} data connector {} is valid", logPrefix, connector.getId());
            returnValue = true;
        } catch (ComponentValidationException e) {
            if (null != failoverId) {
                if (invalidDataConnectors.contains(failoverId)) {
                    log.warn("{} data connector {} is not valid for the following reason and"
                            + " failover data connector {} has already been found to be inavlid", new Object[] {
                            logPrefix, connector.getId(), failoverId, e,});
                    invalidDataConnectors.add(connector.getId());
                    returnValue = false;
                } else {
                    log.warn("{} data connector {} is not valid for the following reason {},"
                            + " checking if failover data connector {} is valid",
                            new Object[] {logPrefix, connector.getId(), e, failoverId,});
                    returnValue = validateDataConnector(dataConnectors.get(failoverId), invalidDataConnectors);
                    if (!returnValue) {
                        invalidDataConnectors.add(failoverId);
                    }
                }
            } else {

                log.warn("{} data connector {} is not valid and has not failover connector", new Object[] {logPrefix,
                        connector.getId(), e,});
                returnValue = false;
            }
        }
        return returnValue;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        HashSet<String> dependencyVerifiedPlugins = new HashSet<String>();
        for (DataConnector plugin : dataConnectors.values()) {
            ComponentSupport.initialize(plugin);
        }
        for (AttributeDefinition plugin : attributeDefinitions.values()) {
            ComponentSupport.initialize(plugin);
        }

        for (DataConnector plugin : dataConnectors.values()) {
            log.debug("{} checking if data connector {} is has a circular dependency", logPrefix, plugin.getId());
            checkPlugInDependencies(plugin.getId(), plugin, dependencyVerifiedPlugins);
        }

        for (AttributeDefinition plugin : attributeDefinitions.values()) {
            log.debug("{} checking if attribute definition {} has a circular dependency", logPrefix, plugin.getId());
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
    protected void checkPlugInDependencies(final String circularCheckPluginId, final ResolverPlugin<?> plugin,
            final Set<String> checkedPlugins) throws ComponentInitializationException {
        final String pluginId = plugin.getId();

        ResolverPlugin<?> dependencyPlugin;
        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            if (checkedPlugins.contains(pluginId)) {
                continue;
            }

            if (circularCheckPluginId.equals(dependency.getDependencyPluginId())) {
                throw new ComponentInitializationException(logPrefix + " Plugin " + circularCheckPluginId
                        + " and plugin " + dependency.getDependencyPluginId()
                        + " have a circular dependecy on each other.");
            }

            dependencyPlugin = dataConnectors.get(dependency.getDependencyPluginId());
            if (dependencyPlugin == null) {
                dependencyPlugin = attributeDefinitions.get(dependency.getDependencyPluginId());
            }
            if (dependencyPlugin == null) {
                throw new ComponentInitializationException(logPrefix + " Plugin " + plugin.getId()
                        + " has a dependency on plugin " + dependency.getDependencyPluginId() + " which doesn't exist");
            }

            checkPlugInDependencies(circularCheckPluginId, dependencyPlugin, checkedPlugins);
            checkedPlugins.add(pluginId);
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull public AttributeResolver getComponent() {
        return this;
    }

    /** {@inheritDoc} */
    @Override @Nullable public String canonicalize(@Nonnull SubjectCanonicalizationContext context)
            throws ResolutionException {
        if (null == principalConnector) {
            return null;
        }
        return principalConnector.canonicalize(context);
    }
}
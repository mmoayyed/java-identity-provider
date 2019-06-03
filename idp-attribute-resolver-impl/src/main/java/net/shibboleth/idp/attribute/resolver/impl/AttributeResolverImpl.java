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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.MetricContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.shibboleth.ext.spring.service.AbstractServiceableComponent;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolvedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolvedDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverPlugin;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.collection.LazyMap;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A component that resolves the attributes for a particular subject.
 * 
 * <em>Note Well</em>This class is about <em>attribute resolution</em>, that is to say the summoning up of attributes in
 * response to the exigies of the provided context. It does <em>not</em> implement
 * {@link net.shibboleth.utilities.java.support.resolver.Resolver} which in about summoning up bits of generic data from
 * the configuration (usually the metadata) in response to specific
 * {@link net.shibboleth.utilities.java.support.resolver.Criterion}s. <br>
 */
@ThreadSafe
public class AttributeResolverImpl extends AbstractServiceableComponent<AttributeResolver> implements
        AttributeResolver {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeResolverImpl.class);

    /** Attribute definitions defined for this resolver. */
    @NonnullAfterInit private Map<String, AttributeDefinition> attributeDefinitions;

    /** Data connectors defined for this resolver. */
    @NonnullAfterInit private Map<String, DataConnector> dataConnectors;

    /** cache for the log prefix - to save multiple recalculations. */
    @NonnullAfterInit private String logPrefix;
  
    /** Whether to strip null attribute values. */
    private boolean stripNulls;
    
    /** Strategy to get the {@link ProfileRequestContext}. */
    @Nonnull private Function<AttributeResolutionContext,ProfileRequestContext> profileContextStrategy;

    /** Constructor. */
    public AttributeResolverImpl() {
        profileContextStrategy = new ParentContextLookup<>();
    }
    
    /** Sets the attribute definitions for this resolver.
     * @param definitions attribute definitions loaded in to this resolver
     */
    public void setAttributeDefinitions(@Nullable @NullableElements final Collection<AttributeDefinition> definitions) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        final Map<String, AttributeDefinition> checkedDefinitions;
        if (definitions != null) {
            checkedDefinitions = new HashMap<>(definitions.size());
            for (final AttributeDefinition definition : definitions) {
                if (definition != null) {
                    if (checkedDefinitions.containsKey(definition.getId())) {
                        throw new IllegalArgumentException(logPrefix + " Duplicate Attribute Definition with id '"
                                + definition.getId() + "'");
                    }
                    checkedDefinitions.put(definition.getId(), definition);
                }
            }
        } else {
            checkedDefinitions = Collections.emptyMap();
        }
        attributeDefinitions = ImmutableMap.copyOf(checkedDefinitions);
    }

    /**
     * Gets the collection of attribute definitions for this resolver.
     * 
     * @return attribute definitions loaded in to this resolver
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, AttributeDefinition>
        getAttributeDefinitions() {
        return attributeDefinitions;
    }
    
    /** Sets the data connectors for this resolver.
     * @param connectors data connectors loaded in to this resolver
     */
    public void setDataConnectors( @Nullable @NullableElements final Collection<DataConnector> connectors){
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        final Map<String, DataConnector> checkedConnectors;
        if (connectors != null) {
            checkedConnectors = new HashMap<>(connectors.size());
            for (final DataConnector connector : connectors) {
                if (connector != null) {
                    if (checkedConnectors.containsKey(connector.getId())) {
                        throw new IllegalArgumentException(logPrefix + " Duplicate Data Connector Definition with id '"
                                + connector.getId() + "'");
                    }
                    checkedConnectors.put(connector.getId(), connector);
                }
            }
        } else {
            checkedConnectors = Collections.emptyMap();
        }
        dataConnectors = ImmutableMap.copyOf(checkedConnectors);
    }

    /**
     * Gets the unmodifiable collection of data connectors for this resolver.
     * 
     * @return data connectors loaded in to this resolver
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, DataConnector> getDataConnectors() {
        return dataConnectors;
    }
    
    /**
     * Do we strip nulls from attribute values.
     * @return Returns whether to strip nulls from attribute values
     */
    public boolean isStripNulls() {
        return stripNulls;
    }

    /** 
     * Sets whether to strip nulls from attribute values.
     * @param doStripNulls what to set 
     */
    public void setStripNulls(final Boolean doStripNulls) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        stripNulls = doStripNulls;
    }

    /**
     * Set the mechanism to obtain the {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setProfileContextLookupStrategy(
            @Nonnull final Function<AttributeResolutionContext,ProfileRequestContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        profileContextStrategy = Constraint.isNotNull(strategy, "ProfileRequestContext lookup strategy cannot be null");
    }
    
    /**
     * Resolves the attribute for the given request. Note, if attributes are requested,
     * {@link AttributeResolutionContext#getRequestedIdPAttributeNames()}, the resolver will <strong>not</strong> fail
     * if they can not be resolved. This information serves only as a hint to the resolver to, potentially, optimize the
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

        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, true);

        final boolean timerStarted = startTimer(resolutionContext);
        try {
            log.debug("{} Initiating attribute resolution", logPrefix);

            boolean hasExportingDataConnector = false;

            for (final Entry<String, DataConnector> dataConnectorEntry : dataConnectors.entrySet()) {
                if (dataConnectorEntry.getValue().isExportAllAttributes() ||
                        !dataConnectorEntry.getValue().getExportAttributes().isEmpty()) {
                    hasExportingDataConnector = true;
                    resolveDataConnector(dataConnectorEntry.getKey(), resolutionContext);
                }
            }

            if (attributeDefinitions.size() == 0 && !hasExportingDataConnector) {
                log.debug("{} No attribute definition available or exporting data connectors" +
                          ", no attributes were resolved", logPrefix);
                return;
            }
    
            final Collection<String> attributeIds = getToBeResolvedAttributeIds(resolutionContext);
            log.debug("{} Attempting to resolve the following attribute definitions {}", logPrefix, attributeIds);
    
            for (final String attributeId : attributeIds) {
                resolveAttributeDefinition(attributeId, resolutionContext);
            }
    
            log.debug("{} Finalizing resolved attributes", logPrefix);
            finalizeResolvedAttributes(resolutionContext);
    
            log.debug("{} Final resolved attribute collection: {}", logPrefix,
                    resolutionContext.getResolvedIdPAttributes().keySet());
        } finally {
            resolutionContext.removeSubcontext(workContext);

            if (timerStarted) {
                stopTimer(resolutionContext);
            }
        }
    }

    /**
     * Gets the list of attributes, identified by IDs, that should be resolved. If the
     * {@link AttributeResolutionContext#getRequestedIdPAttributeNames()} is not empty then those attributes are the
     * ones to be resolved, otherwise all registered attribute definitions are to be resolved.
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
            final Collection<String> attributeIds = new LazyList<>();
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
    // CheckStyle: ReturnCount OFF
    protected void resolveAttributeDefinition(@Nonnull final String attributeId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(attributeId, "Attribute ID can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        log.trace("{} Beginning to resolve attribute definition '{}'", logPrefix, attributeId);

        if (workContext.getResolvedIdPAttributeDefinitions().containsKey(attributeId)) {
            log.trace("{} Attribute definition '{}' was already resolved, nothing to do", logPrefix, attributeId);
            return;
        }

        final AttributeDefinition definition = attributeDefinitions.get(attributeId);
        if (definition == null) {
            log.debug("{} No attribute definition was registered with ID '{}', nothing to do", logPrefix, attributeId);
            return;
        }

        resolveDependencies(definition, resolutionContext);

        log.trace("{} Resolving attribute definition {}", logPrefix, attributeId);
        final IdPAttribute resolvedAttribute = definition.resolve(resolutionContext);

        if (null == resolvedAttribute) {
            log.debug("{} Attribute definition '{}' produced no attribute", logPrefix, attributeId);
        } else {
            log.debug("{} Attribute definition '{}' produced an attribute with {} values", new Object[] {logPrefix,
                    attributeId, resolvedAttribute.getValues().size(),});
        }

        workContext.recordAttributeDefinitionResolution(definition, resolvedAttribute);
    }
    // CheckStyle: ReturnCount ON

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
    // CheckStyle: ReturnCount OFF
    protected void resolveDataConnector(@Nonnull final String connectorId,
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(connectorId, "Data connector ID can not be null");
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);
        
        if (workContext.getResolvedDataConnectors().containsKey(connectorId)) {
            log.trace("{} Data connector '{}' was already resolved, nothing to do", logPrefix, connectorId);
            return;
        }

        final DataConnector connector = dataConnectors.get(connectorId);
        if (connector == null) {
            log.debug("{} No data connector was registered with ID '{}', nothing to do", logPrefix, connectorId);
            return;
        }

        final Instant resolveTime = Instant.now();
        
        if (connector.getLastFail() != null
                && resolveTime.isBefore(connector.getLastFail().plus(connector.getNoRetryDelay()))) {
            log.debug("{} Data connector '{}' failed to resolve previously, still waiting", logPrefix, 
                    connectorId);
            final String failoverDataConnectorId = connector.getFailoverDataConnectorId();
            if (null != failoverDataConnectorId) {
                log.debug("{} Data connector '{}' invoking failover data connector '{}'", logPrefix, connectorId,
                        failoverDataConnectorId);
                resolveDataConnector(failoverDataConnectorId, resolutionContext);
                workContext.recordFailoverResolution(connector, dataConnectors.get(failoverDataConnectorId));
                return;
            } else {
                throw new ResolutionException("Previous resolve failed");
            }
        }

        resolveDependencies(connector, resolutionContext);
        final Map<String, IdPAttribute> resolvedAttributes;
        try {
            log.debug("{} Resolving data connector {}", logPrefix, connectorId);
            resolvedAttributes = connector.resolve(resolutionContext);
        } catch (final ResolutionException e) {
            final String failoverDataConnectorId = connector.getFailoverDataConnectorId();
            if (null != failoverDataConnectorId) {
                log.debug("{} Data connector '{}' failed to resolve, invoking failover data"
                        + " connector '{}'.  Reason for failure:", logPrefix, connectorId, failoverDataConnectorId, e);
                resolveDataConnector(failoverDataConnectorId, resolutionContext);
                workContext.recordFailoverResolution(connector, dataConnectors.get(failoverDataConnectorId));
                return;
            } else {
                // Pass it on. Do not look at propagateException because this is handled in the
                // connector code logic.
                throw e;
            }
        }

        if (null != resolvedAttributes) {
            log.debug("{} Data connector '{}' resolved the following attributes: {}", logPrefix, connectorId,
                    resolvedAttributes.keySet());
        } else {
            log.debug("{} Data connector '{}' produced no attributes", logPrefix, connectorId);
        }
        workContext.recordDataConnectorResolution(connector, resolvedAttributes);
    }
    // CheckStyle: ReturnCount ON

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

        log.debug("{} Resolving dependencies for '{}'", logPrefix, plugin.getId());

        for (final ResolverAttributeDefinitionDependency attrDependency : plugin.getAttributeDependencies()) {
            resolveAttributeDefinition(attrDependency.getDependencyPluginId(), resolutionContext);                
        }

        for (final ResolverDataConnectorDependency dependency : plugin.getDataConnectorDependencies()) { 
            resolveDataConnector(dependency.getDependencyPluginId(), resolutionContext);
        }
        log.debug("{} Finished resolving dependencies for '{}'", logPrefix, plugin.getId());
    }

    /** Helper method for exporting attributes.
     * @param attributeId the if (for logging)
     * @param input the inout list
     * @return a null stripped, or null list of values
     */
    private @Nullable @NonnullElements List<IdPAttributeValue> filterAttributeValues(final String attributeId,
            final List<IdPAttributeValue> input) {

        log.debug("{} De-duping (and null filtering) attribute definition {} result",
                logPrefix, attributeId);
        final List<IdPAttributeValue> result = new ArrayList<>(input.size());
        final Set<IdPAttributeValue> monitor = new HashSet<>(input.size());

        for (final IdPAttributeValue value : input) {
            if (isStripNulls()) {
                if (null == value) {
                    log.debug("{} Stripping null value", logPrefix);
                    continue;
                } else if (value instanceof EmptyAttributeValue) {
                    log.debug("{} Stripping {} value", logPrefix, ((EmptyAttributeValue)value).getValue());
                    continue;
                }
                // ByteAttributeValue, StringAttributeValue and XMLObjectValue are Constrained to not be empty
            }

            if (!monitor.add(value)) {
                log.debug("{} Removing duplicate value {} of attribute '{}' from resolution result", logPrefix,
                        value, attributeId);
            } else {
                result.add(value);
            }
        }

        // No values
        if (monitor.isEmpty()) {
            return null;
        }
        return result;
    }

    /**
     * Helper function to collect suitably resolved attributes.
     * @param resolvedAttributes bucket to collect attributes into
     * @param workContext context to extract attributes from
     */
    private void collectResolvedAttributes(final Map<String, IdPAttribute> resolvedAttributes,
            final AttributeResolverWorkContext workContext) {

        for (final ResolvedAttributeDefinition definition : workContext.getResolvedIdPAttributeDefinitions().values()) {
            final IdPAttribute resolvedAttribute = definition.getResolvedAttribute();

            // Remove nulls.
            if (null == resolvedAttribute) {
                log.debug("{} Removing result of attribute definition '{}', it is null", logPrefix, definition.getId());
                continue;
            }

            // Remove dependency-only attributes.
            if (definition.isDependencyOnly()) {
                log.debug("{} Removing result of attribute definition '{}', is marked as dependency only", logPrefix,
                        definition.getId());
                continue;
            }

            final List<IdPAttributeValue> result =
                    filterAttributeValues(definition.getId(),  resolvedAttribute.getValues());

            // Remove value-less attributes.
            if (result == null) {
                log.debug("{} Removing result of attribute definition '{}', contains no values", logPrefix,
                        definition.getId());
                continue;
            }

            resolvedAttribute.setValues(result);
            log.debug("{} Attribute '{}' has {} values after post-processing", logPrefix, resolvedAttribute.getId(),
                    result.size());

            resolvedAttributes.put(resolvedAttribute.getId(), resolvedAttribute);
        }
    }

    /**
     * Helper function to collect attributes from suitabl data connectors.
     * @param resolvedAttributes bucket to collect attributes into
     * @param workContext context to extract attributes from
     */
    // CheckStyle: CyclomaticComplexit OFF
    private void collectExportingDataConnectors(final Map<String, IdPAttribute> resolvedAttributes,
            final AttributeResolverWorkContext workContext) {

        for (final ResolvedDataConnector dataConnector: workContext.getResolvedDataConnectors().values()) {

            if (!dataConnector.isExportAllAttributes() && dataConnector.getExportAttributes().isEmpty()) {
                continue;
            }

            for (final IdPAttribute attribute:dataConnector.getResolvedAttributes().values()) {
                if (!dataConnector.isExportAllAttributes() &&
                    !dataConnector.getExportAttributes().contains(attribute.getId())) {
                    continue;
                }
                if (resolvedAttributes.get(attribute.getId()) != null) {
                    log.warn("{} could not export attibute '{}' from data connector '{}' since an attribute of " +
                            "that name already exists.", logPrefix, attribute.getId(), dataConnector.getId());
                    continue;
                }
                final List<IdPAttributeValue> values = filterAttributeValues(attribute.getId(), attribute.getValues());
                if (values == null) {
                    log.debug("{} Removing attribute '{}' from data connector '{}' with no values", logPrefix,
                            dataConnector.getId(), attribute.getId());
                    continue;
                }
                final IdPAttribute newAttr = new IdPAttribute(attribute.getId());
                newAttr.setValues(values);
                if (attribute.getDisplayNames().size() > 0) {
                    newAttr.setDisplayNames(attribute.getDisplayNames());
                } else {
                    newAttr.setDisplayNames(Map.of(Locale.getDefault(),
                            dataConnector.getId() + "/" + attribute.getId()));
                }
                if (attribute.getDisplayDescriptions().size() > 0) {
                    newAttr.setDisplayDescriptions(attribute.getDisplayDescriptions());
                }
                resolvedAttributes.put(attribute.getId(), newAttr);
            }
        }
    }
    // CheckStyle: CyclomaticComplexit ON

    /**
     * Finalizes the set of resolved attributes and places them in the {@link AttributeResolutionContext}. The result of
     * each {@link AttributeDefinition} resolution is inspected. If the result is not null, a dependency-only attribute,
     * or an attribute that contains no values then it becomes part of the final set of resolved attributes.
     * <p>
     * Then we handle attribute exports from DataConnectors.
     *
     * <p>
     * Values are also de-duplicated here, so that all the intermediate operations maintain the coherency of
     * multi-valued result sets produced by data connectors.
     * </p>
     *
     * @param resolutionContext current resolution context
     */
    protected void finalizeResolvedAttributes(@Nonnull final AttributeResolutionContext resolutionContext) {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context cannot be null");
        final AttributeResolverWorkContext workContext =
                resolutionContext.getSubcontext(AttributeResolverWorkContext.class, false);

        final Map<String, IdPAttribute> resolvedAttributes = new LazyMap<>();

        collectResolvedAttributes(resolvedAttributes, workContext);

        collectExportingDataConnectors(resolvedAttributes, workContext);

        resolutionContext.setResolvedIdPAttributes(resolvedAttributes.values());
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        logPrefix = new StringBuilder("Attribute Resolver '").append(getId()).append("':").toString();

        if (null == attributeDefinitions) {
            throw new ComponentInitializationException("No Attribute Definitions provided");
        }
        
        if (null == dataConnectors) {
            throw new ComponentInitializationException("No Data Connectors provided");
        }


        final HashSet<String> dependencyVerifiedPlugins = new HashSet<>();
        for (final DataConnector plugin : dataConnectors.values()) {
            log.debug("{} Checking if data connector '{}' is has a circular dependency", logPrefix, plugin.getId());
            checkPlugInDependencies(plugin.getId(), plugin, dependencyVerifiedPlugins);
        }

        for (final AttributeDefinition plugin : attributeDefinitions.values()) {
            log.debug("{} Checking if attribute definition '{}' has a circular dependency", logPrefix, plugin.getId());
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

        for (final ResolverAttributeDefinitionDependency attrDependency : plugin.getAttributeDependencies()) {
            final AttributeDefinition dependencyAttribute;
            if (checkedPlugins.contains(pluginId)) {
                continue;
            }

            if (circularCheckPluginId.equals(attrDependency.getDependencyPluginId())) {
                throw new ComponentInitializationException(logPrefix + " Plugin '" + circularCheckPluginId
                        + "' and attribute definition '" + attrDependency.getDependencyPluginId()
                        + "' have a circular dependency on each other.");
            }
            
            dependencyAttribute = attributeDefinitions.get(attrDependency.getDependencyPluginId());
            if (dependencyAttribute == null) {
                throw new ComponentInitializationException(logPrefix + " Plugin '" + plugin.getId()
                        + "' has a dependency on attribute definition '" + attrDependency.getDependencyPluginId()
                        + "' which doesn't exist");
            }

            checkPlugInDependencies(circularCheckPluginId, dependencyAttribute, checkedPlugins);
            checkedPlugins.add(pluginId);
        }
        
        for (final ResolverDataConnectorDependency dependency : plugin.getDataConnectorDependencies()) {
            final ResolverPlugin<?> dependencyDataConnector;
            
            if (checkedPlugins.contains(pluginId)) {
                continue;
            }

            if (circularCheckPluginId.equals(dependency.getDependencyPluginId())) {
                throw new ComponentInitializationException(logPrefix + " Plugin '" + circularCheckPluginId
                        + "' and data connector '" + dependency.getDependencyPluginId()
                        + "' have a circular dependency on each other.");
            }
            dependencyDataConnector = dataConnectors.get(dependency.getDependencyPluginId());                

            if (dependencyDataConnector == null) {
                throw new ComponentInitializationException(logPrefix + " Plugin '" + plugin.getId()
                        + "' has a dependency on data connector '" + dependency.getDependencyPluginId()
                        + "' which doesn't exist");
            }

            checkPlugInDependencies(circularCheckPluginId, dependencyDataConnector, checkedPlugins);
            checkedPlugins.add(pluginId);
        }

    }

    /** {@inheritDoc} */
    @Override @Nonnull public AttributeResolver getComponent() {
        return this;
    }

    /**
     * Conditionally start a timer at the beginning of the resolution process.
     * 
     * @param resolutionContext attribute resolution context
     * 
     * @return true iff the {@link #stopTimer(AttributeResolutionContext)} method needs to be called
     */
    private boolean startTimer(@Nonnull final AttributeResolutionContext resolutionContext) {
        final BaseContext prc = profileContextStrategy.apply(resolutionContext);
        if (prc != null) {
            final MetricContext timerCtx = prc.getSubcontext(MetricContext.class);
            if (timerCtx != null) {
                timerCtx.start(getId());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Conditionally stop a timer at the end of the resolution process.
     * 
     * @param resolutionContext attribute resolution context
     */
    private void stopTimer(@Nonnull final AttributeResolutionContext resolutionContext) {
        final BaseContext prc = profileContextStrategy.apply(resolutionContext);
        if (prc != null) {
            final MetricContext timerCtx = prc.getSubcontext(MetricContext.class);
            if (timerCtx != null) {
                timerCtx.stop(getId());
            }
        }
    }

}

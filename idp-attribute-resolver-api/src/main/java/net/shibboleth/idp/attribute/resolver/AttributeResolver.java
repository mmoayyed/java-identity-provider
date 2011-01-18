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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.AbstractComponent;
import net.shibboleth.idp.ComponentValidationException;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO perf metrics

/** A component that resolves the attributes for a particular subject. */
@ThreadSafe
public class AttributeResolver extends AbstractComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolver.class);

    /** Attribute definitions defined for this resolver. */
    private Map<String, BaseAttributeDefinition> attributeDefinitions;

    /** Data connectors defined for this resolver. */
    private Map<String, BaseDataConnector> dataConnectors;

    /**
     * Constructor.
     * 
     * @param id the unique ID for this resolver
     */
    public AttributeResolver(final String id) {
        super(id);
    }

    /**
     * Gets the attribute definitions loaded in to this resolver.
     * 
     * @return attribute definitions loaded in to this resolver, never null
     */
    public Map<String, BaseAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    /**
     * Gets the data connectors loaded in to this resolver.
     * 
     * @return data connectors loaded in to this resolver, never null
     */
    public Map<String, BaseDataConnector> getDataConnectors() {
        return dataConnectors;
    }

    /**
     * Gets all the attributes for a given subject.
     * 
     * @param resolutionContext the attribute resolution context that identifies the request subject and accumulates the
     *            resolved attributes
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes for the subject
     */
    public void resolveAttributes(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        if (attributeDefinitions.size() == 0) {
            return;
        }

        final Collection<String> attributeIds = getToBeResolvedAttributes(resolutionContext);
        for (String attributeId : attributeIds) {
            resolveAttribute(attributeId, resolutionContext);
        }
        cleanResolvedAttributes(resolutionContext);

        return;
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
    protected Collection<String> getToBeResolvedAttributes(final AttributeResolutionContext resolutionContext) {
        final Collection<String> attributeIds = new LazyList<String>();
        for (Attribute<?> requestedAttribute : resolutionContext.getRequestedAttributes()) {
            attributeIds.add(requestedAttribute.getId());
        }

        // if no attributes requested, then resolve everything
        if (attributeIds == null || attributeIds.isEmpty()) {
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
    protected void resolveAttribute(final String attributeId, final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        // check to see if the attribute has already been resolved
        if (resolutionContext.getResolvedPlugins().containsKey(attributeId)) {
            return;
        }

        // attribute not yet resolved, so do it
        final BaseAttributeDefinition definition = attributeDefinitions.get(attributeId);

        // check if attribute definition is applicable for this request
        definition.isApplicable(resolutionContext);

        // resolve all the definitions dependencies
        resolveDependencies(definition, resolutionContext);

        // return the actual resolution of the definition
        final Attribute attribute = definition.resolve(resolutionContext);

        // create a static attribute definition to serve as a cached copy of the results of this attribute resolution
        final StaticAttributeDefinition cachedAttributeDefinition = new StaticAttributeDefinition(attributeId,
                attribute);
        cachedAttributeDefinition.getAttributeEncoders().addAll(attribute.getEncoders());
        cachedAttributeDefinition.getDisplayDescriptions().putAll(attribute.getDisplayDescriptions());
        cachedAttributeDefinition.getDisplayNames().putAll(attribute.getDisplayNames());

        // put data in to resolution context
        resolutionContext.getResolvedPlugins().put(attributeId, cachedAttributeDefinition);
        resolutionContext.getResolvedAttributes().put(attributeId, attribute);
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
    protected void resolveDataConnector(final String connectorId, final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        // check to see if the data connector has already been resolved
        if (resolutionContext.getResolvedPlugins().containsKey(connectorId)) {
            return;
        }

        // data connector not yet resolved, so do it
        final BaseDataConnector dataConnector = dataConnectors.get(connectorId);

        // check if data connector is applicable for this request
        dataConnector.isApplicable(resolutionContext);

        // resolve all the connectors dependencies
        resolveDependencies(dataConnector, resolutionContext);

        Map<String, Attribute<?>> resolvedAttributes = null;
        try {
            resolvedAttributes = dataConnector.resolve(resolutionContext);
        } catch (AttributeResolutionException e) {
            String failoverDataConnectorId = dataConnector.getFailoverDependencyId();

            if (StringSupport.isNullOrEmpty(failoverDataConnectorId)) {
                log.error("Received the following error from data connector " + dataConnector.getId()
                        + ", no failover data connector available", e);
                throw e;
            }

            log.warn("Received the following error from data connector " + dataConnector.getId()
                    + ", trying its failover connector " + failoverDataConnectorId, e.getMessage());
            log.debug("Error recieved from data connector " + dataConnector.getId(), e);
            resolveDataConnector(failoverDataConnectorId, resolutionContext);
        }

        final StaticDataConnector cachedDataConnector = new StaticDataConnector(connectorId, resolvedAttributes);
        resolutionContext.getResolvedPlugins().put(connectorId, cachedDataConnector);
        // TODO properly add resolved attributes, have to ensure values are properly merged
    }

    /**
     * Resolves all the dependencies for a given plugin.
     * 
     * @param plugin plugin whose dependencies should be resolved
     * @param resolutionContext current resolution context
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving a dependency
     */
    protected void resolveDependencies(final BaseResolverPlugin<?> plugin,
            final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {

        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            dependency.getDependantAttribute(resolutionContext);
            // TODO store in context
        }
    }

    /**
     * Removes attributes that contain no values or those which are dependency only.
     * 
     * @param resolutionContext current resolution context
     */
    protected void cleanResolvedAttributes(final AttributeResolutionContext resolutionContext) {

        BaseAttributeDefinition attributeDefinition;

        final Iterator<Attribute<?>> attributeItr = resolutionContext.getRequestedAttributes().iterator();
        Attribute<?> resolvedAttribute;
        Set<Object> values;
        while (attributeItr.hasNext()) {
            resolvedAttribute = attributeItr.next();

            // remove nulls
            if (resolvedAttribute == null) {
                attributeItr.remove();
                continue;
            }

            // remove dependency-only attributes
            attributeDefinition = attributeDefinitions.get(resolvedAttribute.getId());
            if (attributeDefinition.isDependencyOnly()) {
                attributeItr.remove();
                continue;
            }

            // remove value-less attributes
            if (resolvedAttribute.getValues().size() == 0) {
                attributeItr.remove();
                continue;
            }

            // remove duplicate attribute values
            Iterator<?> valueItr = resolvedAttribute.getValues().iterator();
            values = new HashSet<Object>();
            while (valueItr.hasNext()) {
                Object value = valueItr.next();
                if (!values.add(value)) {
                    log.debug("Removing duplicate value {} of attribute {} from resolution result", value,
                            resolvedAttribute.getId());
                    valueItr.remove();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This method checks if each registered data connector and attribute definition is valid (via
     * {@link BaseResolverPlugin#validate()} and checks to see if there are any loops in the dependency for all
     * registered plugins.
     */
    public void validate() throws ComponentValidationException {
        final LazyList<String> invalidPluginIds = new LazyList<String>();

        for (BaseDataConnector plugin : dataConnectors.values()) {
            if (plugin != null) {
                try {
                    log.debug("Attribute resolver {}: checking if data connector {} is valid", this.getId(),
                            plugin.getId());
                    plugin.validate();
                    log.debug("Attribute resolver {}: data connector {} is valid", this.getId(), plugin.getId());
                } catch (ComponentValidationException e) {
                    log.warn("Attribute resolver {}: data connector {} is not valid", new Object[] { this.getId(),
                            plugin.getId(), e, });
                    invalidPluginIds.add(plugin.getId());
                }
            }
        }

        for (BaseAttributeDefinition plugin : attributeDefinitions.values()) {
            if (plugin != null) {
                try {
                    log.debug("Attribute resolver {}: checking if attribute definition {} is valid", this.getId(),
                            plugin.getId());
                    plugin.validate();
                    log.debug("Attribute resolver {}: attribute definition {} is valid", this.getId(), plugin.getId());
                } catch (ComponentValidationException e) {
                    log.warn("Attribute resolver {}: attribute definition {} is not valid", new Object[] {
                            this.getId(), plugin.getId(), e, });
                    invalidPluginIds.add(plugin.getId());
                }
            }
        }

        if (!invalidPluginIds.isEmpty()) {
            throw new ComponentValidationException("The following attribute resolver plugins were invalid: "
                    + StringSupport.listToStringValue(invalidPluginIds, ", "));
        }

        log.debug("Attribute resolver {}: checking for dependency loops amongst plugins", this.getId());
        checkForCircularPlugInDependencies();
    }

    /**
     * Checks to ensure that there is no dependency loops amongst the resolver plugins.
     * 
     * @throws ComponentValidationException thrown if there is a dependency loop
     */
    private void checkForCircularPlugInDependencies() throws ComponentValidationException {
        // TODO
    }
}
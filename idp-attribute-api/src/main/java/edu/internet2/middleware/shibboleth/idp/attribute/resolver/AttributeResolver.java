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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.locks.Lock;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.StringSupport;
import org.opensaml.util.collections.LazyList;
import org.opensaml.util.collections.LazyMap;
import org.opensaml.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;
import edu.internet2.middleware.shibboleth.idp.service.AbstractSpringReloadableService;
import edu.internet2.middleware.shibboleth.idp.service.ServiceException;

//TODO perf metrics

/** A service that resolves the attributes for a particular subject. */
@ThreadSafe
public class AttributeResolver extends AbstractSpringReloadableService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolver.class);

    /** Attribute definitions defined for this resolver. */
    private Map<String, BaseAttributeDefinition> attributeDefinitions;

    /** Data connectors defined for this resolver. */
    private Map<String, BaseDataConnector> dataConnectors;

    /**
     * Constructor.
     * 
     * @param id the unique ID for this service
     * @param parent the parent application context for this context, may be null if there is no parent
     * @param configs configuration resources for the service
     * @param backgroundTaskTimer timer used to schedule background processes
     * @param pollingFrequency frequency, in milliseconds, that the configuration resources are polled for changes
     */
    public AttributeResolver(String id, ApplicationContext parent, List<Resource> configs, Timer backgroundTaskTimer,
            long pollingFrequency) {
        super(id, parent, configs, backgroundTaskTimer, pollingFrequency);
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

        Lock readLock = getServiceLock().readLock();
        readLock.lock();
        try {
            Collection<String> attributeIds = getToBeResolvedAttributes(resolutionContext);
            for (String attributeId : attributeIds) {
                resolveAttribute(attributeId, resolutionContext);
            }
            cleanResolvedAttributes(resolutionContext);
        } finally {
            readLock.unlock();
        }

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
    protected Collection<String> getToBeResolvedAttributes(AttributeResolutionContext resolutionContext) {
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
    protected void resolveAttribute(String attributeId, AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        // check to see if the attribute has already been resolved
        BaseAttributeDefinition definition = resolutionContext.getResolvedPlugin(attributeId);
        if (definition != null) {
            return;
        }

        // attribute not yet resolved, so do it
        definition = attributeDefinitions.get(attributeId);

        // check if attribute definition is applicable for this request
        definition.isApplicable(resolutionContext);

        // resolve all the definitions dependencies
        resolveDependencies(definition, resolutionContext);

        // return the actual resolution of the definition
        Attribute attribute = definition.resolve(resolutionContext);

        // create a static attribute definition to serve as a cached copy of the results of this attribute resolution
        StaticAttributeDefinition cachedAttributeDefinition = new StaticAttributeDefinition(attributeId, attribute);
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
    protected void resolveDataConnector(String connectorId, AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        // check to see if the data connector has already been resolved
        BaseDataConnector dataConnector = resolutionContext.getResolvedPlugin(connectorId);
        if (dataConnector != null) {
            return;
        }

        // data connector not yet resolved, so do it
        dataConnector = dataConnectors.get(connectorId);

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
    protected void resolveDependencies(BaseResolverPlugin<?> plugin, AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        for (ResolverPluginDependency dependency : plugin.getDependencies()) {
            dependency.getDependantAttribute(resolutionContext);
            // TODO store in context
        }
    }

    protected void addResolvedAttributesToResolutionContext(AttributeResolutionContext context,
            BaseResolverPlugin<?> resolvedPlugin, Attribute<?>... resolvedAttributes) {

    }

    /**
     * Removes attributes that contain no values or those which are dependency only.
     * 
     * @param resolvedAttributes attribute set to clean up
     * @param resolutionContext current resolution context
     */
    protected void cleanResolvedAttributes(AttributeResolutionContext resolutionContext) {

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
    public void validate() throws ServiceException {
        final LazyList<String> invalidPluginIds = new LazyList<String>();

        for (BaseDataConnector plugin : dataConnectors.values()) {
            if (plugin != null) {
                try {
                    log.debug("Attribute resolver {}: checking if data connector {} is valid", this.getId(),
                            plugin.getId());
                    plugin.validate();
                    log.debug("Attribute resolver {}: data connector {} is valid", this.getId(), plugin.getId());
                } catch (AttributeResolutionException e) {
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
                } catch (AttributeResolutionException e) {
                    log.warn("Attribute resolver {}: attribute definition {} is not valid", new Object[] {
                            this.getId(), plugin.getId(), e, });
                    invalidPluginIds.add(plugin.getId());
                }
            }
        }

        if (!invalidPluginIds.isEmpty()) {
            throw new ServiceException("The following attribute resolver plugins were invalid: "
                    + StringSupport.listToStringValue(invalidPluginIds, ", "));
        }

        log.debug("Attribute resolver {}: checking for dependency loops amongst plugins", this.getId());
        checkForPlugInDependencyLoop();
    }

    /**
     * Checks to ensure that there is no dependency loops amongst the resolver plugins.
     * 
     * @throws ServiceException thrown if there is a dependency loop
     */
    private void checkForPlugInDependencyLoop() throws ServiceException {
        // TODO
    }
}
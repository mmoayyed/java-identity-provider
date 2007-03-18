/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ContextualAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.ContextualDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.PrincipalConnector;

/**
 * Primary implementation of {@link AttributeResolver}.
 * 
 * "Raw" attributes are gathered by the registered {@link DataConnector}s while the {@link AttributeDefinition}s
 * refine the raw attributes or create attributes of their own. Connectors and definitions may depend on each other so
 * implementations must use a directed dependency graph when performing the resolution.
 */
public class ShibbolethAttributeResolver implements AttributeResolver<ShibbolethAttributeRequestContext> {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(ShibbolethAttributeResolver.class.getName());

    /** Data connectors defined for this resolver. */
    private Map<String, DataConnector> dataConnectors;

    /** Attribute definitions defined for this resolver. */
    private Map<String, AttributeDefinition> definitions;

    /** Prinicpal connectors defined for this resolver. */
    private Map<String, PrincipalConnector> principalConnectors;

    /**
     * Constructor.
     */
    public ShibbolethAttributeResolver() {
        dataConnectors = new HashMap<String, DataConnector>();
        definitions = new HashMap<String, AttributeDefinition>();
        principalConnectors = new HashMap<String, PrincipalConnector>();
    }

    /**
     * Gets the attribute definitions registered with this resolver.
     * 
     * @return attribute definitions registered with this resolver
     */
    public Map<String, AttributeDefinition> getAttributeDefinitions() {
        return definitions;
    }

    /**
     * Gets the data connectors registered with this provider.
     * 
     * @return data connectors registered with this provider
     */
    public Map<String, DataConnector> getDataConnectors() {
        return dataConnectors;
    }

    /**
     * Gets the principal connectors registered with this resolver.
     * 
     * @return principal connectors registered with this resolver
     */
    public Map<String, PrincipalConnector> getPrincipalConnectors() {
        return principalConnectors;
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> resolveAttributes(ShibbolethAttributeRequestContext attributeRequest)
            throws AttributeResolutionException {
        ShibbolethResolutionContext resolutionContext = new ShibbolethResolutionContext(attributeRequest);
        Set<String> attributeIDs = attributeRequest.getRequestedAttributes();
        Map<String, Attribute> resolvedAttributes = new HashMap<String, Attribute>();

        // if no attributes requested, then resolve everything
        if (attributeIDs == null || attributeIDs.isEmpty()) {
            attributeIDs = getAttributeDefinitions().keySet();
        }

        for (String attributeID : attributeIDs) {
            if (!getAttributeDefinitions().containsKey(attributeID)) {
                log.warn("No plug-in registered for attribute: (" + attributeID + ")");

            } else if (getAttributeDefinitions().get(attributeID).isDependencyOnly()) {
                log.debug("Attribute (" + attributeID + ") is set to be a dependency only and cannot be released"
                        + " to relying parties.");

            } else {
                log.info("Resolving attribute: (" + attributeID + ")");

                if (resolvedAttributes.containsKey(attributeID)) {
                    log.debug("Attribute (" + attributeID
                            + ") already resolved for this request.  No need for further resolution.");
                } else {
                    Attribute resolution = resolveAttribute(attributeID, resolutionContext);

                    if (resolution != null && resolution.getId() != null) {
                        resolvedAttributes.put(resolution.getId(), resolution);
                    }
                }
            }
        }

        return resolvedAttributes;
    }

    /**
     * Resolve the {@link AttributeDefinition} which has the specified ID. The definition is then added to the
     * {@link AttributeResolutionContext} for use by other {@link ResolutionPlugIn}s and the resolution of the
     * specified definition is added to <code>resolvedAttributes</code> to be returned by the resolver.
     * 
     * @param attributeID id of the attribute definition to resolve
     * @param context resolution context that we are working in
     * 
     * @return resolution of the specified attribute definition
     * 
     * @throws AttributeResolutionException if unable to resolve the requested attribute definition
     */
    private Attribute resolveAttribute(String attributeID, ShibbolethResolutionContext context)
            throws AttributeResolutionException {
        AttributeDefinition definition = getAttributeDefinitions().get(attributeID);

        if (definition == null) {
            return null;
        }

        // resolve attribute and all dependencies if not done so already
        if (!context.getResolvedAttributeDefinitions().containsKey(attributeID)) {

            // wrap attribute definition for use within the given resolution context
            definition = new ContextualAttributeDefinition(definition);

            // resolve DataConnector dependencies
            for (String dependency : definition.getDataConnectorDependencyIds()) {
                log.debug("Attribute (" + attributeID + ") depends on connector (" + dependency + ").");
                resolveConnector(dependency, context);
            }

            // resolve AttributeDefinition dependencies
            for (String dependency : definition.getAttributeDefinitionDependencyIds()) {
                log.debug("Attribute (" + attributeID + ") depends on attribute (" + dependency + ").");
                resolveAttribute(dependency, context);
            }

            // register definition as resolved for this resolution context
            context.getResolvedAttributeDefinitions().put(attributeID, definition);
        }

        // return the actual resolution of the definition
        return definition.resolve(context);
    }

    /**
     * Resolve the {@link DataConnector} which has the specified ID and add it to the resolution context.
     * 
     * @param connectorID id of the data connector to resolve
     * @param context resolution context that we are working in
     * 
     * @throws AttributeResolutionException if unable to resolve the requested connector
     */
    private void resolveConnector(String connectorID, ShibbolethResolutionContext context)
            throws AttributeResolutionException {
        DataConnector dataConnector = getDataConnectors().get(connectorID);

        if (dataConnector == null) {
            return;
        }

        // resolve connector and all dependencies if not done so already
        if (!context.getResolvedDataConnectors().containsKey(connectorID)) {
            log.debug("Resolving DataConnector: (" + connectorID + ").");

            // wrap connector for use within the given resolution context
            dataConnector = new ContextualDataConnector(dataConnector);

            // resolve DataConnector dependencies
            for (String dependency : dataConnector.getDataConnectorDependencyIds()) {
                resolveConnector(dependency, context);
            }

            // resolve AttributeDefinition dependencies
            for (String dependency : dataConnector.getAttributeDefinitionDependencyIds()) {
                resolveAttribute(dependency, context);
            }

            try {
                dataConnector.resolve(context);
            } catch (AttributeResolutionException e) {
                // TODO: use failover connector here?
                return;
            }

            // register connector as resolved for this resolution context
            context.getResolvedDataConnectors().put(connectorID, dataConnector);
        }
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        DirectedGraph<ResolutionPlugIn, DefaultEdge> dependencyGraph;
        CycleDetector<ResolutionPlugIn, DefaultEdge> cycleDetector;

        dependencyGraph = new SimpleDirectedGraph<ResolutionPlugIn, DefaultEdge>(DefaultEdge.class);
        cycleDetector = new CycleDetector<ResolutionPlugIn, DefaultEdge>(dependencyGraph);

        for (AttributeDefinition definition : getAttributeDefinitions().values()) {
            addVertex(dependencyGraph, definition);
        }

        for (DataConnector connector : getDataConnectors().values()) {
            addVertex(dependencyGraph, connector);
        }

        // check for a dependency loop
        if (cycleDetector.detectCycles()) {
            throw new AttributeResolutionException("Attribute Resolver contains a resolution plug-in dependency loop.");
        }
    }

    /**
     * Add a resolution plug-in and dependencies to a directed graph.
     * 
     * @param graph directed graph
     * @param plugin plug-in to add
     */
    private void addVertex(DirectedGraph<ResolutionPlugIn, DefaultEdge> graph, ResolutionPlugIn<?> plugin) {
        graph.addVertex(plugin);

        // add edges for attribute definition dependencies
        for (String id : plugin.getAttributeDefinitionDependencyIds()) {
            AttributeDefinition dependency = getAttributeDefinitions().get(id);
            graph.addVertex(dependency);
            graph.addEdge(plugin, dependency);
        }

        // add edges for data connector dependencies
        for (String id : plugin.getDataConnectorDependencyIds()) {
            DataConnector dependency = getDataConnectors().get(id);
            graph.addVertex(dependency);
            graph.addEdge(plugin, dependency);
        }
    }
}
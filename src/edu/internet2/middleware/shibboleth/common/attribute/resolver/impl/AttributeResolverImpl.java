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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.metadata.provider.MetadataProvider;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.PrincipalConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;

/**
 * Primary implementation of {@link AttributeResolver}.
 */
public class AttributeResolverImpl implements AttributeResolver {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(AttributeResolverImpl.class.getName());

    /** Metadata provider defined for this resolver. */
    private MetadataProvider metadataProvider;

    /** Data connectors defined for this resolver. */
    private Map<String, DataConnector> dataConnectors;

    /** Attribute definitions defined for this resolver. */
    private Map<String, AttributeDefinition> definitions;

    /** Prinicpal connectors defined for this resolver. */
    private Map<String, PrincipalConnector> principalConnectors;

    /**
     * Constructor.
     */
    public AttributeResolverImpl() {
        dataConnectors = new HashMap<String, DataConnector>();
        definitions = new HashMap<String, AttributeDefinition>();
        principalConnectors = new HashMap<String, PrincipalConnector>();
    }

    /** {@inheritDoc} */
    public ResolutionContext createResolutionContext(NameID subject, String attributeRequester, ServletRequest request)
            throws AttributeResolutionException {
        ResolutionContextImpl context = new ResolutionContextImpl();
        context.setSubject(subject);
        context.setAttributeRequester(attributeRequester);
        context.setServletRequest(request);
        
        // TODO: determine which principal connector to use
        PrincipalConnector principalConnector = null;
        String principalName = principalConnector.resolve(context);
        context.setPrincipalName(principalName);
        
        return context;
    }

    /** {@inheritDoc} */
    public ResolutionContext createResolutionContext(String principal, String attributeRequester, ServletRequest request) {
        ResolutionContextImpl context = new ResolutionContextImpl();
        context.setPrincipalName(principal);
        context.setAttributeRequester(attributeRequester);
        context.setServletRequest(request);

        return context;
    }

    /** {@inheritDoc} */
    public Map<String, AttributeDefinition> getAttributeDefinitions() {
        return definitions;
    }

    /** {@inheritDoc} */
    public Map<String, DataConnector> getDataConnectors() {
        return dataConnectors;
    }

    /** {@inheritDoc} */
    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    /** {@inheritDoc} */
    public Map<String, PrincipalConnector> getPrincipalConnectors() {
        return principalConnectors;
    }

    /**
     * Resolve the {@link AttributeDefinition} which has the specified ID. The definition is then added to the
     * {@link ResolutionContext} for use by other {@link ResolutionPlugIn}s and the resolution of the specified
     * definition is added to <code>resolvedAttributes</code> to be returned by the resolver.
     * 
     * @param id id of the attribute definition to resolve
     * @param context resolution context that we are working in
     * @return resolution of the specified attribute definition
     * @throws AttributeResolutionException if unable to resolve the requested attribute definition
     */
    private Attribute resolveAttribute(String id, ResolutionContext context) throws AttributeResolutionException {
        AttributeDefinition definition = getAttributeDefinitions().get(id);

        if (definition == null) {
            return null;
        }

        if (definition.isDependencyOnly()) {
            log.debug("Attribute (" + id + ") is set to be a dependency only and cannot be released"
                    + " to relying parties.");
            return null;
        }

        // resolve attribute and all dependencies if not done so already
        if (!context.getResolvedAttributeDefinitions().containsKey(id)) {

            // wrap attribute definition for use within the given resolution context
            AttributeDefinition contextualDefinition = new ContextualAttributeDefinition(definition);
            definition = contextualDefinition;

            // resolve DataConnector dependencies
            for (String dependency : definition.getDataConnectorDependencyIds()) {
                log.debug("Attribute (" + id + ") depends on connector (" + dependency + ").");
                resolveConnector(dependency, context);
            }

            // resolve AttributeDefinition dependencies
            for (String dependency : definition.getAttributeDefinitionDependencyIds()) {
                log.debug("Attribute (" + id + ") depends on attribute (" + dependency + ").");
                resolveAttribute(dependency, context);
            }

            // register definition as resolved for this resolution context
            context.getResolvedAttributeDefinitions().put(id, definition);
        }

        // return the actual resolution of the definition
        return definition.resolve(context);
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> resolveAttributes(Set<String> attributes, ResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<String> attributeIDs = attributes;
        Map<String, Attribute> resolvedAttributes = new HashMap<String, Attribute>();

        // if no attributes requested, then resolve everything
        if (attributeIDs == null || attributeIDs.isEmpty()) {
            attributeIDs = getAttributeDefinitions().keySet();
        }

        for (String id : attributeIDs) {
            if (!getAttributeDefinitions().containsKey(id)) {
                log.warn("No plug-in registered for attribute: (" + id + ")");
            } else {
                log.info("Resolving attribute: (" + id + ")");

                if (resolvedAttributes.containsKey(id)) {
                    log.debug("Attribute (" + id
                            + ") already resolved for this request.  No need for further resolution.");
                } else {
                    Attribute resolution = resolveAttribute(id, resolutionContext);

                    if (resolution != null && resolution.getId() != null) {
                        resolvedAttributes.put(resolution.getId(), resolution);
                    }
                }
            }
        }

        return resolvedAttributes;
    }

    /**
     * Resolve the {@link DataConnector} which has the specified ID and add it to the {@link ResolutionContext}.
     * 
     * @param id id of the data connector to resolve
     * @param context resolution context that we are working in
     * @throws AttributeResolutionException if unable to resolve the requested connector
     */
    private void resolveConnector(String id, ResolutionContext context) throws AttributeResolutionException {
        DataConnector dataConnector = getDataConnectors().get(id);

        if (dataConnector == null) {
            return;
        }

        // resolve connector and all dependencies if not done so already
        if (!context.getResolvedDataConnectors().containsKey(id)) {
            log.debug("Resolving DataConnector: (" + id + ").");

            // wrap connector for use within the given resolution context
            DataConnector contextualConnector = new ContextualDataConnector(dataConnector);
            dataConnector = contextualConnector;

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
            context.getResolvedDataConnectors().put(id, dataConnector);
        }
    }

    /** {@inheritDoc} */
    public void setMetadataProvider(MetadataProvider provider) {
        this.metadataProvider = provider;
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
     * @param graph directed graph.
     * @param plugin plug-in to add.
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
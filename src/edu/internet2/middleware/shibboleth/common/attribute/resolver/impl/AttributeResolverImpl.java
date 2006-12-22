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

import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import javolution.util.FastMap;

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
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugInMap;

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

    /** Directed Graph of plug-in dependencies. */
    private DirectedGraph<ResolutionPlugIn, DefaultEdge> dependencyGraph;

    /**
     * Constructor.
     */
    public AttributeResolverImpl() {
        dependencyGraph = new SimpleDirectedGraph<ResolutionPlugIn, DefaultEdge>(DefaultEdge.class);
        
        // cycle detector shared by all plug-in maps
        CycleDetector<ResolutionPlugIn, DefaultEdge> cycleDetector = new CycleDetector<ResolutionPlugIn, DefaultEdge>(
                dependencyGraph);

        dataConnectors = new ResolutionPlugInMap<DataConnector>(this, dependencyGraph, cycleDetector);
        definitions = new ResolutionPlugInMap<AttributeDefinition>(this, dependencyGraph, cycleDetector);
        principalConnectors = new ResolutionPlugInMap<PrincipalConnector>(this, dependencyGraph, cycleDetector);
    }

    /** {@inheritDoc} */
    public ResolutionContext createResolutionContext(NameID subject, String attributeRequester, ServletRequest request)
            throws AttributeResolutionException {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public ResolutionContext createResolutionContext(String principal, String attributeRequester, ServletRequest request) {
        // TODO Auto-generated method stub
        return null;
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
     * @param resolvedAttributes attributes that have already been resolved during this request
     * @throws AttributeResolutionException if unable to resolve the requested attribute definition
     */
    private void resolveAttribute(String id, ResolutionContext context, Map<String, Attribute> resolvedAttributes)
            throws AttributeResolutionException {
        AttributeDefinition currentDefinition = definitions.get(id);

        if (currentDefinition == null) {
            return;
        }

        // Check to see if we have already resolved the attribute
        if (resolvedAttributes.containsKey(id)) {
            log.debug("Attribute (" + id + ") already resolved for this request.");
            return;
        }

        // resolve DataConnector dependencies
        for (String dependency : currentDefinition.getDataConnectorDependencyIds()) {
            log.debug("Attribute (" + id + ") depends on connector (" + dependency + ").");
            resolveConnector(dependency, context, resolvedAttributes);
        }

        // resolve AttributeDefinition dependencies
        for (String dependency : currentDefinition.getAttributeDefinitionDependencyIds()) {
            log.debug("Attribute (" + id + ") depends on attribute (" + dependency + ").");
            resolveAttribute(dependency, context, resolvedAttributes);
        }

        resolvedAttributes.put(id, currentDefinition.resolve(context));
        context.getResolvedAttributeDefinitions().put(id, currentDefinition);
    }

    /** {@inheritDoc} */
    public Set<Attribute> resolveAttributes(Set<String> attributes, ResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<String> attributeIDs = attributes;
        Map<String, Attribute> resolvedAttributes = new FastMap<String, Attribute>();

        // if no attributes requested, then resolve everything
        if (attributeIDs == null || attributeIDs.isEmpty()) {
            attributeIDs = definitions.keySet();
        }

        for (String id : attributeIDs) {
            if (!definitions.containsKey(id)) {
                log.warn("No plug-in registered for attribute: (" + id + ")");
            } else {
                log.info("Resolving attribute: (" + id + ")");

                if (resolutionContext.getResolvedAttributeDefinitions().containsKey(id)) {
                    log.debug("Attribute (" + id
                            + ") already resolved for this request.  No need for further resolution.");
                } else {
                    resolveAttribute(id, resolutionContext, resolvedAttributes);
                }
            }
        }

        return (Set<Attribute>) resolvedAttributes.values();
    }

    /**
     * Resolve the {@link DataConnector} which has the specified ID and add it to the {@link ResolutionContext}.
     * 
     * @param id id of the data connector to resolve
     * @param context resolution context that we are working in
     * @param resolvedAttributes attributes that have already been resolved during this request
     * @throws AttributeResolutionException if unable to resolve the requested connector
     */
    private void resolveConnector(String id, ResolutionContext context, Map<String, Attribute> resolvedAttributes)
            throws AttributeResolutionException {
        DataConnector dataConnector = dataConnectors.get(id);

        if (dataConnector == null) {
            return;
        }

        // Check to see if we have already resolved the attribute
        if (context.getResolvedDataConnectors().containsKey(id)) {
            return;
        }

        // wrap connector for use within the given resolution context
        DataConnector contextualConnector = new ContextualDataConnector(dataConnector);
        dataConnector = contextualConnector;

        // resolve DataConnector dependencies
        for (String dependency : dataConnector.getDataConnectorDependencyIds()) {
            resolveConnector(dependency, context, resolvedAttributes);
        }

        // resolve AttributeDefinition dependencies
        for (String dependency : dataConnector.getAttributeDefinitionDependencyIds()) {
            resolveAttribute(dependency, context, resolvedAttributes);
        }

        try {
            dataConnector.resolve(context);
        } catch (AttributeResolutionException e) {
            // TODO: use failover connector
        }

        context.getResolvedDataConnectors().put(id, dataConnector);
    }

    /** {@inheritDoc} */
    public void setMetadataProvider(MetadataProvider provider) {
        this.metadataProvider = provider;
    }

}

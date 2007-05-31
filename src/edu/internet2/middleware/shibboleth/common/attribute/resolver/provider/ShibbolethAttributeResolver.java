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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.opensaml.util.resource.Resource;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ContextualAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.ContextualDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.ContextualPrincipalConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.PrincipalConnector;
import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;

/**
 * Primary implementation of {@link AttributeResolver}.
 * 
 * "Raw" attributes are gathered by the registered {@link DataConnector}s while the {@link AttributeDefinition}s
 * refine the raw attributes or create attributes of their own. Connectors and definitions may depend on each other so
 * implementations must use a directed dependency graph when performing the resolution.
 */
public class ShibbolethAttributeResolver extends BaseReloadableService implements
        AttributeResolver<ShibbolethAttributeRequestContext> {

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
     * 
     * @param resources list of resolver configuration files
     */
    public ShibbolethAttributeResolver(List<Resource> resources) {
        super(resources);
        dataConnectors = new HashMap<String, DataConnector>();
        definitions = new HashMap<String, AttributeDefinition>();
        principalConnectors = new HashMap<String, PrincipalConnector>();
    }

    /**
     * Constructor.
     * 
     * @param timer timer resource polling tasks are scheduled with
     * @param resources list of resolver configuration files
     * @param pollingFrequency the frequency, in milliseconds, to poll the configuration resources for changes, must be
     *            greater than zero
     */
    public ShibbolethAttributeResolver(List<Resource> resources, Timer timer, long pollingFrequency) {
        super(timer, resources, pollingFrequency);
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
    public Map<String, Attribute> resolveAttributes(ShibbolethAttributeRequestContext attributeRequestContext)
            throws AttributeResolutionException {
        ShibbolethResolutionContext resolutionContext = new ShibbolethResolutionContext(attributeRequestContext);

        String principal = resolvePrincipalName(resolutionContext);
        attributeRequestContext.setPrincipalName(principal);

        if (log.isDebugEnabled()) {
            log.debug(getServiceName() + " resolving attributes for principal "
                    + attributeRequestContext.getPrincipalName());
        }
        if (getAttributeDefinitions().size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No attribute definitions loaded in " + getServiceName()
                        + " so no attributes can be resolved for principal "
                        + attributeRequestContext.getPrincipalName());
            }
            return new HashMap<String, Attribute>();
        }

        Map<String, Attribute> resolvedAttributes = resolveAttributes(resolutionContext);
        cleanResolvedAttributes(resolvedAttributes, resolutionContext);

        if (log.isDebugEnabled()) {
            log.debug(getServiceName() + " returning " + resolvedAttributes.size() + " attributes for principal "
                    + attributeRequestContext.getPrincipalName());
        }
        return resolvedAttributes;
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
            throw new AttributeResolutionException(getServiceName()
                    + "configuration contains a resolution plug-in dependency loop.");
        }
    }

    /**
     * Resolves the principal name for the subject of the request.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return principal name for the subject of the request
     * 
     * @throws AttributeResolutionException thrown if the subject identifier information can not be resolved into a
     *             principal name
     */
    protected String resolvePrincipalName(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        String principal = resolutionContext.getAttributeRequestContext().getPrincipalName();
        if (DatatypeHelper.isEmpty(principal)) {
            if (log.isDebugEnabled()) {
                log.debug(getServiceName() + ": Resolving principal name for request from relying party "
                        + resolutionContext.getAttributeRequestContext().getAttributeRequester());
            }
            String nameFormat = resolutionContext.getAttributeRequestContext().getSubjectNameIdFormat();
            if(DatatypeHelper.isEmpty(nameFormat)){
                nameFormat = "urn:oasis:names:tc:SAML:1.0:nameid-format:unspecified";
            }
            PrincipalConnector effectiveConnector = null;
            for (PrincipalConnector connector : principalConnectors.values()) {
                if (connector.getFormat().equals(nameFormat)) {
                    if (connector.getRelyingParties().contains(
                            resolutionContext.getAttributeRequestContext().getAttributeRequester())) {
                        effectiveConnector = connector;
                        break;
                    }

                    if (connector.getRelyingParties().isEmpty()) {
                        effectiveConnector = connector;
                    }
                }
            }

            if (effectiveConnector == null) {
                throw new AttributeResolutionException(
                        "No principal connector available to resolve a subject name with format " + nameFormat
                                + " for relying party "
                                + resolutionContext.getAttributeRequestContext().getAttributeRequester());
            }

            effectiveConnector = new ContextualPrincipalConnector(effectiveConnector);

            // resolve all the connectors dependencies
            resolveDependencies(effectiveConnector, resolutionContext);

            principal = effectiveConnector.resolve(resolutionContext);
        }

        return principal;
    }

    /**
     * Resolves the attributes requested in the resolution context or all attributes if no specific attributes were
     * requested. This method does not remove dependency only attributes or attributes that do not contain values.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return resolved attributes
     * 
     * @throws AttributeResolutionException thrown if the attributes could not be resolved
     */
    protected Map<String, Attribute> resolveAttributes(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<String> attributeIDs = resolutionContext.getAttributeRequestContext().getRequestedAttributes();
        Map<String, Attribute> resolvedAttributes = new HashMap<String, Attribute>();

        // if no attributes requested, then resolve everything
        if (attributeIDs == null || attributeIDs.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Specific attributes for principal "
                        + resolutionContext.getAttributeRequestContext().getPrincipalName()
                        + " were not requested, resolving all attributes.");
            }
            attributeIDs = getAttributeDefinitions().keySet();
        }

        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        for (String attributeID : attributeIDs) {
            Attribute resolvedAttribute = resolveAttribute(attributeID, resolutionContext);
            if (resolvedAttribute != null) {
                resolvedAttributes.put(resolvedAttribute.getId(), resolvedAttribute);
            }
        }
        readLock.unlock();

        if (log.isDebugEnabled()) {
            log.debug(getServiceName() + " resolved " + resolvedAttributes.size() + " attributes for principal "
                    + resolutionContext.getAttributeRequestContext().getPrincipalName());
        }
        return resolvedAttributes;
    }

    /**
     * Resolve the {@link AttributeDefinition} which has the specified ID. The definition is then added to the
     * {@link AttributeResolutionContext} for use by other {@link ResolutionPlugIn}s and the resolution of the
     * specified definition is added to <code>resolvedAttributes</code> to be returned by the resolver.
     * 
     * @param attributeID id of the attribute definition to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @return resolution of the specified attribute definition
     * 
     * @throws AttributeResolutionException if unable to resolve the requested attribute definition
     */
    protected Attribute resolveAttribute(String attributeID, ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        AttributeDefinition definition = resolutionContext.getResolvedAttributeDefinitions().get(attributeID);

        if (definition == null) {
            if (log.isDebugEnabled()) {
                log.debug(getServiceName() + " resolving attribute " + attributeID + " for principal "
                        + resolutionContext.getAttributeRequestContext().getPrincipalName());
            }

            definition = getAttributeDefinitions().get(attributeID);
            if (definition == null) {
                log.warn(resolutionContext.getAttributeRequestContext().getAttributeRequester()
                        + " requested attribute " + attributeID + " of " + getServiceName()
                        + " but no attribute definition exists for that attribute");
                return null;
            }

            // wrap attribute definition for use within the given resolution context
            definition = new ContextualAttributeDefinition(definition);

            // resolve all the definitions dependencies
            resolveDependencies(definition, resolutionContext);

            // register definition as resolved for this resolution context
            resolutionContext.getResolvedAttributeDefinitions().put(attributeID, definition);
        }

        // return the actual resolution of the definition
        return definition.resolve(resolutionContext);
    }

    /**
     * Resolve the {@link DataConnector} which has the specified ID and add it to the resolution context.
     * 
     * @param connectorID id of the data connector to resolve
     * @param resolutionContext resolution context that we are working in
     * 
     * @throws AttributeResolutionException if unable to resolve the requested connector
     */
    protected void resolveDataConnector(String connectorID, ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        DataConnector dataConnector = resolutionContext.getResolvedDataConnectors().get(connectorID);

        if (dataConnector == null) {
            if (log.isDebugEnabled()) {
                log.debug(getServiceName() + " resolving data connector " + connectorID + " for principal "
                        + resolutionContext.getAttributeRequestContext().getPrincipalName());
            }

            dataConnector = getDataConnectors().get(connectorID);
            if (dataConnector == null) {
                log.warn(getServiceName() + " requested to resolve data connector" + connectorID
                        + " but does not have such a data connector");
            }

            // wrap connector for use within the given resolution context
            dataConnector = new ContextualDataConnector(dataConnector);

            // resolve all the connectors dependencies
            resolveDependencies(dataConnector, resolutionContext);

            try {
                dataConnector.resolve(resolutionContext);
            } catch (AttributeResolutionException e) {
                // TODO add failover connector support here
                throw e;
            }

            // register connector as resolved for this resolution context
            resolutionContext.getResolvedDataConnectors().put(connectorID, dataConnector);
        }
    }

    /**
     * Resolves all the dependencies for a given plugin.
     * 
     * @param plugin plugin whose dependencies should be resolved
     * @param resolutionContext current resolution context
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving a dependency
     */
    protected void resolveDependencies(ResolutionPlugIn<?> plugin, ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        // resolve DataConnector dependencies
        for (String dependency : plugin.getDataConnectorDependencyIds()) {
            resolveDataConnector(dependency, resolutionContext);
        }

        // resolve AttributeDefinition dependencies
        for (String dependency : plugin.getAttributeDefinitionDependencyIds()) {
            resolveAttribute(dependency, resolutionContext);
        }
    }

    /**
     * Removes attributes that contain no values or those which are dependency only.
     * 
     * @param resolvedAttributes attribute set to clean up
     * @param resolutionContext current resolution context
     */
    protected void cleanResolvedAttributes(Map<String, Attribute> resolvedAttributes,
            ShibbolethResolutionContext resolutionContext) {
        AttributeDefinition attributeDefinition;
        
        Iterator<Entry<String, Attribute>> attributeItr = resolvedAttributes.entrySet().iterator();
        Attribute<?> resolvedAttribute;
        while(attributeItr.hasNext()){
            resolvedAttribute = attributeItr.next().getValue();
            if (resolvedAttribute.getValues().size() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug(getServiceName() + "removing attribute " + resolvedAttribute.getId()
                            + "  from resolution result for principal "
                            + resolutionContext.getAttributeRequestContext().getPrincipalName()
                            + ".  It contains no values.");
                }
                attributeItr.remove();
                continue;
            }

            attributeDefinition = getAttributeDefinitions().get(resolvedAttribute.getId());
            if (attributeDefinition.isDependencyOnly()) {
                if (log.isDebugEnabled()) {
                    log.debug(getServiceName() + "removing attribute " + resolvedAttribute.getId()
                            + "  from resolution result for principal "
                            + resolutionContext.getAttributeRequestContext().getPrincipalName()
                            + ".  It is a dependency-only attribute.");
                }
                attributeItr.remove();
            }
        }
    }

    /**
     * Add a resolution plug-in and dependencies to a directed graph.
     * 
     * @param graph directed graph
     * @param plugin plug-in to add
     */
    protected void addVertex(DirectedGraph<ResolutionPlugIn, DefaultEdge> graph, ResolutionPlugIn<?> plugin) {
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

    /** {@inheritDoc} */
    protected void newContextCreated(ApplicationContext newServiceContext) throws ResourceException {
        String[] beanNames;

        principalConnectors.clear();
        PrincipalConnector pConnector;
        beanNames = newServiceContext.getBeanNamesForType(PrincipalConnector.class);
        if (log.isDebugEnabled()) {
            log.debug("Loading " + beanNames.length + " principal connectors");
        }
        for (String beanName : beanNames) {
            pConnector = (PrincipalConnector) newServiceContext.getBean(beanName);
            principalConnectors.put(pConnector.getId(), pConnector);
        }

        dataConnectors.clear();
        DataConnector dConnector;
        beanNames = newServiceContext.getBeanNamesForType(DataConnector.class);
        if (log.isDebugEnabled()) {
            log.debug("Loading " + beanNames.length + " data connectors");
        }
        for (String beanName : beanNames) {
            dConnector = (DataConnector) newServiceContext.getBean(beanName);
            dataConnectors.put(dConnector.getId(), dConnector);
        }

        definitions.clear();
        AttributeDefinition aDefinition;
        beanNames = newServiceContext.getBeanNamesForType(AttributeDefinition.class);
        if (log.isDebugEnabled()) {
            log.debug("Loading " + beanNames.length + " attribute defintions");
        }
        for (String beanName : beanNames) {
            aDefinition = (AttributeDefinition) newServiceContext.getBean(beanName);
            definitions.put(aDefinition.getId(), aDefinition);
        }

        try {
            validate();
        } catch (AttributeResolutionException e) {
            throw new ResourceException(getServiceName() + " configuration is not valid", e);
        }
    }
}
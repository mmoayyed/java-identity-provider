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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.NameID;
import org.opensaml.util.resource.Resource;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.AttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition.ContextualAttributeDefinition;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.ContextualDataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.ContextualPrincipalConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector.PrincipalConnector;
import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;

/**
 * Primary implementation of {@link AttributeResolver}.
 * 
 * "Raw" attributes are gathered by the registered {@link DataConnector}s while the {@link AttributeDefinition}s
 * refine the raw attributes or create attributes of their own. Connectors and definitions may depend on each other so
 * implementations must use a directed dependency graph when performing the resolution.
 */
public class ShibbolethAttributeResolver extends BaseReloadableService implements
        AttributeResolver<SAMLProfileRequestContext> {

    /**
     * Resolution plug-in types.
     */
    public static final Collection<Class> PLUGIN_TYPES = Arrays.asList(new Class[] { DataConnector.class,
            AttributeDefinition.class, PrincipalConnector.class, });

    /** Log4j logger. */
    private final Logger log = LoggerFactory.getLogger(ShibbolethAttributeResolver.class.getName());

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
    public Map<String, BaseAttribute> resolveAttributes(SAMLProfileRequestContext attributeRequestContext)
            throws AttributeResolutionException {
        ShibbolethResolutionContext resolutionContext = new ShibbolethResolutionContext(attributeRequestContext);

        log.debug("{} resolving attributes for principal {}", getId(), attributeRequestContext.getPrincipalName());

        if (getAttributeDefinitions().size() == 0) {
            log.debug("No attribute definitions loaded in {} so no attributes can be resolved for principal {}",
                    getId(), attributeRequestContext.getPrincipalName());
            return new HashMap<String, BaseAttribute>();
        }

        Map<String, BaseAttribute> resolvedAttributes = resolveAttributes(resolutionContext);
        cleanResolvedAttributes(resolvedAttributes, resolutionContext);

        log.debug("{} returning attributes for principal {}", getId(), attributeRequestContext.getPrincipalName());
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
            throw new AttributeResolutionException(getId()
                    + "configuration contains a resolution plug-in dependency loop.");
        }
    }

    /**
     * Resolves the principal name for the subject of the request.
     * 
     * @param requestContext current request context
     * 
     * @return principal name for the subject of the request
     * 
     * @throws AttributeResolutionException thrown if the subject identifier information can not be resolved into a
     *             principal name
     */
    public String resolvePrincipalName(SAMLProfileRequestContext requestContext) throws AttributeResolutionException {
        String nameIdFormat = getNameIdentifierFormat(requestContext.getSubjectNameIdentifier());

        log.debug("Resolving principal name from name identifier of format: {}", nameIdFormat);

        PrincipalConnector effectiveConnector = null;
        for (PrincipalConnector connector : principalConnectors.values()) {
            if (connector.getFormat().equals(nameIdFormat)) {
                if (connector.getRelyingParties().contains(requestContext.getInboundMessageIssuer())) {
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
                    "No principal connector available to resolve a subject name with format " + nameIdFormat
                            + " for relying party " + requestContext.getInboundMessageIssuer());
        }
        log.debug("Using principal connector {} to resolve principal name.", effectiveConnector.getId());
        effectiveConnector = new ContextualPrincipalConnector(effectiveConnector);

        ShibbolethResolutionContext resolutionContext = new ShibbolethResolutionContext(requestContext);

        // resolve all the connectors dependencies
        resolveDependencies(effectiveConnector, resolutionContext);

        return effectiveConnector.resolve(resolutionContext);
    }

    /**
     * Gets the format of the name identifier used to identify the subject.
     * 
     * @param nameIdentifier name identifier used to identify the subject
     * 
     * @return format of the name identifier used to identify the subject
     */
    protected String getNameIdentifierFormat(SAMLObject nameIdentifier) {
        String subjectNameFormat = null;

        if (nameIdentifier instanceof NameIdentifier) {
            NameIdentifier identifier = (NameIdentifier) nameIdentifier;
            subjectNameFormat = identifier.getFormat();
        } else if (nameIdentifier instanceof NameID) {
            NameID identifier = (NameID) nameIdentifier;
            subjectNameFormat = identifier.getFormat();
        }

        if (DatatypeHelper.isEmpty(subjectNameFormat)) {
            subjectNameFormat = "urn:oasis:names:tc:SAML:1.0:nameid-format:unspecified";
        }

        return subjectNameFormat;
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
    protected Map<String, BaseAttribute> resolveAttributes(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Collection<String> attributeIDs = resolutionContext.getAttributeRequestContext().getRequestedAttributesIds();
        Map<String, BaseAttribute> resolvedAttributes = new HashMap<String, BaseAttribute>();

        // if no attributes requested, then resolve everything
        if (attributeIDs == null || attributeIDs.isEmpty()) {
            log.debug("Specific attributes for principal {} were not requested, resolving all attributes.",
                    resolutionContext.getAttributeRequestContext().getPrincipalName());
            attributeIDs = getAttributeDefinitions().keySet();
        }

        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        for (String attributeID : attributeIDs) {
            BaseAttribute resolvedAttribute = resolveAttribute(attributeID, resolutionContext);
            if (resolvedAttribute != null) {
                resolvedAttributes.put(resolvedAttribute.getId(), resolvedAttribute);
            }
        }
        readLock.unlock();

        log.debug("{} resolved attributes for principal {}", getId(), resolutionContext.getAttributeRequestContext()
                .getPrincipalName());
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
    protected BaseAttribute resolveAttribute(String attributeID, ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        AttributeDefinition definition = resolutionContext.getResolvedAttributeDefinitions().get(attributeID);

        if (definition == null) {
            log.debug("Resolving attribute {} for principal {}", attributeID, resolutionContext
                    .getAttributeRequestContext().getPrincipalName());

            definition = getAttributeDefinitions().get(attributeID);
            if (definition == null) {
                log.warn("{} requested attribute {} but no attribute definition exists for that attribute",
                        resolutionContext.getAttributeRequestContext().getInboundMessageIssuer(), attributeID);
                return null;
            } else {
                // wrap attribute definition for use within the given resolution context
                definition = new ContextualAttributeDefinition(definition);

                // register definition as resolved for this resolution context
                resolutionContext.getResolvedPlugins().put(attributeID, definition);
            }
        }

        // resolve all the definitions dependencies
        resolveDependencies(definition, resolutionContext);

        // return the actual resolution of the definition
        BaseAttribute attribute = definition.resolve(resolutionContext);
        log.debug("Resolved attribute {}.  Attribtute contains {} values.", attributeID, attribute.getValues().size());
        return attribute;
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
            log.debug("Resolving data connector {} for principal {}", connectorID, resolutionContext
                    .getAttributeRequestContext().getPrincipalName());

            dataConnector = getDataConnectors().get(connectorID);
            if (dataConnector == null) {
                log.warn("{} requested to resolve data connector {} but does not have such a data connector", getId(),
                        connectorID);
            } else {
                // wrap connector for use within the given resolution context
                dataConnector = new ContextualDataConnector(dataConnector);

                // register connector as resolved for this resolution context
                resolutionContext.getResolvedPlugins().put(connectorID, dataConnector);
            }
        }

        // resolve all the connectors dependencies
        resolveDependencies(dataConnector, resolutionContext);

        try {
            dataConnector.resolve(resolutionContext);
        } catch (AttributeResolutionException e) {
            String failoverDataConnectorId = dataConnector.getFailoverDependencyId();
            if (DatatypeHelper.isEmpty(failoverDataConnectorId)) {
                throw e;
            }

            log.error("Received the following error from data connector " + dataConnector.getId()
                    + ", using its failover connector " + failoverDataConnectorId, e);
            try {
                resolveDataConnector(failoverDataConnectorId, resolutionContext);
            } catch (AttributeResolutionException foe) {
                log.error("Recieved the following error from failover data connector " + failoverDataConnectorId, foe);
                throw e;
            }
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

        for (String dependency : plugin.getDependencyIds()) {
            if (dataConnectors.containsKey(dependency)) {
                resolveDataConnector(dependency, resolutionContext);
            } else if (definitions.containsKey(dependency)) {
                resolveAttribute(dependency, resolutionContext);
            }
        }
    }

    /**
     * Removes attributes that contain no values or those which are dependency only.
     * 
     * @param resolvedAttributes attribute set to clean up
     * @param resolutionContext current resolution context
     */
    protected void cleanResolvedAttributes(Map<String, BaseAttribute> resolvedAttributes,
            ShibbolethResolutionContext resolutionContext) {
        AttributeDefinition attributeDefinition;

        Iterator<Entry<String, BaseAttribute>> attributeItr = resolvedAttributes.entrySet().iterator();
        BaseAttribute<?> resolvedAttribute;
        Set<Object> values;
        while (attributeItr.hasNext()) {
            resolvedAttribute = attributeItr.next().getValue();
            
            // remove nulls
            if(resolvedAttribute == null){
                attributeItr.remove();
                continue;
            }
            
            // remove dependency-only attributes
            attributeDefinition = getAttributeDefinitions().get(resolvedAttribute.getId());
            if (attributeDefinition.isDependencyOnly()) {
                log.debug("Removing dependency-only attribute {} from resolution result for principal {}.",
                        resolvedAttribute.getId(), resolutionContext.getAttributeRequestContext().getPrincipalName());
                attributeItr.remove();
                continue;
            }
            
            // remove value-less attributes
            if (resolvedAttribute.getValues().size() == 0) {
                log.debug("Removing attribute {} from resolution result for principal {}.  It contains no values.",
                        resolvedAttribute.getId(), resolutionContext.getAttributeRequestContext().getPrincipalName());
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
     * Add a resolution plug-in and dependencies to a directed graph.
     * 
     * @param graph directed graph
     * @param plugin plug-in to add
     */
    protected void addVertex(DirectedGraph<ResolutionPlugIn, DefaultEdge> graph, ResolutionPlugIn<?> plugin) {
        graph.addVertex(plugin);
        ResolutionPlugIn<?> dependency = null;

        // add edges for dependencies
        for (String id : plugin.getDependencyIds()) {
            if (dataConnectors.containsKey(id)) {
                dependency = dataConnectors.get(id);
            } else if (definitions.containsKey(id)) {
                dependency = definitions.get(id);
            }

            if (dependency != null) {
                graph.addVertex(dependency);
                graph.addEdge(plugin, dependency);
            }
        }
    }

    /** {@inheritDoc} */
    protected void newContextCreated(ApplicationContext newServiceContext) throws ServiceException {
        String[] beanNames;

        principalConnectors.clear();
        PrincipalConnector pConnector;
        beanNames = newServiceContext.getBeanNamesForType(PrincipalConnector.class);
        log.debug("Loading {} principal connectors", beanNames.length);
        for (String beanName : beanNames) {
            pConnector = (PrincipalConnector) newServiceContext.getBean(beanName);
            principalConnectors.put(pConnector.getId(), pConnector);
        }

        dataConnectors.clear();
        DataConnector dConnector;
        beanNames = newServiceContext.getBeanNamesForType(DataConnector.class);
        log.debug("Loading {} data connectors", beanNames.length);
        for (String beanName : beanNames) {
            dConnector = (DataConnector) newServiceContext.getBean(beanName);
            dataConnectors.put(dConnector.getId(), dConnector);
        }

        definitions.clear();
        AttributeDefinition aDefinition;
        beanNames = newServiceContext.getBeanNamesForType(AttributeDefinition.class);
        log.debug("Loading {} attribute definitions", beanNames.length);
        for (String beanName : beanNames) {
            aDefinition = (AttributeDefinition) newServiceContext.getBean(beanName);
            definitions.put(aDefinition.getId(), aDefinition);
        }

        try {
            validate();
        } catch (AttributeResolutionException e) {
            throw new ServiceException(getId() + " configuration is not valid", e);
        }
    }
}
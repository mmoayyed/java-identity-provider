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

package edu.internet2.middleware.shibboleth.common.attribute.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;

/**
 * {@link Map} implementation for {@link ResolutionPlugIn}s within an {@link AttributeResolver}. Plug-ins are checked
 * for consistency as they are added; therefore a plug-in's dependencies must be added to the resolver before the
 * dependent plug-in.
 * 
 * @param <PlugInType> type of plug-in this Map contains
 */
public class ResolutionPlugInMap<PlugInType extends ResolutionPlugIn<?>> implements Map<String, PlugInType> {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(ResolutionPlugInMap.class);

    /** Internal container for this map's elements. */
    private Map<String, PlugInType> elementMap;

    /** AttributeResolver this collection is a part of. */
    private AttributeResolver resolver;

    /** Directed Graph of plug-in dependencies. */
    private DirectedGraph<ResolutionPlugIn, DefaultEdge> dependencyGraph;

    /** CycleDetector to check for dependency loops. */
    private CycleDetector<ResolutionPlugIn, DefaultEdge> cycleDetector;

    /**
     * Constructor.
     * 
     * @param newResolver attribute resolver which contains this map
     * @param newDependencyGraph directed graph of plug-in dependencies
     * @param newCycleDetector detector for dependency loops
     */
    public ResolutionPlugInMap(AttributeResolver newResolver,
            DirectedGraph<ResolutionPlugIn, DefaultEdge> newDependencyGraph,
            CycleDetector<ResolutionPlugIn, DefaultEdge> newCycleDetector) {

        resolver = newResolver;
        dependencyGraph = newDependencyGraph;
        cycleDetector = newCycleDetector;

        elementMap = new FastMap<String, PlugInType>();
    }

    /** {@inheritDoc} */
    public PlugInType put(String id, PlugInType plugin) {

        if (elementMap.containsKey(id)) {
            log.error("Another plug-in already exists with id (" + id + ").");
            throw new IllegalArgumentException("Another plug-in already exists with id (" + id + ").");
        }

        dependencyGraph.addVertex(plugin);

        // add attribute definitions to dependency graph
        for (String dependencyId : plugin.getAttributeDefinitionDependencyIds()) {
            AttributeDefinition dep = resolver.getAttributeDefinitions().get(dependencyId);
            if (dep == null) {
                throw new IllegalArgumentException("Plug-in dependency (" + dependencyId
                        + ") does not exist in resolver.");
            }

            dependencyGraph.addEdge(plugin, dep);
        }

        // add data connectors to dependency graph
        for (String dependencyId : plugin.getDataConnectorDependencyIds()) {
            DataConnector dep = resolver.getDataConnectors().get(dependencyId);
            if (dep == null) {
                throw new IllegalArgumentException("Plug-in dependency (" + dependencyId
                        + ") does not exist in resolver.");
            }

            dependencyGraph.addEdge(plugin, dep);
        }

        // check for a dependency loop
        if (cycleDetector.detectCyclesContainingVertex(plugin)) {
            throw new IllegalArgumentException("Plug-in (" + id + ") is part of a dependency loop.");
        }

        elementMap.put(plugin.getId(), plugin);
        return plugin;
    }

    /** {@inheritDoc} */
    public void clear() {
        elementMap.clear();
    }

    /** {@inheritDoc} */
    public boolean isEmpty() {
        return elementMap.isEmpty();
    }

    /** {@inheritDoc} */
    public boolean containsKey(Object key) {
        return elementMap.containsKey(key);
    }

    /** {@inheritDoc} */
    public boolean containsValue(Object value) {
        return elementMap.containsValue(value);
    }

    /** {@inheritDoc} */
    public Set<java.util.Map.Entry<String, PlugInType>> entrySet() {
        return elementMap.entrySet();
    }

    /** {@inheritDoc} */
    public Set<String> keySet() {
        return elementMap.keySet();
    }

    /** {@inheritDoc} */
    public void putAll(Map<? extends String, ? extends PlugInType> t) {
        elementMap.putAll(t);
    }

    /** {@inheritDoc} */
    public Collection<PlugInType> values() {
        return elementMap.values();
    }

    /** {@inheritDoc} */
    public PlugInType get(Object key) {
        return elementMap.get(key);
    }

    /** {@inheritDoc} */
    public PlugInType remove(Object key) {
        return elementMap.remove(key);
    }

    /** {@inheritDoc} */
    public int size() {
        return elementMap.size();
    }

}
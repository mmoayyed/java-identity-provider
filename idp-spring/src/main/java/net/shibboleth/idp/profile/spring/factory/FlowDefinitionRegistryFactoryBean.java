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

package net.shibboleth.idp.profile.spring.factory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistryImpl;
import org.springframework.webflow.engine.builder.DefaultFlowHolder;
import org.springframework.webflow.engine.builder.FlowAssembler;
import org.springframework.webflow.engine.builder.FlowBuilder;
import org.springframework.webflow.engine.builder.FlowBuilderContext;
import org.springframework.webflow.engine.builder.support.FlowBuilderContextImpl;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.engine.model.builder.DefaultFlowModelHolder;
import org.springframework.webflow.engine.model.builder.FlowModelBuilder;
import org.springframework.webflow.engine.model.builder.xml.XmlFlowModelBuilder;
import org.springframework.webflow.engine.model.registry.FlowModelHolder;
import org.springframework.webflow.engine.model.registry.FlowModelRegistry;
import org.springframework.webflow.engine.model.registry.FlowModelRegistryImpl;

import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A bean factory for creating {@link FlowDefinitionRegistry} instances, based on the programmatic
 * builder built into SWF.
 * 
 * <p>Overrides the resource factory implementation, which they neglected to support, and that's
 * where all the fancy derivation of flow IDs lives.</p>
 */
public class FlowDefinitionRegistryFactoryBean extends AbstractFactoryBean<FlowDefinitionRegistry> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FlowDefinitionRegistryFactoryBean.class);

    /** Explicit flow mappings from flow ID to resource path. */
    @Nonnull @NonnullElements private Map<String,String> flowLocations;

    /** Pattern-based flow mappings from pattern to base location to apply. */
    @Nonnull @NonnullElements private Map<String,String> flowLocationPatterns;

    /** Required collaborator. */
    @Nullable private FlowBuilderServices flowBuilderServices;
    
    /** Base path for registry. */
    @Nullable private String basePath;

    /** Optional parent reference. */
    @Nullable private FlowDefinitionRegistry parent;

    /** Overriden resource factory, the whole reason for this class. */
    @Nullable private FlowDefinitionResourceFactory flowResourceFactory;
    
    /** Constructor. */
    public FlowDefinitionRegistryFactoryBean() {
        flowLocations = Collections.emptyMap();
        flowLocationPatterns = Collections.emptyMap();
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getObjectType() {
        return FlowDefinitionRegistry.class;
    }

    /**
     * Configure the base path where flow definitions are found.
     * 
     * @param path the base path to use
     */
    public void setBasePath(@Nullable final String path) {
        basePath = StringSupport.trimOrNull(path);
    }
    
    /**
     * Set explicit flow ID to resource mappings.
     * 
     * @param locationMap mappings from flow ID to resource
     */
    public void setFlowLocations(@Nonnull @NonnullElements final Map<String,String> locationMap) {
        Constraint.isNotNull(locationMap, "Flow mappings cannot be null");
        
        flowLocations = new LinkedHashMap<>(locationMap.size());
        for (final Map.Entry<String,String> entry : locationMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                flowLocations.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Registers a set of flows resolved from a resource location pattern as the mapping key,
     * with an optional value containing a portion to strip when computing the flow IDs, with
     * the default base location applied if empty/null.
     * 
     * @param patternMap the mappings to use
     */
    public void setFlowLocationPatterns(@Nonnull final Map<String,String> patternMap) {
        Constraint.isNotNull(patternMap, "Pattern mappings cannot be null");
        
        flowLocationPatterns = new LinkedHashMap<>(patternMap.size());
        for (final Map.Entry<String,String> entry : patternMap.entrySet()) {
            if (entry.getKey() != null && (basePath != null || entry.getValue() != null)) {
                flowLocationPatterns.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Set the {@link FlowBuilderServices} to use for defining custom services needed
     * to build the flows registered in this registry.
     * 
     * @param builderServices the {@link FlowBuilderServices} instance
     */
    public void setFlowBuilderServices(@Nonnull final FlowBuilderServices builderServices) {
        flowBuilderServices = Constraint.isNotNull(builderServices, "FlowBuilderServices cannot be null");
    }

    /**
     * Set a parent registry.
     * 
     * @param parentRegistry the parent registry
     */
    public void setParent(@Nullable final FlowDefinitionRegistry parentRegistry) {
        parent = parentRegistry;
    }

    /** {@inheritDoc} */
    @Override
    protected FlowDefinitionRegistry createInstance() throws Exception {

        flowResourceFactory = new FlowDefinitionResourceFactory(flowBuilderServices.getApplicationContext());
        
        final DefaultFlowRegistry flowRegistry = new DefaultFlowRegistry();
        flowRegistry.setParent(this.parent);

        registerFlowLocations(flowRegistry);
        registerFlowLocationPatterns(flowRegistry);

        return flowRegistry;
    }

    /** {@inheritDoc} */
    @Override protected void destroyInstance(final FlowDefinitionRegistry instance) throws Exception {
        ((DefaultFlowRegistry) instance).destroy();
    }

    /**
     * Register explicit flow mappings.
     * 
     * @param flowRegistry the flow registry
     */
    private void registerFlowLocations(@Nonnull final DefaultFlowRegistry flowRegistry) {
        for (final Map.Entry<String,String> location : flowLocations.entrySet()) {
            final LocalAttributeMap<Object> attributes = new LocalAttributeMap<>();
            updateFlowAttributes(attributes);
            final FlowDefinitionResource resource =
                    flowResourceFactory.createResource(basePath, location.getValue(), attributes, location.getKey());
            registerFlow(resource, flowRegistry);
        }
    }

    /**
     * Register flows derived from resource patterns.
     * 
     * @param flowRegistry the flow registry
     */
    private void registerFlowLocationPatterns(@Nonnull final DefaultFlowRegistry flowRegistry) {
        for (final Map.Entry<String,String> pattern : flowLocationPatterns.entrySet()) {
            final LocalAttributeMap<Object> attributes = new LocalAttributeMap<>();
            updateFlowAttributes(attributes);
            final Collection<FlowDefinitionResource> resources;
            try {
                resources = flowResourceFactory.createResources(
                        pattern.getValue() != null ? pattern.getValue() : basePath, pattern.getKey(), attributes);
            } catch (final IOException e) {
                throw new IllegalStateException(
                        "An I/O Exception occurred resolving the flow location pattern '" + pattern.getKey() + "'", e);
            }
            
            // Establish baseline of registered flows. 
            final Set<String> existingFlows = new HashSet<>();
            Arrays.stream(flowRegistry.getFlowDefinitionIds()).forEachOrdered(existingFlows::add);
            
            for (final FlowDefinitionResource resource : resources) {
                if (existingFlows.contains(resource.getId())) {
                    throw new IllegalStateException("Illegal attempt to register pre-existing flow ID '" +
                            resource.getId() + "'" + "via resource: " + resource.getPath());
                }
                registerFlow(resource, flowRegistry);
                
                // Update running tracker.
                existingFlows.add(resource.getId());
            }
        }
    }

    /**
     * Register a flow resource into the registry.
     * 
     * @param resource the flow resource
     * @param flowRegistry the registry
     */
    private void registerFlow(@Nonnull final FlowDefinitionResource resource,
            @Nonnull final DefaultFlowRegistry flowRegistry) {
        FlowModelBuilder flowModelBuilder = null;
        if (resource.getPath().getFilename().endsWith(".xml")) {
            flowModelBuilder = new XmlFlowModelBuilder(resource.getPath(), flowRegistry.getFlowModelRegistry());
        } else {
            throw new IllegalArgumentException(resource
                    + " is not a supported resource type; supported types are [.xml]");
        }
        final FlowModelHolder flowModelHolder = new DefaultFlowModelHolder(flowModelBuilder);
        final FlowBuilder flowBuilder = new FlowModelFlowBuilder(flowModelHolder);
        final FlowBuilderContext builderContext = new FlowBuilderContextImpl(
                resource.getId(), resource.getAttributes(), flowRegistry, this.flowBuilderServices);
        final FlowAssembler assembler = new FlowAssembler(flowBuilder, builderContext);
        final DefaultFlowHolder flowHolder = new DefaultFlowHolder(assembler);

        flowRegistry.getFlowModelRegistry().registerFlowModel(resource.getId(), flowModelHolder);
        flowRegistry.registerFlowDefinition(flowHolder);
        log.debug("Registered flow ID '{}' using '{}'", resource.getId(), resource.getPath());
    }

    /**
     * Update flow attributes with development bit.
     * 
     * @param attributes attribute map to update
     */
    private void updateFlowAttributes(@Nonnull final LocalAttributeMap<Object> attributes) {
        if (flowBuilderServices.getDevelopment()) {
            attributes.put("development", true);
        }
    }

    /** Copied from SWF, a basic registry implementation. */
    private static class DefaultFlowRegistry extends FlowDefinitionRegistryImpl {

        /** The model registry. */
        private FlowModelRegistry flowModelRegistry = new FlowModelRegistryImpl();

        /**
         * Get the model registry.
         * 
         * @return the model registry
         */
        @Nonnull public FlowModelRegistry getFlowModelRegistry() {
            return flowModelRegistry;
        }

        /** {@inheritDoc} */
        @Override
        public void setParent(@Nullable final FlowDefinitionRegistry parent) {
            super.setParent(parent);
            if (parent instanceof DefaultFlowRegistry) {
                final DefaultFlowRegistry parentFlowRegistry = (DefaultFlowRegistry) parent;
                // Link so a flow in the child registry that extends from a flow
                // in the parent registry can find its parent.
                flowModelRegistry.setParent(parentFlowRegistry.getFlowModelRegistry());
            }
        }
    }
    
}
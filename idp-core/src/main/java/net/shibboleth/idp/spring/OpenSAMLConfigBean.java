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

package net.shibboleth.idp.spring;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.metrics.FilteredMetricRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.xml.ParserPool;


/**
 * A simple bean that may be used with Spring to initialize the OpenSAML library
 * with injected instances of some critical objects.
 */
public class OpenSAMLConfigBean extends AbstractInitializableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(OpenSAMLConfigBean.class);
    
    /** Optional {@link ParserPool} to configure. */
    @Nullable private ParserPool parserPool;
    
    /** Optional {@link MetricFilter} to configure. */
    @Nullable private MetricFilter metricFilter;
    
    /**
     * Get the global {@link ParserPool} to configure.
     * 
     * @return the parser pool
     */
    @Nullable public ParserPool getParserPool() {
        return parserPool;
    }

    /**
     * Set the global {@link ParserPool} to configure.
     * 
     * @param newParserPool the parser pool to set
     */
    public void setParserPool(@Nullable final ParserPool newParserPool) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        parserPool = newParserPool;
    }
    
    /**
     * Get the global {@link MetricFilter} to configure.
     * 
     * @return the metric filter to use
     * 
     * @since 3.3.0
     */
    @Nullable public MetricFilter getMetricFilter() {
        return metricFilter;
    }
    
    /**
     * Set the global {@link MetricFilter} to configure.
     * 
     * @param filter the metric filter to use
     * 
     * @since 3.3.0
     */
    public void setMetricFilter(@Nullable final MetricFilter filter) {
        metricFilter = filter;
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {

        // Initialize OpenSAML.
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            throw new ComponentInitializationException("Exception initializing OpenSAML", e);
        }
        
        if (metricFilter != null) {
            final MetricRegistry metricRegistry = ConfigurationService.get(MetricRegistry.class);
            if (metricRegistry != null && metricRegistry instanceof FilteredMetricRegistry) {
                ((FilteredMetricRegistry) metricRegistry).setMetricFilter(metricFilter);
            }
        }
        
        XMLObjectProviderRegistry registry = null;
        synchronized(ConfigurationService.class) {
            registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
            if (registry == null) {
                log.debug("XMLObjectProviderRegistry did not exist in ConfigurationService, will be created");
                registry = new XMLObjectProviderRegistry();
                ConfigurationService.register(XMLObjectProviderRegistry.class, registry);
            }
        }
        
        if (parserPool != null) {
            registry.setParserPool(parserPool);
        }
    }
    
}
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
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.xmlsec.config.DecryptionParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;

import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
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
    
    /** Optional decryption {@link ParserPool} to configure. */
    @Nullable private ParserPool decryptionParserPool;
    
    /** Optional {@link MetricRegistry} to configure. */
    @Nullable private MetricRegistry metricRegistry;
    
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
        checkSetterPreconditions();
        parserPool = newParserPool;
    }
    
    /**
     * Get the global decryption {@link ParserPool} to configure.
     * 
     * @return the decryption parser pool
     */
    @Nullable public ParserPool getDecryptionParserPool() {
        return decryptionParserPool;
    }

    /**
     * Set the global decryption {@link ParserPool} to configure.
     * 
     * @param newParserPool the decryption parser pool to set
     */
    public void setDecryptionParserPool(@Nullable final ParserPool newParserPool) {
        checkSetterPreconditions();
        decryptionParserPool = newParserPool;
    }
    
    /**
     * Get the global {@link MetricRegistry} to configure.
     * 
     * @return the metric registry to use
     * 
     * @since 3.3.0
     */
    @Nullable public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }
    
    /**
     * Set the global {@link MetricRegistry} to configure.
     * 
     * @param registry the metric registry to use
     * 
     * @since 3.3.0
     */
    public void setMetricRegistry(@Nullable final MetricRegistry registry) {
        metricRegistry = registry;
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {

        // Initialize OpenSAML.
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            throw new ComponentInitializationException("Exception initializing OpenSAML", e);
        }
        
        XMLObjectProviderRegistry registry = null;
        synchronized(ConfigurationService.class) {
            if (metricRegistry != null) {
                ConfigurationService.register(MetricRegistry.class, metricRegistry);
            }
            
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
        
        if (decryptionParserPool != null) {
            ConfigurationService.register(DecryptionParserPool.class, new DecryptionParserPool(decryptionParserPool));
        }
    }
    
}
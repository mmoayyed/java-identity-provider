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

package net.shibboleth.idp.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nullable;

import net.shibboleth.idp.service.AbstractReloadableService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.service.ServiceableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;

import com.google.common.io.Closeables;

/**
 * Simple logging service that watches for logback configuration file changes and reloads the file when a change occurs.
 */
public class LogbackLoggingService extends AbstractReloadableService<Object> implements ApplicationContextAware {

    /** Logback logger context. */
    private LoggerContext loggerContext;

    /** Logger used to log messages without relying on the logging system to be full initialized. */
    private StatusManager statusManager;

    /** URL to the fallback logback configuration found in the IdP jar. */
    private Resource fallbackConfiguration;

    /** Logging configuration resource. */
    private Resource configurationResource;
    
    /** Properties resource. */
    @Nullable private Resource propertiesResource;

    /** Spring application context. */
    @Nullable private ApplicationContext applicationContext;

    /**
     * Gets the logging configuration.
     * 
     * @return logging configuration
     */
    public Resource getLoggingConfiguration() {
        return configurationResource;
    }

    /**
     * Sets the logging configuration.
     * 
     * @param configuration logging configuration
     */
    public synchronized void setLoggingConfiguration(Resource configuration) {
        if (isInitialized()) {
            return;
        }

        configurationResource = configuration;
    }

    /**
     * Get the properties resource.
     * 
     * @return the properties resource
     */
    public Resource getProperties() {
        return propertiesResource;
    }

    /**
     * Set the properties resource.
     * 
     * @param properties the properties resource
     */
    public void setProperties(@Nullable final Resource properties) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        propertiesResource = properties;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized boolean shouldReload() {
        try {
            final DateTime lastReload = getLastSuccessfulReloadInstant();
            if (null == lastReload) {
                return true;
            }
            return configurationResource.lastModified() > lastReload.getMillis();
        } catch (IOException e) {
            statusManager.add(new ErrorStatus(
                    "Error checking last modified time of logging service configuration resource "
                            + configurationResource.getDescription(), this, e));
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized void doReload() throws ServiceException {

        loadLoggingConfiguration();
    }

    /**
     * Reads and loads in a new logging configuration.
     * 
     * @throws ServiceException thrown if there is a problem loading the logging configuration
     */
    protected void loadLoggingConfiguration() throws ServiceException {
        InputStream ins = null;
        try {
            statusManager.add(new InfoStatus("Loading new logging configuration resource: "
                    + configurationResource.getDescription(), this));
            ins = configurationResource.getInputStream();
            loadLoggingConfiguration(ins);
        } catch (Exception e) {
            try {
                Closeables.close(ins, true);
            } catch (IOException e1) {
                // swallowed && logged by Closeables but...
                throw new ServiceException(e1);
            }
            statusManager.add(new ErrorStatus("Error loading logging configuration file: "
                    + configurationResource.getDescription(), this, e));
            try {
                statusManager.add(new InfoStatus("Loading fallback logging configuration", this));
                ins = fallbackConfiguration.getInputStream();
                loadLoggingConfiguration(ins);
            } catch (IOException ioe) {
                try {
                    Closeables.close(ins, true);
                } catch (IOException e1) {
                    // swallowed && logged by Closeables
                    throw new ServiceException(e1);
                }
                statusManager.add(new ErrorStatus("Error loading fallback logging configuration", this, e));
                throw new ServiceException("Unable to load fallback logging configuration");
            }
        } finally {
            try {
                Closeables.close(ins, true);
            } catch (IOException e) {
                // swallowed && logged by Closeables
                throw new ServiceException(e);
            }
        }
    }
    // Checkstyle: EmtpyBlock ON

    /**
     * Loads a logging configuration in to the active logger context. Error messages are printed out to the status
     * manager.
     * 
     * @param loggingConfig logging configuration file
     * 
     * @throws ServiceException thrown is there is a problem loading the logging configuration
     */
    protected void loadLoggingConfiguration(InputStream loggingConfig) throws ServiceException {
        try {
            loggerContext.reset();
            loadProperties();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(loggingConfig);
            loggerContext.start();
        } catch (JoranException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Load properties from the properties resource to the active logger context. Include the 'idp.home' property if it
     * is present in the application context environment.
     */
    protected void loadProperties() {
        if (propertiesResource == null) {
            return;
        }
        
        statusManager.add(new InfoStatus("Setting supplied properties on LoggerContext", this));
 
        if (applicationContext != null && applicationContext.getEnvironment().containsProperty("idp.home")) {
            loggerContext.putProperty("idp.home", applicationContext.getEnvironment().getProperty("idp.home"));
        }
        
        try {
            final Properties properties = PropertiesLoaderUtils.loadProperties(propertiesResource);
            for (final String name : properties.stringPropertyNames()) {
                loggerContext.putProperty(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            statusManager.add(new ErrorStatus("Error loading properties resource", this, e));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        if (configurationResource == null) {
            throw new ComponentInitializationException("Logging configuration must be specified.");
        }

        fallbackConfiguration = new ClassPathResource("/logback.xml");
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        statusManager = loggerContext.getStatusManager();
        if (!fallbackConfiguration.exists()) {
            if (isFailFast()) {
                throw new ComponentInitializationException(getLogPrefix() + "Cannot locate fallback logger");
            }
            statusManager.add(new ErrorStatus("Cannot locate fallback logger at "
                    + fallbackConfiguration.getDescription(), this));
        }
        super.doInitialize();

    }

    /** {@inheritDoc}.
     * This service does not support a ServiceableComponent, so return null. */
    @Override
    @Nullable public ServiceableComponent<Object> getServiceableComponent() {
        return null;
    }
}
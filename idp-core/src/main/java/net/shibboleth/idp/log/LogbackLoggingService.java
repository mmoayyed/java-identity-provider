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
import java.net.URL;
import java.util.HashMap;

import net.shibboleth.idp.service.AbstractReloadableService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.resource.ResourceException;

import org.slf4j.LoggerFactory;

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
public class LogbackLoggingService extends AbstractReloadableService {

    /** Logback logger context. */
    private LoggerContext loggerContext;

    /** Logger used to log messages without relying on the logging system to be full initialized. */
    private StatusManager statusManager;

    /** URL to the fallback logback configuration found in the IdP jar. */
    private URL fallbackConfiguraiton;

    /** Logging configuration resource. */
    private Resource configurationResource;

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

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        try {
            if (!configurationResource.exists()) {
                throw new ComponentValidationException("Logging service configuration resource "
                        + configurationResource.getLocation() + " does not exist.");
            }
        } catch (ResourceException e) {
            throw new ComponentValidationException("Unable to determing if logging service configuration resource "
                    + configurationResource.getLocation(), e);
        }
    }

    /** {@inheritDoc} */
    protected boolean shouldReload() {
        try {
            return configurationResource.getLastModifiedTime() > getLastSuccessfulReloadInstant().getMillis();
        } catch (ResourceException e) {
            statusManager.add(new ErrorStatus(
                    "Error checking last modified time of logging service configuration resource "
                            + configurationResource.getLocation(), this, e));
            return false;
        }
    }

    /** {@inheritDoc} */
    protected void doReload(HashMap context) throws ServiceException {
        super.doReload(context);

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
                    + configurationResource.getLocation(), this));
            ins = configurationResource.getInputStream();
            loadLoggingConfiguration(ins);
        } catch (Exception e) {
            Closeables.closeQuietly(ins);
            statusManager.add(new ErrorStatus("Error loading logging configuration file: "
                    + configurationResource.getLocation(), this, e));
            try {
                statusManager.add(new InfoStatus("Loading fallback logging configuration", this));
                ins = fallbackConfiguraiton.openStream();
                loadLoggingConfiguration(ins);
            } catch (IOException ioe) {
                Closeables.closeQuietly(ins);
                statusManager.add(new ErrorStatus("Error loading fallback logging configuration", this, e));
                throw new ServiceException("Unable to load fallback logging configuration");
            }
        } finally {
            Closeables.closeQuietly(ins);
        }
    }

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
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(loggingConfig);
            loggerContext.start();
        } catch (JoranException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.initialize();

        if (configurationResource == null) {
            throw new ComponentInitializationException("Logging configuration must be specified.");
        }

        fallbackConfiguraiton = LogbackLoggingService.class.getResource("/logback.xml");
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        statusManager = loggerContext.getStatusManager();
    }
}
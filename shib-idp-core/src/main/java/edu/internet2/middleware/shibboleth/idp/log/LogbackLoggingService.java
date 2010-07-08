/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.util.Assert;
import org.opensaml.util.Closeables;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import edu.internet2.middleware.shibboleth.idp.service.AbstractService;
import edu.internet2.middleware.shibboleth.idp.service.ReloadableService;
import edu.internet2.middleware.shibboleth.idp.service.ServiceException;

/**
 * Simple logging service that watches for logback configuration file changes and reloads the file when a change occurs.
 */
public class LogbackLoggingService extends AbstractService implements ReloadableService {

    /** URL to the fallback logback configuration found in the IdP jar. */
    private URL fallbackConfiguraiton;

    /** Logging configuration resource. */
    private Resource configurationResource;

    /** Timer used to schedule resource polling tasks. */
    private final Timer resourcePollingTimer;

    /** Frequency policy resources are polled for updates. */
    private final long resourcePollingFrequency;

    /** Watcher that monitors the configuration resource for this service for changes. */
    private ServiceConfigChangeWatcher resourceWatcher;

    /** The last time time the service was reloaded, whether successful or not. */
    private DateTime lastReloadInstant;

    /** The last time the service was reloaded successfully. */
    private DateTime lastSuccessfulReleaseIntant;

    /** The cause of the last reload failure, if the last reload failed. */
    private Throwable reloadFailureCause;

    /**
     * Constructor.
     * 
     * @param id unique ID of this service
     * @param loggingConfiguration logback configuration resource
     * @param backgroundTaskTimer background timer used to schedule resource change polling task
     * @param pollingFrequency frequency, in millisecond, of resource change polling
     */
    public LogbackLoggingService(String id, Resource loggingConfiguration, Timer backgroundTaskTimer,
            long pollingFrequency) {
        super(id);

        fallbackConfiguraiton = LogbackLoggingService.class.getResource("/logback.xml");

        if (pollingFrequency > 0) {
            Assert.isNotNull(backgroundTaskTimer, "Resource polling timer may not be null");
            resourcePollingTimer = backgroundTaskTimer;

            Assert.isGreaterThan(0, pollingFrequency, "Resource polling frequency must be greater than 0");
            resourcePollingFrequency = pollingFrequency;
        } else {
            resourcePollingTimer = null;
            resourcePollingFrequency = 0;
        }
    }

    /** {@inheritDoc} */
    public void validate() throws ServiceException {
        if (!configurationResource.exists()) {
            throw new ServiceException("Logging service configuration file " + configurationResource.getFilename()
                    + " does not exist.");
        }
    }

    /** {@inheritDoc} */
    public DateTime getLastReloadAttemptInstant() {
        return lastReloadInstant;
    }

    /** {@inheritDoc} */
    public DateTime getLastSuccessfulReloadInstant() {
        return lastSuccessfulReleaseIntant;
    }

    /** {@inheritDoc} */
    public Throwable getReloadFailureCause() {
        return reloadFailureCause;
    }

    /** {@inheritDoc} */
    public void reload() {
        try {
            loadLoggingConfiguration();
        } catch (ServiceException e) {
            // TODO
        }
    }

    /** {@inheritDoc} */
    protected void doStart() throws ServiceException {
        super.doStart();
        loadLoggingConfiguration();
    }

    /** {@inheritDoc} */
    protected void doPostStart() throws ServiceException {
        super.doPostStart();
        if (resourcePollingFrequency > 0) {
            resourceWatcher = new ServiceConfigChangeWatcher();
            resourcePollingTimer.schedule(resourceWatcher, resourcePollingFrequency, resourcePollingFrequency);
        }
    }

    /** 
     * Reads and loads in a new logging configuration. 
     * 
     * @throws ServiceException thrown if there is a problem loading the logging configuration
     */
    protected void loadLoggingConfiguration() throws ServiceException {
        lastReloadInstant = new DateTime(ISOChronology.getInstanceUTC());

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusManager statusManager = loggerContext.getStatusManager();

        InputStream ins = null;
        try {
            statusManager.add(new InfoStatus("Loading new logging configuration file: "
                    + configurationResource.getFilename(), this));
            ins = configurationResource.getInputStream();
            loadLoggingConfiguration(statusManager, loggerContext, ins);
            reloadFailureCause = null;
            lastSuccessfulReleaseIntant = lastReloadInstant;
        } catch (Exception e) {
            Closeables.closeQuiety(ins);
            statusManager.add(new ErrorStatus("Error loading logging configuration file: "
                    + configurationResource.getFilename(), this, e));
            reloadFailureCause = e.getCause();

            try {
                statusManager.add(new InfoStatus("Loading fallback logging configuration", this));
                ins = fallbackConfiguraiton.openStream();
                loadLoggingConfiguration(statusManager, loggerContext, ins);
            } catch (IOException ioe) {
                Closeables.closeQuiety(ins);
                statusManager.add(new ErrorStatus("Error loading fallback logging configuration", this, e));
                throw new ServiceException("Unable to load fallback logging configuration");
            }
        } finally {
            Closeables.closeQuiety(ins);
        }
    }

    /**
     * Loads a logging configuration in to the active logger context. Error messages are printed out to the status
     * manager.
     * 
     * @param statusManager status manager that will receive the error messages
     * @param loggerContext active, to-be-configured, logger context
     * @param loggingConfig logging configuration file
     * 
     * @throws ServiceException thrown is there is a problem loading the logging configuration
     */
    protected void loadLoggingConfiguration(StatusManager statusManager, LoggerContext loggerContext,
            InputStream loggingConfig) throws ServiceException {
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

    /** A watcher that determines if the configuration for a service has been changed. */
    class ServiceConfigChangeWatcher extends TimerTask {

        /** {@inheritDoc} */
        public void run() {
            reload();
        }
    }
}
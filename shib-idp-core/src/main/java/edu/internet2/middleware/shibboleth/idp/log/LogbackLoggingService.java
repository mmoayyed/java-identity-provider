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
import java.util.Timer;
import java.util.TimerTask;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.util.Assert;
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

    /** Logging configuration resource. */
    private Resource configurationResource;

    /** Timer used to schedule resource polling tasks. */
    private final Timer resourcePollingTimer;

    /** Frequency policy resources are polled for updates. */
    private final long resourcePollingFrequency;

    /** Watcher that monitors the configuration resource for this service for changes. */
    private ServiceConfigChangeWatcher resourceWatcher;

    /** The last time time the service was reloaded, in milliseconds since the epoch in the UTC time zone. */
    private long lastReloadInstant;

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
    public long getLastReloadInstant() {
        return lastReloadInstant;
    }

    /** {@inheritDoc} */
    public void reload() {
        loadLoggingConfiguration();
        lastReloadInstant = new DateTime(ISOChronology.getInstanceUTC()).getMillis();
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
        lastReloadInstant = new DateTime(ISOChronology.getInstanceUTC()).getMillis();
    }

    /** Reads and loads in a new logging configuration. */
    protected void loadLoggingConfiguration() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.add(new InfoStatus("Loading new logging configuration", this));

        String configurationUrl = null;
        try {
            configurationUrl = configurationResource.getURL().toExternalForm();
        } catch (IOException e) {
            statusManager.add(new ErrorStatus("Unable to determine configuration resource location", this, e));
            return;
        }

        try {
            loggerContext.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(configurationResource.getInputStream());
            loggerContext.start();
        } catch (JoranException e) {
            statusManager
                    .add(new ErrorStatus("Error loading logging configuration file: " + configurationUrl, this, e));
        } catch (IOException e) {
            statusManager
                    .add(new ErrorStatus("Error loading logging configuration file: " + configurationUrl, this, e));
        }
    }

    /** A watcher that determines if the configuration for a service has been changed. */
    class ServiceConfigChangeWatcher extends TimerTask {

        /** {@inheritDoc} */
        public void run() {
            try {
                if (lastReloadInstant == configurationResource.lastModified()) {
                    return;
                }
            } catch (IOException e) {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                StatusManager statusManager = loggerContext.getStatusManager();

                String configurationUrl = null;
                try {
                    configurationUrl = configurationResource.getURL().toExternalForm();
                } catch (IOException e2) {
                    statusManager.add(new ErrorStatus("Unable to determine configuration resource location", this, e2));
                    return;
                }

                statusManager.add(new InfoStatus("Loading logging configuration file: " + configurationUrl, this));
            }

            reload();
        }
    }
}
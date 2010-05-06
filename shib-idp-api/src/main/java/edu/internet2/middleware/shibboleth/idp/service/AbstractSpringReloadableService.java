/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.service;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;

import net.jcip.annotations.ThreadSafe;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import edu.internet2.middleware.shibboleth.idp.spring.SpringSupport;
import edu.internet2.middleware.shibboleth.idp.util.Assert;

/**
 * An extension to {@link AbstractSpringService} that allows the service's context to be reloaded if the underlying
 * configuration resources are changed.
 * 
 * Resources loaded in as configurations to this base class <strong>MUST</strong> must support the
 * {@link Resource#lastModified()} method.
 * 
 * <strong>NOTE:</strong> Service implementations must take out a read lock, through {@link #getServiceLock()}, whenever
 * reading or operating on information controlled by the service context. This will ensure that if a configuration
 * change occurs the service context will not be replaced until after all current reads have completed.
 * 
 * @see AbstractSpringService
 */
@ThreadSafe
public abstract class AbstractSpringReloadableService extends AbstractSpringService implements ReloadableService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractSpringReloadableService.class);

    /** Timer used to schedule resource polling tasks. */
    private final Timer resourcePollingTimer;

    /** Frequency, in milliseconds, that the configuration resources are polled for changes. */
    private final long resourcePollingFrequency;

    /** Watcher that monitors the set of configuration resources for this service for changes. */
    private ServiceConfigSetChangeWatcher resourceWatcher;

    /** The last time time the service was reloaded, in milliseconds since the epoch in the UTC time zone. */
    private long lastReloadInstant;

    /**
     * Constructor.
     * 
     * @param id the unique ID for this service
     * @param parent the parent application context for this context, may be null if there is no parent
     * @param configs configuration resources for the service
     * @param backgroundTaskTimer timer used to schedule background processes
     * @param pollingFrequency frequency, in milliseconds, that the configuration resources are polled for changes
     */
    public AbstractSpringReloadableService(String id, ApplicationContext parent, List<Resource> configs,
            Timer backgroundTaskTimer, long pollingFrequency) {
        super(id, parent, configs);

        if(pollingFrequency > 0){
            Assert.isNotNull(backgroundTaskTimer, "Resource polling timer may not be null");
            resourcePollingTimer = backgroundTaskTimer;
    
            Assert.isGreaterThan(0, pollingFrequency, "Resource polling frequency must be greater than 0");
            resourcePollingFrequency = pollingFrequency;
        }else{
            resourcePollingTimer = null;
            resourcePollingFrequency = 0;
        }
    }

    /**
     * Gets the frequency, in milliseconds, that the configuration resources are polled for changes.
     * 
     * @return frequency, in milliseconds, that the configuration resources are polled for changes
     */
    public long getPollingFrequency() {
        return resourcePollingFrequency;
    }

    /** {@inheritDoc} */
    public long getLastReloadInstant() {
        return lastReloadInstant;
    }

    /** {@inheritDoc} */
    public void reload() {
        log.info("Configuration change detected, reloading service {}", getId());

        GenericApplicationContext newServiceContext = null;
        try {
            newServiceContext = SpringSupport.newContext(getDisplayName(), getServiceConfigurations(),
                    getParentContext());
            log.debug("{} service configuration reloaded", getId());
        } catch (BeansException e) {
            // Here we catch all the other exceptions thrown by Spring when it starts up the context
            Throwable cause = e.getMostSpecificCause();
            log.error("Updated configuration was not loadable for serivce " + getId()
                    + ", the current good configuration will continue to be used.  The root cause of this error was: "
                    + cause.getClass().getCanonicalName() + ": " + cause.getMessage());
            log.trace("Full stacktrace is: ", e);
            return;
        }

        Lock serviceWriteLock = getServiceLock().writeLock();
        serviceWriteLock.lock();
        setServiceContext(newServiceContext);
        setLastReloadInstant();
        serviceWriteLock.unlock();
    }

    /** Sets the last reload instant time to the current time. */
    protected void setLastReloadInstant() {
        setLastReloadInstant(new DateTime());
    }

    /**
     * Sets the last reload instant time to the given time.
     * 
     * @param instant time of the last reload
     */
    protected void setLastReloadInstant(DateTime instant) {
        lastReloadInstant = instant.toDateTimeISO().getMillis();
    }

    /** {@inheritDoc} */
    protected void doPostStart() throws ServiceException {
        super.doPostStart();
        if (resourcePollingFrequency > 0) {
            resourceWatcher = new ServiceConfigSetChangeWatcher();
            resourcePollingTimer.schedule(resourceWatcher, resourcePollingFrequency, resourcePollingFrequency);
        }
        setLastReloadInstant();
    }

    /** {@inheritDoc} */
    protected void doPreStop() throws ServiceException {
        resourceWatcher.cancel();
        super.doPreStop();
    }

    /**
     * A watcher that determines if one or more of configuration files for a service has been created, changed, or
     * deleted.
     */
    class ServiceConfigSetChangeWatcher extends TimerTask {

        /** Number of configuration resources. */
        private final int numOfResources;

        /**
         * Time, in milliseconds, when the service configuration for the given index was last observed to have changed.
         * -1 indicates the configuration resource did not exist.
         */
        private long[] resourceLastModifiedTimes = new long[getServiceConfigurations().size()];

        /** Constructor. */
        public ServiceConfigSetChangeWatcher() {
            List<Resource> serviceConfigs = getServiceConfigurations();
            numOfResources = serviceConfigs.size();
            Resource serviceConfig;
            for (int i = 0; i < numOfResources; i++) {
                serviceConfig = serviceConfigs.get(i);
                if (serviceConfig.exists()) {
                    try {
                        resourceLastModifiedTimes[i] = serviceConfigs.get(i).lastModified();
                    } catch (IOException e) {
                        log.debug("Configuration resource '{}' last modification date could not be determined", e);
                        resourceLastModifiedTimes[i] = -1;
                    }
                } else {
                    resourceLastModifiedTimes[i] = -1;
                }
            }
        }

        /** {@inheritDoc} */
        public void run() {
            boolean configResourceChanged = false;

            List<Resource> serviceConfigs = getServiceConfigurations();
            Resource serviceConfig;
            long serviceConfigLastModified;
            for (int i = 0; i < numOfResources; i++) {
                serviceConfig = serviceConfigs.get(i);

                // check if resource did not exist and still does not exist
                if (resourceLastModifiedTimes[i] == -1 && !serviceConfig.exists()) {
                    continue;
                }

                try {
                    // check to see if the resource did not exist, but does now
                    // or if the resource did exist but does not now
                    if ((resourceLastModifiedTimes[i] == -1 && serviceConfig.exists())
                            || (resourceLastModifiedTimes[i] > -1 && !serviceConfig.exists())) {
                        configResourceChanged = true;
                        resourceLastModifiedTimes[i] = serviceConfig.lastModified();
                        continue;
                    }

                    // check to see if an existing resource, that still exists, has been modified since the last run
                    serviceConfigLastModified = serviceConfigs.get(i).lastModified();
                    if (serviceConfigLastModified != resourceLastModifiedTimes[i]) {
                        configResourceChanged = true;
                        resourceLastModifiedTimes[i] = serviceConfigLastModified;
                    }
                } catch (IOException e) {
                    log.debug("Configuration resource '{}' last modification date could not be determined", e);
                    configResourceChanged = true;
                }
            }

            if (configResourceChanged) {
                reload();
            }
        }
    }
}
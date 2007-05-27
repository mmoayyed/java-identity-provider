/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.config;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.opensaml.resource.Resource;
import org.opensaml.resource.ResourceChangeListener;
import org.opensaml.resource.ResourceChangeWatcher;
import org.opensaml.resource.ResourceException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * An extension to {@link BaseService} that allows the service's context to be reloaded if the underlying configuration
 * resources are changed.
 * 
 * If, at construction time, polling frequency and retry attempt are given then the configuration resources will be
 * watched for changes. If a change is detected then the current service's context will be dropped and a new one created
 * from all resource files. If there is a problem loading a configuration resource during this process the existing
 * service context is kept and an error is logged. The result of this occuring during the initial configuration load is
 * implementation dependent.
 * 
 * <strong>NOTE:</strong> Service implementations must take out a read lock, through {@link #getReadWriteLock()},
 * whenever reading or operating on information controlled by the service context. This will ensure that if a
 * configuration change occurs the service context will not be replaced until after all current reads have completed.
 */
public abstract class BaseReloadableService extends BaseService {

    /** Class logger. */
    private static Logger log = Logger.getLogger(BaseReloadableService.class);

    /** Frequency policy resources are polled for updates. */
    private long resourcePollingFrequency;

    /** Number of policy resource polling retry attempts. */
    private int resourcePollingRetryAttempts;

    /** Read/Write lock for the AFP context. */
    private ReentrantReadWriteLock serviceContextRWLock;

    /** Timer used to schedule resource polling tasks. */
    private Timer pollingTimer;

    /**
     * Constructor. Configuration resources are not monitored for changes.
     * 
     * @param configurations configuration resources for this service
     */
    public BaseReloadableService(List<Resource> configurations) {
        super(configurations);
        serviceContextRWLock = new ReentrantReadWriteLock(true);
        resourcePollingFrequency = 0;
        resourcePollingRetryAttempts = 0;
    }

    /**
     * Constructor.
     * 
     * @param timer timer resource polling tasks are scheduled with
     * @param configurations configuration resources for this service
     * @param pollingFrequency the frequency, in milliseconds, to poll the policy resources for changes, must be greater
     *            than zero
     */
    public BaseReloadableService(Timer timer, List<Resource> configurations, long pollingFrequency) {
        super(configurations);
        if (timer == null) {
            throw new IllegalArgumentException("Resource polling timer may not be null");
        }
        pollingTimer = timer;

        if (pollingFrequency <= 0) {
            throw new IllegalArgumentException("Polling frequency must be greater than zero.");
        }
        resourcePollingFrequency = pollingFrequency;
        resourcePollingRetryAttempts = 5;

        serviceContextRWLock = new ReentrantReadWriteLock(true);
    }

    /**
     * Gets the frequency, in millseconds, that the configuration resources are polled.
     * 
     * @return frequency, in millseconds, that the configuration resources are polled
     */
    public long getPollingFrequency() {
        return resourcePollingFrequency;
    }

    /**
     * Gets the number of times a resource may error out before it is considered permanently invalid.
     * 
     * @return number of times a resource may error out before it is considered permanently invalid
     */
    public int getPollingRetryAttempts() {
        return resourcePollingRetryAttempts;
    }

    /**
     * Sets the number of times a resource may error out before it is considered permanently invalid.
     * 
     * @param attempts number of times a resource may error out before it is considered permanently invalid
     */
    public void setPollingRetryAttempts(int attempts) {
        resourcePollingRetryAttempts = attempts;
    }

    /** {@inheritDoc} */
    public void initialize() throws ResourceException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing " + getServiceName() + " service with resources: " + getServiceConfigurations());
        }
        if (resourcePollingFrequency > 0) {
            ResourceChangeWatcher changeWatcher;
            ResourceChangeListener changeListener = new ConfigurationResourceListener();
            for (Resource configurationResournce : getServiceConfigurations()) {
                changeWatcher = new ResourceChangeWatcher(configurationResournce, resourcePollingFrequency,
                        resourcePollingRetryAttempts);
                changeWatcher.getResourceListeners().add(changeListener);
                pollingTimer.schedule(changeWatcher, resourcePollingFrequency, resourcePollingFrequency);
            }
        }

        loadContext();
    }

    /**
     * Gets the read-write lock guarding the service context.
     * 
     * @return read-write lock guarding the service context
     */
    protected ReadWriteLock getReadWriteLock() {
        return serviceContextRWLock;
    }

    /**
     * Reloads the service context.
     */
    protected void loadContext() {
        if (log.isDebugEnabled()) {
            log.debug("Loading configuration for service: " + getServiceName());
        }
        GenericApplicationContext newServiceContext = new GenericApplicationContext(getApplicationContext());
        try {
            SpringConfigurationUtils.populateRegistry(newServiceContext, getServiceConfigurations());

            Lock writeLock = getReadWriteLock().writeLock();
            writeLock.lock();
            newContextCreated(newServiceContext);
            setServiceContext(newServiceContext);
            writeLock.unlock();
            if (log.isInfoEnabled()) {
                log.info(getServiceName() + " service configuration loaded");
            }
        } catch (ResourceException e) {
            log.error("New configuration was not loaded for " + getServiceName() + " service, unable to load resource",
                    e);
        }
    }

    /**
     * Called after a new context has been created but before it set as the service's context. If an exception is thrown
     * the new context will not be set as the service's context and the current service context will be retained.
     * 
     * @param newServiceContext the newly created context for the service
     * 
     * @throws ResourceException thrown if there is a problem with the given service context
     */
    protected abstract void newContextCreated(ApplicationContext newServiceContext) throws ResourceException;

    /** A listener for policy resource changes that triggers a reloading of the AFP context. */
    protected class ConfigurationResourceListener implements ResourceChangeListener {

        /** {@inheritDoc} */
        public void onResourceCreate(Resource resource) {
            loadContext();
        }

        /** {@inheritDoc} */
        public void onResourceDelete(Resource resource) {
            loadContext();
        }

        /** {@inheritDoc} */
        public void onResourceUpdate(Resource resource) {
            loadContext();
        }
    }
}
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

import java.util.Timer;

import org.opensaml.util.resource.Resource;
import org.opensaml.util.resource.ResourceChangeListener;
import org.opensaml.util.resource.ResourceChangeWatcher;
import org.opensaml.util.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.service.ReloadableService;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;

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
public abstract class BaseReloadableService extends BaseService implements ReloadableService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseReloadableService.class);

    /** Frequency policy resources are polled for updates. */
    private long resourcePollingFrequency;

    /** Number of policy resource polling retry attempts. */
    private int resourcePollingRetryAttempts;

    /** Timer used to schedule resource polling tasks. */
    private Timer pollingTimer;

    /**
     * Constructor. Configuration resources are not monitored for changes.
     */
    public BaseReloadableService() {
        super();
        resourcePollingFrequency = 0;
        resourcePollingRetryAttempts = 0;
    }
    
    /**
     * Gets the timer used to resource polling jobs.
     * 
     * @return timer used to resource polling jobs
     */
    public Timer getPollingTimer(){
        return pollingTimer;
    }
   
    /**
     * Sets the timer used to resource polling jobs.
     * 
     * @param timer timer used to resource polling jobs
     */
    public void setPollingTimer(Timer timer){
        pollingTimer = timer;
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
     * Sets the frequency, in millseconds, that the configuration resources are polled.
     * 
     * @param frequency the frequency, in millseconds, that the configuration resources are polled
     */
    public void setPollingFrequency(long frequency){
        resourcePollingFrequency = frequency;
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
    public void initialize() throws ServiceException {
        try {
            log.debug("Initializing {} service with resources: {}", getId(), getServiceConfigurations());
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
        } catch (ResourceException e) {
            throw new ServiceException("Unable to initialize service: " + getId(), e);
        }
    }

    /** {@inheritDoc} */
    public void reload() throws ServiceException {
        loadContext();
    }

    /** {@inheritDoc} */
    public void destroy() throws ServiceException {
        pollingTimer.cancel();
        super.destroy();
    }

    /** A listener for policy resource changes that triggers a reloading of the AFP context. */
    protected class ConfigurationResourceListener implements ResourceChangeListener {

        /** {@inheritDoc} */
        public void onResourceCreate(Resource resource) {
            try {
                loadContext();
            } catch (ServiceException e) {
                log.error(
                        "Error reloading configuration, upon configuration resource creation, for service " + getId(),
                        e);
            }
        }

        /** {@inheritDoc} */
        public void onResourceDelete(Resource resource) {
            try {
                loadContext();
            } catch (ServiceException e) {
                log.error(
                        "Error reloading configuration, upon configuration resource deletion, for service " + getId(),
                        e);
            }
        }

        /** {@inheritDoc} */
        public void onResourceUpdate(Resource resource) {
            try {
                loadContext();
            } catch (ServiceException e) {
                log.error("Error reloading configuration, upon configuration resource update, for service " + getId(),
                        e);
            }
        }
    }
}
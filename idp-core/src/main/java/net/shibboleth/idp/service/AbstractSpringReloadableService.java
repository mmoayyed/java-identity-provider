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

package net.shibboleth.idp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.spring.SpringSupport;

import org.opensaml.util.Assert;
import org.opensaml.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;


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
public abstract class AbstractSpringReloadableService extends AbstractReloadableService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractSpringReloadableService.class);

    /** List of configuration resources for this service. */
    private final List<Resource> serviceConfigurations;

    /** Application context owning this engine. */
    private final ApplicationContext parentContext;

    /** Context containing loaded with service content. */
    private GenericApplicationContext serviceContext;

    /**
     * Constructor.
     * 
     * @param id the unique ID for this service
     * @param parent the parent application context for this context, may be null if there is no parent
     * @param configs configuration resources for the service
     * @param reloadTaskTimer timer used to schedule service reloading background task
     * @param reloadDelay milliseconds between one reload check and another
     */
    public AbstractSpringReloadableService(final String id, final ApplicationContext parent,
            final List<Resource> configs, final Timer reloadTaskTimer, final long reloadDelay) {
        super(id, reloadTaskTimer, reloadDelay);

        parentContext = parent;

        Assert.isNotEmpty(configs, "Service configuration set may not be null or empty");
        serviceConfigurations = Collections.unmodifiableList(new ArrayList<Resource>(configs));
    }

    /**
     * Gets an unmodifiable list of configurations for this service.
     * 
     * @return unmodifiable list of configurations for this service
     */
    public List<Resource> getServiceConfigurations() {
        return serviceConfigurations;
    }

    /**
     * Gets the application context that is the parent to this service's context.
     * 
     * @return application context that is the parent to this service's context
     */
    protected ApplicationContext getParentContext() {
        return parentContext;
    }

    /**
     * Gets this service's context.
     * 
     * Note, any modifications done to the retrieved service context must be within the bounds of the service write lock
     * retrieved via {@link #getServiceLock()}.
     * 
     * @return this service's context
     */
    protected GenericApplicationContext getServiceContext() {
        return serviceContext;
    }

    /** {@inheritDoc} */
    protected boolean shouldReload() {
        // TODO implement
        // loop over each resource and check if the any resources have been changed since 
        // the last time the service was reloaded.  Also have to take in to account locking
        // issue so that checking if reloading doesn't block use of service but does block
        // actual reloading
        
        return false;
    }

    /** {@inheritDoc} */
    protected void doPreReload(final HashMap context) throws ServiceException {
        log.info("Configuration change detected, reloading configuration for service '{}'", getId());

        try {
            log.debug("Creating new ApplicationContext for service '{}'", getId());
            GenericApplicationContext appContext = SpringSupport.newContext(getId(),
                    getServiceConfigurations(), getParentContext());
            log.debug("New Application Context created for service '{}'", getId());
            context.put(AbstractSpringService.APP_CTX_CTX_KEY, appContext);
        } catch (BeansException e) {
            // Here we catch all the other exceptions thrown by Spring when it starts up the context
            Throwable cause = e.getMostSpecificCause();
            log.error("Error creating new application context for service '{}'.  Cause: {}", getId(), cause
                    .getMessage());
            log.debug("Full stacktrace is: ", e);
            throw new ServiceException("Error creating new application context for service " + getId());
        }
    }

    /** {@inheritDoc} */
    protected void doPostRelaod(final HashMap context) throws ServiceException {
        GenericApplicationContext appCtx = (GenericApplicationContext) context
                .get(AbstractSpringService.APP_CTX_CTX_KEY);
        serviceContext = appCtx;
    }

    /**
     * A watcher that determines if one or more of configuration files for a service has been created, changed, or
     * deleted.
     */
//    protected class ServiceConfigSetChangeWatcher extends TimerTask {
//
//        /** Number of configuration resources. */
//        private final int numOfResources;
//
//        /**
//         * Time, in milliseconds, when the service configuration for the given index was last observed to have changed.
//         * -1 indicates the configuration resource did not exist.
//         */
//        private long[] resourceLastModifiedTimes = new long[getServiceConfigurations().size()];
//
//        /** Constructor. */
//        public ServiceConfigSetChangeWatcher() {
//            List<Resource> serviceConfigs = getServiceConfigurations();
//            numOfResources = serviceConfigs.size();
//            Resource serviceConfig;
//            for (int i = 0; i < numOfResources; i++) {
//                serviceConfig = serviceConfigs.get(i);
//                if (serviceConfig.exists()) {
//                    try {
//                        resourceLastModifiedTimes[i] = serviceConfigs.get(i).lastModified();
//                    } catch (IOException e) {
//                        log.debug("Configuration resource '{}' last modification date could not be determined", e);
//                        resourceLastModifiedTimes[i] = -1;
//                    }
//                } else {
//                    resourceLastModifiedTimes[i] = -1;
//                }
//            }
//        }
//
//        /** {@inheritDoc} */
//        public void run() {
//            boolean configResourceChanged = false;
//
//            List<Resource> serviceConfigs = getServiceConfigurations();
//            Resource serviceConfig;
//            long serviceConfigLastModified;
//            for (int i = 0; i < numOfResources; i++) {
//                serviceConfig = serviceConfigs.get(i);
//
//                // check if resource did not exist and still does not exist
//                if (resourceLastModifiedTimes[i] == -1 && !serviceConfig.exists()) {
//                    continue;
//                }
//
//                try {
//                    // check to see if the resource did not exist, but does now
//                    // or if the resource did exist but does not now
//                    if ((resourceLastModifiedTimes[i] == -1 && serviceConfig.exists())
//                            || (resourceLastModifiedTimes[i] > -1 && !serviceConfig.exists())) {
//                        configResourceChanged = true;
//                        resourceLastModifiedTimes[i] = serviceConfig.lastModified();
//                        continue;
//                    }
//
//                    // check to see if an existing resource, that still exists, has been modified since the last run
//                    serviceConfigLastModified = serviceConfigs.get(i).lastModified();
//                    if (serviceConfigLastModified != resourceLastModifiedTimes[i]) {
//                        configResourceChanged = true;
//                        resourceLastModifiedTimes[i] = serviceConfigLastModified;
//                    }
//                } catch (IOException e) {
//                    log.debug("Configuration resource '{}' last modification date could not be determined", e);
//                    configResourceChanged = true;
//                }
//            }
//
//            if (configResourceChanged) {
//                reload();
//            }
//        }
//    }
}
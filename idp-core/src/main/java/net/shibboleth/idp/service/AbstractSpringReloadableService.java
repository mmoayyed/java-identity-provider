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

package net.shibboleth.idp.service;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.resource.ResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * An extension to {@link AbstractSpringService} that allows the service's context to be reloaded if the underlying
 * configuration resources are changed.
 * 
 * Resources loaded in as configurations to this base class <strong>MUST</strong> support the
 * {@link Resource#lastModified()} method.
 * 
 * <strong>NOTE:</strong> Service implementations must acquire a read lock, through {@link #getServiceLock()}, whenever
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
    private List<Resource> serviceConfigurations;

    /** Application context owning this engine. */
    private ApplicationContext parentContext;

    /** Context containing loaded with service content. */
    private GenericApplicationContext serviceContext;

    /**
     * Time, in milliseconds, when the service configuration for the given index was last observed to have changed.
     * -1 indicates the configuration resource did not exist.
     */
    private long[] resourceLastModifiedTimes;
    
    /**
     * Gets the application context that is the parent to this service's context.
     * 
     * @return application context that is the parent to this service's context
     */
    @Nullable public ApplicationContext getParentContext() {
        return parentContext;
    }

    /**
     * Sets the application context that is the parent to this service's context.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param context context that is the parent to this service's context, may be null
     */
    public synchronized void setParentContext(@Nullable final ApplicationContext context) {
        if (isInitialized()) {
            return;
        }

        parentContext = context;
    }

    /**
     * Gets an unmodifiable list of configurations for this service.
     * 
     * @return unmodifiable list of configurations for this service
     */
    @Nonnull public List<Resource> getServiceConfigurations() {
        return serviceConfigurations;
    }

    /**
     * Sets the list of configurations for this service.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param configs list of configurations for this service, may be null or empty
     */
    public synchronized void setServiceConfigurations(@Nonnull final List<Resource> configs) {
        if (isInitialized()) {
            return;
        }

        serviceConfigurations =
                ImmutableList.<Resource> builder().addAll(Iterables.filter(configs, Predicates.notNull())).build();
        if (!serviceConfigurations.isEmpty()) {
            resourceLastModifiedTimes = new long[serviceConfigurations.size()];

            int numOfResources = serviceConfigurations.size();
            Resource serviceConfig;
            for (int i = 0; i < numOfResources; i++) {
                serviceConfig = serviceConfigurations.get(i);
                try {
                    if (serviceConfig.exists()) {
                        resourceLastModifiedTimes[i] = serviceConfig.getLastModifiedTime();
                    } else {
                        resourceLastModifiedTimes[i] = -1;
                    }
                } catch (ResourceException e) {
                    log.info("Configuration resource '" + serviceConfig.getLocation()
                            + "' last modification date could not be determined", e);
                    resourceLastModifiedTimes[i] = -1;
                }
            }
        } else {
            resourceLastModifiedTimes = null;
        }
    }

    /**
     * Gets this service's context.
     * 
     * Note, any modifications done to the retrieved service context must be within the bounds of the service write lock
     * retrieved via {@link #getServiceLock()}.
     * 
     * @return this service's context
     */
    @Nullable protected GenericApplicationContext getServiceContext() {
        return serviceContext;
    }

    /** {@inheritDoc} */
    protected void doPreStart(@Nonnull final HashMap context) throws ServiceException {
        super.doPreStart(context);
        
        createContext(context);
    }

    /** {@inheritDoc} */
    protected void doPostStart(@Nonnull final HashMap context) throws ServiceException {
        super.doPostStart(context);
        GenericApplicationContext appCtx =
                (GenericApplicationContext) context.get(AbstractSpringService.APP_CTX_CTX_KEY);
        serviceContext = appCtx;
    }

    /**
     * Creates the Spring context during initial startup or as a reload operation.
     * 
     * @param context Collection of data carried through start and reload operations.
     *  This is an appropriate place to keep state as the process progresses.
     * 
     * @throws ServiceException thrown if there is a problem starting or reloading the service
     */
    protected void createContext(@Nonnull final HashMap context) throws ServiceException {
        try {
            log.debug("Creating new ApplicationContext for service '{}'", getId());
            GenericApplicationContext appContext =
                    SpringSupport.newContext(getId(), getServiceConfigurations(), getParentContext());
            log.debug("New Application Context created for service '{}'", getId());
            context.put(AbstractSpringService.APP_CTX_CTX_KEY, appContext);
        } catch (BeansException e) {
            // Here we catch all the other exceptions thrown by Spring when it starts up the context
            Throwable cause = e.getMostSpecificCause();
            log.error("Error creating new application context for service '{}'.  Cause: {}", getId(),
                    cause.getMessage());
            log.debug("Full stacktrace is: ", e);
            throw new ServiceException("Error creating new application context for service " + getId());
        }
    }
    
    /** {@inheritDoc} */
    protected void doPreReload(@Nonnull final HashMap context) throws ServiceException {
        log.info("Configuration change detected, reloading configuration for service '{}'", getId());
        
        createContext(context);
    }
    
    /** {@inheritDoc} */
    protected void doPostReload(@Nonnull final HashMap context) throws ServiceException {
        GenericApplicationContext appCtx =
                (GenericApplicationContext) context.get(AbstractSpringService.APP_CTX_CTX_KEY);
        serviceContext = appCtx;
    }

    /** {@inheritDoc} */
    protected void doStop(@Nonnull final HashMap context) throws ServiceException {
        serviceContext.close();
        super.doStop(context);
    }

    /** {@inheritDoc} */
    protected void doPostStop(@Nonnull final HashMap context) throws ServiceException {
        serviceContext = null;
        serviceConfigurations.clear();
        super.doStop(context);
    }
    
    /** {@inheritDoc} */
    protected boolean shouldReload() {
        // Loop over each resource and check if the any resources have been changed since
        // the last time the service was reloaded. I believe a read lock is all we need here
        // to allow use of the service to proceed while we check on the state. Actual reloading
        // requires the write lock, and the only post-initialization code that reads or writes
        // the array of resource mod-time data is this code, which is on one thread.
        
        Lock readLock = getServiceLock().readLock();
        try {
            readLock.lock();
        
            if (!STATE_STARTED.equals(getCurrentState())) {
                log.debug("Skipping check for changed configuration, service '{}' not yet started", getId());
                return false;
            } else if (resourceLastModifiedTimes == null) {
                return false;
            }
    
            boolean configResourceChanged = false;
            int numOfResources = serviceConfigurations.size();
            
            Resource serviceConfig;
            long serviceConfigLastModified;
            for (int i = 0; i < numOfResources; i++) {
                serviceConfig = serviceConfigurations.get(i);
                try {
                    if (resourceLastModifiedTimes[i] == -1 && !serviceConfig.exists()) {
                        // Resource did not exist and still does not exist.
                        log.debug("Resource remains unavailable/inaccessible: '{}'", serviceConfig.getLocation());
                    } else if (resourceLastModifiedTimes[i] == -1 && serviceConfig.exists()) {
                        // Resource did not exist, but does now.
                        log.debug("Resource was unavailable, now present: '{}'", serviceConfig.getLocation());
                        configResourceChanged = true;
                        resourceLastModifiedTimes[i] = serviceConfig.getLastModifiedTime();
                    } else if (resourceLastModifiedTimes[i] > -1 && !serviceConfig.exists()) {
                        // Resource existed, but is now unavailable.
                        log.debug("Resource was available, now is not: '{}'", serviceConfig.getLocation());
                        configResourceChanged = true;
                        resourceLastModifiedTimes[i] = -1;
                    } else {
                        // Check to see if an existing resource, that still exists, has been modified.
                        serviceConfigLastModified = serviceConfig.getLastModifiedTime();
                        if (serviceConfigLastModified != resourceLastModifiedTimes[i]) {
                            log.debug("Resource has changed: '{}'", serviceConfig.getLocation());
                            configResourceChanged = true;
                            resourceLastModifiedTimes[i] = serviceConfigLastModified;
                        }
                    }
                } catch (ResourceException e) {
                    log.info("Configuration resource '" + serviceConfig.getLocation()
                            + "' last modification date could not be determined", e);
                    configResourceChanged = true;
                }
            }
            
            return configResourceChanged;
            
        } finally {
            readLock.unlock();
        }
    }

}
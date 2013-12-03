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

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

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
 * @see AbstractSpringService
 */
// TODO Do not use - being rewritten
@ThreadSafe
public abstract class AbstractSpringReloadableService extends AbstractReloadableService implements
        ApplicationContextAware {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractSpringReloadableService.class);

    /** List of configuration resources for this service. */
    private List<Resource> serviceConfigurations;

    /** Application context owning this engine. */
    private ApplicationContext parentContext;

    /** Context containing loaded with service content. */
    private GenericApplicationContext serviceContext;

    /**
     * Time, in milliseconds, when the service configuration for the given index was last observed to have changed. -1
     * indicates the configuration resource did not exist.
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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

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
                        resourceLastModifiedTimes[i] = serviceConfig.lastModified();
                    } else {
                        resourceLastModifiedTimes[i] = -1;
                    }
                } catch (IOException e) {
                    log.info("Configuration resource '" + serviceConfig.getDescription()
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

    /**
     * Creates the Spring context during initial startup or as a reload operation.
     * 
     * @throws ServiceException thrown if there is a problem starting or reloading the service
     */
    protected void createContext() throws ServiceException {
        /*
         * TODO try { log.debug("Creating new ApplicationContext for service '{}'", getId()); GenericApplicationContext
         * appContext = SpringSupport.newContext(getId(), getServiceConfigurations(), getParentContext());
         * log.debug("New Application Context created for service '{}'", getId());
         * context.put(AbstractSpringService.APP_CTX_CTX_KEY, appContext); } catch (BeansException e) { // Here we catch
         * all the other exceptions thrown by Spring when it starts up the context Throwable cause =
         * e.getMostSpecificCause(); log.error("Error creating new application context for service '{}'.  Cause: {}",
         * getId(), cause.getMessage()); log.debug("Full stacktrace is: ", e); throw new
         * ServiceException("Error creating new application context for service " + getId()); }
         */
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        /*
         * TODO serviceContext.close(); super.doStop(context);
         */
    }

    /** {@inheritDoc} */
    // Checkstyle: CyclomaticComplexity OFF
    protected boolean shouldReload() {
        // Loop over each resource and check if the any resources have been changed since
        // the last time the service was reloaded. I believe a read lock is all we need here
        // to allow use of the service to proceed while we check on the state. Actual reloading
        // requires the write lock, and the only post-initialization code that reads or writes
        // the array of resource mod-time data is this code, which is on one thread.

        if (resourceLastModifiedTimes == null) {
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
                    log.debug("Resource remains unavailable/inaccessible: '{}'", serviceConfig.getDescription());
                } else if (resourceLastModifiedTimes[i] == -1 && serviceConfig.exists()) {
                    // Resource did not exist, but does now.
                    log.debug("Resource was unavailable, now present: '{}'", serviceConfig.getDescription());
                    configResourceChanged = true;
                    resourceLastModifiedTimes[i] = serviceConfig.lastModified();
                } else if (resourceLastModifiedTimes[i] > -1 && !serviceConfig.exists()) {
                    // Resource existed, but is now unavailable.
                    log.debug("Resource was available, now is not: '{}'", serviceConfig.getDescription());
                    configResourceChanged = true;
                    resourceLastModifiedTimes[i] = -1;
                } else {
                    // Check to see if an existing resource, that still exists, has been modified.
                    serviceConfigLastModified = serviceConfig.lastModified();
                    if (serviceConfigLastModified != resourceLastModifiedTimes[i]) {
                        log.debug("Resource has changed: '{}'", serviceConfig.getDescription());
                        configResourceChanged = true;
                        resourceLastModifiedTimes[i] = serviceConfigLastModified;
                    }
                }
            } catch (IOException e) {
                log.info("Configuration resource '" + serviceConfig.getDescription()
                        + "' last modification date could not be determined", e);
                configResourceChanged = true;
            }
        }

        return configResourceChanged;
    }
    // Checkstyle: CyclomaticComplexity ON
}
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
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * This class provides a reloading interface to a ServiceableComponent.
 * 
 * @param <T> The precise service being implemented.
 */
@ThreadSafe
public class ReloadableSpringService<T> extends AbstractReloadableService implements ApplicationContextAware,
        BeanNameAware {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ReloadableSpringService.class);

    /** List of configuration resources for this service. */
    private List<Resource> serviceConfigurations;

    /** The class we are looking for. */
    private final Class<T> theClaz;

    /** Application context owning this engine. */
    private ApplicationContext parentContext;

    /** The bean name. */
    private String beanName;
    
    /** The last known good component. */
    private ServiceableComponent<T> cachedComponent;

    /** Did the last load fail? An optimization only. */
    private boolean lastLoadFailed = true;

    /**
     * Time, in milliseconds, when the service configuration for the given index was last observed to have changed. -1
     * indicates the configuration resource did not exist.
     */
    private long[] resourceLastModifiedTimes;

    /**
     * Constructor.
     * 
     * @param claz The interface being implemented.
     */
    public ReloadableSpringService(Class<T> claz) {
        theClaz = claz;
    }

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
    public void setParentContext(@Nullable final ApplicationContext context) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

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
    public void setServiceConfigurations(@Nonnull final Collection<Resource> configs) {
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
                    log.info("{} Configuration resource '" + serviceConfig.getDescription()
                            + "' last modification date could not be determined", getLogPrefix(), e);
                    resourceLastModifiedTimes[i] = -1;
                }
            }
        } else {
            resourceLastModifiedTimes = null;
        }
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

        if (lastLoadFailed) {
            return true;
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
                    log.debug("{} Resource remains unavailable/inaccessible: '{}'", getLogPrefix(),
                            serviceConfig.getDescription());
                } else if (resourceLastModifiedTimes[i] == -1 && serviceConfig.exists()) {
                    // Resource did not exist, but does now.
                    log.debug("{} Resource was unavailable, now present: '{}'", getLogPrefix(),
                            serviceConfig.getDescription());
                    configResourceChanged = true;
                    resourceLastModifiedTimes[i] = serviceConfig.lastModified();
                } else if (resourceLastModifiedTimes[i] > -1 && !serviceConfig.exists()) {
                    // Resource existed, but is now unavailable.
                    log.debug("{} Resource was available, now is not: '{}'", getLogPrefix(),
                            serviceConfig.getDescription());
                    configResourceChanged = true;
                    resourceLastModifiedTimes[i] = -1;
                } else {
                    // Check to see if an existing resource, that still exists, has been modified.
                    serviceConfigLastModified = serviceConfig.lastModified();
                    if (serviceConfigLastModified != resourceLastModifiedTimes[i]) {
                        log.debug("{} Resource has changed: '{}'", getLogPrefix(), serviceConfig.getDescription());
                        configResourceChanged = true;
                        resourceLastModifiedTimes[i] = serviceConfigLastModified;
                    }
                }
            } catch (IOException e) {
                log.info("{} Configuration resource '{}' last modification date could not be determined",
                        getLogPrefix(), serviceConfig.getDescription(), e);
                configResourceChanged = true;
            }
        }

        return configResourceChanged;
    }

    // Checkstyle: CyclomaticComplexity ON

    /** {@inheritDoc} */
    protected void doReload() throws ServiceException {
        super.doReload();

        log.debug("Creating new ApplicationContext for service '{}'", getId());
        GenericApplicationContext appContext = null;
        try {
            appContext = SpringSupport.newContext(getId(), getServiceConfigurations(), getParentContext());
        } catch (FatalBeanException e) {
            throw new ServiceException(e);
        }

        log.trace("{} New Application Context created.", getLogPrefix());

        final Collection<ServiceableComponent> components =
                appContext.getBeansOfType(ServiceableComponent.class).values();

        log.debug("{} Context yielded {} beans", getLogPrefix(), components.size());

        if (components.size() == 0) {
            appContext.close();
            throw new ServiceException(getLogPrefix() + "Reload did not produce any ServiceableComponents");
        }
        if (components.size() > 1) {
            appContext.close();
            throw new ServiceException("Reload produced too many ServiceableComponents");
        }

        final ServiceableComponent<T> service = components.iterator().next();
        service.pinComponent();

        //
        // Now check it's the right type before we continue.
        //
        final T theComponent = service.getComponent();

        log.debug("Testing that {} is a superclass of {}", theComponent.getClass(), theClaz);

        if (!theClaz.isAssignableFrom(theComponent.getClass())) {
            //
            // tear it down
            //
            service.unpinComponent();
            service.unloadComponent();
            throw new ServiceException("Class was not the same or a superclass of configured class");
        }

        //
        // Otherwise we are ready to swap in the new component; so only
        // now do we grab the lock.
        //
        // Note that we are grabbing the lock on the component before the lock on this
        // object, which would be an inversion with the getServiceableComponent ranking
        // except the component will never be seen before we drop the lock and so
        // there can be no inversion
        //
        final ServiceableComponent<T> oldComponent;
        synchronized (this) {
            oldComponent = cachedComponent;
            cachedComponent = service;
            service.unpinComponent();
        }
        if (null != oldComponent) {
            oldComponent.unloadComponent();
        }
        lastLoadFailed = false;
        log.debug("Reload complete");
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        final ServiceableComponent<T> oldComponent = cachedComponent;
        cachedComponent = null;
        // And tear down. Note that we are synchronized on this right now
        // and this will grab the lock - but that is OK because the ranking
        // is to lock this object, then the ServicableComponent.
        if (null != oldComponent) {
            oldComponent.unloadComponent();
        }
    }

    /**
     * Get the serviceable component. We do this under interlock and grab the lock on the component.
     * 
     * @return the <em>pinned</em> component.
     */
    public synchronized ServiceableComponent<T> getServiceableComponent() {
        if (null == cachedComponent) {
            return null;
        }
        cachedComponent.pinComponent();
        return cachedComponent;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext applicationContext) {
        setParentContext(applicationContext);
    }

    /** {@inheritDoc} */
    public void setBeanName(String name) {
        beanName = name;        
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (getId() == null) {
            setId(beanName);
        }
        super.doInitialize();
    }
}

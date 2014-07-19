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
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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

    /** List of bean post processors for this service's content. */
    private List<BeanPostProcessor> postProcessors;

    /** The class we are looking for. */
    @Nonnull private final Class<T> theClaz;

    /** How to summon up the {@link ServiceableComponent} from the {@link ApplicationContext}. */
    @Nonnull private final Function<GenericApplicationContext, ServiceableComponent> serviceStrategy;

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
    public ReloadableSpringService(@Nonnull Class<T> claz) {
        this(claz, new ClassBasedServiceStrategy());
    }

    /**
     * Constructor.
     * 
     * @param claz The interface being implemented.
     * @param strategy the strategy to use to look up servicable component to look for.
     */
    public ReloadableSpringService(@Nonnull Class<T> claz,
            @Nonnull Function<GenericApplicationContext, ServiceableComponent> strategy) {
        theClaz = Constraint.isNotNull(claz, "Class cannot be null");
        serviceStrategy = Constraint.isNotNull(strategy, "Strategy cannot be null");
        postProcessors = Collections.emptyList();
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
    public void setServiceConfigurations(@Nonnull @NonnullElements final List<Resource> configs) {
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

    /**
     * Set the list of bean post processors for this service.
     * 
     * @param processors bean post processors to apply
     */
    public void setBeanPostProcessors(@Nonnull @NonnullElements final List<BeanPostProcessor> processors) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        postProcessors = Lists.newArrayList(Collections2.filter(processors, Predicates.notNull()));
    }

    /** {@inheritDoc} */
    // Checkstyle: CyclomaticComplexity OFF
    @Override protected boolean shouldReload() {
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
    @Override protected void doReload() {
        super.doReload();

        log.debug("{} Creating new ApplicationContext for service '{}'", getLogPrefix(), getId());
        log.debug("{} Reloading from {}", getLogPrefix(), getServiceConfigurations());
        final GenericApplicationContext appContext;
        try {
            appContext =
                    SpringSupport.newContext(getId(), getServiceConfigurations(), postProcessors, getParentContext());
        } catch (FatalBeanException e) {
            throw new ServiceException(e);
        }

        log.trace("{} New Application Context created.", getLogPrefix());

        final ServiceableComponent<T> service;
        try {
            service = serviceStrategy.apply(appContext);
        } catch (ServiceException e) {
            appContext.close();
            throw new ServiceException("Failed to load " + getServiceConfigurations(), e);
        }

        service.pinComponent();

        //
        // Now check it's the right type before we continue.
        //
        final T theComponent = service.getComponent();

        log.debug("{} Testing that {} is a superclass of {}", getLogPrefix(), theComponent.getClass(), theClaz);

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
        log.info("{} Reload complete", getLogPrefix());
    }

    /** {@inheritDoc} */
    @Override protected void doDestroy() {
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
    @Override public synchronized ServiceableComponent<T> getServiceableComponent() {
        if (null == cachedComponent) {
            return null;
        }
        cachedComponent.pinComponent();
        return cachedComponent;
    }

    /** {@inheritDoc} */
    @Override public void setApplicationContext(ApplicationContext applicationContext) {
        setParentContext(applicationContext);
    }

    /** {@inheritDoc} */
    @Override public void setBeanName(String name) {
        beanName = name;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        if (getId() == null) {
            setId(beanName);
        }
        super.doInitialize();
    }
}

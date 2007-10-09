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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.opensaml.util.resource.Resource;
import org.opensaml.util.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

import edu.internet2.middleware.shibboleth.common.service.Service;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;

/**
 * A service whose Spring beans are loaded into a service specific {@link ApplicationContext} that is a child of the
 * context provided in {@link #setApplicationContext(ApplicationContext)}.
 * 
 * Services derived from this base class may not be reinitilized after they have been destroyed.
 */
public abstract class BaseService implements Service, ApplicationContextAware, BeanNameAware {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseService.class);

    /** Unqiue name of this service. */
    private String serviceName;

    /** Read/Write lock for the AFP context. */
    private ReentrantReadWriteLock serviceContextRWLock;

    /** Application context owning this engine. */
    private ApplicationContext owningContext;

    /** Context containing loaded with service content. */
    private ApplicationContext serviceContext;

    /** List of configuration resources for this service. */
    private ArrayList<Resource> serviceConfigurations;

    /** Indicates if the service has been initialized already. */
    private boolean isInitialized;

    /** Indicates if the service has been destroyed. */
    private boolean isDestroyed;

    /**
     * Constructor.
     * 
     * @param configurations configuration resources for this service
     */
    public BaseService(List<Resource> configurations) {
        serviceContextRWLock = new ReentrantReadWriteLock(true);
        serviceConfigurations = new ArrayList<Resource>(configurations);
        isInitialized = false;
    }

    /** {@inheritDoc} */
    public String getId() {
        return serviceName;
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return isInitialized();
    }

    /**
     * Sets wether this service has been initialized.
     * 
     * @param initialized wether this service has been initialized
     */
    protected void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    /** {@inheritDoc} */
    public void setBeanName(String name) {
        serviceName = name;
    }

    /**
     * Gets the application context that is the parent to this service's context.
     * 
     * @return application context that is the parent to this service's context
     */
    public ApplicationContext getApplicationContext() {
        return owningContext;
    }

    /**
     * Sets the application context that is the parent to this service's context.
     * 
     * {@inheritDoc}
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        owningContext = applicationContext;
    }

    /**
     * Gets an unmodifiable list of service configuration files.
     * 
     * @return unmodifiable list of service configuration files
     */
    public List<Resource> getServiceConfigurations() {
        return Collections.unmodifiableList(serviceConfigurations);
    }

    /**
     * Gets this service's context.
     * 
     * @return this service's context
     */
    public ApplicationContext getServiceContext() {
        return serviceContext;
    }

    /**
     * Sets this service's context.
     * 
     * @param context this service's context
     */
    protected void setServiceContext(ApplicationContext context) {
        serviceContext = context;
    }

    /** {@inheritDoc} */
    public void initialize() throws ServiceException {
        if (isDestroyed) {
            throw new SecurityException(getId() + " service has been destroyed, it may not be initialized.");
        }

        if (isInitialized) {
            return;
        }

        loadContext();
    }

    /** {@inheritDoc} */
    public void destroy() throws ServiceException {
        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();
        isDestroyed = true;
        serviceContext = null;
        serviceConfigurations.clear();
        setInitialized(false);
        writeLock.unlock();
        serviceContextRWLock = null;
    }

    /**
     * Loads the service context.
     * 
     * @throws ServiceException thrown if the configuration for this service could not be loaded
     */
    protected void loadContext() throws ServiceException {
        log.debug("Loading configuration for service: {}", getId());
        GenericApplicationContext newServiceContext = new GenericApplicationContext(getApplicationContext());
        try {
            SpringConfigurationUtils.populateRegistry(newServiceContext, getServiceConfigurations());
            newServiceContext.refresh();

            Lock writeLock = getReadWriteLock().writeLock();
            writeLock.lock();
            newContextCreated(newServiceContext);
            setServiceContext(newServiceContext);
            writeLock.unlock();
            setInitialized(true);
            log.info("{} service configuration loaded", getId());
        } catch (ResourceException e) {
            setInitialized(false);
            log.error("Configuration was not loaded for " + getId() + " service, unable to load resource", e);
            throw new ServiceException("Configuration was not loaded for " + getId()
                    + " service, unable to load resource", e);
        } catch (Exception e) {
            // Here we catch all the other exceptions thrown by Spring when it starts up the context
            setInitialized(false);
            log.error("Configuration was not loaded for " + getId() + " service, error creating components", e);
            throw new ServiceException("Configuration was not loaded for " + getId()
                    + " service, error creating components", e);
        }
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
     * Called after a new context has been created but before it set as the service's context. If an exception is thrown
     * the new context will not be set as the service's context and the current service context will be retained.
     * 
     * @param newServiceContext the newly created context for the service
     * 
     * @throws ServiceException thrown if there is a problem with the given service context
     */
    protected abstract void newContextCreated(ApplicationContext newServiceContext) throws ServiceException;
}
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
    private GenericApplicationContext serviceContext;

    /** List of configuration resources for this service. */
    private ArrayList<Resource> serviceConfigurations;

    /** Indicates if the service has been initialized already. */
    private boolean isInitialized;

    /** Indicates if the service has been destroyed. */
    private boolean isDestroyed;

    /** Constructor. */
    public BaseService() {
        serviceContextRWLock = new ReentrantReadWriteLock(true);
        isInitialized = false;
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
     * Gets the application context that is the parent to this service's context.
     * 
     * @return application context that is the parent to this service's context
     */
    public ApplicationContext getApplicationContext() {
        return owningContext;
    }

    /** {@inheritDoc} */
    public String getId() {
        return serviceName;
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
     * Gets an unmodifiable list of configurations for this service.
     * 
     * @return unmodifiable list of configurations for this service
     */
    public List<Resource> getServiceConfigurations(){
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

    /** {@inheritDoc} */
    public void initialize() throws ServiceException {
        log.debug("Initializing service {}", getId());
        if (isDestroyed) {
            throw new SecurityException(getId() + " service has been destroyed, it may not be initialized.");
        }

        if (isInitialized) {
            return;
        }

        loadContext();
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Loads the service context.
     * 
     * @throws ServiceException thrown if the configuration for this service could not be loaded
     */
    protected void loadContext() throws ServiceException {
        log.info("Loading configuration for service: {}", getId());
        
        if(serviceConfigurations == null || serviceConfigurations.isEmpty()){
            setInitialized(true);
            return;
        }
        
        GenericApplicationContext newServiceContext = new GenericApplicationContext(getApplicationContext());
        newServiceContext.setDisplayName("ApplicationContext:" + getId());
        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();
        try {
            SpringConfigurationUtils.populateRegistry(newServiceContext, getServiceConfigurations());
            newServiceContext.refresh();

            GenericApplicationContext replacedServiceContext = serviceContext;
            onNewContextCreated(newServiceContext);
            setServiceContext(newServiceContext);
            setInitialized(true);
            if(replacedServiceContext != null){
                replacedServiceContext.close();
            }
            log.info("{} service configuration loaded", getId());
        } catch (Exception e) {
            // Here we catch all the other exceptions thrown by Spring when it starts up the context
            setInitialized(false);
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            log.error("Configuration was not loaded for " + getId()
                    + " service, error creating components.  The root cause of this error was: "
                    + rootCause.getMessage());
            log.trace("Full stacktrace is: ", e);
            throw new ServiceException("Configuration was not loaded for " + getId()
                    + " service, error creating components.");
        }finally{
            writeLock.unlock();
        }
    }

    /**
     * Called after a new context has been created but before it set as the service's context. If an exception is thrown
     * the new context will not be set as the service's context and the current service context will be retained.
     * 
     * @param newServiceContext the newly created context for the service
     * 
     * @throws ServiceException thrown if there is a problem with the given service context
     */
    protected abstract void onNewContextCreated(ApplicationContext newServiceContext) throws ServiceException;

    /**
     * Sets the application context that is the parent to this service's context.
     * 
     * {@inheritDoc}
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        owningContext = applicationContext;
    }

    /** {@inheritDoc} */
    public void setBeanName(String name) {
        serviceName = name;
    }

    /**
     * Sets whether this service has been initialized.
     * 
     * @param initialized whether this service has been initialized
     */
    protected void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    /**
     * Sets the service's configuration resources.
     * 
     * @param configurations configuration resources for the service
     * @throws IllegalStateException thrown if the service has already been initialized
     */
    public void setServiceConfigurations(List<Resource> configurations) throws IllegalStateException{
        if(isInitialized){
            throw new IllegalStateException("Service already initialized");
        }
        serviceConfigurations = new ArrayList<Resource>(configurations);
    }

    /**
     * Sets this service's context.
     * 
     * @param context this service's context
     */
    protected void setServiceContext(GenericApplicationContext context) {
        serviceContext = context;
    }
}
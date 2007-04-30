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

import org.apache.log4j.Logger;
import org.opensaml.resource.Resource;
import org.opensaml.resource.ResourceException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

/**
 * A service is a component whose Spring beans are loaded into a service specific {@link ApplicationContext} that is a
 * child of the context provided in {@link #setApplicationContext(ApplicationContext)}.
 */
public abstract class BaseService implements ApplicationContextAware, BeanNameAware {

    /** Class logger. */
    private final Logger log = Logger.getLogger(BaseService.class);

    /** Unqiue name of this service. */
    private String serviceName;

    /** Application context owning this engine. */
    private ApplicationContext owningContext;

    /** Context containing loaded with service content. */
    private ApplicationContext serviceContext;

    /** List of configuration resources for this service. */
    private ArrayList<Resource> serviceConfigurations;

    /** Indicates if the service has been initialized already. */
    private boolean isInitialized;

    /**
     * Constructor.
     * 
     * @param configurations configuration resources for this service
     */
    public BaseService(List<Resource> configurations) {
        serviceConfigurations = new ArrayList<Resource>(configurations);
        isInitialized = false;
    }

    /**
     * Gets the unique name of this service.
     * 
     * @return unique name of this service
     */
    public String getServiceName() {
        return serviceName;
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

    /**
     * Initializes this service's context by loading all the configurations provided.
     * 
     * @throws ResourceException thrown if the given resource can not be read or parsed
     */
    public void initialize() throws ResourceException {
        if (isInitialized) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Initializing " + getServiceName() + " service with configurations: "
                    + getServiceConfigurations());
        }
        GenericApplicationContext newServiceContext = new GenericApplicationContext();
        SpringConfigurationUtils.populateRegistry(newServiceContext, serviceConfigurations);
        setServiceContext(newServiceContext);

        if (log.isInfoEnabled()) {
            log.info(getServiceName() + " service configuration loaded");
        }
    }
}
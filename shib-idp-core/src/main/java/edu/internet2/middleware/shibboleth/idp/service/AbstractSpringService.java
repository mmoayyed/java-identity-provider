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

package edu.internet2.middleware.shibboleth.idp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import edu.internet2.middleware.shibboleth.idp.spring.SpringSupport;

/**
 * A service whose Spring beans are loaded into a service specific {@link ApplicationContext} that is a child of the
 * context provided at service construction.
 * 
 * Resources loaded in as configurations to this base class <strong>MUST</strong> must support the
 * {@link Resource#getInputStream()} method.
 * 
 * Services derived from this base class may not be re-initialized after they have been destroyed.
 * 
 * <strong>NOTE:</strong> Service implementations must take out a read lock, through {@link #getServiceLock()}, whenever
 * reading or operating on information controlled by the service context. This will ensure that if a configuration
 * change occurs the service context will not be replaced until after all current reads have completed.
 */
@ThreadSafe
public abstract class AbstractSpringService extends AbstractService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractSpringService.class);

    /** Application context owning this engine. */
    private ApplicationContext parentContext;

    /** Context containing loaded with service content. */
    private GenericApplicationContext serviceContext;

    /** List of configuration resources for this service. */
    private ArrayList<Resource> serviceConfigurations;

    /**
     * Constructor.
     * 
     * @param id the unique ID for this service
     * @param parent the parent application context for this context, may be null if there is no parent
     * @param configs the configuration files to be loaded by the service
     */
    public AbstractSpringService(String id, ApplicationContext parent, List<Resource> configs) {
        super(id);
        parentContext = parent;

        Assert.isNotNull(configs, "Service configuration set may not be null");
        Assert.isNotEmpty(configs, "Service configuration set may not be empty");
        serviceConfigurations = new ArrayList<Resource>(configs);
    }

    /**
     * Gets an unmodifiable list of configurations for this service.
     * 
     * @return unmodifiable list of configurations for this service
     */
    public List<Resource> getServiceConfigurations() {
        return Collections.unmodifiableList(serviceConfigurations);
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

    /**
     * Sets this service's context.
     * 
     * @param context this service's context
     */
    protected void setServiceContext(GenericApplicationContext context) {
        serviceContext = context;
    }

    /** {@inheritDoc} */
    protected void doStart() throws ServiceException {
        super.doStart();

        log.info("Loading configuration for service: {}", getId());

        try {
            log.debug("Creating new ApplicationContext for service '{}'", getId());
            GenericApplicationContext newServiceContext = SpringSupport.newContext(getDisplayName(),
                    getServiceConfigurations(), getParentContext());
            setServiceContext(newServiceContext);
            log.info("{} service configuration loaded", getId());
        } catch (BeansException e) {
            // Here we catch all the other exceptions thrown by Spring when it starts up the context
            Throwable cause = e.getMostSpecificCause();
            log.error("Configuration was not loaded for " + getId()
                    + " service, error creating components.  The root cause of this error was: "
                    + cause.getClass().getCanonicalName() + ": " + cause.getMessage());
            log.trace("Full stacktrace is: ", e);
            throw new ServiceException("Configuration was not loaded for " + getId()
                    + " service, error creating components.", (Exception) cause);
        }
    }

    /** {@inheritDoc} */
    protected void doStop() throws ServiceException {
        serviceContext.close();
        super.doStop();
    }

    /** {@inheritDoc} */
    protected void doPostStop() throws ServiceException {
        serviceContext = null;
        serviceConfigurations.clear();
        super.doStop();
    }
}
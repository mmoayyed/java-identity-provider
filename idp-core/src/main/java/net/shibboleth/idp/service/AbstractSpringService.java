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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

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

    /** Key under which the Spring Application Context is stored in the start/stop context. */
    public static final String APP_CTX_CTX_KEY = "appContext";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractSpringService.class);

    /** List of configuration resources for this service. */
    private List<Resource> serviceConfigurations = Collections.emptyList();

    /** Application context owning this engine. */
    private ApplicationContext parentContext;

    /** Context containing loaded with service content. */
    private GenericApplicationContext serviceContext;

    /**
     * Gets the application context that is the parent to this service's context.
     * 
     * @return application context that is the parent to this service's context
     */
    public ApplicationContext getParentContext() {
        return parentContext;
    }

    /**
     * Sets the application context that is the parent to this service's context.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param context context that is the parent to this service's context, may be null
     */
    public synchronized void setParentContext(ApplicationContext context) {
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
    public List<Resource> getServiceConfigurations() {
        return serviceConfigurations;
    }

    /**
     * Sets the list of configurations for this service.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param configs list of configurations for this service, may be null or empty
     */
    public synchronized void setServiceConfigurations(List<Resource> configs) {
        if (isInitialized()) {
            return;
        }

        serviceConfigurations =
                ImmutableList.<Resource> builder().addAll(Iterables.filter(configs, Predicates.notNull())).build();
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
     * {@inheritDoc}
     * 
     * This method creates a new {@link GenericApplicationContext} from the service's configuration and stores it in the
     * context under the key {@link #APP_CTX_CTX_KEY}.
     */
    protected void doPreStart(HashMap context) throws ServiceException {
        super.doPreStart(context);

        try {
            log.debug("Creating new ApplicationContext for service '{}'", getId());
            GenericApplicationContext appContext =
                    SpringSupport.newContext(getId(), getServiceConfigurations(), getParentContext());
            log.debug("New Application Context created for service '{}'", getId());
            context.put(APP_CTX_CTX_KEY, appContext);
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
    protected void doPostStart(HashMap context) throws ServiceException {
        super.doPostStart(context);
        GenericApplicationContext appCtx = (GenericApplicationContext) context.get(APP_CTX_CTX_KEY);
        serviceContext = appCtx;
    }

    /** {@inheritDoc} */
    protected void doStop(final HashMap context) throws ServiceException {
        serviceContext.close();
        super.doStop(context);
    }

    /** {@inheritDoc} */
    protected void doPostStop(final HashMap context) throws ServiceException {
        serviceContext = null;
        serviceConfigurations.clear();
        super.doStop(context);
    }
}
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A service whose Spring beans are loaded into a service specific {@link ApplicationContext} that is a child of the
 * context provided at service construction.
 * 
 * Resources loaded in as configurations to this base class <strong>MUST</strong> support the
 * {@link Resource#getInputStream()} method.
 * 
 * Services derived from this base class may not be re-initialized after they have been destroyed.
 * 
 * <strong>NOTE:</strong> Service implementations must acquire a read lock, through {@link #getServiceLock()}, whenever
 * reading or operating on information controlled by the service context. This will ensure that if a configuration
 * change occurs the service context will not be replaced until after all current reads have completed.
 */
@ThreadSafe
public abstract class AbstractSpringService extends AbstractService implements ApplicationContextAware,
        ResourceLoaderAware {

    /** Key under which the Spring Application Context is stored in the start/stop context. */
    public static final String APP_CTX_CTX_KEY = "appContext";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractSpringService.class);

    /** List of configuration resources for this service. */
    private List<Resource> serviceConfigurations = Collections.emptyList();

    /** List of configuration resource objects for this service. */
    private List<Object> configurations;

    /** Application context owning this service. */
    private ApplicationContext parentContext;

    /** Context containing service content. */
    private GenericApplicationContext serviceContext;

    /**
     * The Resource Loader.
     */
    private ResourceLoader resourceLoader;

    /**
     * Gets the application context that is the parent to this service's context.
     * 
     * @return application context that is the parent to this service's context
     */
    @Nullable public ApplicationContext getParentContext() {
        return parentContext;
    }

    /** {@inheritDoc} */
    public void setResourceLoader(ResourceLoader loader) {
        resourceLoader = loader;
    }

    /**
     * Gets the Resource loader associated with this bean.
     * 
     * @return the loader.
     */
    public ResourceLoader getResourceLoader() {

        return resourceLoader;
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
     * {@inheritDoc}
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        setParentContext(applicationContext);
    }

    /**
     * Gets the untyped configurations.
     * 
     * @return Returns the configurations - if any have been set.
     */
    @Nullable public List<Object> getConfigurations() {
        return configurations;
    }

    /**
     * Sets the configurations (suitable for injection as a &lt;util:List&gt;). <br/>
     * These have to be staged because the resource loader may be set after this.
     * 
     * @param configs The configurations to set.
     */
    public void setConfigurations(@Nonnull List<Object> configs) {
        configurations = configs;
    }

    /**
     * Sets the list of configurations for this service.<br/>
     * This is a legacy api left in place to allow the IdP to continue to work 
     * in backwards 
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param configs list of configurations for this service, may be null or empty. The configurations can be spring or
     */
    public void setServiceConfigurations(@Nonnull final List<Resource> configs) {
        if (isInitialized()) {
            return;
        }

        serviceConfigurations =
                ImmutableList.<Resource> builder().addAll(Iterables.filter(configs, Predicates.notNull())).build();
    }

    /**
     * {@inheritDoc}. <br/>
     * This method will convert the untyped list of configurations into a typed one.
     */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == getConfigurations()) {
            return;
        }
        final List<Resource> resources = new ArrayList<Resource>(getConfigurations().size());

        for (Object obj : getConfigurations()) {
            if (obj instanceof Resource) {
                Resource resource = (Resource) obj;
                resources.add(resource);
            } else if (obj instanceof org.springframework.core.io.Resource) {
                Resource resource = (org.springframework.core.io.Resource) obj;
                resources.add(resource);
            } else if (obj instanceof String) {
                resources.add(getResourceLoader().getResource((String) obj));
            } else {
                log.error("Provided object {} was not a recognised resource type");
            }
        }
        setServiceConfigurations(resources);
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
     * {@inheritDoc}
     * 
     * This method creates a new {@link GenericApplicationContext} from the service's configuration and stores it in the
     * context under the key {@link #APP_CTX_CTX_KEY}.
     */
    protected void doPreStart(@Nonnull final HashMap context) throws ServiceException {
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
    protected void doPostStart(@Nonnull final HashMap context) throws ServiceException {
        super.doPostStart(context);
        GenericApplicationContext appCtx = (GenericApplicationContext) context.get(APP_CTX_CTX_KEY);
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
}
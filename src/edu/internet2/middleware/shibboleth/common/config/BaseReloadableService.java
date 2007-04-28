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

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.opensaml.resource.Resource;
import org.opensaml.resource.ResourceChangeListener;
import org.opensaml.resource.ResourceChangeWatcher;
import org.opensaml.resource.ResourceException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.xml.sax.InputSource;


/**
 * An extension to {@link BaseService} that allows the service's context to be reloaded if the underlying configuration
 * resources are changed.
 * 
 * If, at construction time, polling frequency and retry attempt are given then the configuration resources will be
 * watched for changes. If a change is detected then the current service's context will be dropped and a new one created
 * from all resource files. If there is a problem loading a configuration resource during this process the existing
 * service context is kept and an error is logged. The result of this occuring during the initial configuration load is
 * implementation dependent.
 * 
 * <strong>NOTE:</strong> Service implementations must take out a read lock, through {@link #getReadWriteLock()},
 * whenever reading or operating on information controlled by the service context. This will ensure that if a
 * configuration change occurs the service context will not be replaced until after all current reads have completed.
 */
public abstract class BaseReloadableService extends BaseService {

    /** Class logger. */
    private static Logger log = Logger.getLogger(BaseReloadableService.class);

    /** Frequency policy resources are polled for updates. */
    private long resourcePollingFrequency;

    /** Number of policy resource polling retry attempts. */
    private int resourcePollingRetryAttempts;

    /** Read/Write lock for the AFP context. */
    private ReentrantReadWriteLock serviceContextRWLock;

    /**
     * Constructor. Configuration resources are not monitored for changes.
     * 
     * @param configurations configuration resources for this service
     */
    public BaseReloadableService(List<Resource> configurations) {
        super(configurations);
        serviceContextRWLock = new ReentrantReadWriteLock(true);
        resourcePollingFrequency = 0;
        resourcePollingRetryAttempts = 0;
    }

    /**
     * Constructor.
     * 
     * @param configurations configuration resources for this service
     * @param pollingFrequency the frequency, in milliseconds, to poll the policy resources for changes, must be greater
     *            than zero
     * @param pollingRetryAttempts maximum number of poll attempts before a policy resource is considered inaccessible,
     *            must be greater than zero
     */
    public BaseReloadableService(List<Resource> configurations, long pollingFrequency, int pollingRetryAttempts) {
        super(configurations);
        serviceContextRWLock = new ReentrantReadWriteLock(true);

        if (pollingFrequency <= 0 || pollingRetryAttempts <= 0) {
            throw new IllegalArgumentException("Polling frequency and retry attempts must be greater than zero.");
        }
        resourcePollingFrequency = pollingFrequency;
        resourcePollingRetryAttempts = pollingRetryAttempts;
    }

    /** {@inheritDoc} */
    public void initialize() throws ResourceException {
        if (resourcePollingFrequency > 0) {
            ResourceChangeWatcher changeWatcher;
            ResourceChangeListener changeListener = new ConfigurationResourceListener();
            for (Resource configurationResournce : getServiceConfigurations()) {
                changeWatcher = new ResourceChangeWatcher(configurationResournce, resourcePollingFrequency,
                        resourcePollingRetryAttempts);
                changeWatcher.getResourceListeners().add(changeListener);
                changeWatcher.run();
            }
        }

        reloadContext();
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
     * Reloads the service context.
     */
    protected void reloadContext() {
        GenericApplicationContext newServiceContext = new GenericApplicationContext(getApplicationContext());
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(newServiceContext);

        Resource configurationResource = null;
        try {
            for (Resource resource : getServiceConfigurations()) {
                configurationResource = resource;
                configReader.loadBeanDefinitions(new InputSource(configurationResource.getInputStream()));
            }

            Lock writeLock = getReadWriteLock().writeLock();
            writeLock.lock();
            newContextCreated(newServiceContext);
            setServiceContext(newServiceContext);
            writeLock.unlock();
        } catch (ResourceException e) {
            log.error("New filter policy configuration was not loaded, unable to load resource: "
                    + configurationResource, e);
        } catch (BeanDefinitionStoreException e) {
            log.error("New filter policy configuration was not loaded, error parsing policy resource: "
                    + configurationResource, e);
        }
    }

    /**
     * Called after a new context has been created but before it set as the service's context. If an exception is thrown
     * the new context will not be set as the service's context and the current service context will be retained.
     * 
     * @param newServiceContext the newly created context for the service
     * 
     * @throws ResourceException thrown if there is a problem with the given service context
     */
    protected abstract void newContextCreated(ApplicationContext newServiceContext) throws ResourceException;

    /** A listener for policy resource changes that triggers a reloading of the AFP context. */
    protected class ConfigurationResourceListener implements ResourceChangeListener {

        /** {@inheritDoc} */
        public void onResourceCreate(Resource resource) {
            reloadContext();
        }

        /** {@inheritDoc} */
        public void onResourceDelete(Resource resource) {
            reloadContext();
        }

        /** {@inheritDoc} */
        public void onResourceUpdate(Resource resource) {
            reloadContext();
        }
    }
}
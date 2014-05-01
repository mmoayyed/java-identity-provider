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

import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Implementation of {@link ServiceableComponent} that does most of the work required.
 * 
 * @param <T> The type of service.
 */
public abstract class AbstractServiceableComponent<T> extends AbstractIdentifiedInitializableComponent implements
        ServiceableComponent<T>, ApplicationContextAware {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractServiceableComponent.class);

    /** The context used to load this bean. */
    private ApplicationContext applicationContext;

    /**
     * Lock for this service. We make it unfair since we will control access and there will only ever be contention
     * during unload.
     */
    private final ReentrantReadWriteLock serviceLock = new ReentrantReadWriteLock(false);

    /** {@inheritDoc} */
    @Override public void setApplicationContext(ApplicationContext context) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        applicationContext = context;
    }

    /**
     * Get the context used to load this bean.
     * 
     * @return the context.
     */
    @Nullable public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * {@inheritDoc}.
     */
    @Override @Nonnull public abstract T getComponent();

    /**
     * {@inheritDoc} Grab the service lock shared. This will block unloads until {@link #unpinComponent()} is called.
     */
    @Override public void pinComponent() {
        serviceLock.readLock().lock();
    }

    /** {@inheritDoc} drop the shared lock. */
    @Override public void unpinComponent() {
        serviceLock.readLock().unlock();
    }

    /** {@inheritDoc}. Grab the service lock ex and then call spring to tear everything down. */
    @Override public void unloadComponent() {
        if (null == applicationContext) {
            log.debug("Component '{}': Component already unloaded", getId());
            return;
        }

        ConfigurableApplicationContext component = null;
        log.debug("Component '{}': Component unload called", getId());
        try {
            log.trace("Component '{}': Queueing for write lock", getId());
            serviceLock.writeLock().lock();
            log.trace("Component '{}': Got write lock", getId());
            component = (ConfigurableApplicationContext) applicationContext;
            applicationContext = null;
        } finally {
            serviceLock.writeLock().unlock();
        }

        if (null != component) {
            log.debug("Component '{}': Closing the appcontext", getId());
            component.close();
        }
    }

    /**
     * {@inheritDoc}. Force unload; this will usually be a no-op since the component should have been explicitly
     * unloaded, but we do the unload here so that error cases also clean up.
     */
    @Override protected void doDestroy() {
        unloadComponent();
        super.doDestroy();
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (applicationContext != null && !(applicationContext instanceof ConfigurableApplicationContext)) {
            throw new ComponentInitializationException(getId()
                    + ": Application context did not implement ConfigurableApplicationContext");
        }
    }
}

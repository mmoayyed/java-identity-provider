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

import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Implementation of {@link ServiceableComponent} that does most of the work required.
 * 
 * @param <T> The type of service.
 */
public abstract class AbstractServicableComponent<T> extends AbstractDestructableIdentifiableInitializableComponent
        implements ServiceableComponent<T>, ApplicationContextAware, DisposableBean  {

    /** The context used to load this bean. */
    private ApplicationContext applicationContext;

    /**
     * Lock for this service. We make it unfair since we will control access and there will only ever be contention
     * during unload.
     */
    private final ReentrantReadWriteLock serviceLock = new ReentrantReadWriteLock(false);

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext context) {
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
     @Nonnull public abstract T getComponent();

    /**{@inheritDoc}
     * Grab the service lock shared. This will block unloads until {@link #unpinComponent()} is called.
     */
    public void pinComponent() {
        serviceLock.readLock().lock();
    }

    /** {@inheritDoc} drop the shared lock. */
    public void unpinComponent() {
        serviceLock.readLock().unlock();
    }

    /** {@inheritDoc}.  Grab the service lock ex and then call spring to tear everything down. */
    public void unloadComponent() {
        try {
            serviceLock.writeLock().lock();
            ConfigurableApplicationContext c = (ConfigurableApplicationContext) applicationContext;
            c.close();
        } finally {
            serviceLock.writeLock().unlock();
        }
        destroy();
    }
}

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

import net.shibboleth.utilities.java.support.annotation.Duration;

import org.springframework.context.Lifecycle;
import org.springframework.core.io.Resource;

/**
 * This class provides a reloading interface to a ServiceableComponent.
 * 
 * @param <T>  The precise service being implemented.
 */
public class ReloadableSpringService<T> implements Lifecycle{

    /**
     * Constructor.
     *
     * @param claz The interface being implemented.
     * @param resources the configuration.
     * @param reloadFrequency How frequently to reload.
     */
    public ReloadableSpringService(Class<T> claz, Resource[] resources, @Duration long reloadFrequency) {
    }
    
    /**
     * Get the serviceable component.
     * @param <S> the type returned.
     * @return the component (with reference).
     */
    public <S extends ServiceableComponent<T>> S getServiceableComponent() {
        return null;
    }
    
    /** {@inheritDoc} */
    public void start() {
        // TODO Auto-generated method stub        
    }

    /** {@inheritDoc} */
    public void stop() {
        // TODO Auto-generated method stub
        
    }

    /** {@inheritDoc} */
    public boolean isRunning() {
        // TODO Auto-generated method stub
        return false;
    }

    
}

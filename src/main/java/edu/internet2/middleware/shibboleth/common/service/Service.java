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

package edu.internet2.middleware.shibboleth.common.service;

/**
 * A simple interface that represents coarse grained services, or components, within the software.
 */
public interface Service {

    /**
     * Gets the ID of this service.
     * 
     * @return ID of this service
     */
    public String getId();

    /**
     * Gets whether the service is initialized and ready for use.
     * 
     * @return true if the service is ready for use, false it not
     */
    public boolean isInitialized();

    /**
     * Initializes this service. Calling this on an initialized service should return immediately without affecting any
     * service state.
     * 
     * @throws ServiceException thrown if there is a problem initializing the service
     */
    public void initialize() throws ServiceException;

    /**
     * Gets whether the service has been destroyed.
     * 
     * @return true if the service has been destroyed
     */
    public boolean isDestroyed();

    /**
     * Destroys a service, freeing any resources it may currently be using. Whether a service can be re-initialized
     * after being destroyed is implementation dependent.
     * 
     * @throws ServiceException thrown if there is a problem destroying the service
     */
    public void destroy() throws ServiceException;
}
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

import javax.annotation.Nullable;

/**
 * Base exception for Service related errors.
 */
public class ServiceException extends RuntimeException {

    /** Serial version UID. */
    private static final long serialVersionUID = 5875639786427807960L;

    /**
     * Constructor.
     */
    public ServiceException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public ServiceException(@Nullable final String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param wrappedException exception to be wrapped by this one
     */
    public ServiceException(@Nullable final Exception wrappedException) {
        super(wrappedException);
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public ServiceException(@Nullable final String message, @Nullable final Exception wrappedException) {
        super(message, wrappedException);
    }
}
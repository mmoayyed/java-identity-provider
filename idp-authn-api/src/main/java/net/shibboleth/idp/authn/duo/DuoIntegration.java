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

package net.shibboleth.idp.authn.duo;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import javax.annotation.Nonnull;

/**
 * Interface to a particular Duo integration point.
 * 
 * @since 3.3.0
 */
public interface DuoIntegration {
    
    /**
     * Get the name of the API host to contact.
     * 
     * @return name of API host
     */
    @Nonnull @NotEmpty String getAPIHost();

    /**
     * Get the application key.
     * 
     * @return the application key
     */
    @Nonnull @NotEmpty String getApplicationKey();

    /**
     * Get the integration key.
     * 
     * @return the integration key
     */
    @Nonnull @NotEmpty String getIntegrationKey();

    /**
     * Get the secret key.
     * 
     * @return the secret key
     */
    @Nonnull @NotEmpty String getSecretKey();
    
}
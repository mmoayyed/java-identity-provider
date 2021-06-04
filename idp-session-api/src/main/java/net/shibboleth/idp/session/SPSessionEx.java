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

package net.shibboleth.idp.session;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Temporary extension of {@link SPSession} to add protocol access.
 * 
 * TODO: remove in V5
 * 
 * @since 4.1.0
 * @deprecated
 */
@Deprecated(since="4.1.0", forRemoval=true)
public interface SPSessionEx extends SPSession {

    /**
     * Get a prototocol constant for the session suitable for metadata lookup.
     *
     * @return a protocol constant
     */
    @Nullable @NotEmpty String getProtocol();
    
    /**
     * Gets whether the session allows for logout propagation.
     * 
     * @return whether the session allows for logout propagation
     * 
     * @since 4.2.0
     */
    default boolean supportsLogoutPropagation() {
        return false;
    }
    
}
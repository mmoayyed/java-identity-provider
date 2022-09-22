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

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.IdentifiedComponent;

/**
 * Describes a session with a service in turn associated with an {@link IdPSession}.
 * 
 * This is a high level interface that should be extended to expose data associated with
 * particular protocols used to establish sessions.
 */
@ThreadSafe
public interface SPSession extends IdentifiedComponent {

    /**
     * Get the time when this session was created.
     * 
     * @return time when this session was created
     */
    @Nonnull Instant getCreationInstant();

    /**
     * Get the time when this session will expire.
     * 
     * @return time when this session will expire
     */
    @Nonnull Instant getExpirationInstant();
    
    /**
     * Get a unique key identifying this subject's session with the service.
     * 
     * <p>This will vary based on the type of session, typically based on the protocol
     * used, but it provides a secondary lookup key that may be required in support
     * of other use cases involving that protocol. Not all protocols may require such
     * a key, so null may be returned.</p>
     * 
     * @return a unique key identifying this subject's session with the service, or null
     */
    @Nullable String getSPSessionKey();

    /**
     * Get a protocol constant for the session suitable for metadata lookup.
     *
     * @return a protocol constant
     * 
     * @since 5.0.0
     */
    @Nullable @NotEmpty String getProtocol();
    
    /**
     * Gets whether the session allows for logout propagation.
     * 
     * @return whether the session allows for logout propagation
     * 
     * @since 5.0.0
     */
    boolean supportsLogoutPropagation();

}
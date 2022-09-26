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
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.MoreObjects;

/**
 * Implementation support for a concrete {@link SPSession} implementation.
 */
@ThreadSafe
public class BasicSPSession implements SPSession {
    
    /** The unique identifier of the service. */
    @Nonnull @NotEmpty private final String serviceId;
    
    /** The time when this session was created. */
    @Nonnull private final Instant creationInstant;

    /** The time when this session expires. */
    @Nonnull private final Instant expirationInstant;
    
    /**
     * Constructor.
     * 
     * @param id the identifier of the service associated with this session
     * @param creation creation time of session
     * @param expiration expiration time of session
     */
    public BasicSPSession(@Nonnull @NotEmpty final String id, @Nonnull final Instant creation,
            @Nonnull final Instant expiration) {
        serviceId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Service ID cannot be null nor empty");
        creationInstant = Constraint.isNotNull(creation, "Creation instant cannot be null");
        expirationInstant = Constraint.isNotNull(expiration, "Expiration instant cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getId() {
        return serviceId;
    }
    
    /** {@inheritDoc} */
    @Nonnull public Instant getCreationInstant() {
        return creationInstant;
    }

    /** {@inheritDoc} */
    @Nonnull public Instant getExpirationInstant() {
        return expirationInstant;
    }

    /** {@inheritDoc} */
    public String getSPSessionKey() {
        // A basic session doesn't have a secondary lookup key.
        return null;
    }
    
    /** {@inheritDoc} */
    @Nullable @NotEmpty public String getProtocol() {
        return null;
    }

    /** {@inheritDoc} */
    public boolean supportsLogoutPropagation() {
        return false;
    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        return serviceId.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(@Nullable final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof BasicSPSession) {
            return Objects.equals(serviceId, ((BasicSPSession) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", serviceId)
                .add("creationInstant", creationInstant)
                .add("expirationInstant", expirationInstant).toString();
    }
    
}
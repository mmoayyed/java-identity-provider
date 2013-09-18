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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;

import com.google.common.base.Objects;

/** Describes a session with a service associated with an {@link IdPSession}. */
@ThreadSafe
public final class ServiceSession implements IdentifiableComponent {

    /** The unique identifier of the service. */
    @Nonnull @NotEmpty private final String serviceId;

    /** Identifies authentication flow used to authenticate to this service. */
    @Nonnull @NotEmpty private final String authenticationFlowId;
    
    /** The time, in milliseconds since the epoch, when this session was created. */
    @Duration @Positive private long creationInstant;

    /** The last activity instant, in milliseconds since the epoch, for the session. */
    @Duration @Positive private long lastActivityInstant;

    /**
     * Constructor. Initializes creation and last activity instant to the current time.
     * 
     * @param id the identifier of the service associated with this session
     * @param flowId authentication flow used to authenticate the principal to this service
     */
    public ServiceSession(@Nonnull @NotEmpty final String id, @Nonnull @NotEmpty final String flowId) {
        serviceId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Service ID cannot be null nor empty");
        authenticationFlowId = Constraint.isNotNull(
                StringSupport.trimOrNull(flowId), "Authentication flow ID cannot be null or empty");
        creationInstant = System.currentTimeMillis();
        lastActivityInstant = creationInstant;
    }

    /**
     * Get the unique identifier of the service.
     * 
     * @return unique identifier of the service
     */
    @Nonnull @NotEmpty public String getId() {
        return serviceId;
    }

    /**
     * Get the time, in milliseconds since the epoch, when this session was created.
     * 
     * @return time, in milliseconds since the epoch, when this session was created, never less than 0
     */
    public long getCreationInstant() {
        return creationInstant;
    }

    /**
     * Set the time, in milliseconds since the epoch, when this session was created.
     * 
     * @param time time, in milliseconds since the epoch, when this session was created
     */
    public void setCreationInstant(@Duration @Positive final long time) {
        creationInstant = Constraint.isGreaterThan(0, time, "Creation instant must be greater than 0");
    }
    
    /**
     * Get the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @return last activity instant, in milliseconds since the epoch, for the session, never less than 0
     */
    public long getLastActivityInstant() {
        return lastActivityInstant;
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session, must be greater than 0
     */
    public void setLastActivityInstant(@Duration @Positive final long instant) {
        lastActivityInstant = Constraint.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for the session to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /**
     * Gets the authentication flow ID associated with this service session.
     * 
     * @return authentication flow ID associated with this service session
     */
    @Nonnull @NotEmpty public String getAuthenticationFlowId() {
        return authenticationFlowId;
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

        if (obj instanceof ServiceSession) {
            return Objects.equal(serviceId, ((ServiceSession) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("Id", serviceId)
                .add("creationInstant", new DateTime(creationInstant)).add("lastActivityInstant", lastActivityInstant)
                .add("authenticationFlowId", authenticationFlowId).toString();
    }
    
}
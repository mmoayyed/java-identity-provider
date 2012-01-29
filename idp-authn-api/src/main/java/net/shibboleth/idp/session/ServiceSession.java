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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Objects;

/** Describes a session with a service associated with an {@link IdPSession}. */
@ThreadSafe
public final class ServiceSession extends BaseContext {

    /** The unique identifier of the service. */
    private String serviceId;

    /** The time, in milliseconds since the epoch, when this session was created. */
    private long creationInstant;

    /** The last activity instant, in milliseconds since the epoch, for the session. */
    private long lastActivityInstant;

    /** The authentication event associated with this service. */
    private AuthenticationEvent authenticationEvent;

    /**
     * Constructor. Initializes creation and last activity instant to the current time.
     * 
     * @param id the identifier of the service associated with this session, can not be null or empty
     */
    public ServiceSession(String id) {
        serviceId = Assert.isNotNull(StringSupport.trimOrNull(id), "Service ID can not be null nor empty");
        creationInstant = System.currentTimeMillis();
        lastActivityInstant = creationInstant;
    }

    /**
     * Gets the unique identifier of the service.
     * 
     * @return unique identifier of the service, never null nor empty
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Gets the time, in milliseconds since the epoch, when this session was created.
     * 
     * @return time, in milliseconds since the epoch, when this session was created, never less than 0
     */
    public long getCreationInstant() {
        return creationInstant;
    }

    /**
     * Gets the last activity instant, in milliseconds since the epoch, for the session.
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
    public void setLastActivityInstant(long instant) {
        lastActivityInstant = Assert.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for the session to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /**
     * Gets the authentication event currently associated with this session.
     * 
     * @return authentication event currently associated with this session, may be null
     */
    public AuthenticationEvent getAuthenticationEvent() {
        return authenticationEvent;
    }

    /**
     * Set the authentication event currently associated with this session.
     * 
     * @param event authentication event currently associated with this session, may be null
     */
    public void setAuthenticationEvent(AuthenticationEvent event) {
        authenticationEvent = event;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return serviceId.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof ServiceSession) {
            return Objects.equal(serviceId, ((ServiceSession) obj).getServiceId());
        }

        return false;
    }
}
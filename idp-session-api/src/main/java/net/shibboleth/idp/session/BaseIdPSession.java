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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

/**
 * Abstract base for implementations of {@link IdPSession}, handles basic
 * management of the instance data without addressing persistence.
 */
@ThreadSafe
public abstract class BaseIdPSession implements IdPSession {

    /** Name of {@link org.slf4j.MDC} attribute that holds the current session ID: <code>idp.session.id</code>. */
    public static final String MDC_ATTRIBUTE = "idp.session.id";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseIdPSession.class);
    
    /** Unique ID of this session. */
    @Nonnull @NotEmpty private final String id;
    
    /** A canonical name for the subject of the session. */
    @Nonnull @NotEmpty private final String principalName;

    /** Time, in milliseconds since the epoch, when this session was created. */
    @Duration private long creationInstant;

    /** Last activity instant, in milliseconds since the epoch, for this session. */
    @Duration private long lastActivityInstant;

    /** An IPv4 address to which the session is bound. */
    @Nullable private String ipV4Address;
    
    /** An IPv6 address to which the session is bound. */
    @Nullable private String ipV6Address;
        
    /** Tracks authentication results that have occurred during this session. */
    @Nonnull @NonnullElements private final ConcurrentMap<String, AuthenticationResult> authenticationResults;

    /** Tracks services which have been issued authentication tokens during this session. */
    @Nonnull @NonnullElements private final ConcurrentMap<String, ServiceSession> serviceSessions;

    /**
     * Constructor.
     * 
     * @param sessionId identifier for this session
     * @param canonicalName canonical name of subject
     */
    public BaseIdPSession(@Nonnull @NotEmpty final String sessionId, @Nonnull @NotEmpty final String canonicalName) {
        id = Constraint.isNotNull(StringSupport.trimOrNull(sessionId), "Session ID cannot be null or empty");
        principalName = Constraint.isNotNull(StringSupport.trimOrNull(canonicalName),
                "Principal name cannot be null or empty.");
        
        creationInstant = System.currentTimeMillis();
        lastActivityInstant = creationInstant;

        authenticationResults = new ConcurrentHashMap(5);
        serviceSessions = new ConcurrentHashMap(10);
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getId() {
        return id;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getPrincipalName() {
        return principalName;
    }

    /** {@inheritDoc} */
    public long getCreationInstant() {
        return creationInstant;
    }

    /**
     * Set the time, in milliseconds since the epoch, when this session was created.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session, must be greater than 0
     */
    public void setCreationInstant(@Duration @Positive final long instant) {
        creationInstant = Constraint.isGreaterThan(0, instant, "Creation instant must be greater than 0");
    }

    /** {@inheritDoc} */
    public long getLastActivityInstant() {
        return lastActivityInstant;
    }

    /**
     * Set the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session, must be greater than 0
     */
    public void setLastActivityInstant(@Duration @Positive final long instant) {
        lastActivityInstant = Constraint.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
    }

    /**
     * Set the last activity instant, in milliseconds since the epoch, for the session to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /**
     * Get the IPv4 address to which this session is bound.
     * 
     * @return bound IPv4 address, or null
     */
    @Nullable public String getIPV4Address() {
        return ipV4Address;
    }

    /**
     * Set the IPv4 address to which this session is bound.
     * 
     * @param address the address to set, or null
     */
    public void setIPV4Address(@Nullable final String address) {
        ipV4Address = StringSupport.trimOrNull(address);
    }

    /**
     * Get the IPv6 address to which this session is bound.
     * 
     * @return bound IPv6 address, or null
     */
    @Nullable public String getIPV6Address() {
        return ipV6Address;
    }

    /**
     * Set the IPv6 address to which this session is bound.
     * 
     * @param address the address to set, or null
     */
    public void setIPV6Address(@Nullable final String address) {
        ipV6Address = StringSupport.trimOrNull(address);
    }
    
    /** {@inheritDoc} */
    public boolean validate(@Nonnull @NotEmpty final String address) throws SessionException {
        if (!doTimeoutCheck()) {
            return false;
        } else if (isV6Address(address)) {
            return validateV6(address);
        } else {
            return validateV4(address);
        }
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<AuthenticationResult> getAuthenticationResults() {
        return ImmutableSet.copyOf(authenticationResults.values());
    }

    /** {@inheritDoc} */
    @Nullable public AuthenticationResult getAuthenticationResult(@Nonnull @NotEmpty final String flowId) {
        return authenticationResults.get(StringSupport.trimOrNull(flowId));
    }

    /**
     * Add a new {@link AuthenticationResult} to this {@link BaseIdPSession}, replacing any
     * existing result of the same flow ID.
     * 
     * @param result the result to add
     */
    public void addAuthenticationResult(@Nonnull final AuthenticationResult result) {
        Constraint.isNotNull(result, "AuthenticationResult cannot be null");

        AuthenticationResult prev = authenticationResults.put(result.getAuthenticationFlowId(), result);
        if (prev != null) {
            log.debug("IdPSession {}: replaced old AuthenticationResult for flow ID {}", id,
                    prev.getAuthenticationFlowId());
        }
    }

    /**
     * Disassociate an {@link AuthenticationResult} from this IdP session.
     * 
     * @param result the result to disassociate
     * 
     * @return true iff the given result had been associated with this IdP session and now is not
     */
    public boolean removeAuthenticationResult(@Nonnull final AuthenticationResult result) {
        Constraint.isNotNull(result, "Authentication event can not be null");

        return authenticationResults.remove(result.getAuthenticationFlowId(), result);
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<ServiceSession> getServiceSessions() {
        return ImmutableSet.copyOf(serviceSessions.values());
    }

    /** {@inheritDoc} */
    @Nullable public ServiceSession getServiceSession(@Nonnull @NotEmpty final String serviceId) {
        return serviceSessions.get(StringSupport.trimOrNull(serviceId));
    }

    /**
     * Add a new service session to this IdP session, replacing any existing session for the same
     * service.
     * 
     * @param serviceSession the service session
     */
    public void addServiceSession(@Nonnull final ServiceSession serviceSession) {
        Constraint.isNotNull(serviceSession, "Service session cannot be null");

        ServiceSession prev = serviceSessions.put(serviceSession.getId(), serviceSession);
        if (prev != null) {
            log.debug("IdPSession {}: replaced old ServiceSession for service {}", id, prev.getId());
        }
    }

    /**
     * Disassociate the given service session from this IdP session.
     * 
     * @param session the service session
     * 
     * @return true iff the given session had been associated with this IdP session and now is not
     */
    public boolean removeServiceSession(@Nonnull final ServiceSession session) {
        Constraint.isNotNull(session, "Service session cannot be null");

        return serviceSessions.remove(session.getId(), session);
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof BaseIdPSession) {
            return Objects.equal(getId(), ((BaseIdPSession) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("sessionId", id).add("principalName", principalName)
                .add("IPv4", ipV4Address).add("IPv6", ipV6Address)
                .add("creationInstant", new DateTime(creationInstant))
                .add("lastActivityInstant", new DateTime(lastActivityInstant))
                .add("authenticationResults", getAuthenticationResults()).add("serviceSessions", getServiceSessions())
                .toString();
    }

    /**
     * Returns true iff the address is an IPv6 address.
     * 
     * @param address   the string to check
     * @return true iff the input is an IPv6 address
     */
    protected boolean isV6Address(@Nonnull @NotEmpty final String address) {
        return address.contains(":");
    }
    
    /**
     * Test the session's validity based on the supplied IPv6 client address, and binds the
     * address to the session if no IPv6 binding exists.
     * 
     * @param address client address for validation
     * 
     * @return true iff the session is valid for the specified client
     * @throws SessionException if an error occurs validating the session
     */
    protected boolean validateV6(@Nonnull @NotEmpty final String address) throws SessionException {
        if (ipV6Address != null) {
            if (!ipV6Address.equals(address)) {
                log.warn("Client address is {} but session {} already bound to {}", address, id, ipV6Address);
                return false;
            }
        } else {
            log.info("Session {} not yet locked to a V6 address, locking it to {}", id, address);
            setIPV6Address(address);
        }
        
        return true;
    }

    /**
     * Test the session's validity based on the supplied IPv4 client address, and binds the
     * address to the session if no IPv4 binding exists.
     * 
     * @param address client address for validation
     * 
     * @return true iff the session is valid for the specified client
     * @throws SessionException if an error occurs validating the session
     */
    protected boolean validateV4(@Nonnull @NotEmpty final String address) throws SessionException {
        if (ipV4Address != null) {
            if (!ipV4Address.equals(address)) {
                log.warn("Client address is {} but session {} already bound to {}", address, id, ipV4Address);
                return false;
            }
        } else {
            log.info("Session {} not yet locked to a V4 address, locking it to {}", id, address);
            setIPV4Address(address);
        }
        
        return true;
    }

    /**
     * Implementations must override this method to enforce appropriate timeout checks on the session.
     * 
     * @return true iff the session remains valid
     */
    protected abstract boolean doTimeoutCheck();
}
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
 * Abstract base for implementations of {@link IdPSession}, handles basic management of the
 * instance data without addressing persistence.
 * 
 * <p>Data that can change post-construction can be modified using doSet/doAdd/doRemove methods
 * that maintain the object state. Abstract methods defined here or left unimplemented from the
 * interface should be implemented to call these methods and perform any additional work required
 * to maintain the coherence of the underlying store, if any.</p>
 * 
 * <p>The {@link #checkAddress(String)} method is implemented by calling into other abstract and defined
 * methods to check session state and update address information as required.</p> 
 */
@ThreadSafe
public abstract class BaseIdPSession implements IdPSession {

    /** Address syntaxes supported for address binding. */
    public enum AddressFamily {
        /** IP version 4 (dotted decimal). */
        IPV4,
        
        /** IP version 6 (colon-sep hex). */
        IPV6,
        
        /** Unknown. */
        UNKNOWN
    }
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseIdPSession.class);
    
    /** Unique ID of this session. */
    @Nonnull @NotEmpty private final String id;
    
    /** A canonical name for the subject of the session. */
    @Nonnull @NotEmpty private final String principalName;

    /** Time, in milliseconds since the epoch, when this session was created. */
    @Duration private final long creationInstant;

    /** Last activity instant, in milliseconds since the epoch, for this session. */
    @Duration private long lastActivityInstant;

    /** Addresses to which the session is bound. */
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
     * @param creationTime creation time of session in milliseconds
     */
    public BaseIdPSession(@Nonnull @NotEmpty final String sessionId, @Nonnull @NotEmpty final String canonicalName,
            @Positive final long creationTime) {
        id = Constraint.isNotNull(StringSupport.trimOrNull(sessionId), "Session ID cannot be null or empty");
        principalName = Constraint.isNotNull(StringSupport.trimOrNull(canonicalName),
                "Principal name cannot be null or empty.");
        
        creationInstant = Constraint.isGreaterThan(0, creationTime, "Creation time must be greater than 0");
        lastActivityInstant = creationTime;

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

    /** {@inheritDoc} */
    public long getLastActivityInstant() {
        return lastActivityInstant;
    }

    /**
     * Set the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session, must be greater than 0
     * @throws SessionException if an error occurs updating the session
     */
    public void setLastActivityInstant(@Duration @Positive final long instant) throws SessionException {
        doSetLastActivityInstant(instant);
    }
    
    /**
     * Set the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #setLastActivityInstant(long)}
     * method must be overridden to support other persistence requirements.</p>
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session, must be greater than 0
     */
    public void doSetLastActivityInstant(@Duration @Positive final long instant) {
        lastActivityInstant = Constraint.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
    }

    /**
     * Get an address to which this session is bound.
     * 
     * @param family the address family to inquire
     * 
     * @return bound address or null
     */
    @Nullable public String getAddress(@Nonnull final AddressFamily family) {
        switch (family) {
            case IPV4:
                return ipV4Address;
            case IPV6:
                return ipV6Address;
            default:
                return null;
        }
    }

    /**
     * Associate an address with this session.
     * 
     * @param address the address to associate
     * @throws SessionException if an error occurs binding the address to the session
     */
    public void bindToAddress(@Nonnull @NotEmpty final String address) throws SessionException {
        doBindToAddress(address);
    }

    /**
     * Associate an address with this session.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #bindToAddress(String)}
     * method must be overridden to support other persistence requirements.</p>
     * 
     * @param address the address to associate
     */
    public void doBindToAddress(@Nonnull @NotEmpty final String address) {
        String trimmed = Constraint.isNotNull(StringSupport.trimOrNull(address), "Address cannot be null or empty");
        switch (getAddressFamily(address)) {
            case IPV6:
                ipV6Address = StringSupport.trimOrNull(trimmed);
                break;
                
            case IPV4:
                ipV4Address = StringSupport.trimOrNull(trimmed);
                break;
                
            default:
                log.warn("Unsupported address form {}", address);
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

    /** {@inheritDoc} */
    public void addAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException {
        doAddAuthenticationResult(result);
    }

    /** {@inheritDoc} */
    public boolean removeAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException {
        return doRemoveAuthenticationResult(result);
    }

    /**
     * Add a new {@link AuthenticationResult} to this IdP session, replacing any
     * existing result of the same flow ID.
     * 
     * <p>This manipulates only the internal state of the object. The
     * {@link #addAuthenticationResult(AuthenticationResult)} method must be implemented to support
     * other persistence requirements.</p>
     * 
     * @param result the result to add
     */
    public void doAddAuthenticationResult(@Nonnull final AuthenticationResult result) {
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
     * <p>This manipulates only the internal state of the object. The
     * {@link #removeAuthenticationResult(AuthenticationResult)} method must be implemented to support
     * other persistence requirements.</p>
     * 
     * @param result the result to disassociate
     * 
     * @return true iff the given result had been associated with this IdP session and now is not
     */
    public boolean doRemoveAuthenticationResult(@Nonnull final AuthenticationResult result) {
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

    /** {@inheritDoc} */
    public void addServiceSession(@Nonnull final ServiceSession serviceSession) throws SessionException {
        doAddServiceSession(serviceSession);
    }

    /** {@inheritDoc} */
    public boolean removeServiceSession(@Nonnull final ServiceSession serviceSession) throws SessionException {
        return doRemoveServiceSession(serviceSession);
    }

    /**
     * Add a new service session to this IdP session, replacing any existing session for the same
     * service.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #addServiceSession(ServiceSession)}
     * method must be implemented to support other persistence requirements.</p>
     * 
     * @param serviceSession the service session
     */
    public void doAddServiceSession(@Nonnull final ServiceSession serviceSession) {
        Constraint.isNotNull(serviceSession, "Service session cannot be null");
    
        ServiceSession prev = serviceSessions.put(serviceSession.getId(), serviceSession);
        if (prev != null) {
            log.debug("IdPSession {}: replaced old ServiceSession for service {}", id, prev.getId());
        }
    }

    /**
     * Disassociate the given service session from this IdP session.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #removeServiceSession(ServiceSession)}
     * method must be implemented to support other persistence requirements.</p>
     * 
     * @param session the service session
     * 
     * @return true iff the given session had been associated with this IdP session and now is not
     */
    public boolean doRemoveServiceSession(@Nonnull final ServiceSession session) {
        Constraint.isNotNull(session, "Service session cannot be null");
    
        return serviceSessions.remove(session.getId(), session);
    }

    /** {@inheritDoc} */
    public boolean checkAddress(@Nonnull @NotEmpty final String address) throws SessionException {
        AddressFamily family = getAddressFamily(address);
        String bound = getAddress(family);
        if (bound != null) {
            if (!bound.equals(address)) {
                log.warn("Client address is {} but session {} already bound to {}", address, id, bound);
                return false;
            }
        } else {
            log.info("Session {} not yet locked to a {} address, locking it to {}", id, family, address);
            bindToAddress(address);
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    public boolean checkTimeout() throws SessionException {
        setLastActivityInstant(System.currentTimeMillis());
        return true;
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
     * Returns the address family for an input address.
     * 
     * @param address   the string to check
     * @return the address family
     */
    @Nonnull private static AddressFamily getAddressFamily(@Nonnull @NotEmpty final String address) {
        if (address.contains(":")) {
            return AddressFamily.IPV6;
        } else if (address.contains(".")) {
            return AddressFamily.IPV4;
        } else {
            return AddressFamily.UNKNOWN;
        }
    }
    
}
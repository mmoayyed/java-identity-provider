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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 * An identity provider session.
 */
@ThreadSafe
public final class IdPSession implements IdentifiableComponent {

    /** Name of {@link org.slf4j.MDC} attribute that holds the current session ID: <code>idp.session.id</code>. */
    public static final String MDC_ATTRIBUTE = "idp.session.id";

    /** Unique ID of this session. */
    private final String id;

    /** Secret associated with this session. */
    private final byte[] secret;

    /** Time, in milliseconds since the epoch, when this session was created. */
    private final long creationInstant;

    /** Last activity instant, in milliseconds since the epoch, for this session. */
    private long lastActivityInstant;

    /** Tracks authentication results that have occurred during this session. */
    private final ConcurrentMap<String, AuthenticationResult> authenticationResults;

    /** Tracks services which have been issued authentication tokens during this session. */
    private final ConcurrentMap<String, ServiceSession> serviceSessions;

    /**
     * Lock used to serialize requests that operate on {@link #authenticationResults} and {@link #serviceSessions}
     * in the same call.
     */
    private final Lock authnServiceStateLock;

    /**
     * Constructor.
     * 
     * @param sessionId identifier for this session
     * @param sessionSecret secrete for this session
     */
    public IdPSession(@Nonnull @NotEmpty final String sessionId, @Nonnull final byte[] sessionSecret) {
        id = Constraint.isNotNull(StringSupport.trimOrNull(sessionId), "Session ID can not be null or empty");

        Constraint.isNotNull(sessionSecret, "Session secret cannot be null");
        secret = new byte[sessionSecret.length];
        System.arraycopy(sessionSecret, 0, secret, 0, sessionSecret.length);

        creationInstant = System.currentTimeMillis();
        lastActivityInstant = creationInstant;

        authenticationResults = new ConcurrentHashMap<String, AuthenticationResult>(5);
        serviceSessions = new ConcurrentHashMap<String, ServiceSession>(10);
        
        authnServiceStateLock = new ReentrantLock();
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getId() {
        return id;
    }

    /**
     * Get a secret associated with the session. This is useful for things like encrypting session cookies.
     * 
     * @return secret associated with the session
     */
    @Nonnull public byte[] getSecret() {
        return secret;
    }

    /**
     * Get the time, in milliseconds since the epoch, when this session was created.
     * 
     * @return time this session was created, never less than 0
     */
    public long getCreationInstant() {
        return creationInstant;
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
     * Set the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session, must be greater than 0
     */
    public void setLastActivityInstant(@Positive final long instant) {
        lastActivityInstant = Constraint.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
    }

    /**
     * Set the last activity instant, in milliseconds since the epoch, for the session to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /**
     * Get the unmodifiable set of authentication results that have occurred during this session.
     * 
     * @return unmodifiable set of authentication results
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<AuthenticationResult> getAuthenticationResults() {
        return new HashSet(authenticationResults.values());
    }

    /**
     * Get the authentication event given its workflow ID.
     * 
     * @param flowId the ID of the authentication workflow
     * 
     * @return the authentication event
     */
    @Nullable public AuthenticationResult getAuthenticationResult(@Nonnull @NotEmpty final String flowId) {
        return authenticationResults.get(
                Constraint.isNotNull(StringSupport.trimOrNull(flowId), "Flow ID cannot be null or empty"));
    }

    /**
     * Gets the unmodifiable collection of service sessions associated with this session.
     * 
     * @return unmodifiable collection of service sessions associated with this session
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<ServiceSession> getServiceSessions() {
        return ImmutableSet.copyOf(serviceSessions.values());
    }

    /**
     * Get the ServiceSession for a given service.
     * 
     * @param serviceId ID of the service
     * 
     * @return the session service or null if no session exists for that service, may be null
     */
    @Nonnull public Optional<ServiceSession> getServiceSession(@Nullable String serviceId) {
        final String trimmedId = StringSupport.trimOrNull(serviceId);

        if (trimmedId == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(serviceSessions.get(trimmedId));
    }

    /**
     * Adds a new service session to this IdP session. The {@link AuthenticationResult} associated with the
     * {@link ServiceSession} is associated with this {@link IdPSession} if it has not been so already.
     * 
     * @param session the service session
     */
    public void addServiceSession(@Nonnull final ServiceSession session) {
        Constraint.isNotNull(session, "Service session can not be null");

        final String serviceId = session.getId();
        try {
            authnServiceStateLock.lock();
            Constraint.isFalse(serviceSessions.containsKey(serviceId), "A session for service " + serviceId
                    + " already exists");

            final AuthenticationResult authnEvent = session.getAuthenticationEvent();
            if (!authenticationResults.containsKey(authnEvent.getAuthenticationFlowId())) {
                authenticationResults.put(authnEvent.getAuthenticationFlowId(), authnEvent);
            }
            
            //TODO(lajoie) don't we need to update the authn event if it already exists?

            serviceSessions.put(serviceId, session);
        } finally {
            authnServiceStateLock.unlock();
        }
    }

    /**
     * Disassociates the given service session from this IdP session.
     * 
     * @param session the service session
     * 
     * @return true if the given session had been associated with this IdP session and now is not
     */
    public boolean removeServiceSession(@Nonnull final ServiceSession session) {
        Constraint.isNotNull(session, "Service session can not be null");

        return serviceSessions.remove(session.getId(), session);
    }

    /**
     * Disassociated a given authentication event from this IdP session. This is only possible if all
     * {@link ServiceSession}s that were associated with the authentication event have already disassociated from this
     * IdP session. Otherwise an {@link IllegalStateException} is thrown.
     * 
     * @param event the event to disassociate
     */
    public void removeAuthenticationEvent(@Nonnull final AuthenticationResult event) {
        Constraint.isNotNull(event, "Authentication event can not be null");

        try {
            authnServiceStateLock.lock();

            for (ServiceSession session : serviceSessions.values()) {
                if (session.getAuthenticationEvent().equals(event)) {
                    throw new IllegalStateException("Authentication event " + event.getAuthenticationFlowId()
                            + " is associated with the session for service " + session.getId()
                            + " and so can not be removed");
                }
            }

            authenticationResults.remove(event.getAuthenticationFlowId(), event);
        } finally {
            authnServiceStateLock.unlock();
        }
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof IdPSession) {
            return Objects.equal(getId(), ((IdPSession) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("sessionId", id).add("creatingInstant", new DateTime(creationInstant))
                .add("lastActivityInstant", new DateTime(lastActivityInstant))
                .add("authenticationResults", getAuthenticationResults()).add("serviceSessions", getServiceSessions())
                .toString();
    }
}
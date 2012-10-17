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

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * An identity provider session.
 * 
 * <strong>NOTE:</strong> if a session has been persisted by means of a {@link SessionStore} be sure to persist the
 * session if you make any changes to its state.
 */
@ThreadSafe
public final class IdPSession extends BaseContext implements IdentifiableComponent {

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

    /** The authentication events that have occurred within the scope of this session. */
    private final ConcurrentMap<String, AuthenticationEvent> authenticationEvents;

    /** The service which have been authenticated to in this session. */
    private final ConcurrentMap<String, ServiceSession> serviceSessions;

    /**
     * Lock used to serialize requests that operate on {@link #authenticationEvents} and {@link #serviceSessions} in the
     * same call.
     */
    private final Lock authnServiceStateLock = new ReentrantLock();

    /**
     * Constructor.
     * 
     * @param sessionId identifier for this session
     * @param sessionSecret secrete for this session
     */
    public IdPSession(@Nonnull @NotEmpty final String sessionId, @Nonnull byte[] sessionSecret) {
        id = Constraint.isNotNull(StringSupport.trimOrNull(sessionId), "Session ID can not be null or empty");

        Constraint.isNotNull(sessionSecret, "Session secret can not be null");
        secret = new byte[sessionSecret.length];
        System.arraycopy(sessionSecret, 0, secret, 0, sessionSecret.length);

        creationInstant = System.currentTimeMillis();
        lastActivityInstant = creationInstant;

        authenticationEvents = new ConcurrentHashMap<String, AuthenticationEvent>(5);
        serviceSessions = new ConcurrentHashMap<String, ServiceSession>(10);
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getId() {
        return id;
    }

    /**
     * Gets a secret associated with the session. This is useful for things like encrypting session cookies.
     * 
     * @return secret associated with the session
     */
    @Nonnull public byte[] getSecret() {
        return secret;
    }

    /**
     * Gets the time, in milliseconds since the epoch, when this session was created.
     * 
     * @return time this session was created, never less than 0
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
        lastActivityInstant = Constraint.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for the session to the current time.
     */
    public void setLastActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /**
     * Gets the unmodifiable set of authentication events that have occurred within the scope of this session.
     * 
     * @return unmodifiable set of authentication events that have occurred within the scope of this session
     */
    @Nonnull @NonnullElements @NotLive public Set<AuthenticationEvent> getAuthenticateEvents() {
        return new HashSet(authenticationEvents.values());
    }

    /**
     * Gets the authentication event given its workflow ID.
     * 
     * @param workflowId the ID of the authentication workflow
     * 
     * @return the authentication event
     */
    @Nonnull public Optional<AuthenticationEvent> getAuthenticationEvent(@Nullable final String workflowId) {
        final String trimmedId = StringSupport.trimOrNull(workflowId);

        if (trimmedId == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(authenticationEvents.get(trimmedId));
    }

    /**
     * Gets the unmodifiable collection of service sessions associated with this session.
     * 
     * @return unmodifiable collection of service sessions associated with this session
     */
    @Nonnull @NonnullElements @NotLive public Set<ServiceSession> getServiceSessions() {
        return new HashSet(serviceSessions.values());
    }

    /**
     * The session service for the given service.
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
     * Adds a new service session to this IdP session. The {@link AuthenticationEvent} associated with the
     * {@link ServiceSession} is associated with this {@link IdPSession} if it has not been so already.
     * 
     * @param session the service session
     */
    public void addServiceSession(@Nonnull final ServiceSession session) {
        Constraint.isNotNull(session, "Service session can not be null");

        final String serviceId = session.getServiceId();
        try {
            authnServiceStateLock.lock();
            Constraint.isFalse(serviceSessions.containsKey(serviceId), "A session for service " + serviceId
                    + " already exists");

            final AuthenticationEvent authnEvent = session.getAuthenticationEvent();
            if (!authenticationEvents.containsKey(authnEvent.getAuthenticationWorkflow())) {
                authenticationEvents.put(authnEvent.getAuthenticationWorkflow(), authnEvent);
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

        return serviceSessions.remove(session.getServiceId(), session);
    }

    /**
     * Disassociated a given authentication event from this IdP session. This is only possible if all
     * {@link ServiceSession}s that were associated with the authentication event have already disassociated from this
     * IdP session. Otherwise an {@link IllegalStateException} is thrown.
     * 
     * @param event the event to disassociate
     */
    public void removeAuthenticationEvent(@Nonnull final AuthenticationEvent event) {
        Constraint.isNotNull(event, "Authentication event can not be null");

        try {
            authnServiceStateLock.lock();

            for (ServiceSession session : serviceSessions.values()) {
                if (session.getAuthenticationEvent().equals(event)) {
                    throw new IllegalStateException("Authentication event " + event.getAuthenticationWorkflow()
                            + " is associated with the session for service " + session.getServiceId()
                            + " and so can not be removed");
                }
            }

            authenticationEvents.remove(event.getAuthenticationWorkflow(), event);
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
                .add("authenticationEvents", getAuthenticateEvents()).add("serviceSessions", getServiceSessions())
                .toString();
    }
}
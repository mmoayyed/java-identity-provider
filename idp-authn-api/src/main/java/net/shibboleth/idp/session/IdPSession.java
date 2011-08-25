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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.component.IdentifiedComponent;

/**
 * An identity provider session.
 * 
 * <strong>NOTE:</strong> if a session has been persisted by means of a {@link SessionStore} be sure to persist the
 * session if you make any changes to its state.
 */
@ThreadSafe
public final class IdPSession extends AbstractSubcontextContainer implements IdentifiedComponent {

    /** Unique ID of this session. */
    private final String id;

    /** Secret associated with this session. */
    private final byte[] secret;

    /** Time, in milliseconds since the epoch, when this session was created. */
    private final long creationInstant;

    /** Last activity instant, in milliseconds since the epoch, for this session. */
    private long lastActivityInstant;

    /** Unmodifiable authentication events that have occurred within the scope of this session. */
    private Set<AuthenticationEvent> authenticationEvents;

    /** Unmodifiable service session tied to this IdP session. */
    private Set<ServiceSession> serviceSessions;

    /** Service sessions indexed by service ID. */
    private Map<String, ServiceSession> serviceSessionIndex;

    /**
     * Constructor.
     * 
     * @param sessionId identifier for this session, can not be null or empty
     * @param sessionSecret secrete for this session, can not be null
     */
    public IdPSession(String sessionId, byte[] sessionSecret) {
        id = StringSupport.trimOrNull(sessionId);
        Assert.isNotNull(id, "Session ID can not be null or empty");

        Assert.isNotNull(sessionSecret, "Session secret can not be null");
        secret = new byte[sessionSecret.length];
        System.arraycopy(sessionSecret, 0, secret, 0, sessionSecret.length);

        creationInstant = System.currentTimeMillis();
        lastActivityInstant = creationInstant;

        authenticationEvents = Collections.emptySet();
        serviceSessions = Collections.emptySet();
        serviceSessionIndex = new HashMap<String, ServiceSession>();
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Gets a secret associated with the session. This is useful for things like encrypting session cookies.
     * 
     * @return secret associated with the session, never null or empty
     */
    public byte[] getSecret() {
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
        Assert.isGreaterThan(0, instant, "Last activity instant must be greater than 0");
        lastActivityInstant = instant;
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
     * @return unmodifiable set of authentication events that have occurred within the scope of this session, never null
     *         nor containing null elements
     */
    public Set<AuthenticationEvent> getAuthenticateEvents() {
        return authenticationEvents;
    }

    /**
     * Adds an authentication event to this session. If the given event is null or has already been added to this
     * session this method simply returns.
     * 
     * @param event event to be added, may be null
     */
    public synchronized void addAuthenticationEvent(AuthenticationEvent event) {
        if (event == null || authenticationEvents.contains(event)) {
            return;
        }

        HashSet<AuthenticationEvent> eventsCopy = new HashSet<AuthenticationEvent>(authenticationEvents);
        eventsCopy.add(event);
        authenticationEvents = Collections.unmodifiableSet(eventsCopy);
    }

    /**
     * Removes an authentication event from this IdP session and disassociates it from any {@link ServiceSession}. If
     * the given event is null or is not associated with this session this method simply returns.
     * 
     * @param event the event to be removed, may be null
     */
    public synchronized void removeAuthenticationEvent(AuthenticationEvent event) {
        if (event == null || !authenticationEvents.contains(event)) {
            return;
        }

        HashSet<AuthenticationEvent> eventsCopy = new HashSet<AuthenticationEvent>(authenticationEvents);
        eventsCopy.remove(event);

        for (ServiceSession session : serviceSessions) {
            if (event.equals(session.getAuthenticationEvent())) {
                session.setAuthenticationEvent(null);
            }
        }

        authenticationEvents = Collections.unmodifiableSet(eventsCopy);
    }

    /**
     * Gets the unmodifiable collection of service sessions associated with this session.
     * 
     * @return unmodifiable collection of service sessions associated with this session, never null nor containing null
     *         elements
     */
    public Set<ServiceSession> getServiceSessions() {
        return serviceSessions;
    }

    /**
     * The session service for the given service.
     * 
     * @param serviceId ID of the service
     * 
     * @return the session service or null if no session exists for that service, may be null
     */
    public ServiceSession getServiceSession(String serviceId) {
        return serviceSessionIndex.get(serviceId);
    }

    /**
     * Associates a service session with this IdP session. If the given service session is null or has already been
     * added to this session this method simply returns.
     * 
     * @param serviceSession service session to be associated with this IdP session, may be null
     */
    public synchronized void addServiceSession(ServiceSession serviceSession) {
        if (serviceSession == null || serviceSessions.contains(serviceSession)) {
            return;
        }

        HashSet<ServiceSession> sessionsCopy = new HashSet<ServiceSession>(serviceSessions);
        sessionsCopy.add(serviceSession);
        serviceSessionIndex.put(serviceSession.getServiceId(), serviceSession);
        serviceSessions = Collections.unmodifiableSet(sessionsCopy);
    }

    /**
     * Removes the service session from this IdP session. If the given service session is null or is not associated with
     * this session this method simply returns.
     * 
     * @param serviceSession the service session to be removed, may be null
     */
    public void removeServiceSession(ServiceSession serviceSession) {
        if (serviceSession == null || !serviceSessions.contains(serviceSession)) {
            return;
        }

        HashSet<ServiceSession> sessionsCopy = new HashSet<ServiceSession>(serviceSessions);
        sessionsCopy.remove(serviceSession);
        serviceSessionIndex.remove(serviceSession.getServiceId());
        serviceSessions = Collections.unmodifiableSet(sessionsCopy);
    }
}
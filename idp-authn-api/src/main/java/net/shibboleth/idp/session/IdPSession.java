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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;

/**
 * An identity provider session.
 * 
 * Properties of this object <strong>must not</strong> be modifiable directly. Instead, use the modification methods
 * available via the {@link SessionStore} that created this session.
 */
public class IdPSession extends AbstractSubcontextContainer {

    /** Unique ID of this session. */
    private String id;

    /** Secret associated with this session. */
    private byte[] secret;

    /** Time, in milliseconds since the epoch, when this session was created. */
    private long creationInstant;

    /** Last activity instant, in milliseconds since the epoch, for this session. */
    private long lastActivityInstant;

    /** Gets the authentication events that have occurred within the scope of this session. */
    private Collection<AuthenticationEvent> authnEvents;

    /** Gets the service session tied to this IdP session. */
    private Map<String, ServiceSession> serviceSessions;

    /**
     * Constructor.
     * 
     * Creation and last activity instant are initialized to now. Authentication events collection is initialized to an
     * empty {@link Vector}. Service session collection is initialized to an empty {@link ConcurrentHashMap}.
     */
    public IdPSession() {
        super();

        creationInstant = System.currentTimeMillis();
        lastActivityInstant = creationInstant;
        authnEvents = new Vector<AuthenticationEvent>();
        serviceSessions = new ConcurrentHashMap<String, ServiceSession>();
    }

    /**
     * Gets the unique identifier for this session.
     * 
     * @return unique identifier for this session, never null or empty
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this session.
     * 
     * @param sessionId unique identifier for this session
     */
    protected void setId(String sessionId) {
        String trimmedId = StringSupport.trimOrNull(sessionId);
        Assert.isNotNull(trimmedId, "Session ID can not be null or empty");
        id = trimmedId;
    }

    /**
     * Gets a secret associated with the session. This is useful for things like encrypting session cookies.
     * 
     * @return secret associated with the session
     */
    public byte[] getSecret() {
        return secret;
    }

    /**
     * Sets the secret associated with the session.
     * 
     * @param sessionSecret secret associated with the session
     */
    protected void setSecret(byte[] sessionSecret) {
        secret = sessionSecret;
    }

    /**
     * Gets the time, in milliseconds since the epoch, when this session was created.
     * 
     * @return time this session was created
     */
    public long getCreationInstant() {
        return creationInstant;
    }

    /**
     * Sets the time, in milliseconds since the epoch, when this session was created.
     * 
     * @param instant time when this session was created, must be greater than 0
     */
    protected void setCreationInstant(long instant) {
        Assert.isGreaterThan(0, instant, "IdP Session creation instant must be greater than 0");
        creationInstant = instant;
    }

    /**
     * Gets the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @return last activity instant, in milliseconds since the epoch, for the session
     */
    public long getLastActivityInstant() {
        return lastActivityInstant;
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for the session.
     * 
     * @param instant last activity instant, in milliseconds since the epoch, for the session
     */
    protected void setActivityInstant(long instant) {
        lastActivityInstant = instant;
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for the session to the current time.
     */
    protected void setActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /**
     * Gets the unmodifiable collection of authentication events that have occurred within the scope of this session.
     * 
     * @return unmodifiable collection of authentication events that have occurred within the scope of this session,
     *         never null
     */
    public Collection<AuthenticationEvent> getAuthenticateEvents() {
        return Collections.unmodifiableCollection(authnEvents);
    }

    /**
     * Sets the internal authentication event collection to the given collection.
     * 
     * @param events collection used to store authentication events, can not be null
     */
    protected void setAuthenticationEvents(Collection<AuthenticationEvent> events) {
        Assert.isNotNull(events, "Authentication event collection can not be null");
        authnEvents = events;
    }

    /**
     * Gets all the authentication events for a particular method that have occurred within this session.
     * 
     * @param authenticationMethod the authentication method, may be null or empty
     * 
     * @return all the authentication events for a particular method that have occurred within this session, never null
     */
    public Collection<AuthenticationEvent> getAuthenticationEvents(String authenticationMethod) {
        String trimmedMethod = StringSupport.trimOrNull(authenticationMethod);
        if (trimmedMethod == null || authnEvents.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<AuthenticationEvent> matchingEvents = new ArrayList<AuthenticationEvent>();

        for (AuthenticationEvent event : authnEvents) {
            if (ObjectSupport.equals(event.getAuthenticationMethod(), authenticationMethod)) {
                matchingEvents.add(event);
            }
        }

        return matchingEvents;
    }

    /**
     * Adds an authentication event to this session. Prior to the event being added, this IdP session must have a
     * {@link ServiceSession} recorded for the service referenced by the {@link AuthenticationEvent}. If the given event
     * is null or has already been added to this session this method simply returns.
     * 
     * @param event event to be added, may be null
     */
    public void addAuthenticationEvent(AuthenticationEvent event) {
        if (event == null || authnEvents.contains(event)) {
            return;
        }

        String serviceId = StringSupport.trimOrNull(event.getServiceId());
        Assert.isNotNull(serviceId, "Authentication event service ID can not be null or empty");
        ServiceSession serviceSession = serviceSessions.get(event.getServiceId());
        Assert.isNotNull(serviceSession, "Authentication event references a session for which there is no session");

        authnEvents.add(event);
    }

    /**
     * Gets the unmodifiable collection of service sessions associated with this session.
     * 
     * @return unmodifiable collection of service sessions associated with this session, never null
     */
    public Collection<ServiceSession> getServiceSessions() {
        return Collections.unmodifiableCollection(serviceSessions.values());
    }

    /**
     * Sets the internal service session collection to the given collection.
     * 
     * @param sessions collection used to store service session, can not be null
     */
    protected void setServiceSessions(Map<String, ServiceSession> sessions) {
        Assert.isNotNull(sessions, "Service session collection can not be null");
        serviceSessions = sessions;
    }

    /**
     * The session service for the given service.
     * 
     * @param serviceId ID of the service
     * 
     * @return the session service or null if no session exists for that service
     */
    public ServiceSession getServiceSession(String serviceId) {
        String trimmedId = StringSupport.trimOrNull(serviceId);
        if (trimmedId == null || serviceSessions.isEmpty()) {
            return null;
        }

        return serviceSessions.get(trimmedId);
    }

    /**
     * Associates a service session with this IdP session. If the given service session is null or has already been
     * added to this session this method simply returns.
     * 
     * @param serviceSession service session to be associated with this IdP session
     */
    public void addServiceSession(ServiceSession serviceSession) {
        if (serviceSession == null || serviceSessions.containsKey(serviceSession.getServiceId())) {
            return;
        }

        serviceSessions.put(serviceSession.getServiceId(), serviceSession);
    }

    /**
     * Gets all the authentication events, of a particular method, for a particular service that have occurred within
     * this IdP session.
     * 
     * @param serviceId the ID of the service
     * @param authenticationMethod the authentication method
     * 
     * @return all the authentication events, of a particular method, for a particular service that have occurred within
     *         this IdP session, never null
     */
    public Collection<AuthenticationEvent> getAuthenticationEventForService(String serviceId,
            String authenticationMethod) {
        String trimmedMethod = StringSupport.trimOrNull(authenticationMethod);
        String trimmedId = StringSupport.trimOrNull(serviceId);
        if (trimmedMethod == null || trimmedId == null || authnEvents.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<AuthenticationEvent> matchingEvents = new ArrayList<AuthenticationEvent>();

        for (AuthenticationEvent event : authnEvents) {
            if (ObjectSupport.equals(event.getServiceId(), trimmedId)) {
                matchingEvents.add(event);
            }
        }

        return matchingEvents;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (other == this) {
            return true;
        }

        if (other instanceof IdPSession) {
            return ObjectSupport.equals(id, ((IdPSession) other).getId());
        }

        return false;
    }
}
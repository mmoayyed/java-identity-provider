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

import java.util.Collection;
import java.util.Collections;

import javax.security.auth.Subject;

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

//TODO implement hashCode/equals - need to implement this for AbstractSubcontextContainer as well

/**
 * An identity provider session.
 * 
 * Properties of this object <strong>must not</strong> be modifiable directly. Instead, use the modification methods
 * available via the {@link SessionManager} that created this session.
 */
public class IdPSession extends AbstractSubcontextContainer {

    /** Unique ID of this session. */
    private String id;

    /** Secret associated with this session. */
    private byte[] secret;

    /**
     * Time, in milliseconds since the epoch, when this session expires, regardless of activity. A value of 0 or less
     * indicates the session does not have an absolute expiration instant.
     */
    private long expirationInstant;

    /**
     * Amount of time, in milliseconds, a session may be inactive before it is considered expired. A value of 0 or less
     * indicates the session never expires due to inactivity.
     */
    private long inactivityTimeout;

    /** Last activity instant, in milliseconds since the epoch, for this session. */
    private long lastActivityInstant;

    /** Gets the subject associated with this session. */
    private Subject subject;

    /** Gets the authentication events that have occurred within the scope of this session. */
    private Collection<AuthenticationEvent> authnEvents;

    /** Gets the service session tied to this IdP session. */
    private Collection<ServiceSession> serviceSessions;

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
     * Gets the time, in milliseconds since the epoch, when this session expires regardless of activity. A value of 0 or
     * less indicates the session does not have an absolute expiration instant.
     * 
     * @return time, in milliseconds since the epoch, when this session expires regardless of activity
     */
    public long getExpirationInstant() {
        return expirationInstant;
    }

    /**
     * Sets the time, in milliseconds since the epoch, when this session expires regardless of activity. A value of 0 or
     * less indicates the session does not have an absolute expiration instant.
     * 
     * @param instant time, in milliseconds since the epoch, when this session expires regardless of activity
     */
    protected void setExipriationInstant(long instant) {
        expirationInstant = instant;
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
     * Gets the amount of time, in milliseconds, a session may be inactive before it is considered expired. A value of 0
     * or less indicates the session never expires due to inactivity.
     * 
     * @return amount of time, in milliseconds, a session may be inactive before it is considered expired
     */
    public long getInactivityTimeout() {
        return inactivityTimeout;
    }

    /**
     * Sets the amount of time, in milliseconds, a session may be inactive before it is considered expired. A value of 0
     * or less indicates the session never expires due to inactivity.
     * 
     * @param timeout amount of time, in milliseconds, a session may be inactive before it is considered expired
     */
    protected void setInactivityTimeout(long timeout) {
        inactivityTimeout = timeout;
    }

    /**
     * Gets whether this session has expired. A session is considered expired if the current time is after the
     * expiration instant or the current time minus the last activity instant is greater than the inactivity timeout.
     * 
     * @return whether this session has expired
     */
    public boolean isExpired() {
        long now = System.currentTimeMillis();

        if (expirationInstant > 0 && now > expirationInstant) {
            return true;
        }

        if (inactivityTimeout > 0 && now - lastActivityInstant > inactivityTimeout) {
            return true;
        }

        return false;
    }

    /**
     * Gets the subject associated with this session.
     * 
     * @return subject associated with this session, never null
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Sets the subject associated with this session.
     * 
     * @param sessionSubject subject associated with this session, never null
     */
    protected void setSubject(Subject sessionSubject) {
        Assert.isNotNull(sessionSubject, "Session subject may not be null");
        subject = sessionSubject;
    }

    /**
     * Gets the unmodifiable collection of authentication events that have occurred within the scope of this session.
     * 
     * @return unmodifiable collection of authentication events that have occurred within the scope of this session
     */
    public Collection<AuthenticationEvent> getAuthenticateEvents() {
        return Collections.unmodifiableCollection(authnEvents);
    }

    /**
     * Gets the modifiable collection of authentication events that have occurred within the scope of this session.
     * 
     * @return modifiable collection of authentication events that have occurred within the scope of this session
     */
    protected Collection<AuthenticationEvent> getModifiableAuthenticationEventCollection() {
        return authnEvents;
    }

    /**
     * Gets the unmodifiable collection of service sessions associated with this session.
     * 
     * @return unmodifiable collection of service sessions associated with this session
     */
    public Collection<ServiceSession> getServiceSessions() {
        return Collections.unmodifiableCollection(serviceSessions);
    }

    /**
     * Gets the modifiable collection of service sessions associated with this session.
     * 
     * @return modifiable collection of service sessions associated with this session
     */
    protected Collection<ServiceSession> getModifiableSeviceSessionCollection() {
        return serviceSessions;
    }
    
    public ServiceSession getServiceSession(String serviceId){
        
    }
}
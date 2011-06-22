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

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/** An identity provider session. */
public class Session extends AbstractSubcontextContainer {

    /** Unique ID of this session. */
    private final String id;

    /** Secret associated with this session. */
    private final byte[] secret;

    /** Time, in milliseconds since the epoch, when this session expires, regardless of activity. */
    private final long ttl;

    /** Amount of time, in milliseconds, a session may be inactive before it is considered expired. */
    private final long inactivityTimeout;

    /** Last activity instant, in milliseconds since the epoch, for this session. */
    private long lastActivityInstant;

    /**
     * Constructor.
     * 
     * @param sessionId unique identifier for this session, can not be null or empty
     * @param sessionSecret secret associated with this session, may be null
     * @param lifetime Maximum amount of time, in milliseconds, for which a session is valid. A value of 0 or less
     *            indicates that the session does not have an absolute timeout.
     * @param inactivity Amount of time, in milliseconds, a session may be inactive before it is considered expired. A
     *            time of 0 or less indicates that the session never expires due to inactivity.
     */
    public Session(String sessionId, byte[] sessionSecret, long lifetime, long inactivity) {
        String trimmedId = StringSupport.trimOrNull(sessionId);
        Assert.isNotNull(trimmedId, "Session ID can not be null or empty");
        id = trimmedId;

        secret = sessionSecret;
        ttl = System.currentTimeMillis() + lifetime;
        inactivityTimeout = inactivity;
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
     * Gets a secret associated with the session. This is useful for things like encrypting session cookies.
     * 
     * @return secret associated with the session
     */
    public byte[] getSecret() {
        return secret;
    }

    /**
     * Gets the time, in milliseconds since the epoch, when this session expires, regardless of activity.
     * 
     * @return time, in milliseconds since the epoch, when this session expires, regardless of activity
     */
    public long getTimeToLive() {
        return ttl;
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
    public void setActivityInstant(long instant) {
        lastActivityInstant = instant;
    }

    /**
     * Sets the last activity instant, in milliseconds since the epoch, for the session to the current time.
     */
    public void setActivityInstantToNow() {
        lastActivityInstant = System.currentTimeMillis();
    }

    /**
     * Gets the amount of time, in milliseconds, a session may be inactive before it is considered expired.
     * 
     * @return amount of time, in milliseconds, a session may be inactive before it is considered expired
     */
    public long getInactivityTimeout() {
        return inactivityTimeout;
    }

    /**
     * Gets whether this session has expired. A session is considered expired if the current time is after the
     * expiration instant or the current time minus the last activity instant is greater than the inactivity timeout.
     * 
     * @return whether this session has expired
     */
    public boolean isExpired() {
        long now = System.currentTimeMillis();

        if (ttl > 0 && now > ttl) {
            return true;
        }

        if (inactivityTimeout > 0 && now - lastActivityInstant > inactivityTimeout) {
            return true;
        }

        return false;
    }
}
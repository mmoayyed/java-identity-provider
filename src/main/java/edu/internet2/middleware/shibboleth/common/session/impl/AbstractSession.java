/*
 * Copyright 2006 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.session.impl;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.joda.time.DateTime;

import edu.internet2.middleware.shibboleth.common.session.Session;

/** Base class for Shibboleth sessions. */
public abstract class AbstractSession implements Session {

    /** The session ID. */
    private final String sessionId;

    /** Subject of this session. */
    private Subject subject;

    /** Session inactivity timeout in milliseconds. */
    private long inactivityTimeout;

    /** The last activity time of the user. */
    private DateTime lastActivity;

    /**
     * Constructor.
     * 
     * @param id ID of the session
     * @param timeout inactivity timeout for the session in milliseconds
     */
    public AbstractSession(String id, long timeout) {
        sessionId = id;
        subject = new Subject();
        inactivityTimeout = timeout;
        lastActivity = new DateTime();
    }

    /** {@inheritDoc} */
    public String getSessionID() {
        return sessionId;
    }

    /** {@inheritDoc} */
    public Subject getSubject() {
        return subject;
    }

    /** {@inheritDoc} */
    public void setSubject(Subject newSubject) {
        subject = newSubject;
    }

    /** {@inheritDoc} */
    public String getPrincipalName() {
        Set<Principal> principals = subject.getPrincipals();
        if (principals != null && !principals.isEmpty()) {
            return principals.iterator().next().getName();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public long getInactivityTimeout() {
        return inactivityTimeout;
    }

    /** {@inheritDoc} */
    public DateTime getLastActivityInstant() {
        return lastActivity;
    }

    /** {@inheritDoc} */
    public void setLastActivityInstant(DateTime activity) {
        lastActivity = activity;
    }
}
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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import net.shibboleth.idp.AbstractComponent;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.util.Assert;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.resolver.ResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vt.middleware.crypt.util.HexConverter;

/** Component that creates, manages and locates session. */
public class BasicSessionManager extends AbstractComponent implements SessionResolver {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BasicSessionManager.class);

    /** Number of random bits within a session ID. */
    private final int sessionIDSize = 32;

    /** A {@link SecureRandom} PRNG to generate session IDs. */
    private final SecureRandom prng;

    /** Converts byte to hex and vice versa. */
    private final HexConverter hexCodec;

    /**
     * Maximum amount of time, in milliseconds, for which a session is valid. A value of 0 or less indicates that the
     * session does not have an absolute timeout.
     */
    private final long sessionLifetime;

    /**
     * Maximum amount of time, in millisecond, that a session may be "inactive" before being considered expired. A value
     * of 0 or less indicates that the session never times out due to inactivity.
     */
    private final long sessionTimeout;

    /** Session storage keyed by session ID. */
    private final Map<String, IdPSession> sessionStore;

    /**
     * Constructor.
     * 
     * @param componentId unique identifier for this component
     * @param lifetime Maximum amount of time, in milliseconds, for which a session is valid. A value of 0 or less
     *            indicates that the session does not have an absolute timeout.
     * @param timeout Maximum amount of time, in millisecond, that a session may be "inactive" before being considered
     *            expired. A value of 0 or less indicates that the session never times out due to inactivity.
     * @param storage session store keyed by session value, never null
     */
    public BasicSessionManager(String componentId, long lifetime, long timeout, Map<String, IdPSession> storage) {
        super(componentId);

        prng = new SecureRandom();
        hexCodec = new HexConverter();

        sessionLifetime = lifetime;
        sessionTimeout = timeout;

        Assert.isNotNull(storage, "Session store can not be null");
        sessionStore = storage;
    }
    
    public Iterable<IdPSession> getSessions(){
        return sessionStore.values();
    }

    /**
     * Creates a new session.
     * 
     * @return the new session
     */
    public IdPSession newSession() {
        // generate a random session ID
        byte[] sid = new byte[sessionIDSize];
        prng.nextBytes(sid);
        final String sessionId = hexCodec.fromBytes(sid);

        // generate a random secret
        final byte[] sessionSecret = new byte[16];
        prng.nextBytes(sessionSecret);

        long now = System.currentTimeMillis();
        IdPSession session = new IdPSession();
        session.setActivityInstant(now);
        session.setExipriationInstant(now + sessionLifetime);
        session.setId(sessionId);
        session.setInactivityTimeout(sessionTimeout);
        session.setSecret(sessionSecret);
        sessionStore.put(sessionId, session);

        return session;
    }

    public void removeSession(String sessionId) {
        sessionStore.remove(sessionId);
    }

    public void updateSessionLastActivityInstant(IdPSession idpSession) {

    }

    public void addAuthenticationEvent(IdPSession idpSession, String serviceId, AuthenticationEvent event) {

    }

    public void addIdPSessionContext(IdPSession idpSession, Subcontext subcontext) {

    }

    public void addAuthenticationEventContext(IdPSession idpSession, AuthenticationEvent authnEvent, Subcontext subcontext) {

    }

    public void addServiceSessonContext(IdPSession idpSession, ServiceSession serviceSession, Subcontext subcontext) {

    }

    /** {@inheritDoc} */
    public Iterable<IdPSession> resolve(EvaluableCriterion<IdPSession> criteria) throws ResolverException {
        log.debug("Resolving sessions");

        if (criteria == null || sessionStore.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<IdPSession> sessions = sessionStore.values();
        ArrayList<IdPSession> matchedSessions = new ArrayList<IdPSession>();
        for (IdPSession session : sessions) {
            log.debug("Checking if session {} meets the selection criteria", session.getId());
            if (criteria.evaluate(session) == Boolean.TRUE) {
                log.debug("Session {} meets the selection criteria", session.getId());
                matchedSessions.add(session);
            } else {
                log.debug("Session {} did not meet the selection criteria", session.getId());
            }
        }

        return matchedSessions;
    }

    /** {@inheritDoc} */
    public IdPSession resolveSingle(EvaluableCriterion<IdPSession> criteria) throws ResolverException {
        log.debug("Resolving session");

        if (criteria == null || sessionStore.isEmpty()) {
            return null;
        }

        Collection<IdPSession> sessions = sessionStore.values();
        for (IdPSession session : sessions) {
            log.debug("Checking if session {} meets the selection criteria", session.getId());
            if (criteria.evaluate(session) == Boolean.TRUE) {
                log.debug("Session {} meets the selection criteria", session.getId());
                return session;
            } else {
                log.debug("Session {} did not meet the selection criteria", session.getId());
            }
        }

        log.debug("No session meets the selection criteria");
        return null;
    }
}
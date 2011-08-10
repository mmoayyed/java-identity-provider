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

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.UninitializedComponentException;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.opensaml.util.resolver.ResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vt.middleware.crypt.util.HexConverter;

/**
 * Component that creates, manages and locates session.
 * 
 * All modifications made to a session result in the session being stored back in to the session (i.e.,
 * {@link Map#put(Object, Object)} is called). This allows session stores which replicate information to know that the
 * session has been updated and information needs to be pushed out.
 */
public class BasicSessionManager implements SessionResolver, SessionManager, InitializableComponent,
        UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BasicSessionManager.class);

    /** Number of random bits within a session ID. Default value: {@value} */
    private final int sessionIDSize = 32;

    /** A {@link SecureRandom} PRNG to generate session IDs. */
    private final SecureRandom prng;

    /** Converts byte to hex and vice versa. */
    private final HexConverter hexCodec;

    /** Whether this component has been initialized. */
    private boolean initialized;

    /** The unique identifier for this session manager. */
    private String id;

    /**
     * Maximum amount of time, in milliseconds, for which a session is valid. A value of 0 or less indicates that the
     * session does not have an absolute timeout. Default value: {@value}
     */
    private long sessionLifetime = 1000 * 60 * 60 * 8;

    /**
     * Maximum amount of time, in millisecond, that a session may be "inactive" before being considered expired. A value
     * of 0 or less indicates that the session never times out due to inactivity. Default value: {@value}
     */
    private long sessionInactivityTimeout = 1000 * 60 * 30;

    /** Session storage keyed by session ID. */
    private Map<String, IdPSession> sessionStore;

    /** Constructor. */
    public BasicSessionManager() {
        prng = new SecureRandom();
        hexCodec = new HexConverter();
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this session manager.
     * 
     * This property can not be changed after this session manager has been initialized.
     * 
     * @param managerId unique identifier for this session manager, can not be null or empty
     */
    public synchronized void setId(String managerId) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Session manager has already been initialized");
        }

        id = StringSupport.trimOrNull(managerId);
    }

    /**
     * Gets the maximum amount of time, in milliseconds, for which a session is valid. A value of 0 or less indicates
     * that the session does not have an absolute timeout.
     * 
     * @return maximum amount of time, in milliseconds, for which a session is valid
     */
    public long getSessionLifetime() {
        return sessionLifetime;
    }

    /**
     * Sets the maximum amount of time, in milliseconds, for which a session is valid. A value of 0 or less indicates
     * that the session does not have an absolute timeout.
     * 
     * This property may be changed after this session manager has been initialized. However, changes to this property
     * only affect newly created session, not any existing sessions.
     * 
     * @param lifetime maximum amount of time, in milliseconds, for which a session is valid
     */
    public void setSessionLifetime(long lifetime) {
        sessionLifetime = lifetime;
    }

    /**
     * Gets the maximum amount of time, in millisecond, that a session may be "inactive" before being considered
     * expired. A value of 0 or less indicates that the session never times out due to inactivity.
     * 
     * @return maximum amount of time, in millisecond, that a session may be "inactive" before being considered expired
     */
    public long getSessionInactivityTimeout() {
        return sessionInactivityTimeout;
    }

    /**
     * Sets the maximum amount of time, in millisecond, that a session may be "inactive" before being considered
     * expired. A value of 0 or less indicates that the session never times out due to inactivity.
     * 
     * This property may be changed after this session manager has been initialized. However, changes to this property
     * only affect newly created session, not any existing sessions.
     * 
     * @param timeout maximum amount of time, in millisecond, that a session may be "inactive" before being considered
     *            expired
     */
    public void setSessionInactivityTimeout(long timeout) {
        sessionInactivityTimeout = timeout;
    }

    /**
     * Gets the unmodifiable store of sessions.
     * 
     * @return unmodifiable store of sessions
     */
    public Map<String, IdPSession> getSessionStore() {
        return Collections.unmodifiableMap(sessionStore);
    }

    /**
     * Sets the store used for sessions.
     * 
     * This property can not be changed after this session manager has been initialized.
     * 
     * @param store store used for sessions
     */
    public synchronized void setSessionStore(Map<String, IdPSession> store) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Session manager has already been initialized");
        }

        sessionStore = store;
    }

    /** {@inheritDoc} */
    public Iterable<IdPSession> getSessions() {
        return sessionStore.values();
    }

    /** {@inheritDoc} */
    public IdPSession createSession() {
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
        session.setInactivityTimeout(sessionInactivityTimeout);
        session.setSecret(sessionSecret);
        sessionStore.put(sessionId, session);

        return session;
    }

    /** {@inheritDoc} */
    public void removeSession(IdPSession idpSession) {
        sessionStore.remove(idpSession.getId());
    }

    /** {@inheritDoc} */
    public void updateSessionLastActivityInstant(IdPSession idpSession) {
        checkIdPSession(idpSession);

        idpSession.setActivityInstantToNow();
        sessionStore.put(idpSession.getId(), idpSession);
    }

    /** {@inheritDoc} */
    public void addAuthenticationEvent(IdPSession idpSession, String serviceId, AuthenticationEvent event) {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Session manager has not been initialized");
        }

        checkIdPSession(idpSession);

        String trimmedServiceId = StringSupport.trimOrNull(serviceId);
        Assert.isNotNull(trimmedServiceId, "Service ID can not be null or empty");

        Assert.isNotNull(event, "Authentication event can not be null or empty");

        ServiceSession serviceSesson = idpSession.getServiceSession(trimmedServiceId);
        if (serviceSesson == null) {
            serviceSesson = new ServiceSession();
            serviceSesson.setServiceId(trimmedServiceId);
            // TODO add service session to IdP session
        }

        // TODO
    }

    /** {@inheritDoc} */
    public void addIdPSessionContext(IdPSession idpSession, Subcontext subcontext) {
        if (subcontext == null) {
            return;
        }

        if (!isInitialized()) {
            throw new UninitializedComponentException("Session manager has not be initialized");
        }

        checkIdPSession(idpSession);
        // TODO
    }

    /** {@inheritDoc} */
    public void addAuthenticationEventContext(IdPSession idpSession, AuthenticationEvent event, Subcontext subcontext) {
        if (subcontext == null) {
            return;
        }

        if (!isInitialized()) {
            throw new UninitializedComponentException("Session manager has not be initialized");
        }

        checkIdPSession(idpSession);

        Assert.isNotNull(event, "Authentication event can not be null or empty");

        // TODO
    }

    /** {@inheritDoc} */
    public void addServiceSessonContext(IdPSession idpSession, ServiceSession serviceSession, Subcontext subcontext) {
        if (subcontext == null) {
            return;
        }

        if (!isInitialized()) {
            throw new UninitializedComponentException("Session manager has not be initialized");
        }

        checkIdPSession(idpSession);

        Assert.isNotNull(serviceSession, "Service session can not be null or empty");

        // TODO
    }

    /** {@inheritDoc} */
    public Iterable<IdPSession> resolve(EvaluableCriterion<IdPSession> criteria) throws ResolverException {
        log.debug("Resolving sessions");

        if (criteria == null || sessionStore.isEmpty()) {
            return Collections.emptyList();
        }

        if (!isInitialized()) {
            throw new UninitializedComponentException("Session manager has not be initialized");
        }

        Collection<IdPSession> sessions = sessionStore.values();
        ArrayList<IdPSession> matchedSessions = new ArrayList<IdPSession>();
        try {
            for (IdPSession session : sessions) {
                log.debug("Checking if session {} meets the selection criteria", session.getId());
                if (criteria.evaluate(session) == Boolean.TRUE) {
                    log.debug("Session {} meets the selection criteria", session.getId());
                    matchedSessions.add(session);
                } else {
                    log.debug("Session {} did not meet the selection criteria", session.getId());
                }
            }
        } catch (EvaluationException e) {
            throw new ResolverException("Error evaluating session resolution criteria", e);
        }

        return matchedSessions;
    }

    /** {@inheritDoc} */
    public IdPSession resolveSingle(EvaluableCriterion<IdPSession> criteria) throws ResolverException {
        log.debug("Resolving session");

        if (criteria == null || sessionStore.isEmpty()) {
            return null;
        }

        if (!isInitialized()) {
            throw new UninitializedComponentException("Session manager has not be initialized");
        }

        Collection<IdPSession> sessions = sessionStore.values();
        try {
            for (IdPSession session : sessions) {
                log.debug("Checking if session {} meets the selection criteria", session.getId());
                if (criteria.evaluate(session) == Boolean.TRUE) {
                    log.debug("Session {} meets the selection criteria", session.getId());
                    return session;
                } else {
                    log.debug("Session {} did not meet the selection criteria", session.getId());
                }
            }
        } catch (EvaluationException e) {
            throw new ResolverException("Error evaluating session resolution criteria", e);
        }

        log.debug("No session meets the selection criteria");
        return null;
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return initialized;
    }

    /** {@inheritDoc} */
    public void initialize() throws ComponentInitializationException {
        if (initialized) {
            return;
        }

        if (id == null) {
            throw new ComponentInitializationException("Session manager ID can not be null");
        }

        if (sessionStore == null) {
            throw new ComponentInitializationException("Session store can not be null");
        }

        initialized = true;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        // nothing to do here
    }

    /**
     * Checks that the given session is not null and is owned by this session manager.
     * 
     * @param idpSession the session to check
     */
    private void checkIdPSession(IdPSession idpSession) {
        Assert.isNotNull(idpSession, "Session can not be null");

        IdPSession session = sessionStore.get(idpSession.getId());
        Assert.isTrue(ObjectSupport.equals(idpSession, session), "Given session is not owned by this session manager");
    }
}
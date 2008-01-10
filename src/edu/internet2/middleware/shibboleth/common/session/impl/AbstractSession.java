
package edu.internet2.middleware.shibboleth.common.session.impl;

import org.joda.time.DateTime;

import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Base class for Shibboleth sessions.
 */
public abstract class AbstractSession implements Session {

    /** The session ID. */
    private final String sessionID;

    /** The principal ID. */
    private String principalID;

    /** Session inactivity timeout in milliseconds. */
    private long inactivityTimeout;

    /** The last activity time of the user. */
    private DateTime lastActivity;

    /**
     * Default constructor.
     * 
     * @param id ID of the session
     * @param principal principal ID of the user
     * @param timeout inactivity timeout for the session in milliseconds
     */
    public AbstractSession(String id, String principal, long timeout) {
        sessionID = id;
        principalID = principal;
        inactivityTimeout = timeout;
        lastActivity = new DateTime();
    }

    /** {@inheritDoc} */
    public String getSessionID() {
        return sessionID;
    }

    /** {@inheritDoc} */
    public String getPrincipalName() {
        return principalID;
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
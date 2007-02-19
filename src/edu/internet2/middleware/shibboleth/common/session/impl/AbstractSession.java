
package edu.internet2.middleware.shibboleth.common.session.impl;

import java.net.InetAddress;
import java.security.SecureRandom;

import edu.internet2.middleware.shibboleth.common.session.Session;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;

/**
 * Base class for Shibboleth sessions.
 */
public abstract class AbstractSession implements Session {

    /** The size of the session ID, in bytes. */
    private static final int SESSION_SIZE = 32;

    /** A {@link SecureRandom} PRNG to generate session IDs. */
    private static SecureRandom prng = new SecureRandom();

    /** The session ID. */
    private final String sessionID;
    
    /** IP address of the presenter. */
    private InetAddress presenterAddress;

    /** The principal ID. */
    private String principalID;

    /** The last activity time of the user. */
    private DateTime lastActivity;

    /**
     * Default constructor.
     * 
     * @param presenter IP address of the presenter
     * @param principal principal ID of the user
     */
    public AbstractSession(InetAddress presenter, String principal) {

        // generate a random session ID
        byte[] sid = new byte[SESSION_SIZE];
        prng.nextBytes(sid);
        sessionID = new String(Base64.encodeBase64(sid));

        principalID = principal;
        lastActivity = new DateTime();
    }

    /** {@inheritDoc} */
    public String getSessionID() {
        return sessionID;
    }
    
    /** {@inheritDoc} */
    public InetAddress getPresenterAddress() {
        return presenterAddress;
    }

    /** {@inheritDoc} */
    public String getPrincipalID() {
        return principalID;
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
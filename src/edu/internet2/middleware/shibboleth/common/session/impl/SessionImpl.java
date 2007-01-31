
package edu.internet2.middleware.shibboleth.common.session.impl;

import java.security.SecureRandom;

import edu.internet2.middleware.shibboleth.common.session.Session;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;

public class SessionImpl implements Session {

   /** The size of the session ID, in bytes */
    private static final int SESSION_SIZE = 32; 

    /** A {@link SecureRandom} PRNG to generate session IDs */
    private static SecureRandom prng = new SecureRandom();

    /** The session ID */
    private final String sessionID;
    
    /** The principal ID */
    private String principalID;
    
    /** The last activity time of the user */
    private DateTime lastActivity;
    
    
    /**
     * Default constructor
     *
     * @param principalID principal ID of the user
     */
    public SessionImpl(String principalID) {
        
       	// generate a random session ID
    	byte[] sid = new byte[SESSION_SIZE];
    	prng.nextBytes(sid);
    	this.sessionID = new String(Base64.encodeBase64(sid));
    
        this.principalID = principalID;
    	this.lastActivity = new DateTime();
    }
    
    
    /** {@inheritDoc} */
    public String getSessionID() {
    	return this.sessionID;
    }


    /** {@inheritDoc} */
    public String getPrincipalID() {
    	return this.principalID;
    }


    /** {@inheritDoc} */
    public DateTime getLastActivityInstant() {
    	return this.lastActivity;
    }
}
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

package net.shibboleth.idp.saml.impl.profile;

import javax.annotation.concurrent.NotThreadSafe;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.Type4UuidIdentifierGenerationStrategy;

import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.AbstractHttpServletRequestMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes an incoming Shibboleth Authentication Request message.
 * 
 * @param <RequestType> type of decoded message
 */
@NotThreadSafe
public abstract class BaseIdpInitiatedSsoRequestMessageDecoder<RequestType> extends
        AbstractHttpServletRequestMessageDecoder<RequestType> {
    
    /**
     * Deprecated name of the query parameter carrying the service provider entity ID: {@value} . Use of
     * {@link #ENTITY_ID_PARAM} is preferred.
     */
    public static final String PROVIDER_ID_PARAM = "providerId";

    /** Name of the query parameter carrying the service provider entity ID: {@value} . */
    public static final String ENTITY_ID_PARAM = "entityId";

    /**
     * Deprecated name of the query parameter carrying the service provider's assertion consumer service URL: {@value} .
     * Use of {@link #ACS_URL_PARAM} is preferred.
     */
    public static final String SHIRE_PARAM = "shire";

    /** Name of the query parameter carrying the service provider's assertion consumer service URL: {@value} . */
    public static final String ACS_URL_PARAM = "acs";

    /** Deprecated name of the query parameter carrying the service provider's target information: {@value} . */
    public static final String TARGET_PARAM = "target";

    /** Name of the query parameter carrying the service provider's relay state information: {@value} . */
    public static final String RELAY_STATE_PARAM = "relayState";

    /** Name of the query parameter carrying the current time at the service provider: {@value. } */
    public static final String TIME_PARAM = "time";
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseIdpInitiatedSsoRequestMessageDecoder.class);
    
    /** Used to log protocol messages. */
    private final Logger protocolMessageLog = LoggerFactory.getLogger("PROTOCOL_MESSAGE");
    
    /** ID generator. */
    private final IdentifierGenerationStrategy idGenerator = new Type4UuidIdentifierGenerationStrategy();
    
    /** {@inheritDoc} */
    public void decode() throws MessageDecodingException {
        log.debug("Beginning to decode message from HttpServletRequest");
        
        super.decode();
        
        logDecodedMessage();

        log.debug("Successfully decoded message from HttpServletRequest.");
    }
    
    /**
     * Build a new IdP-initiated request structure from the inbound HTTP request.
     * 
     * @return the new SSO request structure
     * @throws MessageDecodingException if the request doesn't contain an entityID
     */
    protected IdpInitatedSsoRequest buildIdpInitiatedSsoRequest() throws MessageDecodingException {
        final HttpServletRequest request = getHttpServletRequest();
        return new IdpInitatedSsoRequest(getEntityId(request), getAcsUrl(request),
                        getTarget(request), getTime(request)); 
    }

    /**
     * Gets the entity ID of the service provider.
     * 
     * @param request current HTTP request
     * 
     * @return the entity ID of the service provider
     * 
     * @throws MessageDecodingException thrown if the request does not contain a service provider entity ID
     */
    protected String getEntityId(HttpServletRequest request) throws MessageDecodingException {
        String entityId = StringSupport.trimOrNull(request.getParameter(ENTITY_ID_PARAM));
        if (entityId == null) {
            entityId = StringSupport.trimOrNull(request.getParameter(PROVIDER_ID_PARAM));
        }

        if (entityId == null) {
            throw new MessageDecodingException("Shibboleth Authentication Request message did not contain either the "
                    + ENTITY_ID_PARAM + " (preferred) or " + PROVIDER_ID_PARAM + " query parameter.");
        }
        return entityId;
    }

    /**
     * Gets the assertion consumer service URL for the service provider.
     * 
     * @param request current HTTP request
     * 
     * @return the assertion consumer service URL, may be null if none is given in the request
     */
    protected String getAcsUrl(HttpServletRequest request) {
        String acsUrl = StringSupport.trimOrNull(request.getParameter(ACS_URL_PARAM));
        if (acsUrl == null) {
            acsUrl = StringSupport.trimOrNull(request.getParameter(SHIRE_PARAM));
        }

        return acsUrl;
    }

    /**
     * Gets the opaque relay state sent by the service provider.
     * 
     * @param request current HTTP request
     * 
     * @return the relay state, or null if the service provider did not send one
     */
    protected String getTarget(HttpServletRequest request) {
        String relayState = StringSupport.trimOrNull(request.getParameter(RELAY_STATE_PARAM));
        if (relayState == null) {
            relayState = StringSupport.trimOrNull(request.getParameter(TARGET_PARAM));
        }

        return relayState;
    }

    /**
     * Gets the current time, in milliseconds since the epoch, at the service provider, if the service provider sent it.
     * 
     * @param request current HTTP request
     * 
     * @return the time sent by the service provider or 0 if not time was provided
     * 
     * @throws MessageDecodingException thrown if the time parameter given by the service provider is non-numeric or a
     *             negative time
     */
    protected long getTime(HttpServletRequest request) throws MessageDecodingException {
        String timeString = StringSupport.trimOrNull(request.getParameter(TIME_PARAM));
        if (timeString == null) {
            return 0;
        }

        try {
            long time = Long.parseLong(timeString);
            if (time < 0) {
                throw new MessageDecodingException("Shibboleth Authentication Request contained a negative time value");
            }
            return time * 1000;
        } catch (NumberFormatException e) {
            throw new MessageDecodingException("Shibboleth Authentication Request contained a non-numeric time value");
        }
    }
    
    /**
     * Log the decoded message to the protocol message logger.
     */
    protected void logDecodedMessage() {
        if (protocolMessageLog.isDebugEnabled() ){
            String message = getMessageToLog();
            if (message == null) {
                log.warn("Decoded message was null, nothing to log");
                return;
            }
            
            protocolMessageLog.debug("\n" + message);
        }
    }
    
    /**
     * Construct a message ID for the request.
     * 
     * @return the message ID to use
     */
    protected String getMessageID() {
        HttpServletRequest request = getHttpServletRequest();
        String timeString = StringSupport.trimOrNull(request.getParameter(TIME_PARAM));
        
        // If both a timestamp and session ID are available, construct a pseudo message ID 
        // by combining them. Otherwise return a generated one.
        if (timeString != null) {
            String sessionID = request.getRequestedSessionId();
            if (sessionID != null) {
                return "_" + sessionID + '!' + timeString;
            } else {
                return idGenerator.generateIdentifier();
            }
        } else {
            return idGenerator.generateIdentifier();
        }
    }
    
    /**
     * Get the string representation of what will be logged as the protocol message.
     * 
     * @return the string representing the protocol message for logging purposes
     */
    protected abstract String getMessageToLog();
    
}
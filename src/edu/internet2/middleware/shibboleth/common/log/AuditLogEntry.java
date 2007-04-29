/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.log;

import java.util.List;

import org.joda.time.DateTime;

/**
 * Represents an auditable event in the system.
 */
public class AuditLogEntry {
    
    /** Name of the Logger for the shibboleth audit log. */
    public static final String AUDIT_LOGGER_NAME = "Shibboleth-Audit";

    /** UTC IS8601 timestamp of the audit event. */
    private DateTime auditEventTime;
    
    /** Entity ID of the relying party. */
    private String relyingParty;
    
    /** URI of binding used by the relying party. */
    private String requestBinding;
    
    /** URI of binding used to respond to relying party. */
    private String responseBinding;
    
    /** URI of profile in use. */
    private String messageProfile;
    
    /** Unique ID of the request message. */
    private String requestId;
    
    /** Principal ID of the user the request was made about. */
    private String principalId;
    
    /** URIs of the authentication methods currently active for the user. */
    private List<String> principalAuthenticationMethods;
    
    /** Internal ID of the user attributes released. */
    private List<String> releasedAttributes;
    
    /** Constructor. */
    public AuditLogEntry(){
        auditEventTime = new DateTime();
    }

    /**
     * Gets the URI of the message profile being used.
     * 
     * @return URI of the message profile being used
     */
    public String getMessageProfile() {
        return messageProfile;
    }

    /**
     * Sets the URI of the message profile being used.
     * 
     * @param profileURI URI of the message profile being used
     */
    public void setMessageProfile(String profileURI) {
        messageProfile = profileURI;
    }

    /**
     * Gets the authentication methods, identified by their URI, currently active for the user.
     * 
     * @return  authentication methods, identified by their URI, currently active for the user
     */
    public List<String> getPrincipalAuthenticationMethods() {
        return principalAuthenticationMethods;
    }

    /**
     * Sets the authentication methods, identified by their URI, currently active for the user.
     * 
     * @param methods authentication methods, identified by their URI, currently active for the user
     */
    public void setPrincipalAuthenticationMethods(List<String> methods) {
        principalAuthenticationMethods = methods;
    }

    /**
     * Gets the principal ID of the user.
     * 
     * @return principal ID of the user
     */
    public String getPrincipalId() {
        return principalId;
    }

    /**
     * Sets the principal ID of the user.
     * 
     * @param id principal ID of the user
     */
    public void setPrincipalId(String id) {
        principalId = id;
    }

    /**
     * Gets the list of internal IDs of the attributes that were released.
     * 
     * @return internal IDs of the attributes that were released
     */
    public List<String> getReleasedAttributes() {
        return releasedAttributes;
    }

    /**
     * Sets the internal IDs of the attributes that were released.
     * 
     * @param attributes internal IDs of the attributes that were released
     */
    public void setReleasedAttributes(List<String> attributes) {
        releasedAttributes = attributes;
    }

    /**
     * Gets the entity ID of the relying party.
     * 
     * @return  entity ID of the relying party
     */
    public String getRelyingParty() {
        return relyingParty;
    }

    /**
     * Sets the entity ID of the relying party.
     * 
     * @param entityId entity ID of the relying party
     */
    public void setRelyingParty(String entityId) {
        relyingParty = entityId;
    }

    /**
     * Gets the URI of the binding used during the request.
     * 
     * @return URI of the binding used during the request
     */
    public String getRequestBinding() {
        return requestBinding;
    }

    /**
     * Sets the URI of the binding used during the request.
     * 
     * @param bindingURI URI of the binding used during the request
     */
    public void setRequestBinding(String bindingURI) {
        requestBinding = bindingURI;
    }

    /**
     * Gets the unique ID of the request.
     * 
     * @return unique ID of the request
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the unique ID of the request.
     * 
     * @param id unique ID of the request
     */
    public void setRequestId(String id) {
        requestId = id;
    }

    /**
     * Gets the URI of the binding used during the response.
     * 
     * @return URI of the binding used during the response
     */
    public String getResponseBinding() {
        return responseBinding;
    }

    /**
     * Sets the URI of the binding used during the response.
     * 
     * @param bindingURI URI of the binding used during the response
     */
    public void setResponseBinding(String bindingURI) {
        responseBinding = bindingURI;
    }

    /**
     * Gets the timestamp for this audit event.
     * 
     * @return timestamp for this audit event
     */
    public DateTime getAuditEventTime() {
        return auditEventTime;
    }
}
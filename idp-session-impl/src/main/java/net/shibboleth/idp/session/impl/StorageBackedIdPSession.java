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

package net.shibboleth.idp.session.impl;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.session.BaseIdPSession;
import net.shibboleth.idp.session.ServiceSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Implementation of {@link IdPSession} for use with {@link StorageBackedSessionManager}.
 */
public class StorageBackedIdPSession extends BaseIdPSession {
    
    /** Back-reference to parent instance. */
    @Nonnull private final StorageBackedSessionManager sessionManager;
    
    /** Storage version used to synchronize changes. */
    private int version;
    
    /** Collection of flow IDs representing current session state. */
    @Nonnull @NonnullElements private Set<String> flowIds;

    /** Collection of service IDs representing current session state. */
    @Nonnull @NonnullElements private Set<String> serviceIds;
    
    /**
     * Constructor.
     *
     * @param manager parent SessionManager instance
     * @param sessionId unique ID of session
     * @param canonicalName canonical name of session subject
     * @param creationTime creation time of session in milliseconds
     */
    public StorageBackedIdPSession(@Nonnull final StorageBackedSessionManager manager,
            @Nonnull @NotEmpty final String sessionId, @Nonnull @NotEmpty final String canonicalName,
            final long creationTime) {
        super(sessionId, canonicalName, creationTime);
        
        sessionManager = Constraint.isNotNull(manager, "SessionManager cannot be null");
        
        version = 1;
        flowIds = new HashSet(5);
        serviceIds = new HashSet(10);
    }
    
    /** {@inheritDoc} */
    public void setLastActivityInstant(long instant) throws SessionException {
        // TODO Auto-generated method stub
        super.setLastActivityInstant(instant);
    }

    /** {@inheritDoc} */
    public void addAuthenticationResult(AuthenticationResult result) throws SessionException {
        // TODO Auto-generated method stub
        super.addAuthenticationResult(result);
    }

    /** {@inheritDoc} */
    public boolean removeAuthenticationResult(AuthenticationResult result) throws SessionException {
        // TODO Auto-generated method stub
        return super.removeAuthenticationResult(result);
    }

    /** {@inheritDoc} */
    public void addServiceSession(ServiceSession serviceSession) throws SessionException {
        // TODO Auto-generated method stub
        super.addServiceSession(serviceSession);
    }

    /** {@inheritDoc} */
    public boolean removeServiceSession(ServiceSession serviceSession) throws SessionException {
        // TODO Auto-generated method stub
        return super.removeServiceSession(serviceSession);
    }
    
    /** {@inheritDoc} */
    public boolean checkTimeout() throws SessionException {
        if (getLastActivityInstant() + sessionManager.getSessionTimeout() > System.currentTimeMillis()) {
            return super.checkTimeout();
        } else {
            return false;
        }
    }

    /**
     * Get the record version.
     * 
     * @return current version of the storage record
     */
    protected int getVersion() {
        return version;
    }
    
    /**
     * Set the record version.
     * 
     * @param ver version to set
     */
    protected void setVersion(final int ver) {
        version = ver;
    }
    
    /**
     * Get the set of authentication flow IDs associated with the session.
     * 
     * @return live set of flow IDs
     */
    @Nonnull @NonnullElements @Live protected Set<String> getAuthenticationFlowIds() {
        return flowIds;
    }

    /**
     * Get the set of service IDs associated with the session.
     * 
     * @return live set of service IDs
     */
    @Nonnull @NonnullElements @Live protected Set<String> getServiceIds() {
        return serviceIds;
    }

}
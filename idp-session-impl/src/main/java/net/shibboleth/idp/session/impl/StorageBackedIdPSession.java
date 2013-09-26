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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.session.AbstractIdPSession;
import net.shibboleth.idp.session.ServiceSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Implementation of {@link IdPSession} for use with {@link StorageBackedSessionManager}.
 */
public class StorageBackedIdPSession extends AbstractIdPSession {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageBackedIdPSession.class);
    
    /** Back-reference to parent instance. */
    @Nonnull private final StorageBackedSessionManager sessionManager;
    
    /** Storage version used to synchronize changes. */
    private int version;
    
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
    }
    
    /** {@inheritDoc} */
    public void setLastActivityInstant(@Duration @Positive final long instant) throws SessionException {
        // TODO Auto-generated method stub
        super.setLastActivityInstant(instant);
    }

    /** {@inheritDoc} */
    public void bindToAddress(@Nonnull @NotEmpty final String address) throws SessionException {
        // TODO Auto-generated method stub
        super.bindToAddress(address);
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<AuthenticationResult> getAuthenticationResults() {
        
        // Check for any sparse/null values in the map, which need to be loaded before returning a complete set.
        for (Map.Entry<String, Optional<AuthenticationResult>> entry : getAuthenticationResultMap().entrySet()) {
            if (!entry.getValue().isPresent()) {
                AuthenticationResult result = loadAuthenticationResultFromStorage(entry.getKey());
                entry.setValue(Optional.of(result));
            }
        }
        
        return super.getAuthenticationResults();
    }

    /** {@inheritDoc} */
    @Nullable public AuthenticationResult getAuthenticationResult(@Nonnull @NotEmpty final String flowId) {
        // Check existing map.
        AuthenticationResult result = super.getAuthenticationResult(flowId);
        if (result != null) {
            return result;
        }
        
        // See if such an ID is purported to exist.
        final String trimmed = StringSupport.trimOrNull(flowId);
        if (!getAuthenticationResultMap().containsKey(trimmed)) {
            return null;
        }
        
        // Load and add to map.
        result = loadAuthenticationResultFromStorage(trimmed);
        if (result != null) {
            doAddAuthenticationResult(result);
        }
        return result;
    }
    
    /** {@inheritDoc} */
    public void addAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException {
        // TODO Auto-generated method stub
        super.addAuthenticationResult(result);
    }

    /** {@inheritDoc} */
    public boolean removeAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException {
        // TODO Auto-generated method stub
        return super.removeAuthenticationResult(result);
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<ServiceSession> getServiceSessions() {
        
        if (sessionManager.isTrackServiceSessions()) {
            // Check for any sparse/null values in the map, which need to be loaded before returning a complete set.
            for (Map.Entry<String, Optional<ServiceSession>> entry : getServiceSessionMap().entrySet()) {
                if (!entry.getValue().isPresent()) {
                    ServiceSession result = loadServiceSessionFromStorage(entry.getKey());
                    entry.setValue(Optional.of(result));
                }
            }
        } else {
            log.warn("Request for ServiceSessions will return nothing, ServiceManager is not tracking them");
        }
        
        return super.getServiceSessions();
    }

    /** {@inheritDoc} */
    @Nullable public ServiceSession getServiceSession(@Nonnull @NotEmpty final String serviceId) {
        if (sessionManager.isTrackServiceSessions()) {
            // Check existing map.
            ServiceSession result = super.getServiceSession(serviceId);
            if (result != null) {
                return result;
            }
            
            // See if such an ID is purported to exist.
            final String trimmed = StringSupport.trimOrNull(serviceId);
            if (!getServiceSessionMap().containsKey(trimmed)) {
                return null;
            }
            
            // Load and add to map.
            result = loadServiceSessionFromStorage(trimmed);
            if (result != null) {
                doAddServiceSession(result);
            }
            return result;
        } else {
            log.warn("Request for ServiceSession will return nothing, ServiceManager is not tracking them");
            return null;
        }
    }
    
    /** {@inheritDoc} */
    public void addServiceSession(@Nonnull final ServiceSession serviceSession) throws SessionException {
        // TODO Auto-generated method stub
        super.addServiceSession(serviceSession);
    }

    /** {@inheritDoc} */
    public boolean removeServiceSession(@Nonnull final ServiceSession serviceSession) throws SessionException {
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

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Live public Map<String, Optional<AuthenticationResult>> getAuthenticationResultMap() {
        return super.getAuthenticationResultMap();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Live public Map<String, Optional<ServiceSession>> getServiceSessionMap() {
        return super.getServiceSessionMap();
    }
    
    /**
     * Loads an {@link AuthenticationResult} record from storage and deserializes it using the object
     * attached to the corresponding {@link AuthenticationFlowDescriptor}.
     * 
     * @param flowId ID of result/flow to load
     * @return the stored result, or null
     */
    @Nullable private AuthenticationResult loadAuthenticationResultFromStorage(@Nonnull @NotEmpty final String flowId) {
        log.debug("Loading AuthenticationResult for flow {} in session {}", flowId, getId());
        
        AuthenticationFlowDescriptor flow = sessionManager.getAuthenticationFlowDescriptor(flowId);
        if (flow == null) {
            log.warn("No flow descriptor installed for ID {}, unable to load result from storage", flowId);
            return null;
        } else if (flow.getResultSerializer() == null) {
            log.warn("No serializer installed for flow ID {}, unable to load result from storage", flowId);
            return null;
        }
        
        try {
            final StorageRecord<AuthenticationResult> record =
                    sessionManager.getStorageService().read(getId(), flowId);
            return record.getValue(flow.getResultSerializer(), getId(), flowId);
        } catch (IOException e) {
            log.error("Exception loading AuthenticationResult for flow " + flowId + " from storage", e);
        }
        
        return null;
    }

    /**
     * Loads a {@link ServiceSession} record from storage and deserializes it using the object
     * registered in the attached {@link ServiceSessionSerializerRegistry}.
     * 
     * @param serviceId ID of service for session to load
     * @return the stored session, or null
     */
    @Nullable private ServiceSession loadServiceSessionFromStorage(@Nonnull @NotEmpty final String serviceId) {
        log.debug("Loading ServiceSession for service {} in session {}", serviceId, getId());

        final String key;
        if (serviceId.length() > sessionManager.getStorageService().getCapabilities().getKeySize()) {
            key = DigestUtils.sha256Hex(serviceId);
        } else {
            key = serviceId;
        }
        
        try {
            final StorageRecord<ServiceSession> record = sessionManager.getStorageService().read(getId(), key);
            
            // Parse out the class type.
            int pos = record.getValue().indexOf(':');
            if (pos == -1) {
                throw new IOException("No class type found prefixed to record");
            }
            
            final String sessionClassName = record.getValue().substring(0,  pos);
            
            // Look up the serializer instance for that class type.
            StorageSerializer<? extends ServiceSession> serviceSessionSerializer =
                    sessionManager.getServiceSessionSerializerRegistry().lookup(
                            Class.forName(sessionClassName).asSubclass(ServiceSession.class));
            if (serviceSessionSerializer == null) {
                throw new ClassNotFoundException("No serializer registered for ServiceSession type "
                        + sessionClassName);
            }
            
            // Deserializer starting past the colon delimiter.
            return serviceSessionSerializer.deserialize(
                    record.getVersion(), getId(), key, record.getValue().substring(pos + 1), record.getExpiration());
            
        } catch (IOException | ClassNotFoundException e) {
            log.error("Exception loading ServiceSession for service " + serviceId + " from storage", e);
        }
        
        return null;
    }
}
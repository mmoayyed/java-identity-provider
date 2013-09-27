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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.VersionMismatchException;
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
        
        long exp = instant + sessionManager.getSessionTimeout() + sessionManager.getSessionSlop();
        log.debug("Updating expiration of master record for session {} to {}", getId(), new DateTime(exp));
        
        try {
            sessionManager.getStorageService().updateExpiration(
                    getId(), StorageBackedSessionManager.SESSION_MASTER_KEY, exp);
            super.setLastActivityInstant(instant);
        } catch (IOException e) {
            log.error("Exception updating expiration of master record for session " + getId(), e);
            if (!sessionManager.isMaskStorageFailure()) {
                throw new SessionException("Exception updating expiration of session record", e);
            }
        }
    }

    /** {@inheritDoc} */
    public void bindToAddress(@Nonnull @NotEmpty final String address) throws SessionException {
        // Update ourselves and then attempt to write back.
        super.bindToAddress(address);
        try {
            int attempts = 10;
            boolean success = writeToStorage();
            while (!success && attempts-- > 0) {
                // The record may have changed underneath, so we need to re-check the address.
                String nowBound = getAddress(getAddressFamily(address));
                if (nowBound != null) {
                    // The same address type is now set, so recheck. No need to update storage regardless.
                    if (nowBound.equals(address)) {
                        return;
                    } else {
                        log.warn("Client address is {} but session {} already bound to {}", address, getId(), nowBound);
                        throw new SessionException("A different address of the same type was bound to the session");
                    }
                } else {
                    // We're still clear, so update ourselves again and try to write back.
                    super.bindToAddress(address);
                    success = writeToStorage();
                }
            }
            log.error("Exhausted retry attempts updating record for session {}", getId());
        } catch (IOException e) {
            log.error("Exception updating address binding of master record for session " + getId(), e);
            if (!sessionManager.isMaskStorageFailure()) {
                throw new SessionException("Exception updating address binding of session record", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<AuthenticationResult> getAuthenticationResults() {
        
        boolean dirty = false;

        // Check for any sparse/null values in the map, which need to be loaded before returning a complete set.
        Iterator<Map.Entry<String, Optional<AuthenticationResult>>> entries =
                getAuthenticationResultMap().entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Optional<AuthenticationResult>> entry = entries.next();
            if (!entry.getValue().isPresent()) {
                try {
                    AuthenticationResult result = loadAuthenticationResultFromStorage(entry.getKey());
                    if (result != null) {
                        entry.setValue(Optional.of(result));
                    } else {
                        // A null here means the reference to the record should be removed.
                        entries.remove();
                        dirty = true;
                    }
                } catch (IOException e) {
                    // An exception implies the record *might* still be accessible later.
                }
            }
        }
        
        if (dirty) {
            // TODO: update record
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
        try {
            result = loadAuthenticationResultFromStorage(trimmed);
            if (result != null) {
                doAddAuthenticationResult(result);
            } else {
                // A null here means the reference to the record should be removed.
                getAuthenticationResultMap().remove(trimmed);
                // TODO: update the record
            }
        } catch (IOException e) {
            // An exception implies the record *might* still be accessible later.
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
        
        boolean dirty = false;
        
        if (sessionManager.isTrackServiceSessions()) {
            // Check for any sparse/null values in the map, which need to be loaded before returning a complete set.
            Iterator<Map.Entry<String, Optional<ServiceSession>>> entries =
                    getServiceSessionMap().entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, Optional<ServiceSession>> entry = entries.next();
                if (!entry.getValue().isPresent()) {
                    try {
                        ServiceSession result = loadServiceSessionFromStorage(entry.getKey());
                        if (result != null) {
                            entry.setValue(Optional.of(result));
                        } else {
                            // A null here means the reference to the record should be removed.
                            entries.remove();
                            dirty = true;
                        }
                    } catch (IOException e) {
                        // An exception implies the record *might* still be accessible later.
                    }
                }
            }
            
            if (dirty) {
                // TODO: update record
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
            try {
                result = loadServiceSessionFromStorage(trimmed);
                if (result != null) {
                    doAddServiceSession(result);
                } else {
                    // A null here means the reference to the record should be removed.
                    getServiceSessionMap().remove(trimmed);
                    // TODO: update the record
                }
            } catch (IOException e) {
                // An exception implies the record *might* still be accessible later.
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
     * 
     * @return the stored result, or null if the record is missing or unusable
     * @throws IOException if a possibly transitory storage-related error occurs
     */
    @Nullable private AuthenticationResult loadAuthenticationResultFromStorage(@Nonnull @NotEmpty final String flowId)
            throws IOException {
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
            if (record != null) {
                return record.getValue(flow.getResultSerializer(), getId(), flowId);
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("Exception loading AuthenticationResult for flow " + flowId + " from storage", e);
            throw e;
        }
    }

    /**
     * Loads a {@link ServiceSession} record from storage and deserializes it using the object
     * registered in the attached {@link ServiceSessionSerializerRegistry}.
     * 
     * @param serviceId ID of service for session to load
     * 
     * @return the stored session, or null if the record is missing or unusable
     * @throws IOException if a possibly transitory storage-related error occurs
     */
    @Nullable private ServiceSession loadServiceSessionFromStorage(@Nonnull @NotEmpty final String serviceId)
            throws IOException {
        log.debug("Loading ServiceSession for service {} in session {}", serviceId, getId());

        final String key;
        if (serviceId.length() > sessionManager.getStorageService().getCapabilities().getKeySize()) {
            key = DigestUtils.sha256Hex(serviceId);
        } else {
            key = serviceId;
        }
        
        try {
            final StorageRecord<ServiceSession> record = sessionManager.getStorageService().read(getId(), key);
            if (record == null) {
                return null;
            }
            
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
                throw new IOException("No serializer registered for ServiceSession type " + sessionClassName);
            }
            
            // Deserializer starting past the colon delimiter.
            return serviceSessionSerializer.deserialize(
                    record.getVersion(), getId(), key, record.getValue().substring(pos + 1), record.getExpiration());
            
        } catch (IOException e) {
            log.error("Exception loading ServiceSession for service " + serviceId + " from storage", e);
            throw e;
        } catch (ClassNotFoundException e) {
            log.error("Exception loading ServiceSession for service " + serviceId + " from storage", e);
            throw new IOException(e);
        }
    }
    
    /**
     * Update the master session record based on the current contents of this object.
     * 
     * @return true iff the update succeeds, false iff a version mismatch resulted in overwrite of this object
     * @throws IOException if an error occurs trying to perform an update
     */
    private boolean writeToStorage() throws IOException {
        try {
            Integer ver = sessionManager.getStorageService().updateWithVersion(version, getId(),
                    StorageBackedSessionManager.SESSION_MASTER_KEY, this, sessionManager.getStorageSerializer(),
                    getLastActivityInstant() + sessionManager.getSessionTimeout() + sessionManager.getSessionSlop());
            if (ver == null) {
                log.error("Record for session {} has disappeared from backing store", getId());
                throw new IOException("Unable to update session, record disappeared");
            }
            version = ver;
            return true;
        } catch (VersionMismatchException e) {
            // The record has changed underneath. We need to deserialize the session back into the
            // same object by passing ourselves as the target object to a new serializer instance.
            StorageRecord<StorageBackedIdPSession> record =
                    sessionManager.getStorageService().read(getId(), StorageBackedSessionManager.SESSION_MASTER_KEY);
            if (record == null) {
                log.error("Record for session {} has disappeared from backing store", getId());
                throw new IOException("Unable to update session, record disappeared");
            }
            record.getValue(new StorageBackedIdPSessionSerializer(sessionManager, this),
                    getId(), StorageBackedSessionManager.SESSION_MASTER_KEY);
            return false;
        }
    }
}
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.session.AbstractIdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import org.apache.commons.codec.digest.DigestUtils;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.VersionMismatchException;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Implementation of {@link net.shibboleth.idp.session.IdPSession} for use with {@link StorageBackedSessionManager}.
 */
public class StorageBackedIdPSession extends AbstractIdPSession {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageBackedIdPSession.class);
    
    /** Back-reference to parent instance. */
    @Nonnull private final StorageBackedSessionManager sessionManager;
    
    /** Storage version used to synchronize changes. */
    private long version;
    
    /**
     * Constructor.
     *
     * @param manager parent SessionManager instance
     * @param sessionId unique ID of session
     * @param canonicalName canonical name of session subject
     * @param creationTime creation time of session
     */
    public StorageBackedIdPSession(@Nonnull final StorageBackedSessionManager manager,
            @Nonnull @NotEmpty final String sessionId, @Nonnull @NotEmpty final String canonicalName,
            @Nonnull final Instant creationTime) {
        super(sessionId, canonicalName, creationTime);
        
        sessionManager = Constraint.isNotNull(manager, "SessionManager cannot be null");
        
        version = 1;
    }
    
    /** {@inheritDoc} */
    @Override public void setLastActivityInstant(@Nonnull final Instant instant) 
            throws SessionException {
        
        final Instant exp =
                instant.plus(sessionManager.getSessionTimeout()).plus(sessionManager.getSessionSlop());
        log.debug("Updating expiration of primary record for session {} to {}", getId(), exp);
        
        try {
            sessionManager.getStorageService().updateExpiration(
                    getId(), StorageBackedSessionManager.SESSION_PRIMARY_KEY, exp.toEpochMilli());
            super.setLastActivityInstant(instant);
        } catch (final IOException e) {
            if (!sessionManager.isMaskStorageFailure()) {
                throw new SessionException("Exception updating expiration of session record", e);
            }
            log.error("Exception updating expiration of primary record for session {}", getId(), e);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean checkAddress(@Nonnull @NotEmpty final String address) throws SessionException {

        final AddressFamily family = getAddressFamily(address);
        final String bound = getAddress(family);
        if (bound != null) {
            if (!sessionManager.getConsistentAddressCondition().test(bound, address)) {
                log.warn("Client address {} invalid for session {} bound to {}", address, getId(), bound);
                return false;
            }
        } else {
            log.info("Session {} not yet bound to {} address, binding to {}", getId(), family, address);
            try {
                bindToAddress(address);
            } catch (final SessionException e) {
                log.error("Unable to bind session {} to address {}", getId(), address);
                return false;
            }
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void bindToAddress(@Nonnull @NotEmpty final String address) throws SessionException {
        
        // Update ourselves and then attempt to write back.
        super.bindToAddress(address);
        try {
            int attempts = 10;
            boolean success = writeToStorage();
            while (!success && attempts-- > 0) {
                // The record may have changed underneath, so we need to re-check the address.
                final String nowBound = getAddress(getAddressFamily(address));
                if (nowBound != null) {
                    // The same address type is now set, so recheck. No need to update storage regardless.
                    if (nowBound.equals(address)) {
                        return;
                    }
                    log.warn("Client address is {} but session {} already bound to {}", address, getId(), nowBound);
                    throw new SessionException("A different address of the same type was bound to the session");
                }
                // We're still clear, so update ourselves again and try to write back.
                super.bindToAddress(address);
                success = writeToStorage();
            }
            if (!success) {
                log.error("Exhausted retry attempts updating record for session {}", getId());
            }
        } catch (final IOException e) {
            if (!sessionManager.isMaskStorageFailure()) {
                throw new SessionException("Exception updating address binding of session record", e);
            }
            log.error("Exception updating address binding of primary record for session {}", getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotLive @Unmodifiable public Set<AuthenticationResult> getAuthenticationResults() {
        
        // Check for any sparse/null values in the map, which need to be loaded before returning a complete set.
        final Iterator<Map.Entry<String,Optional<AuthenticationResult>>> entries =
                getAuthenticationResultMap().entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry<String,Optional<AuthenticationResult>> entry = entries.next();
            if (entry.getValue().isEmpty()) {
                try {
                    final String key = entry.getKey();
                    assert key != null;
                    final AuthenticationResult result = loadAuthenticationResultFromStorage(key);
                    if (result != null) {
                        entry.setValue(Optional.of(result));
                    } else {
                        // A null here means the reference to the record should be removed.
                        entries.remove();
                    }
                } catch (final IOException e) {
                    // An exception implies the record *might* still be accessible later.
                }
            }
        }
        
        return super.getAuthenticationResults();
    }

    /** {@inheritDoc} */
    @Override
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
            result = loadAuthenticationResultFromStorage(Constraint.isNotNull(trimmed, "FlowID was empty"));
            if (result != null) {
                doAddAuthenticationResult(result);
            } else {
                // A null here means the reference to the record should be removed.
                getAuthenticationResultMap().remove(trimmed);
            }
        } catch (final IOException e) {
            // An exception implies the record *might* still be accessible later.
        }
        
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable public AuthenticationResult addAuthenticationResult(@Nonnull final AuthenticationResult result)
            throws SessionException {
        
        try {
            // Store the record.
            if (!saveAuthenticationResultToStorage(result) && !sessionManager.isMaskStorageFailure()) {
                throw new SessionException("Unable to save AuthenticationResult to storage");
            }
            final AuthenticationResult prev = super.addAuthenticationResult(result);
            if (prev == null) {
                // If no previous record, the add operation changed the primary record, requiring an update.
                int attempts = 10;
                boolean success = writeToStorage();
                while (!success && attempts-- > 0) {
                    // The record may have changed underneath, see if we need to reapply the add.
                    if (!getAuthenticationResultMap().containsKey(result.getAuthenticationFlowId())) {
                        super.addAuthenticationResult(result);
                        success = writeToStorage();
                    } else {
                        success = true;
                    }
                }
                if (!success) {
                    log.error("Exhausted retry attempts updating record for session {}", getId());
                }
            }
            return prev;
        } catch (final IOException e) {
            if (!sessionManager.isMaskStorageFailure()) {
                throw new SessionException("Exception saving AuthenticationResult record to storage", e);
            }
            log.error("Exception saving AuthenticationResult record for session {} and flow {}", getId(),
                    result.getAuthenticationFlowId(), e);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateAuthenticationResultActivity(@Nonnull final AuthenticationResult result) throws SessionException {
        final String flowId = result.getAuthenticationFlowId();
        final AuthenticationFlowDescriptor flow = sessionManager.getAuthenticationFlowDescriptor(flowId);
        if (flow != null) {
            try {
                if (!sessionManager.getStorageService().updateExpiration(getId(), result.getAuthenticationFlowId(),
                        result.getLastActivityInstant().plus(flow.getInactivityTimeout()).plus(
                            AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET).toEpochMilli())) {
                    log.warn("Skipping update, AuthenticationResult for flow {} in session {} not found in storage",
                            flowId, getId());
                }
            } catch (final IOException e) {
                if (!sessionManager.isMaskStorageFailure()) {
                    throw new SessionException("Exception updating AuthenticationResult expiration in storage", e);
                }
                log.error("Exception updating AuthenticationResult expiration for session {} and flow {}", getId(),
                        flowId, e);
            }
        } else {
            log.warn("No flow descriptor installed for ID {}, unable to update result in storage", flowId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException {
        if (super.removeAuthenticationResult(result)) {
            try {
                // Remove the separate record.
                sessionManager.getStorageService().delete(getId(), result.getAuthenticationFlowId());
            } catch (final IOException e) {
                if (!sessionManager.isMaskStorageFailure()) {
                    throw new SessionException("Exception removing AuthenticationResult record from storage", e);
                }
                log.error("Exception removing AuthenticationResult record for session {} and flow {}", getId(),
                        result.getAuthenticationFlowId(), e);
            }
            
            // Try and update the primary record with the updated flow list.
            try {
                int attempts = 10;
                boolean success = writeToStorage();
                while (!success && attempts-- > 0) {
                    // The record may have changed underneath, so we need to reapply the removal.
                    // If that succeeds, then we need to reattempt the update. If not, the result
                    // wasn't present in the updated map, but it was originally, so we return true.
                    if (super.removeAuthenticationResult(result)) {
                        success = writeToStorage();
                    } else {
                        return true;
                    }
                }
                if (!success) {
                    log.error("Exhausted retry attempts updating record for session {}", getId());
                }
            } catch (final IOException e) {
                if (!sessionManager.isMaskStorageFailure()) {
                    throw new SessionException(
                            "Exception updating session record after AuthenticationResult removal", e);
                }
                log.error("Exception updating record for session {}", getId(), e);
            }
            // If we reach here and a problem occurred, we must be masking storage problems.
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotLive @Unmodifiable public Set<SPSession> getSPSessions() {
        
        if (sessionManager.isTrackSPSessions() && sessionManager.storageServiceMeetsThreshold()) {
            // Check for any sparse/null values in the map, which need to be loaded before returning a complete set.
            final Iterator<Map.Entry<String, Optional<SPSession>>> entries =
                    getSPSessionMap().entrySet().iterator();
            while (entries.hasNext()) {
                final Map.Entry<String, Optional<SPSession>> entry = entries.next();
                if (entry.getValue().isEmpty()) {
                    try {
                        final String key = entry.getKey();
                        assert key != null;
                        final SPSession result = loadSPSessionFromStorage(key);
                        if (result != null) {
                            entry.setValue(Optional.of(result));
                        } else {
                            // A null here means the reference to the record should be removed.
                            entries.remove();
                        }
                    } catch (final IOException e) {
                        // An exception implies the record *might* still be accessible later.
                    }
                }
            }
        }
        
        return super.getSPSessions();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public SPSession getSPSession(@Nonnull @NotEmpty final String serviceId) {
        if (sessionManager.isTrackSPSessions() && sessionManager.storageServiceMeetsThreshold()) {
            // Check existing map.
            SPSession result = super.getSPSession(serviceId);
            if (result != null) {
                return result;
            }
            
            // See if such an ID is purported to exist.
            final String trimmed = StringSupport.trimOrNull(serviceId);
            if (!getSPSessionMap().containsKey(trimmed)) {
                return null;
            }
            
            // Load and add to map.
            try {
                result = loadSPSessionFromStorage(Constraint.isNotNull(trimmed, "ServiceId was empty"));
                if (result != null) {
                    doAddSPSession(result);
                } else {
                    // A null here means the reference to the record should be removed.
                    getSPSessionMap().remove(trimmed);
                }
            } catch (final IOException e) {
                // An exception implies the record *might* still be accessible later.
            }
            return result;
        }
        return null;
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    @Nullable public SPSession addSPSession(@Nonnull final SPSession spSession) throws SessionException {
        if (sessionManager.isTrackSPSessions()) {
            if (!sessionManager.storageServiceMeetsThreshold()) {
                log.debug("Unable to add SP session due to to storage service limitations");
                return null;
            }
                        
            try {
                // Prime things to make sure any previous instance from this SP is loaded so
                // we know to remove it.
                getSPSession(Constraint.isNotNull(spSession.getId(), "SessionID was empty"));

                // Store the record.
                if (!saveSPSessionToStorage(spSession) && !sessionManager.isMaskStorageFailure()) {
                    throw new SessionException("Unable to save SPSession to storage");
                }
                final SPSession prev = super.addSPSession(spSession);
                if (prev == null) {
                    // If no previous record, the add operation changed the primary record, requiring an update.
                    int attempts = 10;
                    boolean success = writeToStorage();
                    while (!success && attempts-- > 0) {
                        // The record may have changed underneath, see if we need to reapply the add.
                        if (!getSPSessionMap().containsKey(spSession.getId())) {
                            super.addSPSession(spSession);
                            success = writeToStorage();
                        } else {
                            success = true;
                        }
                    }
                    if (!success) {
                        log.error("Exhausted retry attempts updating record for session {}", getId());
                    }
                } else {
                    sessionManager.unindexSPSession(this, prev, 10);
                }
                sessionManager.indexBySPSession(this, spSession, 10);
                return prev;
            } catch (final IOException e) {
                if (!sessionManager.isMaskStorageFailure()) {
                    throw new SessionException("Exception saving SPSession record to storage", e);
                }
                log.error("Exception saving SPSession record for IdP session {} and service {}", getId(),
                        spSession.getId(), e);
                return null;
            }
        }
        log.debug("Ignoring SPSession add, session manager is not configured to track them");
        return null;
    }
 // Checkstyle: CyclomaticComplexity ON

    /** {@inheritDoc} */
    @Override
    public boolean removeSPSession(@Nonnull final SPSession spSession) throws SessionException {
        if (super.removeSPSession(spSession)) {
            try {
                // Remove the separate record.
                sessionManager.getStorageService().delete(getId(),
                        getSPSessionStorageKey(Constraint.isNotNull(spSession.getId(), "SessionID was empty")));
            } catch (final IOException e) {
                log.error("Exception removing SPSession record for IdP session {} and service {}", getId(),
                        spSession.getId(), e);
                if (!sessionManager.isMaskStorageFailure()) {
                    throw new SessionException("Exception removing SPSession record from storage", e);
                }
            }
            
            // Try and update the primary record with the updated service session list.
            try {
                int attempts = 10;
                boolean success = writeToStorage();
                while (!success && attempts-- > 0) {
                    // The record may have changed underneath, so we need to reapply the removal.
                    // If that succeeds, then we need to reattempt the update. If not, the result
                    // wasn't present in the updated map, but it was originally, so we return true.
                    if (super.removeSPSession(spSession)) {
                        success = writeToStorage();
                    } else {
                        return true;
                    }
                }
                if (!success) {
                    log.error("Exhausted retry attempts updating record for session {}", getId());
                }
            } catch (final IOException e) {
                if (!sessionManager.isMaskStorageFailure()) {
                    throw new SessionException(
                            "Exception updating session record after SPSession removal", e);
                }
                log.error("Exception updating record for session {}", getId(), e);
            }
            // If we reach here and a problem occurred, we must be masking storage problems.
            return true;
        }
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean checkTimeout() throws SessionException {
        if (getLastActivityInstant().plus(sessionManager.getSessionTimeout()).isAfter(Instant.now())) {
            return super.checkTimeout();
        }
        return false;
    }

    /**
     * Get the record version.
     * 
     * @return current version of the storage record
     */
    protected long getVersion() {
        return version;
    }
    
    /**
     * Set the record version.
     * 
     * @param ver version to set
     */
    protected void setVersion(final long ver) {
        version = ver;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @Live protected Map<String, Optional<AuthenticationResult>> getAuthenticationResultMap() {
        return super.getAuthenticationResultMap();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @Live protected Map<String, Optional<SPSession>> getSPSessionMap() {
        return super.getSPSessionMap();
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
        
        final AuthenticationFlowDescriptor flow = sessionManager.getAuthenticationFlowDescriptor(flowId);
        if (flow == null) {
            log.warn("No flow descriptor installed for ID {}, unable to load result from storage", flowId);
            return null;
        }
        
        try {
            final StorageRecord<AuthenticationResult> record = sessionManager.getStorageService().read(getId(), flowId);
            if (record != null) {
                return record.getValue(flow, getId(), flowId);
            }
            log.debug("No AuthenticationResult for flow {} in session {}", flowId, getId());
            return null;
        } catch (final IOException e) {
            log.error("Exception loading AuthenticationResult for flow {} from storage: {}", flowId, e.getMessage());
            throw e;
        }
    }

    /**
     * Saves an {@link AuthenticationResult} record to storage, serializing it using the object
     * attached to the corresponding {@link AuthenticationFlowDescriptor}.
     * 
     * @param result the object to store
     * 
     * @return true iff the record was successfully saved
     * @throws IOException if a possibly transitory storage-related error occurs
     */
    private boolean saveAuthenticationResultToStorage(@Nonnull final AuthenticationResult result) throws IOException {
        final String flowId = result.getAuthenticationFlowId();
        log.debug("Saving AuthenticationResult for flow {} in session {}", flowId, getId());
        
        final AuthenticationFlowDescriptor flow = sessionManager.getAuthenticationFlowDescriptor(flowId);
        if (flow == null) {
            log.warn("No flow descriptor installed for ID {}, unable to save result to storage", flowId);
            return false;
        }
        
        try {
            // Create / update loop until we succeed or exhaust attempts.
            int attempts = 10;
            boolean success = false;
            do {
                success = sessionManager.getStorageService().create(getId(), flowId, result, flow,
                        result.getLastActivityInstant().plus(flow.getInactivityTimeout()).plus(
                            AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET).toEpochMilli());
                if (!success) {
                    // The record already exists, so we need to overwrite via an update.
                    success = sessionManager.getStorageService().update(getId(), flowId, result, flow,
                            result.getLastActivityInstant().plus(flow.getInactivityTimeout()).plus(
                                AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET).toEpochMilli());
                }
            } while (!success && attempts-- > 0);
            
            if (!success) {
                log.error("Exhausted retry attempts storing AuthenticationResult for flow {} in session {}",
                        flowId, getId());
            }
            
            return success;
        } catch (final IOException e) {
            log.error("Exception saving AuthenticationResult for flow {} to storage: {}", flowId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Loads a {@link SPSession} record from storage and deserializes it using the object
     * registered in the attached {@link net.shibboleth.idp.session.SPSessionSerializerRegistry}.
     * 
     * @param serviceId ID of service for session to load
     * 
     * @return the stored session, or null if the record is missing or unusable
     * @throws IOException if a possibly transitory storage-related error occurs
     */
    @Nullable private SPSession loadSPSessionFromStorage(@Nonnull @NotEmpty final String serviceId) throws IOException {
        log.debug("Loading SPSession for service {} in session {}", serviceId, getId());

        final String key = getSPSessionStorageKey(serviceId);
        
        try {
            final StorageRecord<SPSession> record = sessionManager.getStorageService().read(getId(), key);
            if (record == null) {
                log.debug("No SPSession found for service {} in session {}", serviceId, getId());
                return null;
            }
            
            // Parse out the class type.
            final int pos = record.getValue().indexOf(':');
            if (pos <= 0) {
                throw new IOException("No class type found prefixed to record");
            }
            
            final String sessionClassName = record.getValue().substring(0,  pos);
            final SPSessionSerializerRegistry registry =
                    Constraint.isNotNull(sessionManager.getSPSessionSerializerRegistry(),
                            "Session Serializer Registry not set up");

            // Look up the serializer instance for that class type.
            final Class<? extends SPSession> claz = Class.forName(sessionClassName).asSubclass(SPSession.class);
            assert claz != null;
            final StorageSerializer<? extends SPSession> spSessionSerializer = registry.lookup(claz);
            if (spSessionSerializer == null) {
                throw new IOException("No serializer registered for SPSession type " + sessionClassName);
            }
            
            // Deserializer starting past the colon delimiter.
            final String recordValue =record.getValue().substring(pos + 1);
            assert recordValue!=null;
            return spSessionSerializer.deserialize(
                    record.getVersion(), getId(), key, recordValue, record.getExpiration());
            
        } catch (final IOException e) {
            log.error("IOException loading SPSession for service {} from storage: {}", serviceId, e.getMessage());
            throw e;
        } catch (final ClassNotFoundException e) {
            log.error("ClassNotFoundException loading SPSession for service {} from storage: {}", serviceId,
                    e.getMessage());
            throw new IOException(e);
        }
    }

    /**
     * Saves a {@link SPSession} record to storage.
     * 
     * @param session the object to store
     * 
     * @return true iff the record was successfully saved
     * @throws IOException if a possibly transitory storage-related error occurs
     */
    private boolean saveSPSessionToStorage(@Nonnull final SPSession session) throws IOException {
        log.debug("Saving SPSession for service {} in session {}", session.getId(), getId());

        final SPSessionSerializerRegistry registry =
                Constraint.isNotNull(sessionManager.getSPSessionSerializerRegistry(),
                        "Session Serializer Registry not set up");
        // Look up the serializer instance for that class type.
        final Class<? extends SPSession> claz = session.getClass();
        assert claz != null;
        @SuppressWarnings("unchecked")
        final StorageSerializer<SPSession> spSessionSerializer =
                (StorageSerializer<SPSession>) registry.lookup(claz);
        if (spSessionSerializer == null) {
            throw new IOException("No serializer registered for SPSession type " + session.getClass().getName());
        }
        final String id = session.getId();
        assert id != null;
        final String key = getSPSessionStorageKey(id);
        
        // Prefix the class name to the serialized data.
        final StringBuilder builder = new StringBuilder(session.getClass().getName());
        builder.append(':').append(spSessionSerializer.serialize(session));
        
        try {
            // Create / update loop until we succeed or exhaust attempts.
            int attempts = 10;
            boolean success = false;
            do {
                final Instant exp = session.getExpirationInstant().plus(sessionManager.getSessionSlop());
                final String builtString = builder.toString();
                assert builtString != null; 
                success = sessionManager.getStorageService().create(getId(), key, builtString, exp.toEpochMilli());
                if (!success) {
                    // The record already exists, so we need to overwrite via an update.
                    success = sessionManager.getStorageService().update(getId(), key, builtString, exp.toEpochMilli());
                }
            } while (!success && attempts-- > 0);
            
            if (!success) {
                log.error("Exhausted retry attempts storing SPService for service {} in session {}",
                        session.getId(), getId());
            }
            
            return success;
        } catch (final IOException e) {
            log.error("IOException saving SPSession for service {} to storage: {}", session.getId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Convert a service identifier into a suitable key for the underlying storage service.
     * 
     * @param serviceId the service identifier
     * = 
     * @return  an appropriately sized storage key
     */
    @Nonnull @NotEmpty private String getSPSessionStorageKey(@Nonnull @NotEmpty final String serviceId) {
        if (serviceId.length() > sessionManager.getStorageService().getCapabilities().getKeySize()) {
            final String result = DigestUtils.sha256Hex(serviceId);
            assert result != null;
            return result;
        }
        return serviceId;
    }
    
    /**
     * Update the primary session record based on the current contents of this object.
     * 
     * @return true iff the update succeeds, false iff a version mismatch resulted in overwrite of this object
     * @throws IOException if an error occurs trying to perform an update
     */
    private boolean writeToStorage() throws IOException {
        try {
            final Instant exp = getLastActivityInstant().plus(sessionManager.getSessionTimeout()).plus(
                    sessionManager.getSessionSlop());
            final Long ver = sessionManager.getStorageService().updateWithVersion(version, getId(),
                    StorageBackedSessionManager.SESSION_PRIMARY_KEY, this, sessionManager.getStorageSerializer(),
                    exp.toEpochMilli());
            if (ver == null) {
                log.error("Record for session {} has disappeared from backing store", getId());
                throw new IOException("Unable to update session, record disappeared");
            }
            version = ver;
            return true;
        } catch (final VersionMismatchException e) {
            // The record has changed underneath. We need to deserialize the session back into the
            // same object by passing ourselves as the target object to a new serializer instance.
            final StorageRecord<StorageBackedIdPSession> record =
                    sessionManager.getStorageService().read(getId(), StorageBackedSessionManager.SESSION_PRIMARY_KEY);
            if (record == null) {
                log.error("Record for session {} has disappeared from backing store", getId());
                throw new IOException("Unable to update session, record disappeared");
            }
            record.getValue(new StorageBackedIdPSessionSerializer(sessionManager, this),
                    getId(), StorageBackedSessionManager.SESSION_PRIMARY_KEY);
            return false;
        }
    }
    
}
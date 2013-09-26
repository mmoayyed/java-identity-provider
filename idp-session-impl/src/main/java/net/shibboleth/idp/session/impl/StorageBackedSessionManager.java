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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.ClientStorageService;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.opensaml.storage.VersionMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.ServiceSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.criterion.ServiceSessionCriterion;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

/**
 * Implementation of {@link SessionManager} and {@link SessionResolver} interfaces that relies on
 * a {@link StorageService} for persistence and lifecycle management of data.
 * 
 * <p>The storage layout here is to store most data in a context named for the session ID.
 * Within that context, the master {@link IdPSession} record lives under a key called "_session",
 * with an expiration based on the session timeout value plus a configurable amount of "slop" to
 * prevent premature disappearance in case of logout.</p>
 * 
 * <p>Each {@link AuthenticationResult} is stored in a record keyed by the flow ID. The expiration
 * is set based on the underlying flow's timeout plus the "slop" value.</p>
 * 
 * <p>Each {@link ServiceSession} is stored in a record keyed by the service ID. The expiration
 * is set based on the ServiceSession's own expiration plus the "slop" value.</p>
 * 
 * <p>For cross-referencing, lists of flow and service IDs are tracked within the master "_session"
 * record, so adding either requires an update to the master record plus the creation of a new one.
 * Post-creation, there are no updates to the AuthenticationResult or ServiceSession records, but
 * the expiration of the result records can be updated to reflect activity updates.</p>
 * 
 * <p>When a ServiceSession is added, it may expose an optional secondary "key". If set, this is a
 * signal to add a secondary lookup of the ServiceSession. This is a record containing a list of
 * relevant IdPSession IDs stored under a context/key pair consisting of the Service ID and the
 * exposed secondary key from the object. The expiration of this record is set based on the larger
 * of the current list expiration, if any, and the expiration of the ServiceSession plus the configured
 * slop value. In other words, the lifetime of the index record is pushed out as far as needed to
 * avoid premature expiration while any of the ServiceSessions producing it remain around.</p>
 * 
 * <p>The primary purpose of the secondary list is SAML logout, and is an optional feature that can be
 * disabled. In the case of a SAML 2 session, the secondary key is some form of the NameID issued
 * to the service.</p>
 */
public class StorageBackedSessionManager extends AbstractDestructableIdentifiableInitializableComponent implements
        SessionManager, SessionResolver {

    /** Storage key of master session records. */
    @Nonnull public static final String SESSION_MASTER_KEY = "_session";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageBackedSessionManager.class);
    
    /** Inactivity timeout for sessions in milliseconds. */
    @Duration @Positive private long sessionTimeout;
    
    /** Amount of time in milliseconds to defer expiration of records for better handling of logout. */
    @Duration @NonNegative private long sessionSlop;
    
    /** Indicates that storage service failures should be masked as much as possible. */
    private boolean maskStorageFailure;

    /** Indicates whether to store and track ServiceSessions. */
    private boolean trackServiceSessions;

    /** Indicates whether to secondary-index ServiceSessions. */
    private boolean secondaryServiceIndex;
    
    /** The back-end for managing data. */
    @NonnullAfterInit private StorageService storageService;

    /** Generator for XML ID attribute values. */
    @NonnullAfterInit private IdentifierGenerationStrategy idGenerator;

    /** Serializer for sessions. */
    @Nonnull private final StorageBackedIdPSessionSerializer serializer;
    
    /** Flows that could potentially be used to authenticate the user. */
    @Nonnull @NonnullElements private final Map<String, AuthenticationFlowDescriptor> flowDescriptorMap;
    
    /** Mappings between a ServiceSession type and a serializer implementation. */
    @Nullable private ServiceSessionSerializerRegistry serviceSessionSerializerRegistry;
    
    /**
     * Constructor.
     *
     */
    public StorageBackedSessionManager() {
        sessionTimeout = 60 * 60 * 1000;
        serializer = new StorageBackedIdPSessionSerializer(this, null);
        flowDescriptorMap = new HashMap();
    }
    
    /**
     * Get the session inactivity timeout policy in milliseconds.
     * 
     * @return  inactivity timeout
     */
    @Positive public long getSessionTimeout() {
        return sessionTimeout;
    }
    
    /**
     * Set the session inactivity timeout policy in milliseconds, must be greater than zero.
     * 
     * @param timeout the policy to set
     */
    public void setSessionTimeout(@Duration @Positive final long timeout) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        sessionTimeout = Constraint.isGreaterThan(0, timeout, "Timeout must be greater than zero");
    }

    /**
     * Get the amount of time in milliseconds to defer expiration of records.
     * 
     * @return  expiration deferrence time
     */
    @Positive public long getSessionSlop() {
        return sessionSlop;
    }
    
    /**
     * Set the amount of time in milliseconds to defer expiration of records.
     * 
     * @param slop the policy to set
     */
    public void setSessionSlop(@Duration @NonNegative final long slop) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        sessionSlop = Constraint.isGreaterThanOrEqual(0, slop, "Slop must be greater than or equal to zero");
    }
    
    /**
     * Get whether to mask StorageService failures where possible.
     * 
     * @return true iff StorageService failures should be masked
     */
    public boolean isMaskStorageFailure() {
        return maskStorageFailure;
    }

    /**
     * Set whether to mask StorageService failures where possible.
     * 
     * @param flag flag to set
     */
    public void setMaskStorageFailure(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        maskStorageFailure = flag;
    }
    
    /**
     * Get whether to track ServiceSessions.
     * 
     * @return true iff ServiceSessions should be persisted
     */
    public boolean isTrackServiceSessions() {
        return trackServiceSessions;
    }

    /**
     * Set whether to track ServiceSessions.
     * 
     * <p>This feature requires a StorageService that is not client-side because of space limitations.</p> 
     * 
     * @param flag flag to set
     */
    public void setTrackServiceSessions(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        trackServiceSessions = flag;
    }

    /**
     * Get whether to create a secondary index for ServiceSession lookup.
     * 
     * @return true iff a secondary index for ServiceSession lookup should be maintained
     */
    public boolean isSecondaryServiceIndex() {
        return secondaryServiceIndex;
    }

    /**
     * Set whether to create a secondary index for ServiceSession lookup.
     * 
     * <p>This feature requires a StorageService that is not client-side.</p> 
     * 
     * @param flag flag to set
     */
    public void setSecondaryServiceIndex(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        secondaryServiceIndex = flag;
    }

    /**
     * Get the StorageService back-end to use.
     * 
     * @return the back-end to use
     */
    @Nonnull public StorageService getStorageService() {
       return storageService;
    }
    
    /**
     * Set the StorageService back-end to use.
     * 
     * @param storage the back-end to use
     */
    public void setStorageService(@Nonnull final StorageService storage) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        storageService = Constraint.isNotNull(storage, "StorageService cannot be null");
    }

    /**
     * Set the generator to use when creating XML ID attribute values.
     * 
     * @param newIDGenerator the new IdentifierGenerator to use
     */
    public void setIDGenerator(@Nonnull final IdentifierGenerationStrategy newIDGenerator) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        idGenerator = Constraint.isNotNull(newIDGenerator, "IdentifierGenerationStrategy cannot be null");
    }

    /**
     * Get a matching {@link AuthenticatonFlowDescriptor}.
     * 
     * @param flowId the ID of the flow to return
     * 
     * @return the matching flow descriptor, or null
     */
    @Nullable public AuthenticationFlowDescriptor getAuthenticationFlowDescriptor(
            @Nonnull @NotEmpty final String flowId) {
        return flowDescriptorMap.get(flowId);
    }
    
    /**
     * Set the {@link AuthenticationFlowDescriptor} collection active in the system.
     * 
     * @param flows the flows available for possible use
     */
    public void setAuthenticationFlowDescriptors(
            @Nonnull @NonnullElements final Iterable<AuthenticationFlowDescriptor> flows) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(flows, "Flow collection cannot be null");
        
        flowDescriptorMap.clear();
        for (AuthenticationFlowDescriptor desc : Iterables.filter(flows, Predicates.notNull())) {
            flowDescriptorMap.put(desc.getId(), desc);
        }
    }
    
    /**
     * Get the attached {@link ServiceSessionSerializerRegistry}.
     * 
     * @return a registry of ServiceSession class to serializer mappings
     */
    @Nullable public ServiceSessionSerializerRegistry getServiceSessionSerializerRegistry() {
        return serviceSessionSerializerRegistry;
    }
    
    /**
     * Set the {@link ServiceSessionSerializerRegistry} to use.
     * 
     * @param registry  a registry of ServiceSession class to serializer mappings
     */
    public void setServiceSessionSerializerRegistry(@Nullable final ServiceSessionSerializerRegistry registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        serviceSessionSerializerRegistry = registry;
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (storageService == null) {
            throw new ComponentInitializationException(
                    "Initialization of StorageBackedSessionManager requires non-null StorageService");
        } else if (idGenerator == null) {
            throw new ComponentInitializationException(
                    "Initialization of StorageBackedSessionManager requires non-null IdentifierGenerationStrategy");
        } else if ((trackServiceSessions || secondaryServiceIndex) && storageService instanceof ClientStorageService) {
            throw new ComponentInitializationException(
                    "Tracking ServiceSessions requires a server-side StorageService");
        } else if (trackServiceSessions && serviceSessionSerializerRegistry == null) {
            throw new ComponentInitializationException(
                    "Tracking ServiceSessions requires a ServiceSessionSerializerRegistry");
        }
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        storageService.validate();
    }

    /** {@inheritDoc} */
    @Nonnull public IdPSession createSession(@Nonnull @NotEmpty final String principalName,
            @Nullable final String bindToAddress) throws SessionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        String sessionId = idGenerator.generateIdentifier(false);
        if (sessionId.length() > storageService.getCapabilities().getContextSize()) {
            throw new SessionException("Session IDs are too large for StorageService, check configuration");
        }
        
        StorageBackedIdPSession newSession = new StorageBackedIdPSession(this, sessionId, principalName,
                System.currentTimeMillis());
        if (bindToAddress != null) {
            newSession.doBindToAddress(bindToAddress);
        }
        
        try {
            if (!storageService.create(sessionId, SESSION_MASTER_KEY, newSession, serializer,
                    newSession.getCreationInstant() + sessionTimeout + sessionSlop)) {
                throw new SessionException("A duplicate session ID was generated, unable to create session");
            }
        } catch (IOException e) {
            log.error("Exception while storing new session for principal " + principalName, e);
            if (!maskStorageFailure) {
                throw new SessionException("Exception while storing new session", e);
            }
        }
        
        log.info("Created new session {} for principal {}", sessionId, principalName);
        return newSession;
    }

    /** {@inheritDoc} */
    public void destroySession(@Nonnull @NotEmpty final String sessionId) throws SessionException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        // Note that this can leave entries in the secondary ServiceSession records, but those
        // will eventually expire outright, or can be cleaned up if the index is searched.
        
        try {
            storageService.deleteContext(sessionId);
            log.info("Destroyed session {}", sessionId);
        } catch (IOException e) {
            log.error("Exception while destroying session " + sessionId, e);
            throw new SessionException("Exception while destroying session", e);
        }
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements public Iterable<IdPSession> resolve(@Nullable final CriteriaSet criteria)
            throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        // We support either session ID lookup, or secondary lookup by service ID and key, if
        // a secondary index is being maintained.
        
        if (criteria != null) {
            SessionIdCriterion sessionIdCriterion = criteria.get(SessionIdCriterion.class);
            if (sessionIdCriterion != null) {
                return ImmutableList.of(lookupBySessionId(sessionIdCriterion.getSessionId()));
            }
            
            ServiceSessionCriterion serviceCriterion = criteria.get(ServiceSessionCriterion.class);
            if (serviceCriterion != null) {
                if (!secondaryServiceIndex) {
                    throw new ResolverException("Secondary service index is disabled");
                }
                
                return lookupByServiceSession(serviceCriterion);
            }
        }
        
        throw new ResolverException("No supported criterion supplied");
    }

    /** {@inheritDoc} */
    @Nullable public IdPSession resolveSingle(@Nullable final CriteriaSet criteria) throws ResolverException {
        Iterator<IdPSession> i = resolve(criteria).iterator();
        if (i != null && i.hasNext()) {
            return i.next();
        }
        
        return null;
    }

    /**
     * Performs a lookup and deserializes a record based on session ID.
     * 
     * @param sessionId the session to lookup
     * 
     * @return the IdPSession object, or null
     * @throws ResolverException if an error occurs during lookup
     */
    @Nullable private IdPSession lookupBySessionId(@Nonnull @NotEmpty final String sessionId) throws ResolverException {
        try {
            StorageRecord<StorageBackedIdPSession> sessionRecord = storageService.read(sessionId, SESSION_MASTER_KEY);
            return sessionRecord.getValue(serializer, sessionId, SESSION_MASTER_KEY);
        } catch (IOException e) {
            log.error("Exception while querying for session " + sessionId, e);
            if (!maskStorageFailure) {
                throw new ResolverException("Exception while querying for session", e);
            }
        }
        
        return null;
    }
    
    /**
     * Performs a lookup and deserializes records potentially matching a ServiceSession.
     * 
     * @param criterion the ServiceSessionCriterion to apply
     * 
     * @return collection of zero or more sessions
     * @throws ResolverException if an error occurs during lookup
     */
    @Nonnull @NonnullElements private Iterable<IdPSession> lookupByServiceSession(
            @Nonnull final ServiceSessionCriterion criterion) throws ResolverException {
        
        int contextSize = storageService.getCapabilities().getContextSize();
        int keySize = storageService.getCapabilities().getKeySize();
        
        String serviceId = criterion.getServiceId();
        String serviceKey = criterion.getServiceSessionKey();
        log.debug("Performing secondary lookup on service ID {} and key {}", serviceId, serviceKey);

        // Truncate context and key if needed.
        if (serviceId.length() > contextSize) {
            serviceId = serviceId.substring(0, contextSize);
        }
        if (serviceKey.length() > keySize) {
            serviceKey = serviceKey.substring(0, keySize);
        }

        StorageRecord sessionList = null;
        
        try {
            sessionList = storageService.read(serviceId, serviceKey);
        } catch (IOException e) {
            log.error("Exception while querying based service ID " + serviceId + " and key " + serviceKey, e);
            if (!maskStorageFailure) {
                throw new ResolverException("Exception while querying based on ServiceSession", e);
            }
        }

        if (sessionList == null) {
            log.debug("Secondary lookup found nothing");
            return ImmutableList.of();
        }

        ImmutableList.Builder builder = ImmutableList.<IdPSession>builder();
        
        StringBuilder writeBackSessionList = new StringBuilder(sessionList.getValue().length());
        
        for (String sessionId : sessionList.getValue().split(",")) {
            IdPSession session = lookupBySessionId(sessionId);
            if (session != null) {
                // Session was found, so add it to the return set and to the updated index record.
                builder.add(session);
                writeBackSessionList.append(sessionId);
                writeBackSessionList.append(',');
            }
        }
        
        try {
            storageService.updateWithVersion(sessionList.getVersion(), serviceId, serviceKey,
                    writeBackSessionList.toString(), sessionList.getExpiration());
        } catch (IOException e) {
            log.warn("Ignoring exception while updating secondary index", e);
        } catch (VersionMismatchException e) {
            log.debug("Ignoring version mismatch while updating secondary index");
        }
        
        return builder.build();
    }

}
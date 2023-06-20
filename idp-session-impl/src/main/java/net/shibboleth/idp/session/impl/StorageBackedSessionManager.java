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
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.opensaml.storage.VersionMismatchException;
import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SPSessionSerializerRegistry;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.idp.session.criterion.SPSessionCriterion;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.net.CookieManager;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.NonnullSupplier;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.servlet.HttpServletSupport;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Implementation of {@link SessionManager} and {@link SessionResolver} interfaces that relies on a
 * {@link StorageService} for persistence and lifecycle management of data.
 * 
 * <p>
 * The storage layout here is to store most data in a context named for the session ID. Within that context, the
 * {@link IdPSession} record lives under a key called "_session", with an expiration based on the session timeout value
 * plus a configurable amount of "slop" to prevent premature disappearance in case of logout.
 * </p>
 * 
 * <p>
 * Each {@link net.shibboleth.idp.authn.AuthenticationResult} is stored in a record keyed by the flow ID. The expiration
 * is set based on the underlying flow's timeout.
 * </p>
 * 
 * <p>
 * Each {@link SPSession} is stored in a record keyed by the service ID. The expiration is set based on the SPSession's
 * own expiration plus the "slop" value.
 * </p>
 * 
 * <p>
 * For cross-referencing, lists of flow and service IDs are tracked within the "_session" record, so adding
 * either requires an update to that record plus the creation of a new one. Post-creation, there are no updates to
 * the AuthenticationResult or SPSession records, but the expiration of the result records can be updated to reflect
 * activity updates.
 * </p>
 * 
 * <p>
 * When a SPSession is added, it may expose an optional secondary "key". If set, this is a signal to add a secondary
 * lookup of the SPSession. This is a record containing a list of relevant IdPSession IDs stored under a context/key
 * pair consisting of the Service ID and the exposed secondary key from the object. The expiration of this record is set
 * based on the larger of the current list expiration, if any, and the expiration of the SPSession plus the configured
 * slop value. In other words, the lifetime of the index record is pushed out as far as needed to avoid premature
 * expiration while any of the SPSessions producing it remain around.
 * </p>
 * 
 * <p>
 * The primary purpose of the secondary list is SAML logout, and is an optional feature that can be disabled. In the
 * case of a SAML 2 session, the secondary key is some form of the NameID issued to the service.
 * </p>
 */
public class StorageBackedSessionManager extends AbstractIdentifiableInitializableComponent implements SessionManager,
        SessionResolver {

    /** Storage key of primary session records. */
    @Nonnull @NotEmpty public static final String SESSION_PRIMARY_KEY = "_session";

    /** Default cookie name for session tracking. */
    @Nonnull @NotEmpty protected static final String DEFAULT_COOKIE_NAME = "shib_idp_session";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageBackedSessionManager.class);

    /** Servlet request to read from. */
    @NonnullAfterInit private NonnullSupplier<HttpServletRequest> httpRequestSupplier;

    /** Inactivity timeout for sessions. */
    @Nonnull private Duration sessionTimeout;

    /** Amount of time to defer expiration of records for better handling of logout. */
    @Nonnull private Duration sessionSlop;

    /** Indicates that storage service failures should be masked as much as possible. */
    private boolean maskStorageFailure;

    /** Indicates whether to store and track SPSessions. */
    private boolean trackSPSessions;

    /** Indicates whether to secondary-index SPSessions. */
    private boolean secondaryServiceIndex;

    /** Indicates how bound session addresses and client addresses are compared. */
    @Nonnull private BiPredicate<String,String> consistentAddressCondition;

    /** Manages creation of cookies. */
    @NonnullAfterInit private CookieManager cookieManager;

    /** Name of cookie used to track sessions. */
    @Nonnull @NotEmpty private String cookieName;
    
    /** The back-end for managing data. */
    @NonnullAfterInit private StorageService storageService;

    /** Size boundary below which "large" data can't be stored. */
    private long storageServiceThreshold;
    
    /** Generator for XML ID attribute values. */
    @NonnullAfterInit private IdentifierGenerationStrategy idGenerator;

    /** Serializer for sessions. */
    @Nonnull private final StorageBackedIdPSessionSerializer serializer;

    /** Flows that could potentially be used to authenticate the user. */
    @Nonnull private final Map<String,AuthenticationFlowDescriptor> flowDescriptorMap;

    /** Mappings between a SPSession type and a serializer implementation. */
    @Nullable private SPSessionSerializerRegistry spSessionSerializerRegistry;

    /**
     * Constructor.
     * 
     */
    public StorageBackedSessionManager() {
        final Duration oneHour = Duration.ofHours(1);
        final Duration zeroDuration = Duration.ZERO;
        assert oneHour!=null && zeroDuration!= null;
        sessionTimeout = oneHour;
        sessionSlop = zeroDuration;
        serializer = new StorageBackedIdPSessionSerializer(this, null);
        flowDescriptorMap = new HashMap<>();
        consistentAddressCondition =
                DefaultConsistentAddressConditionFactory.getDefaultConsistentAddressCondition(true);
        cookieName = DEFAULT_COOKIE_NAME;
        storageServiceThreshold = 1024 * 1024;
    }

    /**
     * Set the {@link NonnullSupplier} for the servlet request to read from.
     * 
     * @param requestSupplier servlet request Supplier
     */
    public void setHttpServletRequestSupplier(@Nullable final NonnullSupplier<HttpServletRequest> requestSupplier) {
        checkSetterPreconditions();

        httpRequestSupplier = requestSupplier;
    }

    /**
     * Get the session inactivity timeout policy.
     * 
     * @return inactivity timeout
     */
    @Nonnull public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Set the session inactivity timeout policy.
     * 
     * @param timeout the policy to set
     */
    public void setSessionTimeout(@Nonnull final Duration timeout) {
        checkSetterPreconditions();
        sessionTimeout = Constraint.isNotNull(timeout, "Timeout cannot be null");
    }

    /**
     * Get the amount of time to defer expiration of records.
     * 
     * @return expiration amount of time to defer expiration of records
     */
    @Nonnull public Duration getSessionSlop() {
        return sessionSlop;
    }

    /**
     * Set the amount of time to defer expiration of records.
     * 
     * @param slop amount of time to defer expiration of records
     */
    public void setSessionSlop(@Nonnull final Duration slop) {
        checkSetterPreconditions();
        sessionSlop = Constraint.isNotNull(slop, "Slop cannot be null");
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
    public void setMaskStorageFailure(final boolean flag) {
        checkSetterPreconditions();
        maskStorageFailure = flag;
    }

    /**
     * Get whether to track SPSessions.
     * 
     * @return true iff SPSessions should be persisted
     */
    public boolean isTrackSPSessions() {
        return trackSPSessions;
    }

    /**
     * Set whether to track SPSessions.
     * 
     * <p>
     * This feature requires a StorageService that is not client-side because of space limitations.
     * </p>
     * 
     * @param flag flag to set
     */
    public void setTrackSPSessions(final boolean flag) {
        checkSetterPreconditions();
        trackSPSessions = flag;
    }

    /**
     * Get whether to create a secondary index for SPSession lookup.
     * 
     * @return true iff a secondary index for SPSession lookup should be maintained
     */
    public boolean isSecondaryServiceIndex() {
        return secondaryServiceIndex;
    }

    /**
     * Set whether to create a secondary index for SPSession lookup.
     * 
     * <p>
     * This feature requires a StorageService that is not client-side.
     * </p>
     * 
     * @param flag flag to set
     */
    public void setSecondaryServiceIndex(final boolean flag) {
        checkSetterPreconditions();
        secondaryServiceIndex = flag;
    }

    /**
     * Get condition to evaluate bound session and client addresses for consistency.
     * 
     * @return condition
     * 
     * @since 4.0.0
     */
    @Nonnull public BiPredicate<String,String> getConsistentAddressCondition() {
        return consistentAddressCondition;
    }

    /**
     * Set whether sessions are bound to client addresses either via disabling the comparison
     * or testing simple equality.
     * 
     * @param flag flag to set
     */
    public void setConsistentAddress(final boolean flag) {
        checkSetterPreconditions();
        consistentAddressCondition =
                DefaultConsistentAddressConditionFactory.getDefaultConsistentAddressCondition(flag);
    }
    
    /**
     * Set condition to evaluate bound session and client addresses for consistency.
     * 
     * @param condition condition to set
     * 
     * @since 4.0.0
     */
    public void setConsistentAddressCondition(@Nonnull final BiPredicate<String,String> condition) {
        checkSetterPreconditions();
        consistentAddressCondition = Constraint.isNotNull(condition, "Consistent address condition cannot be null");
    }

    /**
     * Set the cookie name to use for session tracking.
     * 
     * @param name cookie name to use
     */
    public void setCookieName(@Nonnull @NotEmpty final String name) {
        checkSetterPreconditions();
        cookieName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Cookie name cannot be null or empty");
    }

    /**
     * Set the {@link CookieManager} to use.
     * 
     * @param manager the CookieManager to use.
     */
    public void setCookieManager(@Nonnull final CookieManager manager) {
        checkSetterPreconditions();
        cookieManager = Constraint.isNotNull(manager, "CookieManager cannot be null");
    }
    
    /**
     * Get the {@link StorageService} back-end to use.
     * 
     * @return the back-end to use
     */
    @Nonnull public StorageService getStorageService() {
        checkComponentActive();
        assert storageService != null;
        return storageService;
    }

    /**
     * Set the {@link StorageService} back-end to use.
     * 
     * @param storage the back-end to use
     */
    public void setStorageService(@Nonnull final StorageService storage) {
        checkSetterPreconditions();
        storageService = Constraint.isNotNull(storage, "StorageService cannot be null");
    }
    
    /**
     * Get whether the configured {@link StorageService}'s
     * {@link org.opensaml.storage.StorageCapabilities#getValueSize()} method meets the
     * value set via {@link #setStorageServiceThreshold(long)}.
     * 
     * @return true iff the threshold is met
     */
    public boolean storageServiceMeetsThreshold() {
        checkComponentActive();
        return storageService.getCapabilities().getValueSize() >= storageServiceThreshold;
    }
    
    /**
     * Set the size in characters that the configured {@link StorageService} must support in order for
     * "larger" data to be stored, specifically the data involved with the {@link #trackSPSessions}
     * and {@link #secondaryServiceIndex} options.
     * 
     * <p>The implementation will query the configured service each time it needs to honor those options,
     * to handle cases where the size limit can vary by request.</p>
     * 
     * <p>Defaults to 1024 * 1024 characters.</p>
     * 
     * @param size  size in characters
     */
    public void setStorageServiceThreshold(final long size) {
        checkSetterPreconditions();
        storageServiceThreshold = size;
    }
    
    /**
     * Set the generator to use when creating XML ID attribute values.
     * 
     * @param newIDGenerator the new IdentifierGenerator to use
     */
    public void setIDGenerator(@Nonnull final IdentifierGenerationStrategy newIDGenerator) {
        checkSetterPreconditions();
        idGenerator = Constraint.isNotNull(newIDGenerator, "IdentifierGenerationStrategy cannot be null");
    }

    /**
     * Get the serializer for the {@link IdPSession} objects managed by this implementation.
     * 
     * @return the serializer to use when writing back session objects
     */
    @Nonnull public StorageSerializer<StorageBackedIdPSession> getStorageSerializer() {
        return serializer;
    }

    /**
     * Get a matching {@link AuthenticationFlowDescriptor}.
     * 
     * @param flowId the ID of the flow to return
     * 
     * @return the matching flow descriptor, or null
     */
    @Nullable public AuthenticationFlowDescriptor
            getAuthenticationFlowDescriptor(@Nonnull @NotEmpty final String flowId) {
        return flowDescriptorMap.get(flowId);
    }
    
    /** Get the {@link HttpServletRequest} associated with this operation.
     * @return the  {@link HttpServletRequest} 
     */
    @Nullable final private HttpServletRequest getHttpRequest() {
        final NonnullSupplier<HttpServletRequest> supplier = httpRequestSupplier;
        if (supplier == null) {
            return null;
        }
        return supplier.get();
    }

    /**
     * Set the {@link AuthenticationFlowDescriptor} collection active in the system.
     * 
     * @param flows the flows available for possible use
     */
    public void setAuthenticationFlowDescriptors(@Nonnull final Iterable<AuthenticationFlowDescriptor> flows) {
        checkSetterPreconditions();
        flowDescriptorMap.clear();
        for (final AuthenticationFlowDescriptor desc : Constraint.isNotNull(flows, "Flow collection cannot be null")) {
            if (desc != null) { 
                flowDescriptorMap.put(desc.getId(), desc);
            }
        }
    }

    /**
     * Get the attached {@link SPSessionSerializerRegistry}.
     * 
     * @return a registry of SPSession class to serializer mappings
     */
    @Nullable public SPSessionSerializerRegistry getSPSessionSerializerRegistry() {
        return spSessionSerializerRegistry;
    }

    /**
     * Set the {@link SPSessionSerializerRegistry} to use.
     * 
     * @param registry a registry of SPSession class to serializer mappings
     */
    public void setSPSessionSerializerRegistry(@Nullable final SPSessionSerializerRegistry registry) {
        checkSetterPreconditions();
        spSessionSerializerRegistry = registry;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (storageService == null) {
            throw new ComponentInitializationException(
                    "Initialization of StorageBackedSessionManager requires non-null StorageService");
        } else if (idGenerator == null) {
            throw new ComponentInitializationException(
                    "Initialization of StorageBackedSessionManager requires non-null IdentifierGenerationStrategy");
        } else if (cookieManager == null) {
            throw new ComponentInitializationException(
                    "Initialization of StorageBackedSessionManager requires non-null CookieManager");
        } else if (trackSPSessions && spSessionSerializerRegistry == null) {
            throw new ComponentInitializationException("Tracking SPSessions requires a spSessionSerializerRegistry");
        }
        // This is our private instance, so we initialize it.
        serializer.initialize();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public IdPSession createSession(@Nonnull @NotEmpty final String principalName)
            throws SessionException {
        checkComponentActive();

        final HttpServletRequest httpRequest = getHttpRequest();
        if (httpRequest == null) {
            throw new SessionException("No HttpServletRequest available, can't bind to client address");
        }
        
        final String remoteAddr = StringSupport.trimOrNull(HttpServletSupport.getRemoteAddr(httpRequest));
        if (remoteAddr == null) {
            throw new SessionException("No client address to bind");
        }

        final String sessionId = idGenerator.generateIdentifier(false);
        if (sessionId.length() > storageService.getCapabilities().getContextSize()) {
            throw new SessionException("Session IDs are too large for StorageService, check configuration");
        }

        final Instant now = Instant.now();
        assert now != null;
        final StorageBackedIdPSession newSession =
                new StorageBackedIdPSession(this, sessionId, principalName, now);
        newSession.doBindToAddress(remoteAddr);

        try {
            if (!storageService.create(sessionId, SESSION_PRIMARY_KEY, newSession, serializer,
                    newSession.getCreationInstant().plus(sessionTimeout).plus(sessionSlop).toEpochMilli())) {
                throw new SessionException("A duplicate session ID was generated, unable to create session");
            }
        } catch (final IOException e) {
            log.error("Exception while storing new session for principal {}", principalName, e);
            if (!maskStorageFailure) {
                throw new SessionException("Exception while storing new session", e);
            }
        }

        log.debug("Created new session {} for principal {}", sessionId, principalName);
        cookieManager.addCookie(cookieName, sessionId);
        return newSession;
    }

    /** {@inheritDoc} */
    @Override
    public void destroySession(@Nonnull @NotEmpty final String sessionId, final boolean unbind)
            throws SessionException {
        checkComponentActive();

        // Note that this can leave entries in the secondary SPSession records, but those
        // will eventually expire outright, or can be cleaned up if the index is searched.

        if (unbind) {
            cookieManager.unsetCookie(cookieName);
        }

        try {
            storageService.deleteContext(sessionId);
            log.debug("Destroyed session {}", sessionId);
        } catch (final IOException e) {
            log.error("Exception while destroying session {}: {}", sessionId, e.getMessage());
            throw new SessionException("Exception while destroying session", e);
        }
    }

    /** {@inheritDoc} */
    // Checkstyle: CyclomaticComplexity OFF
    @Override
    @Nonnull @Unmodifiable @NotLive public Iterable<IdPSession> resolve(@Nullable final CriteriaSet criteria)
            throws ResolverException {
        checkComponentActive();

        // We support either session ID lookup, or secondary lookup by service ID and key, if
        // a secondary index is being maintained.

        if (criteria != null) {
            final HttpServletRequestCriterion requestCriterion = criteria.get(HttpServletRequestCriterion.class);
            if (requestCriterion != null) {
                final HttpServletRequest httpRequest = getHttpRequest();
                if (httpRequest != null) {
                    final Cookie[] cookies = httpRequest.getCookies();
                    if (cookies != null) {
                        for (final Cookie cookie : cookies) {
                            if (cookieName.equals(cookie.getName())) {
                                final IdPSession session = lookupBySessionId(cookie.getValue());
                                if (session != null) {
                                    return CollectionSupport.singletonList(session);
                                }
                            }
                        }
                    }
                    return CollectionSupport.emptyList();
                }
                throw new ResolverException("HttpServletRequest is null");
            }

            final SessionIdCriterion sessionIdCriterion = criteria.get(SessionIdCriterion.class);
            if (sessionIdCriterion != null) {
                final IdPSession session = lookupBySessionId(sessionIdCriterion.getSessionId());
                if (session != null) {
                    return CollectionSupport.singletonList(session);
                }
                return CollectionSupport.emptyList();
            }

            final SPSessionCriterion serviceCriterion = criteria.get(SPSessionCriterion.class);
            if (serviceCriterion != null) {
                if (!secondaryServiceIndex) {
                    throw new ResolverException(
                            "Secondary service index is disabled (must be enabled for SAML logout)");
                }

                return lookupBySPSession(serviceCriterion);
            }
        }

        throw new ResolverException("No supported criterion supplied");
    }
    // Checkstyle: CyclomaticComplexity On
    
    /** {@inheritDoc} */
    @Override
    @Nullable public IdPSession resolveSingle(@Nullable final CriteriaSet criteria) throws ResolverException {
        final Iterator<IdPSession> i = resolve(criteria).iterator();
        if (i != null && i.hasNext()) {
            return i.next();
        }

        return null;
    }

    /**
     * Insert or update a secondary index record from an SPSession to a parent IdPSession.
     * 
     * @param idpSession the parent session
     * @param spSession the SPSession to index
     * @param attempts number of times to retry operation in the event of a synchronization issue
     * 
     * @throws SessionException if a fatal error occurs
     */
    protected void indexBySPSession(@Nonnull final IdPSession idpSession, @Nonnull final SPSession spSession,
            final int attempts) throws SessionException {
        if (attempts <= 0) {
            log.error("Exceeded retry attempts while adding to secondary index");
            if (!maskStorageFailure) {
                throw new SessionException("Exceeded retry attempts while adding to secondary index");
            }
        } else if (secondaryServiceIndex && storageServiceMeetsThreshold()) {
            String serviceId = spSession.getId();
            assert serviceId != null;
            String serviceKey = spSession.getSPSessionKey();
            if (serviceKey == null) {
                return;
            }
            log.debug("Maintaining secondary index for service ID {} and key {}", serviceId, serviceKey);

            final int contextSize = storageService.getCapabilities().getContextSize();
            final int keySize = storageService.getCapabilities().getKeySize();

            // Truncate context and key if needed.
            if (serviceId.length() > contextSize) {
                serviceId = serviceId.substring(0, contextSize);
            }
            if (serviceKey.length() > keySize) {
                serviceKey = serviceKey.substring(0, keySize);
            }
            assert serviceId != null && serviceKey != null;
            StorageRecord<?> sessionList = null;

            try {
                sessionList = storageService.read(serviceId, serviceKey);
            } catch (final IOException e) {
                log.error("Exception while querying based service ID {} and key {}", serviceId, serviceKey, e);
                if (!maskStorageFailure) {
                    throw new SessionException("Exception while querying based on SPSession", e);
                }
            }

            try {
                if (sessionList != null) {
                    if (!sessionList.getValue().contains(idpSession.getId() + ',')) {
                        // Need to update record.
                        final String updated = sessionList.getValue() + idpSession.getId() + ',';
                        if (storageService.updateWithVersion(sessionList.getVersion(), serviceId, serviceKey, updated,
                                Math.max(Constraint.isNotNull(sessionList.getExpiration(),"Session List Expiration not set"), 
                                         spSession.getExpirationInstant().plus(sessionSlop).toEpochMilli())) == null) {
                            log.debug("Secondary index record disappeared, retrying as insert");
                            indexBySPSession(idpSession, spSession, attempts - 1);
                        }
                    } else {
                        log.debug("IdP session {} already indexed against service ID {} and key {}", idpSession.getId(),
                                serviceId, serviceKey);
                    }
                } else if (!storageService.create(serviceId, serviceKey, idpSession.getId() + ',',
                        spSession.getExpirationInstant().plus(sessionSlop).toEpochMilli())) {
                    log.debug("Secondary index record appeared, retrying as update");
                    indexBySPSession(idpSession, spSession, attempts - 1);
                }
            } catch (final IOException e) {
                log.error("Exception maintaining secondary index for service ID {} and key {}",
                        serviceId, serviceKey, e);
                if (!maskStorageFailure) {
                    throw new SessionException("Exception maintaining secondary index", e);
                }
            } catch (final VersionMismatchException e) {
                log.debug("Secondary index record was updated between read/update, retrying");
                indexBySPSession(idpSession, spSession, attempts - 1);
            }
        }
    }

    /**
     * Remove or update a secondary index record from an SPSession to a parent IdPSession.
     * 
     * @param idpSession the parent session
     * @param spSession the SPSession to de-index
     * @param attempts number of times to retry operation in the event of a synchronization issue
     * 
     * @throws SessionException if a fatal error occurs
     */
    protected void unindexSPSession(@Nonnull final IdPSession idpSession, @Nonnull final SPSession spSession,
            final int attempts) throws SessionException {
        if (attempts <= 0) {
            log.error("Exceeded retry attempts while removing from secondary index");
            if (!maskStorageFailure) {
                throw new SessionException("Exceeded retry attempts while removing from secondary index");
            }
        } else if (secondaryServiceIndex && storageServiceMeetsThreshold()) {
            String serviceId = spSession.getId();
            assert serviceId != null;
            String serviceKey = spSession.getSPSessionKey();
            if (serviceKey == null) {
                return;
            }
            log.debug("Removing secondary index for service ID {} and key {}", serviceId, serviceKey);

            final int contextSize = storageService.getCapabilities().getContextSize();
            final int keySize = storageService.getCapabilities().getKeySize();

            // Truncate context and key if needed.
            if (serviceId.length() > contextSize) {
                serviceId = serviceId.substring(0, contextSize);
            }
            if (serviceKey.length() > keySize) {
                serviceKey = serviceKey.substring(0, keySize);
            }
            assert serviceId != null && serviceKey != null;

            StorageRecord<?> sessionList = null;

            try {
                sessionList = storageService.read(serviceId, serviceKey);
            } catch (final IOException e) {
                log.error("Exception while querying based service ID {} and key {}", serviceId, serviceKey, e);
                if (!maskStorageFailure) {
                    throw new SessionException("Exception while querying based on SPSession", e);
                }
            }

            try {
                if (sessionList != null) {
                    final String recordValue = sessionList.getValue();
                    if (recordValue.contains(idpSession.getId() + ',')) {
                        // Need to update or delete record.
                        final String updated = recordValue.replace(idpSession.getId() + ',', "");
                        if (updated.length() > 0) {
                            if (storageService.updateWithVersion(sessionList.getVersion(), serviceId, serviceKey,
                                    updated, sessionList.getExpiration()) == null) {
                                log.debug("Secondary index record disappeared, nothing to do");
                            }
                        } else {
                            storageService.deleteWithVersion(sessionList.getVersion(), serviceId, serviceKey);
                        }
                    } else {
                        log.debug("IdP session {} not indexed against service ID {} and key {}", idpSession.getId(),
                                serviceId, serviceKey);
                    }
                } else {
                    log.debug("Secondary index record not found, nothing to do");
                }
            } catch (final IOException e) {
                log.error("Exception removing secondary index for service ID {} and key {}",
                        serviceId, serviceKey, e);
                if (!maskStorageFailure) {
                    throw new SessionException("Exception maintaining secondary index", e);
                }
            } catch (final VersionMismatchException e) {
                log.debug("Secondary index record was updated between read/update/delete, retrying");
                unindexSPSession(idpSession, spSession, attempts - 1);
            }
        }
    }
    
    /**
     * Performs a lookup and deserializes a record based on session ID.
     * 
     * @param sessionId the session to lookup
     * 
     * @return the IdPSession object, or null
     * @throws ResolverException if an error occurs during lookup
     */
    @Nullable private IdPSession lookupBySessionId(@Nullable final String sessionId) throws ResolverException {
        if (Strings.isNullOrEmpty(sessionId)) {
            log.debug("Lookup of null/empty session ID");
            return null;
        }
        assert sessionId != null;
        
        log.debug("Performing primary lookup on session ID {}", sessionId);

        try {
            final StorageRecord<StorageBackedIdPSession> sessionRecord =
                    storageService.read(sessionId, SESSION_PRIMARY_KEY);
            if (sessionRecord != null) {
                return sessionRecord.getValue(serializer, sessionId, SESSION_PRIMARY_KEY);
            }
            log.debug("Primary lookup failed for session ID {}", sessionId);
        } catch (final IOException e) {
            log.error("Exception while querying for session ID {}", sessionId, e);
            if (!maskStorageFailure) {
                throw new ResolverException("Exception while querying for session", e);
            }
        }

        return null;
    }

    /**
     * Performs a lookup and deserializes records potentially matching a SPSession.
     * 
     * @param criterion the SPSessionCriterion to apply
     * 
     * @return collection of zero or more sessions
     * @throws ResolverException if an error occurs during lookup
     */
    @Nonnull private Iterable<IdPSession> lookupBySPSession(@Nonnull final SPSessionCriterion criterion)
            throws ResolverException {

        final int contextSize = storageService.getCapabilities().getContextSize();
        final int keySize = storageService.getCapabilities().getKeySize();

        String serviceId = criterion.getServiceId();
        String serviceKey = criterion.getSPSessionKey();
        log.debug("Performing secondary lookup on service ID {} and key {}", serviceId, serviceKey);

        // Truncate context and key if needed.
        if (serviceId.length() > contextSize) {
            serviceId = serviceId.substring(0, contextSize);
            assert serviceId != null;
        }
        if (serviceKey.length() > keySize) {
            serviceKey = serviceKey.substring(0, keySize);
            assert serviceKey != null;
        }

        StorageRecord<?> sessionList = null;

        try {
            sessionList = storageService.read(serviceId, serviceKey);
        } catch (final IOException e) {
            log.error("Exception while querying based service ID {} and key {}", serviceId, serviceKey, e);
            if (!maskStorageFailure) {
                throw new ResolverException("Exception while querying based on SPSession", e);
            }
        }

        if (sessionList == null) {
            log.debug("Secondary lookup failed on service ID {} and key {}", serviceId, serviceKey);
            return CollectionSupport.emptyList();
        }

        final ImmutableList.Builder<IdPSession> builder = ImmutableList.builder();

        final StringBuilder writeBackSessionList = new StringBuilder(sessionList.getValue().length());

        for (final String sessionId : sessionList.getValue().split(",")) {
            final IdPSession session = lookupBySessionId(sessionId);
            if (session != null) {
                // Session was found, so add it to the return set and to the updated index record.
                builder.add(session);
                writeBackSessionList.append(sessionId);
                writeBackSessionList.append(',');
            }
        }

        try {
            final String writeBackValue = writeBackSessionList.toString();
            if (writeBackValue.length() == 0) {
                storageService.deleteWithVersion(sessionList.getVersion(), serviceId, serviceKey);
            } else if (!writeBackValue.equals(sessionList.getValue())) {
                storageService.updateWithVersion(sessionList.getVersion(), serviceId, serviceKey, writeBackValue,
                        sessionList.getExpiration());
            }
        } catch (final IOException e) {
            log.warn("Ignoring exception while updating secondary index", e);
        } catch (final VersionMismatchException e) {
            log.debug("Ignoring version mismatch while updating secondary index");
        }
        final Iterable<IdPSession> result = builder.build();
        assert result != null;
        return result;
    }
    
    /**
     * Simplifies Spring wiring of a true/false condition for the consistentAddress feature.
     * 
     * @since 4.0.0
     */
    public static class DefaultConsistentAddressConditionFactory {
        
        /**
         * Returns a suitable {@link BiPredicate} to satisfy a simple true/false value for the
         * consistentAddress feature.
         * 
         * @param flag true/false value for the feature
         * 
         * @return a {@link BiPredicate} that satisfies the input
         */
        @Nonnull public static BiPredicate<String,String> getDefaultConsistentAddressCondition(final boolean flag) {
            if (flag) {
                return (A,B) -> Objects.equals(A,B);
            }
            
            return (A,B) -> true;
        }
    }

}
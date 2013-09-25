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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.storage.ClientStorageService;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.SessionResolver;
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

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageBackedSessionManager.class);
    
    /** Inactivity timeout for sessions in milliseconds. */
    @Duration @Positive private long sessionTimeout;
    
    /** Amount of time in milliseconds to defer expiration of records for better handling of logout. */
    @Duration @NonNegative private long sessionSlop;
    
    /** Indicates that storage service failures should be masked as much as possible. */
    private boolean maskStorageFailure;
    
    /** The back-end for managing data. */
    @NonnullAfterInit private StorageService storageService;

    /** Generator for XML ID attribute values. */
    @NonnullAfterInit private IdentifierGenerationStrategy idGenerator;

    /** Serializer for sessions. */
    @Nonnull private final StorageBackedIdPSessionSerializer serializer;
    
    /**
     * Constructor.
     *
     */
    public StorageBackedSessionManager() {
        sessionTimeout = 60 * 60 * 1000;
        serializer = new StorageBackedIdPSessionSerializer(this, null);
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
        maskStorageFailure = flag;
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
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (storageService == null) {
            throw new ComponentInitializationException(
                    "Initialization of StorageBackedSessionManager requires non-null StorageService");
        } else if (idGenerator == null) {
            throw new ComponentInitializationException(
                    "Initialization of StorageBackedSessionManager requires non-null IdentifierGenerationStrategy");
        }
        
        serializer.setCompactForm(storageService instanceof ClientStorageService);
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        storageService.validate();
    }

    /** {@inheritDoc} */
    @Nonnull public IdPSession createSession(@Nonnull @NotEmpty final String principalName,
            @Nullable final String bindToAddress) throws SessionException {

        String sessionId = idGenerator.generateIdentifier(false);
        StorageBackedIdPSession newSession = new StorageBackedIdPSession(this, sessionId, principalName,
                System.currentTimeMillis());
        if (bindToAddress != null) {
            newSession.doBindToAddress(bindToAddress);
        }
        
        try {
            if (!storageService.create(sessionId, "_session", newSession, serializer,
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
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Nullable public IdPSession resolveSingle(@Nullable final CriteriaSet criteria) throws ResolverException {
        // TODO Auto-generated method stub
        return null;
    }

}
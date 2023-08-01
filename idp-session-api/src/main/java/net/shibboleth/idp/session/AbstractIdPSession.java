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

package net.shibboleth.idp.session;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

import org.slf4j.Logger;

import com.google.common.base.MoreObjects;

/**
 * Abstract base for implementations of {@link IdPSession}, handles basic management of the
 * instance data without addressing persistence.
 * 
 * <p>Data that can change post-construction can be modified using doSet/doAdd/doRemove methods
 * that maintain the object state. Abstract methods defined here or left unimplemented from the
 * interface should be implemented to call these methods and perform any additional work required
 * to maintain the coherence of the underlying store, if any.</p>
 * 
 * <p>The {@link #checkAddress(String)} method is implemented by calling into other abstract and defined
 * methods to check session state and update address information as required.</p> 
 */
@ThreadSafe
public abstract class AbstractIdPSession implements IdPSession {

    /** Address syntaxes supported for address binding. */
    public enum AddressFamily {
        /** IP version 4 (dotted decimal). */
        IPV4,
        
        /** IP version 6 (colon-sep hex). */
        IPV6,
        
        /** Unknown. */
        UNKNOWN
    }
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractIdPSession.class);
    
    /** Unique ID of this session. */
    @Nonnull @NotEmpty private final String id;
    
    /** A canonical name for the subject of the session. */
    @Nonnull @NotEmpty private final String principalName;

    /** Time when this session was created. */
    @Nonnull private final Instant creationInstant;

    /** Last activity instant for this session. */
    @Nonnull private Instant lastActivityInstant;

    /** An IPv4 address to which the session is bound. */
    @Nullable private String ipV4Address;
    
    /** An IPv6 address to which the session is bound. */
    @Nullable private String ipV6Address;
    
    /** An "unknown" address to which the session is bound.  */
    @Nullable private String unknownAddress;
        
    /** Tracks authentication results that have occurred during this session. */
    @Nonnull private final ConcurrentMap<String,Optional<AuthenticationResult>> authenticationResults;

    /** Tracks services which have been issued authentication tokens during this session. */
    @Nonnull private final ConcurrentMap<String,Optional<SPSession>> spSessions;

    /**
     * Constructor.
     * 
     * @param sessionId identifier for this session
     * @param canonicalName canonical name of subject
     * @param creationTime creation time of session
     */
    public AbstractIdPSession(@Nonnull @NotEmpty final String sessionId, @Nonnull @NotEmpty final String canonicalName,
            @Nonnull final Instant creationTime) {
        id = Constraint.isNotNull(StringSupport.trimOrNull(sessionId), "Session ID cannot be null or empty");
        principalName = Constraint.isNotNull(StringSupport.trimOrNull(canonicalName),
                "Principal name cannot be null or empty.");
        
        creationInstant = Constraint.isNotNull(creationTime, "Creation time cannot be null");
        lastActivityInstant = creationTime;

        authenticationResults = new ConcurrentHashMap<>(5);
        spSessions = new ConcurrentHashMap<>(10);
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getId() {
        return id;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getPrincipalName() {
        return principalName;
    }

    /** {@inheritDoc} */
    @Nonnull public Instant getCreationInstant() {
        return creationInstant;
    }

    /** {@inheritDoc} */
    @Nonnull public Instant getLastActivityInstant() {
        return lastActivityInstant;
    }

    /**
     * Set the last activity instant for the session.
     * 
     * @param instant last activity instant for the session
     * @throws SessionException if an error occurs updating the session
     */
    public void setLastActivityInstant(@Nonnull final Instant instant) throws SessionException {
        doSetLastActivityInstant(instant);
    }
    
    /**
     * Set the last activity instant for the session.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #setLastActivityInstant(Instant)}
     * method must be overridden to support other persistence requirements.</p>
     * 
     * @param instant last activity instant for the session
     */
    public void doSetLastActivityInstant(@Nonnull final Instant instant) {
        lastActivityInstant = Constraint.isNotNull(instant, "Last activity instant cannot be null");
    }

    /** {@inheritDoc} */
    public boolean checkAddress(@Nonnull @NotEmpty final String address) throws SessionException {
        final AddressFamily family = getAddressFamily(address);
        final String bound = getAddress(family);
        if (bound != null) {
            if (!bound.equals(address)) {
                log.warn("Client address is {} but session {} already bound to {}", address, id, bound);
                return false;
            }
        } else {
            log.info("Session {} not yet locked to {} address, locking it to {}", id, family, address);
            try {
                bindToAddress(address);
            } catch (final SessionException e) {
                log.error("Unable to bind session {} to address {}", id, address);
                return false;
            }
        }
        
        return true;
    }

    /**
     * Get an address to which this session is bound.
     * 
     * @param family the address family to inquire
     * 
     * @return bound address or null
     */
    @Nullable public String getAddress(@Nonnull final AddressFamily family) {
        switch (family) {
            case IPV4:
                return ipV4Address;
            case IPV6:
                return ipV6Address;
            case UNKNOWN:
                return unknownAddress;
            default:
                return null;
        }
    }

    /**
     * Associate an address with this session.
     * 
     * @param address the address to associate
     * @throws SessionException if an error occurs binding the address to the session
     */
    public void bindToAddress(@Nonnull @NotEmpty final String address) throws SessionException {
        doBindToAddress(address);
    }

    /**
     * Associate an address with this session.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #bindToAddress(String)}
     * method must be overridden to support other persistence requirements.</p>
     * 
     * @param address the address to associate
     */
    public void doBindToAddress(@Nonnull @NotEmpty final String address) {
        final String trimmed = Constraint.isNotNull(StringSupport.trimOrNull(address),
                "Address cannot be null or empty");
        
        switch (getAddressFamily(address)) {
            case IPV6:
                ipV6Address = StringSupport.trimOrNull(trimmed);
                break;
                
            case IPV4:
                ipV4Address = StringSupport.trimOrNull(trimmed);
                break;
                
            case UNKNOWN:
                unknownAddress = StringSupport.trimOrNull(trimmed);
                break;
                
            default:
                log.warn("Unsupported address form {}", address);
        }
    }

    /** {@inheritDoc} */
    public boolean checkTimeout() throws SessionException {
        setLastActivityInstant(Instant.now());
        return true;
    }

    /** {@inheritDoc} */
    @Nonnull @NotLive @Unmodifiable public Set<AuthenticationResult> getAuthenticationResults() {
        return authenticationResults.values()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::orElseThrow)
                    .collect(CollectionSupport.nonnullCollector(Collectors.toUnmodifiableSet())).get();
    }

    /** {@inheritDoc} */
    @Nullable public AuthenticationResult getAuthenticationResult(@Nonnull @NotEmpty final String flowId) {
        final Optional<AuthenticationResult> mapped = authenticationResults.get(StringSupport.trimOrNull(flowId));
        return (mapped != null) ? mapped.orElse(null) : null;
    }

    /** {@inheritDoc} */
    @Nullable public AuthenticationResult addAuthenticationResult(@Nonnull final AuthenticationResult result)
            throws SessionException {
        return doAddAuthenticationResult(result);
    }

    /** {@inheritDoc} */
    public boolean removeAuthenticationResult(@Nonnull final AuthenticationResult result) throws SessionException {
        return doRemoveAuthenticationResult(result);
    }

    /**
     * Add a new {@link AuthenticationResult} to this IdP session, replacing any
     * existing result of the same flow ID.
     * 
     * <p>This manipulates only the internal state of the object. The
     * {@link #addAuthenticationResult(AuthenticationResult)} method must be implemented to support
     * other persistence requirements.</p>
     * 
     * @param result the result to add
     * 
     * @return a previously existing result replaced by the new one, if any
     */
    @Nullable public AuthenticationResult doAddAuthenticationResult(@Nonnull final AuthenticationResult result) {
        Constraint.isNotNull(result, "AuthenticationResult cannot be null");
    
        final Optional<AuthenticationResult> prev =
                authenticationResults.put(result.getAuthenticationFlowId(), Optional.of(result));
        if (prev != null && prev.isPresent()) {
            log.debug("IdPSession {}: replaced old AuthenticationResult for flow ID {}", id,
                    prev.get().getAuthenticationFlowId());
            return prev.get();
        }
        return null;
    }

    /**
     * Disassociate an {@link AuthenticationResult} from this IdP session.
     * 
     * <p>This manipulates only the internal state of the object. The
     * {@link #removeAuthenticationResult(AuthenticationResult)} method must be implemented to support
     * other persistence requirements.</p>
     * 
     * @param result the result to disassociate
     * 
     * @return true iff the given result had been associated with this IdP session and now is not
     */
    public boolean doRemoveAuthenticationResult(@Nonnull final AuthenticationResult result) {
        Constraint.isNotNull(result, "Authentication event can not be null");
    
        // Record may be actually present, or not yet loaded.
        if (authenticationResults.remove(result.getAuthenticationFlowId(), Optional.of(result))) {
            return true;
        }
        return authenticationResults.remove(result.getAuthenticationFlowId(), Optional.empty());
    }

    /** {@inheritDoc} */
    @Nonnull @NotLive @Unmodifiable public Set<SPSession> getSPSessions() {
        return spSessions.values()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::orElseThrow)
                    .collect(CollectionSupport.nonnullCollector(Collectors.toUnmodifiableSet())).get();
    }

    /** {@inheritDoc} */
    @Nullable public SPSession getSPSession(@Nonnull @NotEmpty final String serviceId) {
        final Optional<SPSession> mapped = spSessions.get(StringSupport.trimOrNull(serviceId));
        return (mapped != null) ? mapped.orElse(null) : null;
    }

    /** {@inheritDoc} */
    @Nullable public SPSession addSPSession(@Nonnull final SPSession spSession)
            throws SessionException {
        return doAddSPSession(spSession);
    }

    /** {@inheritDoc} */
    public boolean removeSPSession(@Nonnull final SPSession spSession) throws SessionException {
        return doRemoveSPSession(spSession);
    }

    /**
     * Add a new SP session to this IdP session, replacing any existing session for the same
     * service.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #addSPSession(SPSession)}
     * method must be implemented to support other persistence requirements.</p>
     * 
     * @param spSession the SP session
     * 
     * @return a previously existing SPSession replaced by the new one, if any
     */
    @Nullable public SPSession doAddSPSession(@Nonnull final SPSession spSession) {
        Constraint.isNotNull(spSession, "SPSession cannot be null");
    
        final Optional<SPSession> prev = spSessions.put(spSession.getId(), Optional.of(spSession));
        if (prev != null && prev.isPresent()) {
            log.debug("IdPSession {}: replaced old SPSession for service {}", id, prev.get().getId());
            return prev.get();
        }
        return null;
    }

    /**
     * Disassociate the given SP session from this IdP session.
     * 
     * <p>This manipulates only the internal state of the object. The {@link #removeSPSession(SPSession)}
     * method must be implemented to support other persistence requirements.</p>
     * 
     * @param spSession the SP session
     * 
     * @return true iff the given SP session had been associated with this IdP session and now is not
     */
    public boolean doRemoveSPSession(@Nonnull final SPSession spSession) {
        Constraint.isNotNull(spSession, "SPSession cannot be null");
    
        // Record may be actually present, or not yet loaded.
        if (spSessions.remove(spSession.getId(), Optional.of(spSession))) {
            return true;
        }
        return spSessions.remove(spSession.getId(), Optional.empty());
    }

    /** {@inheritDoc} */
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
    
        if (this == obj) {
            return true;
        }
    
        if (obj instanceof AbstractIdPSession) {
            return Objects.equals(getId(), ((AbstractIdPSession) obj).getId());
        }
    
        return false;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return MoreObjects.toStringHelper(this).add("sessionId", id).add("principalName", principalName)
                .add("IPv4", ipV4Address).add("IPv6", ipV6Address).add("Unk", unknownAddress)
                .add("creationInstant", creationInstant)
                .add("lastActivityInstant", lastActivityInstant)
                .add("authenticationResults", getAuthenticationResults()).add("spSessions", getSPSessions())
                .toString();
    }
    
    /**
     * Accessor for the underlying {@link AuthenticationResult} map maintained with the IdP session.
     * 
     * @return direct access to the result map
     */
    @Nonnull @Live protected Map<String, Optional<AuthenticationResult>> getAuthenticationResultMap() {
        return authenticationResults;
    }

    /**
     * Accessor for the underlying {@link SPSession} map maintained with the IdP session.
     * 
     * @return direct access to the service session map
     */
    @Nonnull @Live protected Map<String, Optional<SPSession>> getSPSessionMap() {
        return spSessions;
    }
    
    /**
     * Returns the address family for an input address.
     * 
     * @param address   the string to check
     * @return the address family
     */
    @Nonnull protected static AddressFamily getAddressFamily(@Nonnull @NotEmpty final String address) {
        if (address.contains(":")) {
            return AddressFamily.IPV6;
        } else if (address.contains(".")) {
            return AddressFamily.IPV4;
        } else {
            return AddressFamily.UNKNOWN;
        }
    }
    
}
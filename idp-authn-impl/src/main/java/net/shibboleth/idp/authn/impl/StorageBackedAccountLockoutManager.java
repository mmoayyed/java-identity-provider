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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageCapabilities;
import org.opensaml.storage.StorageCapabilitiesEx;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.AccountLockoutManager;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.LockoutManagerContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

/**
 * Implementation of {@link AccountLockoutManager} interface that relies on a {@link StorageService}
 * to track lockout state.
 */
public class StorageBackedAccountLockoutManager extends AbstractIdentifiableInitializableComponent
        implements AccountLockoutManager {
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(StorageBackedAccountLockoutManager.class);
    
    /** Backing service. */
    @NonnullAfterInit private StorageService storageService;

    /** Lookup function to produce account lockout keys. */
    @Nullable private Function<ProfileRequestContext,String> lockoutKeyStrategy;

    /** Lookup function for maximum failed attempts within window. */
    @Nonnull private Function<ProfileRequestContext,Integer> maxAttemptsLookupStrategy;

    /** Lookup function for interval after which counter is reset. */
    @Nonnull private Function<ProfileRequestContext,Duration> counterIntervalLookupStrategy;

    /** Lookup function for duration of lockout. */
    @Nonnull private Function<ProfileRequestContext,Duration> lockoutDurationLookupStrategy;
    
    /** Controls whether attempts against locked accounts extend duration. */
    private boolean extendLockoutDuration;
    
    /** Constructor. */
    public StorageBackedAccountLockoutManager() {
        setMaxAttempts(5);
        setCounterInterval(Duration.ofMinutes(5));
        setLockoutDuration(Duration.ofMinutes(5));
    }

    /**
     * Set the {@link StorageService} back-end to use.
     * 
     * @param storage the back-end to use
     */
    public void setStorageService(@Nonnull final StorageService storage) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        storageService = Constraint.isNotNull(storage, "StorageService cannot be null");
        final StorageCapabilities caps = storageService.getCapabilities();
        if (caps instanceof StorageCapabilitiesEx) {
            Constraint.isTrue(((StorageCapabilitiesEx) caps).isServerSide(), "StorageService cannot be client-side");
            if (!((StorageCapabilitiesEx) caps).isClustered()) {
                log.info("Use of non-clustered storage service will result in per-node lockout behavior");
            }
        }
    }

    /**
     * Set the strategy function to compute the account lockout key.
     * 
     * <p>Defaults to a concatenation of the username and client address.</p>
     * 
     * @param strategy strategy function
     */
    public void setLockoutKeyStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        lockoutKeyStrategy = Constraint.isNotNull(strategy, "Lockout key strategy cannot be null");
    }
    
    /**
     * Set the maximum failed attempts within window.
     * 
     * <p>Defaults to 5.</p>
     * 
     * @param attempts maximum failed attempts
     */
    public void setMaxAttempts(@Positive final int attempts) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        maxAttemptsLookupStrategy = FunctionSupport.constant(
                (int) Constraint.isGreaterThan(0, attempts, "Attempts must be greater than zero"));
    }
    
    /**
     * Set lookup function for maximum failed attempts within window.
     * 
     * <p>The function MUST return a positive value.</p>
     * 
     * @param strategy lookup function
     */
    public void setMaxAttemptsLookupStrategy(@Nonnull final Function<ProfileRequestContext,Integer> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        maxAttemptsLookupStrategy = Constraint.isNotNull(strategy, "Max attempts lookup strategy cannot be null");
    }
    
    /**
     * Set interval after which counter is reset.
     * 
     * <p>Defaults to 5 minutes.</p>
     * 
     * @param window counter window
     */
    public void setCounterInterval(@Nonnull final Duration window) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        counterIntervalLookupStrategy = FunctionSupport.constant(
                Constraint.isNotNull(window, "Counter interval cannot be null"));
    }
    
    /**
     * Set lookup function for interval after which counter is reset.
     * 
     * <p>The function MUST return a positive value.</p>
     * 
     * @param strategy lookup function
     */
    public void setCounterIntervalLookupStrategy(@Nonnull final Function<ProfileRequestContext,Duration> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        counterIntervalLookupStrategy = Constraint.isNotNull(strategy,
                "Counter interval lookup strategy cannot be null");
    }
    
    /**
     * Set lockout duration.
     * 
     * <p>Defaults to 5 minutes.</p>
     * 
     * @param duration lockout duration
     */
    public void setLockoutDuration(@Nonnull final Duration duration) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        lockoutDurationLookupStrategy = FunctionSupport.constant(
                Constraint.isNotNull(duration, "Lockout duration cannot be null"));
    }
    
    /**
     * Set lookup function for lockout duration.
     * 
     * <p>The function MUST return a positive value. Use a large value for permanent lockout.</p>
     * 
     * @param strategy lookup function
     */
    public void setLockoutDurationLookupStrategy(@Nonnull final Function<ProfileRequestContext,Duration> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        lockoutDurationLookupStrategy = Constraint.isNotNull(strategy,
                "Lockout duration lookup strategy cannot be null");
    }
    
    /**
     * Set whether to extend the lockout duration on attempts during lockout.
     * 
     * @param flag flag to set
     */
    public void setExtendLockoutDuration(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        extendLockoutDuration = flag;
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (storageService == null) {
            throw new ComponentInitializationException("StorageService cannot be null");
        } else if (lockoutKeyStrategy == null) {
            throw new ComponentInitializationException("Lockout key strategy cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    public boolean check(@Nonnull final ProfileRequestContext profileRequestContext) {
        final String key = lockoutKeyStrategy.apply(profileRequestContext);
        if (key == null) {
            log.warn("No lockout key returned for request");
            return false;
        }

        // Read back account state. No state obviously means no lockout, but in the case of errors
        // that does fail open. Of course, in-memory won't fail...
        StorageRecord sr = null;
        try {
            sr = storageService.read(getId(), key);
        } catch (final IOException e) {
            sr = null;
            log.error("Error reading back account lockout state for '{}'", key, e);
        }
        if (sr == null) {
            log.debug("No lockout record available for '{}'", key);
            return false;
        }
        
        try {
            // Read counter and check if we've exceeded the limit.
            final int counter = Integer.parseInt(sr.getValue());
            if (counter >= maxAttemptsLookupStrategy.apply(profileRequestContext)) {
                // Recover time of last attempt from the record expiration and find the time elapsed since.
                // If that's under the lockout duration, we're locked out.
                final long lockoutDuration = lockoutDurationLookupStrategy.apply(profileRequestContext).toMillis();
                final long counterInterval = counterIntervalLookupStrategy.apply(profileRequestContext).toMillis();
                final long lastAttempt = sr.getExpiration() - Math.max(lockoutDuration, counterInterval);
                final long timeDifference = System.currentTimeMillis() - lastAttempt;
                if (timeDifference <= lockoutDuration) {
                    log.info("Lockout threshold reached for '{}', invalid count is {}", key, counter);
                    if (extendLockoutDuration) {
                        doIncrement(profileRequestContext, key, 10);
                    }
                    return true;
                } else {
                    log.debug("Lockout for '{}' has elapsed", key);
                }
            } else {
                log.debug("Invalid attempts counter for '{}' has only reached {}", key, counter);
            }
        } catch (final NumberFormatException e) {
            log.error("Error converting lockout data for '{}' into integer", key, e);
        }

        return false;
    }

    /** {@inheritDoc} */
    public boolean increment(@Nonnull final ProfileRequestContext profileRequestContext) {
        // Work is done by helper method to track storage retries.
        final String key = lockoutKeyStrategy.apply(profileRequestContext);
        if (key == null) {
            log.warn("No lockout key returned for request");
            return false;
        }
        
        return doIncrement(profileRequestContext, key, 10);
    }

    /** {@inheritDoc} */
    public boolean clear(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            final String key = lockoutKeyStrategy.apply(profileRequestContext);
            if (key != null) {
                log.debug("Clearing lockout state for '{}'", key);
                storageService.delete(getId(), key);
                return true;
            } else {
                log.warn("No lockout key returned for request");
            }
        } catch (final IOException e) {
            log.error("Error deleting lockout entry", e);
        }
        return false;
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Implement invalid login attempt counter via storage service, retrying as necessary.
     * 
     * @param profileRequestContext current profile request context
     * @param key account lockout key
     * @param retries number of additional retries to allow
     * 
     * @return true iff successful
     */
    protected boolean doIncrement(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull @NotEmpty final String key, final int retries) {

        if (retries <= 0) {
            log.error("Account lockout increment attempts for '{}' exceeded retry limit", key);
            return false;
        }
        
        // Read back account record, initializing counter to zero otherwise.
        log.debug("Reading account lockout data for '{}'", key);
        
        int counter = 0;
        StorageRecord sr = null;
        try {
            sr = storageService.read(getId(), key);
            if (sr != null) {
                counter = Integer.parseInt(sr.getValue());
            }
        } catch (final IOException e) {
            sr = null;
            counter = 0;
            log.error("Error reading back account lockout state for '{}'", key, e);
        } catch (final NumberFormatException e) {
            sr = null;
            counter = 0;
            log.error("Error converting lockout data for '{}' into integer", key, e);
        }
        
        final long now = System.currentTimeMillis();
        final long lockoutDuration = lockoutDurationLookupStrategy.apply(profileRequestContext).toMillis();
        final long counterInterval = counterIntervalLookupStrategy.apply(profileRequestContext).toMillis();
        
        // Compute last access time by backing off from record expiration.
        long lastAccess = now;
        if (sr != null) {
            lastAccess = sr.getExpiration() - Math.max(lockoutDuration, counterInterval);
        }
        
        // If difference between now and last access exceeds the counter interval, zero it.
        if (now - lastAccess > counterInterval) {
            counter = 0;
        }
            
        // Increment, and set expiration to longer of the two settings to ensure it hangs around.
        ++counter;
        final long expiration = System.currentTimeMillis() + Math.max(lockoutDuration, counterInterval);

        log.debug("Invalid login count for '{}' will be {}, expiring at {}", key, counter,
                Instant.ofEpochMilli(expiration));

        // Create or update as required. Retry on errors.
        if (sr == null) {
            try {
                if (storageService.create(getId(), key, Integer.toString(counter), expiration)) {
                    return true;
                }
            } catch (final IOException e) {
                log.error("Unable to create account lockout record for '{}'", key, e);
            }
        } else {
            try {
                if (storageService.update(getId(), key, Integer.toString(counter), expiration)) {
                    return true;
                }
            } catch (final IOException e) {
                log.error("Unable to update account lockout record for '{}'", key, e);
            }
        }
        
        return doIncrement(profileRequestContext, key, retries-1);
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * A function to generate a key for lockout storage. This effectively defines
     * the scope of the lockout; e.g. if the key depends on the supplied username and
     * client IP address, the lockout will affect only attempts for that username
     * from that client IP.
     */
    public static class UsernameIPLockoutKeyStrategy implements Function<ProfileRequestContext,String> { 
        
        /** Servlet request to pull client ip from. **/
        @Nullable private HttpServletRequest httpRequest;
        
        /**
         * Set the servlet request to read from.
         * 
         * @param request servlet request
         */
        public void setHttpServletRequest(@Nonnull final HttpServletRequest request) {
            httpRequest = Constraint.isNotNull(request, "HttpServletRequest cannot be null");
        }

        /** {@inheritDoc} */
        @Nullable public String apply(@Nullable final ProfileRequestContext profileRequestContext) {
            if (profileRequestContext == null) {
                return null;
            }
            
            final LockoutManagerContext lockoutManagerContext =
                    profileRequestContext.getSubcontext(LockoutManagerContext.class);
            if (lockoutManagerContext != null) {
                return lockoutManagerContext.getKey();
            }

            if (httpRequest == null) {
                return null;
            }

            final AuthenticationContext authenticationContext =
                    profileRequestContext.getSubcontext(AuthenticationContext.class);
            if (authenticationContext == null) {
                return null;
            }
            
            final UsernamePasswordContext upContext =
                    authenticationContext.getSubcontext(UsernamePasswordContext.class);
            if (upContext == null) {
                return null;
            }
            
            final String username = upContext.getUsername();
            final String ipAddr = httpRequest.getRemoteAddr();
            if (username == null || username.isEmpty() || ipAddr == null || ipAddr.isEmpty()) {
                return null;
            }
        
            return username.toLowerCase() + '!' + ipAddr;
        }
    }

}
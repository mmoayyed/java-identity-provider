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

package net.shibboleth.idp.authn.revocation.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

import org.opensaml.messaging.context.ScratchContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.RevocationCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A condition for login flows that checks for revocation against a {@link RevocationCache}.
 * 
 * @since 4.3.0
 */
public class RevocationCacheCondition extends AbstractInitializableComponent
        implements BiPredicate<ProfileRequestContext,AuthenticationResult> {

    /** Revocation context. */
    @Nonnull @NotEmpty public static final String REVOCATION_CONTEXT = "LoginFlowRevocation";

    /** Prefix of keys for principal-based revocation. */
    @Nonnull @NotEmpty public static final String PRINCIPAL_REVOCATION_PREFIX = "prin!";

    /** Prefix of keys for address-based revocation. */
    @Nonnull @NotEmpty public static final String ADDRESS_REVOCATION_PREFIX = "addr!";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RevocationCacheCondition.class);
    
    /** Cache to use. */
    @NonnullAfterInit private RevocationCache revocationCache;
    
    /** Lookup strategy for principal name. */
    @NonnullAfterInit private Function<ProfileRequestContext,String> principalNameLookupStrategy;

    /** Servlet request Supplier. */
    @Nullable private Supplier<HttpServletRequest> httpServletRequestSupplier;
    
    /**
     * Set {@link RevocationCache} to use.
     * 
     * @param cache cache to use
     */
    public void setRevocationCache(@Nonnull final RevocationCache cache) {
        checkSetterPreconditions();        
        revocationCache = Constraint.isNotNull(cache, "RevocationCache cannot be null");
    }
    
    /**
     * Set lookup strategy for principal name.
     * 
     * @param strategy lookup strategy
     */
    public void setPrincipalNameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        
        principalNameLookupStrategy = Constraint.isNotNull(strategy, "Principal name lookup strategy cannot be null");
    }
    
    /**
     * Set {@link HttpServletRequest} in order to obtain client address.
     * 
     * @param supplier servlet request interface
     */
    public void setHttpServletRequestSupplier(@Nullable final Supplier<HttpServletRequest> supplier) {
        checkSetterPreconditions();
        httpServletRequestSupplier = supplier;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (revocationCache == null) {
            throw new ComponentInitializationException("RevocationCache cannot be null");
        } else if (principalNameLookupStrategy == null) {
            throw new ComponentInitializationException("Principal name lookup strategy cannot be null");
        }
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input,  @Nullable final AuthenticationResult input2) {
        checkComponentActive();
        
        if (input == null || input2 == null) {
            log.error("Called with null inputs");
            return true;
        }
        
        final String principal = principalNameLookupStrategy.apply(input);
        if (principal == null) {
            log.error("Principal lookup strategy returned null value");
            return true;
        }
        
        log.debug("Checking revocation for principal name {} for {} result", principal,
                input2.getAuthenticationFlowId());
        
        final ScratchContext context = input.getSubcontext(ScratchContext.class, true);
        if (!context.getMap().containsKey(getClass())) {
            try {
                final String principalRecord = revocationCache.getRevocationRecord(REVOCATION_CONTEXT,
                        PRINCIPAL_REVOCATION_PREFIX + principal);
                final HttpServletRequest request = httpServletRequestSupplier == null? null :
                    httpServletRequestSupplier.get();
                final String addressRecord = request != null ?
                        revocationCache.getRevocationRecord(REVOCATION_CONTEXT,
                                ADDRESS_REVOCATION_PREFIX + request.getRemoteAddr()) :
                            null;
                final Collection<String> records = new ArrayList<>(2);
                if (principalRecord != null) {
                    records.add(principalRecord);
                }
                if (addressRecord != null) {
                    records.add(addressRecord);
                }
                context.getMap().put(getClass(), records);
            } catch (final IOException e) {
                log.error("Error checking revocation cache for principal {}, treating as revoked",
                        principal, e);
                return true;
            }
        }
        
        return isRevoked(principal, input2, (Collection<String>) context.getMap().get(getClass()));
    }

    /**
     * Check the revocation records' timestamps for applicability.
     * 
     * @param principal name of principal
     * @param result active result being checked 
     * @param revocationRecords the records from the cache
     * 
     * @return true iff the revocation applies to this result
     */
    protected boolean isRevoked(@Nonnull @NotEmpty final String principal, @Nonnull final AuthenticationResult result,
            @Nonnull @NonnullElements final Collection<String> revocationRecords) {
        
        for (final String r : revocationRecords) {
            if (result.getAuthenticationInstant().isBefore(Instant.ofEpochSecond(Long.valueOf(r)))) {
                log.info("Authentication result {} for principal {} has been revoked", result.getAuthenticationFlowId(),
                        principal);
                return true;
            }
        }
        
        return false;
    }
    
}

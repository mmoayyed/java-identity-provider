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

package net.shibboleth.idp.authn.principal;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;

import com.google.common.base.MoreObjects;

/**
 * Principal that wraps a set of proxied authentication authorities and any restrictions
 * on subsequent re-use.
 * 
 * <p>The {@link Predicate} interface implements restriction logic that returns true iff
 * the restrictions embedded in the object do NOT apply to the input request.</p>
 * 
 * @since 3.4.0
 */
public class ProxyAuthenticationPrincipal implements Principal, Predicate<ProfileRequestContext> {

    /** The authorities. */
    @Nonnull @NonnullElements private Collection<String> authorities;

    /** The audiences. */
    @Nonnull @NonnullElements private Set<String> audiences;

    /** Constrains additional proxy hops. */
    @Nullable private Integer proxyCount;

    /** Constructor. */
    public ProxyAuthenticationPrincipal() {
        authorities = new ArrayList<>();
        audiences = new HashSet<>();
    }

    /**
     * Constructor.
     *
     * @param proxiedAuthorities initial set of authorities
     */
    public ProxyAuthenticationPrincipal(@Nonnull @NonnullElements final Collection<String> proxiedAuthorities) {
        Constraint.isNotNull(proxiedAuthorities, "Proxied authority collection cannot be null");
        
        authorities = new ArrayList<>(List.copyOf(proxiedAuthorities));
        audiences = new HashSet<>();
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getName() {
        return authorities.toString();
    }
    
    /**
     * Get the mutable authority collection.
     * 
     * @return the authorities
     */
    @Nonnull @NonnullElements @Live public Collection<String> getAuthorities() {
        return authorities;
    }

    /**
     * Get the mutable audience set, the set of relying parties for which proxying
     * is permissable.
     * 
     * @return the audiences
     */
    @Nonnull @NonnullElements @Live public Set<String> getAudiences() {
        return audiences;
    }
    
    /**
     * Gets the number of additional proxy hops that should be permitted.
     * 
     * <p>A value of 0 disallows further proxying, while a null implies no limit.</p>
     * 
     * @return proxy count
     * 
     * @since 4.0.0
     */
    @Nullable @NonNegative public Integer getProxyCount() {
        return proxyCount;
    }
    
    /**
     * Sets the number of additional proxy hops that should be permitted.
     * 
     * <p>A value of 0 disallows further proxying, while a null implies no limit.</p>
     * 
     * @param count proxy count
     * 
     * @since 4.0.0
     */
    public void setProxyCount(@Nullable @NonNegative final Integer count) {
        if (count != null) {
            proxyCount = Constraint.isGreaterThanOrEqual(0, count, "Proxy count cannot be negative");
        }
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        // Check for local flow as relying party.
        final RelyingPartyContext rpCtx = input != null ? input.getSubcontext(RelyingPartyContext.class) : null;
        if (rpCtx == null) {
            return true;
        }

        final ProfileConfiguration pc = rpCtx.getProfileConfig();
        if (!(pc instanceof AuthenticationProfileConfiguration)) {
            return true;
        } else if (((AuthenticationProfileConfiguration) pc).isLocal()) {
            return true;
        }

        if (proxyCount != null && proxyCount == 0) {
            return false;
        } else if (rpCtx.getRelyingPartyId() != null && !audiences.isEmpty() &&
                !audiences.contains(rpCtx.getRelyingPartyId())) {
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return authorities.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (other instanceof ProxyAuthenticationPrincipal) {
            return authorities.equals(((ProxyAuthenticationPrincipal) other).getAuthorities()) &&
                    Objects.equals(proxyCount, ((ProxyAuthenticationPrincipal) other).getProxyCount()) &&
                    audiences.equals(((ProxyAuthenticationPrincipal) other).getAudiences());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("authorities", authorities)
                .add("proxyCount", proxyCount)
                .add("audiences", audiences)
                .toString();
    }
    
}
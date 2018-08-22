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

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * Principal that wraps a set of proxied authentication authorities.
 * 
 * @since 3.4.0
 */
public class ProxyAuthenticationPrincipal implements Principal {

    /** The authorities. */
    @Nonnull @NonnullElements private Collection<String> authorities;

    /** Constructor. */
    public ProxyAuthenticationPrincipal() {
        authorities = new ArrayList<>();
    }

    /**
     * Constructor.
     *
     * @param proxiedAuthorities initial set of authorities
     */
    public ProxyAuthenticationPrincipal(@Nonnull @NonnullElements final Collection<String> proxiedAuthorities) {
        Constraint.isNotNull(proxiedAuthorities, "Proxied authority collection cannot be null");
        
        authorities = new ArrayList<>(Collections2.filter(proxiedAuthorities, Predicates.notNull()));
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
            return authorities.equals(((ProxyAuthenticationPrincipal) other).getAuthorities());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("authorities", authorities).toString();
    }
    
}
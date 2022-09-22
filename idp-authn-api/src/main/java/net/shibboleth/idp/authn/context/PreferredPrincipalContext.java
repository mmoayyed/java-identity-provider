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

package net.shibboleth.idp.authn.context;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;

/**
 * A context that holds information about an authentication request's
 * preference for a specific custom {@link Principal}.
 * 
 * <p>Authentication protocols with features for preferring specific forms of
 * authentication with optional semantics will populate this context type with
 * an expression of those preferences in the form of an ordered list of custom
 * {@link Principal} objects.</p>
 * 
 * @parent {@link AuthenticationContext}
 * @added Before the authentication process begins
 * 
 * @since 3.4.0
 */
public final class PreferredPrincipalContext extends BaseContext {

    /** The principals reflecting the preference. */
    @Nonnull @NonnullElements private List<Principal> preferredPrincipals;
    
    /** Constructor. */
    public PreferredPrincipalContext() {
        preferredPrincipals = Collections.emptyList();
    }

    /**
     * Get an immutable list of principals reflecting the request preferences.
     * 
     * @return  immutable list of principals 
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<Principal> getPreferredPrincipals() {
        return preferredPrincipals;
    }
    
    /**
     * Set list of principals reflecting the request preferences.
     * 
     * @param principals list of principals
     * 
     * @return this context
     */
    @Nonnull public PreferredPrincipalContext setPreferredPrincipals(
            @Nonnull @NonnullElements final List<Principal> principals) {
        
        preferredPrincipals = List.copyOf(Constraint.isNotNull(principals, "Principal list cannot be null"));
        return this;
    }
        
    /**
     * Helper method that evaluates a {@link PrincipalSupportingComponent} against
     * this context to determine if the input is compatible with it.
     * 
     * @param component component to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication preferences
     */
    public boolean isAcceptable(@Nonnull final PrincipalSupportingComponent component) {
        
        return !Collections.disjoint(preferredPrincipals, component.getSupportedPrincipals(Principal.class));
    }

    /**
     * Helper method that evaluates {@link Principal} objects against this context
     * to determine if the input is compatible with it.
     * 
     * @param principals principal(s) to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication preferences
     */
    public boolean isAcceptable(@Nonnull @NonnullElements final Collection<Principal> principals) {
        return !Collections.disjoint(preferredPrincipals, principals);
    }

    /**
     * Helper method that evaluates a {@link Principal} object against this context
     * to determine if the input is compatible with it.
     * 
     * @param <T> type of principal
     * @param principal principal to evaluate
     * 
     * @return true iff the input is compatible with the requested authentication preferences
     */
    public <T extends Principal> boolean isAcceptable(@Nonnull final T principal) {
        return preferredPrincipals.contains(principal);
    }

}
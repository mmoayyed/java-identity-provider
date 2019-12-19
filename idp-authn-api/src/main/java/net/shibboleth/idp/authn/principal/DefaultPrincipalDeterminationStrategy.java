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
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Function that returns the first custom {@link Principal} of a particular type found on the
 * {@link net.shibboleth.idp.authn.AuthenticationResult} returned by
 * {@link AuthenticationContext#getAuthenticationResult()}.
 * 
 * <p>
 * The context is located using a lookup strategy, by default a child of the input context.
 * </p>
 * 
 * <p>
 * If for any reason a matching Principal can't be located, a default is returned.
 * </p>
 * 
 * @param <T> the custom Principal type to locate
 */
public class DefaultPrincipalDeterminationStrategy<T extends Principal> implements Function<ProfileRequestContext,T> {

    /** Type of Principal to return. */
    @Nonnull private final Class<T> principalType;

    /** Default Principal to return. */
    @Nonnull private final T defaultPrincipal;

    /** Authentication context lookup strategy. */
    @Nonnull private Function<ProfileRequestContext,AuthenticationContext> authnContextLookupStrategy;

    /**
     * Constructor.
     * 
     * @param type class type for Principal type
     * @param principal default Principal to return
     */
    public DefaultPrincipalDeterminationStrategy(@Nonnull @ParameterName(name="type") final Class<T> type,
            @Nonnull  @ParameterName(name="principal") final T principal) {
        principalType = Constraint.isNotNull(type, "Class type cannot be null");
        defaultPrincipal = Constraint.isNotNull(principal, "Default Principal cannot be null");
        authnContextLookupStrategy = new ChildContextLookup<>(AuthenticationContext.class, false);
    }

    /**
     * Set lookup strategy for {@link AuthenticationContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setAuthenticationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AuthenticationContext> strategy) {
        authnContextLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Nullable public T apply(@Nullable final ProfileRequestContext input) {
        final AuthenticationContext ac = authnContextLookupStrategy.apply(input);
        if (ac == null || ac.getAuthenticationResult() == null) {
            return defaultPrincipal;
        }
        
        final AuthenticationFlowDescriptor descriptor = ac.getAvailableFlows().get(
                ac.getAuthenticationResult().getAuthenticationFlowId());
        if (descriptor == null) {
            return defaultPrincipal;
        }

        final Set<T> principals = ac.getAuthenticationResult().getSupportedPrincipals(principalType);
        if (principals.isEmpty()) {
            return defaultPrincipal;
        }
        return descriptor.getHighestWeighted(principals);
    }
    
}
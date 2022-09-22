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

package net.shibboleth.idp.cas.config;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicates;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;


/**
 * CAS protocol configuration that applies to the <code>/login</code> URI.
 *
 * @author Marvin S. Addison
 */
public class LoginConfiguration extends AbstractProtocolConfiguration
        implements AuthenticationProfileConfiguration {

    /** Proxy ticket profile URI. */
    @Nonnull @NotEmpty public static final String PROFILE_ID = PROTOCOL_URI + "/login";

    /** Default ticket prefix. */
    @Nonnull @NotEmpty public static final String DEFAULT_TICKET_PREFIX = "ST";

    /** Default ticket length (random part). */
    public static final int DEFAULT_TICKET_LENGTH = 25;

    /** Lookup function to supply authenticationFlows property. */
    @Nonnull private Function<ProfileRequestContext,Set<String>> authenticationFlowsLookupStrategy;

    /** Lookup function to supply postAuthenticationFlows property. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> postAuthenticationFlowsLookupStrategy;

    /** Lookup function to supply defaultAuthenticationContexts property. */
    @Nonnull private Function<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>>
            defaultAuthenticationContextsLookupStrategy;
    
    /** Whether to mandate forced authentication for the request. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;

    /** Whether to store consent in service tickets. */
    @Nonnull private Predicate<ProfileRequestContext> storeConsentInTicketsPredicate;

    /** Lookup function to supply proxyCount property. */
    @Nonnull private Function<ProfileRequestContext,Integer> proxyCountLookupStrategy;
    
    /** Creates a new instance. */
    public LoginConfiguration() {
        super(PROFILE_ID);
        
        authenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        postAuthenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        defaultAuthenticationContextsLookupStrategy = FunctionSupport.constant(null);
        forceAuthnPredicate = Predicates.alwaysFalse();
        storeConsentInTicketsPredicate = Predicates.alwaysFalse();
        proxyCountLookupStrategy = FunctionSupport.constant(null);
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<AuthnContextClassRefPrincipal> methods =
                defaultAuthenticationContextsLookupStrategy.apply(profileRequestContext);
        if (methods != null) {
            return List.copyOf(methods);
        }
        return Collections.emptyList();
    }
        
    /**
     * Set the default authentication contexts to use, expressed as custom principals.
     * 
     * @param contexts default authentication contexts to use
     */
    public void setDefaultAuthenticationMethods(
            @Nullable @NonnullElements final Collection<AuthnContextClassRefPrincipal> contexts) {
        if (contexts != null) {
            defaultAuthenticationContextsLookupStrategy = FunctionSupport.constant(List.copyOf(contexts));
        } else {
            defaultAuthenticationContextsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the default authentication contexts to use.
     *
     * @param strategy  lookup strategy
     */
    public void setDefaultAuthenticationMethodsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>> strategy) {
        defaultAuthenticationContextsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final Set<String> flows = authenticationFlowsLookupStrategy.apply(profileRequestContext);
        if (flows != null) {
            return Set.copyOf(flows);
        }
        return Collections.emptySet();
    }

    /**
     * Set the authentication flows to use.
     * 
     * @param flows   flow identifiers to use
     */
    public void setAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            authenticationFlowsLookupStrategy =
                    FunctionSupport.constant(Set.copyOf(StringSupport.normalizeStringCollection(flows)));
        } else {
            authenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the authentication flows to use.
     *
     * @param strategy  lookup strategy
     */
    public void setAuthenticationFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Set<String>> strategy) {
        authenticationFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getPostAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<String> flows = postAuthenticationFlowsLookupStrategy.apply(profileRequestContext);
        if (flows != null) {
            return List.copyOf(flows);
        }
        return Collections.emptyList();
    }

    /**
     * Set the ordered collection of post-authentication interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setPostAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            postAuthenticationFlowsLookupStrategy =
                    FunctionSupport.constant(List.copyOf(StringSupport.normalizeStringCollection(flows)));
        } else {
            postAuthenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the post-authentication interceptor flows to enable.
     *
     * @param strategy  lookup strategy
     */
    public void setPostAuthenticationFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        postAuthenticationFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean isForceAuthn(@Nullable final ProfileRequestContext profileRequestContext) {
        return forceAuthnPredicate.test(profileRequestContext);
    }

    /**
     * Set whether a fresh user presence proof should be required for this request.
     * 
     * @param flag flag to set
     */
    public void setForceAuthn(final boolean flag) {
        forceAuthnPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }
    
    /**
     * Set a condition to determine whether a fresh user presence proof should be required for this request.
     * 
     * @param condition condition to set
     */
    public void setForceAuthnPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        forceAuthnPredicate = Constraint.isNotNull(condition, "Forced authentication predicate cannot be null");
    }

    /**
     * Get whether to store consent in service tickets.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return whether to store consent in service tickets
     * 
     * @since 4.2.0
     */
    public boolean isStoreConsentInTickets(@Nullable final ProfileRequestContext profileRequestContext) {
        return storeConsentInTicketsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether to store consent in service tickets.
     * 
     * @param flag flag to set
     * 
     * @since 4.2.0
     */
    public void setStoreConsentInTickets(final boolean flag) {
        storeConsentInTicketsPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }

    /**
     * Set condition for whether to store consent in service tickets.
     * 
     * @param condition condition to set
     * 
     * @since 4.2.0
     */
    public void setStoreConsentInTicketsPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        storeConsentInTicketsPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nullable public Integer getProxyCount(@Nullable final ProfileRequestContext profileRequestContext) {
        final Integer count = proxyCountLookupStrategy.apply(profileRequestContext);
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        return count;
    }

    /**
     * Sets the maximum number of times an assertion may be proxied outbound and/or
     * the maximum number of hops between the relying party and a proxied authentication
     * authority inbound.
     * 
     * @param count proxy count
     * 
     * @since 4.0.0
     */
    public void setProxyCount(@Nullable @NonNegative final Integer count) {
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        proxyCountLookupStrategy = FunctionSupport.constant(count);
    }

    /**
     * Set a lookup strategy for the maximum number of times an assertion may be proxied outbound and/or
     * the maximum number of hops between the relying party and a proxied authentication authority inbound.
     *
     * @param strategy  lookup strategy
     * 
     * @since 4.0.0
     */
    public void setProxyCountLookupStrategy(@Nonnull final Function<ProfileRequestContext,Integer> strategy) {
        proxyCountLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getDefaultTicketPrefix() {
        return DEFAULT_TICKET_PREFIX;
    }

    /** {@inheritDoc} */
    @Override
    protected int getDefaultTicketLength() {
        return DEFAULT_TICKET_LENGTH;
    }
}

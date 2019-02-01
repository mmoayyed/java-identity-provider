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

package net.shibboleth.idp.cas.config.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * CAS protocol configuration that applies to the <code>/login</code> URI.
 *
 * @author Marvin S. Addison
 */
public class LoginConfiguration extends AbstractProtocolConfiguration
        implements AuthenticationProfileConfiguration {

    /** Proxy ticket profile URI. */
    public static final String PROFILE_ID = PROTOCOL_URI + "/login";

    /** Default ticket prefix. */
    public static final String DEFAULT_TICKET_PREFIX = "ST";

    /** Default ticket length (random part). */
    public static final int DEFAULT_TICKET_LENGTH = 25;

    /** Lookup function to supply {@link #authenticationFlows} property. */
    @Nullable private Function<ProfileRequestContext,Set<String>> authenticationFlowsLookupStrategy;
    
    /** Filters the usable authentication flows. */
    @Nonnull @NonnullElements private Set<String> authenticationFlows;

    /** Lookup function to supply {@link #postAuthenticationFlows} property. */
    @Nullable private Function<ProfileRequestContext,Collection<String>> postAuthenticationFlowsLookupStrategy;

    /** Enables post-authentication interceptor flows. */
    @Nonnull @NonnullElements private List<String> postAuthenticationFlows;

    /** Lookup function to supply {@link #defaultAuthenticationContexts} property. */
    @Nullable private Function<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>>
            defaultAuthenticationContextsLookupStrategy;
    
    /** Selects, and limits, the authentication contexts to use for requests. */
    @Nonnull @NonnullElements private List<AuthnContextClassRefPrincipal> defaultAuthenticationContexts;

    /** Lookup function to supply {@link #nameIDFormatPrecedence} property. */
    @Nullable private Function<ProfileRequestContext,Collection<String>> nameIDFormatPrecedenceLookupStrategy;

    /** Precedence of name identifier formats to use for requests. */
    @Nonnull @NonnullElements private List<String> nameIDFormatPrecedence;
    
    /** Whether to mandate forced authentication for the request. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;
    
    /** Creates a new instance. */
    public LoginConfiguration() {
        super(PROFILE_ID);
        // Service tickets valid for 15s by default
        setTicketValidityPeriod(15000);
        authenticationFlows = Collections.emptySet();
        postAuthenticationFlows = Collections.emptyList();
        defaultAuthenticationContexts = Collections.emptyList();
        nameIDFormatPrecedence = Collections.emptyList();
        forceAuthnPredicate = Predicates.alwaysFalse();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods() {
        return ImmutableList.<Principal>copyOf(getIndirectProperty(defaultAuthenticationContextsLookupStrategy,
                defaultAuthenticationContexts));
    }
        
    /**
     * Set the default authentication contexts to use, expressed as custom principals.
     * 
     * @param contexts default authentication contexts to use
     */
    public void setDefaultAuthenticationMethods(
            @Nullable @NonnullElements final Collection<AuthnContextClassRefPrincipal> contexts) {
        if (contexts != null) {
            defaultAuthenticationContexts = new ArrayList<>(Collections2.filter(contexts, Predicates.notNull()));
        } else {
            defaultAuthenticationContexts = Collections.emptyList();
        }
    }
    
    /**
     * Set a lookup strategy for the {@link #defaultAuthenticationContexts} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setDefaultAuthenticationMethodsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>> strategy) {
        defaultAuthenticationContextsLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getAuthenticationFlows() {
        return ImmutableSet.copyOf(getIndirectProperty(authenticationFlowsLookupStrategy, authenticationFlows));
    }

    /**
     * Set the authentication flows to use.
     * 
     * @param flows   flow identifiers to use
     */
    public void setAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {

        if (flows != null) {
            authenticationFlows = new HashSet<>(StringSupport.normalizeStringCollection(flows));
        } else {
            authenticationFlows = Collections.emptySet();
        }
    }

    /**
     * Set a lookup strategy for the {@link #authenticationFlows} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setAuthenticationFlowsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Set<String>> strategy) {
        authenticationFlowsLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getPostAuthenticationFlows() {
        return ImmutableList.copyOf(
                getIndirectProperty(postAuthenticationFlowsLookupStrategy, postAuthenticationFlows));
    }

    /**
     * Set the ordered collection of post-authentication interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setPostAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {

        if (flows != null) {
            postAuthenticationFlows = new ArrayList<>(StringSupport.normalizeStringCollection(flows));
        } else {
            postAuthenticationFlows = Collections.emptyList();
        }
    }

    /**
     * Set a lookup strategy for the {@link #postAuthenticationFlows} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setPostAuthenticationFlowsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<String>> strategy) {
        postAuthenticationFlowsLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getNameIDFormatPrecedence() {
        return ImmutableList.copyOf(getIndirectProperty(nameIDFormatPrecedenceLookupStrategy, nameIDFormatPrecedence));
    }

    /**
     * Set the name identifier formats to use.
     * 
     * @param formats   name identifier formats to use
     */
    public void setNameIDFormatPrecedence(@Nonnull @NonnullElements final Collection<String> formats) {
        Constraint.isNotNull(formats, "List of formats cannot be null");
        
        nameIDFormatPrecedence = new ArrayList<>(StringSupport.normalizeStringCollection(formats));
    }

    /**
     * Set a lookup strategy for the {@link #nameIDFormatPrecedence} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setNameIDFormatPrecedenceLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<String>> strategy) {
        nameIDFormatPrecedenceLookupStrategy = strategy;
    }

    /**
     * Get a condition to determine whether a fresh user presence proof should be required for this request.
     * 
     * @return condition
     * 
     * @since 3.4.0
     */
    @Nonnull public Predicate<ProfileRequestContext> getForceAuthnPredicate() {
        return forceAuthnPredicate;
    }
    
    /**
     * Set a condition to determine whether a fresh user presence proof should be required for this request.
     * 
     * @param condition condition to set
     * 
     * @since 3.4.0
     */
    public void setForceAuthnPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        forceAuthnPredicate = Constraint.isNotNull(condition, "Forced authentication predicate cannot be null");
    }
    
    /**
     * Set whether a fresh user presence proof should be required for this request.
     * 
     * @param flag flag to set
     * 
     * @since 3.4.0
     */
    public void setForceAuthn(final boolean flag) {
        forceAuthnPredicate = flag ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
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

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

package net.shibboleth.idp.saml.saml1.profile.config;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Configuration for SAML 1 Browser SSO profile requests. */
public class BrowserSSOProfileConfiguration extends AbstractSAML1ArtifactAwareProfileConfiguration
        implements AuthenticationProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/saml1/sso/browser";

    /** Whether attributes should be resolved in the course of the profile. */
    @Nonnull private Predicate<ProfileRequestContext> resolveAttributesPredicate;

    /** Whether responses to the authentication request should include an attribute statement. */
    @Nonnull private Predicate<ProfileRequestContext> includeAttributeStatementPredicate;
    
    /** Whether to mandate forced authentication for the request. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;

    /** Lookup function to supply {@link #defaultAuthenticationMethods} property. */
    @Nullable private Function<ProfileRequestContext,Collection<AuthenticationMethodPrincipal>>
            defaultAuthenticationMethodsLookupStrategy;

    /** Selects, and limits, the authentication methods to use for requests. */
    @Nonnull @NonnullElements private List<AuthenticationMethodPrincipal> defaultAuthenticationMethods;

    /** Lookup function to supply {@link #authenticationFlows} property. */
    @Nullable private Function<ProfileRequestContext,Set<String>> authenticationFlowsLookupStrategy;

    /** Filters the usable authentication flows. */
    @Nonnull @NonnullElements private Set<String> authenticationFlows;

    /** Lookup function to supply {@link #postAuthenticationFlows} property. */
    @Nullable private Function<ProfileRequestContext,Collection<String>> postAuthenticationFlowsLookupStrategy;

    /** Enables post-authentication interceptor flows. */
    @Nonnull @NonnullElements private List<String> postAuthenticationFlows;

    /** Lookup function to supply {@link #nameIDFormatPrecedence} property. */
    @Nullable private Function<ProfileRequestContext,Collection<String>> nameIDFormatPrecedenceLookupStrategy;

    /** Precedence of name identifier formats to use for requests. */
    @Nonnull @NonnullElements private List<String> nameIDFormatPrecedence;
    
    /** Constructor. */
    public BrowserSSOProfileConfiguration() {
        this(PROFILE_ID);
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected BrowserSSOProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        setSignResponses(Predicates.<ProfileRequestContext>alwaysTrue());
        resolveAttributesPredicate = Predicates.alwaysTrue();
        includeAttributeStatementPredicate = Predicates.alwaysFalse();
        forceAuthnPredicate = Predicates.alwaysFalse();
        defaultAuthenticationMethods = Collections.emptyList();
        authenticationFlows = Collections.emptySet();
        postAuthenticationFlows = Collections.emptyList();
        nameIDFormatPrecedence = Collections.emptyList();
    }

    /**
     * Get whether attributes should be resolved during the profile.
     *
     * <p>Default is true</p>
     * 
     * @return true iff attributes should be resolved
     * 
     * @deprecated Use {@link #getResolveAttributesPredicate()} instead.
     */
    public boolean resolveAttributes() {
        return resolveAttributesPredicate.apply(getProfileRequestContext());
    }
    
    /**
     * Set whether attributes should be resolved during the profile.
     * 
     * @param flag flag to set
     */
    public void setResolveAttributes(final boolean flag) {
        resolveAttributesPredicate = flag ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
    }

    /**
     * Get a condition to determine whether attributes should be resolved during the profile.
     * 
     * @return condition
     * 
     * @since 3.3.0
     */
    @Nonnull public Predicate<ProfileRequestContext> getResolveAttributesPredicate() {
        return resolveAttributesPredicate;
    }
    
    /**
     * Set a condition to determine whether attributes should be resolved during the profile.
     *
     * @param condition  condition to set
     * 
     * @since 3.3.0
     */
    public void setResolveAttributesPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        resolveAttributesPredicate = Constraint.isNotNull(condition, "Resolve attributes predicate cannot be null");
    }

    /**
     * Get whether responses to the authentication request should include an attribute statement.
     *
     * <p>Default is true</p>
     *
     * @return whether responses to the authentication request should include an attribute statement
     * 
     * @deprecated Use {@link #getIncludeAttributeStatementPredicate()} instead.
     */
    public boolean includeAttributeStatement() {
        return includeAttributeStatementPredicate.apply(getProfileRequestContext());
    }

    /**
     * Set whether responses to the authentication request should include an attribute statement.
     * 
     * @param include flag to set
     */
    public void setIncludeAttributeStatement(final boolean include) {
        includeAttributeStatementPredicate = include ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
    }

    /**
     * Get a condition to determine whether responses to the authentication request should include an
     * attribute statement.
     * 
     * @return condition
     * 
     * @since 3.3.0
     */
    @Nonnull public Predicate<ProfileRequestContext> getIncludeAttributeStatementPredicate() {
        return includeAttributeStatementPredicate;
    }
    
    /**
     * Set a condition to determine whether responses to the authentication request should include an
     * attribute statement.
     *
     * @param condition  condition to set
     * 
     * @since 3.3.0
     */
    public void setIncludeAttributeStatementPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        includeAttributeStatementPredicate = Constraint.isNotNull(condition,
                "Include attribute statement predicate cannot be null");
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
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods() {
        return ImmutableList.<Principal>copyOf(
                getIndirectProperty(defaultAuthenticationMethodsLookupStrategy, defaultAuthenticationMethods));
    }
    
    /**
     * Set the default authentication methods to use, expressed as custom principals.
     * 
     * @param methods   default authentication methods to use
     */
    public void setDefaultAuthenticationMethods(
            @Nullable @NonnullElements final Collection<AuthenticationMethodPrincipal> methods) {

        if (methods != null) {
            defaultAuthenticationMethods = new ArrayList<>(Collections2.filter(methods, Predicates.notNull()));
        } else {
            defaultAuthenticationMethods = Collections.emptyList();
        }
    }

    /**
     * Set a lookup strategy for the {@link #defaultAuthenticationMethods} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setDefaultAuthenticationMethodsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<AuthenticationMethodPrincipal>> strategy) {
        defaultAuthenticationMethodsLookupStrategy = strategy;
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

}
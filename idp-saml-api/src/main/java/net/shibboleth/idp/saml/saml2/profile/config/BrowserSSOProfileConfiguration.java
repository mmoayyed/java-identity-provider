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

package net.shibboleth.idp.saml.saml2.profile.config;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactAwareProfileConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Configuration support for SAML 2 Browser SSO. */
public class BrowserSSOProfileConfiguration extends AbstractSAML2ProfileConfiguration
        implements SAMLArtifactAwareProfileConfiguration, AuthenticationProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/saml2/sso/browser";

    /** Lookup function to supply {@link #artifactConfig} property. */
    @Nullable private Function<ProfileRequestContext,SAMLArtifactConfiguration> artifactConfigurationLookupStrategy;

    /** SAML artifact configuration. */
    @Nullable private SAMLArtifactConfiguration artifactConfig;
    
    /** Whether attributes should be resolved in the course of the profile. */
    @Nonnull private Predicate<ProfileRequestContext> resolveAttributesPredicate;

    /** Whether responses to the authentication request should include an attribute statement. */
    @Nonnull private Predicate<ProfileRequestContext> includeAttributeStatementPredicate;

    /** Whether the response endpoint should be validated if the request is signed. */
    @Nonnull private Predicate<ProfileRequestContext> skipEndpointValidationWhenSignedPredicate;

    /** Lookup function to supply {@link #maximumSPSessionLifetime} property. */
    @Nullable private Function<ProfileRequestContext,Long> maximumSPSessionLifetimeLookupStrategy;
    
    /**
     * The maximum amount of time, in milliseconds, the service provider should maintain a session for the user. A value
     * of 0 (the default) indicates no cap is put on the SP's session lifetime.
     */
    @Duration @NonNegative private long maximumSPSessionLifetime;

    /** 
     * The predicate used to determine if produced assertions may be delegated.
     */
    @Nonnull private Predicate<ProfileRequestContext> allowDelegationPredicate;
    
    /** Lookup function to supply {@link #maximumTokenDelegationChainLength} property. */
    @Nullable private Function<ProfileRequestContext,Long> maximumTokenDelegationChainLengthLookupStrategy;
    
    /** Limits the total number of delegates that may be derived from the initial SAML token. Default value: 1. */
    @NonNegative private long maximumTokenDelegationChainLength;

    /** Lookup function to supply {@link #defaultAuthenticationContexts} property. */
    @Nullable private Function<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>>
            defaultAuthenticationContextsLookupStrategy;
    
    /** Selects, and limits, the authentication contexts to use for requests. */
    @Nonnull @NonnullElements private List<AuthnContextClassRefPrincipal> defaultAuthenticationContexts;

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
        setEncryptAssertions(Predicates.<ProfileRequestContext>alwaysTrue());
        resolveAttributesPredicate = Predicates.alwaysTrue();
        includeAttributeStatementPredicate = Predicates.alwaysTrue();
        skipEndpointValidationWhenSignedPredicate = Predicates.alwaysFalse();
        maximumSPSessionLifetime = 0;
        maximumTokenDelegationChainLength = 1;
        allowDelegationPredicate = Predicates.<ProfileRequestContext>alwaysFalse();
        defaultAuthenticationContexts = Collections.emptyList();
        authenticationFlows = Collections.emptySet();
        postAuthenticationFlows = Collections.emptyList();
        nameIDFormatPrecedence = Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override @Nullable public SAMLArtifactConfiguration getArtifactConfiguration() {
        return getIndirectProperty(artifactConfigurationLookupStrategy, artifactConfig);
    }

    /**
     * Set the SAML artifact configuration, if any.
     * 
     * @param config configuration to set
     */
    public void setArtifactConfiguration(@Nullable final SAMLArtifactConfiguration config) {
        artifactConfig = config;
    }

    /**
     * Set a lookup strategy for the {@link #artifactConfig} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setArtifactConfigurationLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLArtifactConfiguration> strategy) {
        artifactConfigurationLookupStrategy = strategy;
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
    @Deprecated
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
    @Deprecated
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
     * Get whether the response endpoint should be validated if the request is signed.
     * 
     * @return whether the response endpoint should be validated if the request is signed
     * 
     * @deprecated Use {@link #getSkipEndpointValidationWhenSignedPredicate()} instead.
     */
    @Deprecated
    public boolean skipEndpointValidationWhenSigned() {
        return skipEndpointValidationWhenSignedPredicate.apply(getProfileRequestContext());
    }

    /**
     * Set whether the response endpoint should be validated if the request is signed.
     * 
     * @param skip whether the response endpoint should be validated if the request is signed
     */
    public void setSkipEndpointValidationWhenSigned(final boolean skip) {
        skipEndpointValidationWhenSignedPredicate = skip ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
    }
    
    /**
     * Get condition to determine whether the response endpoint should be validated if the request is signed.
     * 
     * @return condition
     * 
     * @since 3.3.0
     */
    @Nonnull public Predicate<ProfileRequestContext> getSkipEndpointValidationWhenSignedPredicate() {
        return skipEndpointValidationWhenSignedPredicate;
    }

    /**
     * Set condition to determine whether the response endpoint should be validated if the request is signed.
     * 
     * @param condition condition to set
     * 
     * @since 3.3.0
     */
    public void setSkipEndpointValidationWhenSignedPredicate(
            @Nonnull final Predicate<ProfileRequestContext> condition) {
        skipEndpointValidationWhenSignedPredicate = Constraint.isNotNull(condition,
                "Skip endpoint validation predicate cannot be null");
    }

    /**
     * Get the maximum amount of time, in milliseconds, the service provider should maintain a session for the user
     * based on the authentication assertion. A value of 0 is interpreted as an unlimited lifetime.
     * 
     * @return max lifetime of service provider should maintain a session
     */
    @NonNegative @Duration public long getMaximumSPSessionLifetime() {
        return Constraint.isGreaterThanOrEqual(0,
                getIndirectProperty(maximumSPSessionLifetimeLookupStrategy, maximumSPSessionLifetime),
                "Maximum SP session lifetime must be greater or equal to 0");
    }

    /**
     * Set the maximum amount of time, in milliseconds, the service provider should maintain a session for the user
     * based on the authentication assertion. A value of 0 is interpreted as an unlimited lifetime.
     * 
     * @param lifetime max lifetime of service provider should maintain a session
     */
    @Duration public void setMaximumSPSessionLifetime(@Duration @NonNegative final long lifetime) {
        maximumSPSessionLifetime =
                Constraint.isGreaterThanOrEqual(0, lifetime,
                        "Maximum SP session lifetime must be greater than or equal to 0");
    }
    
    /**
     * Set a lookup strategy for the {@link #maximumSPSessionLifetime} property.
     * 
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setMaximumSPSessionLifetimeLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Long> strategy) {
        maximumSPSessionLifetimeLookupStrategy = strategy;
    }

    /**
     * Get whether produced assertions may be delegated.
     * 
     * @return whether produced assertions may be delegated, as a {@link Boolean}.  May be null.
     * 
     * @deprecated use instead {@link #getAllowDelegation()} predicate
     */
    @Deprecated
    public Boolean getAllowingDelegation() {
        return allowDelegationPredicate.apply(getProfileRequestContext());
    }

    /**
     * Set whether produced assertions may be delegated.
     * 
     * @param isAllowed whether produced assertions may be delegated
     */
    @Deprecated
    public void setAllowingDelegation(final Boolean isAllowed) {
        if (isAllowed != null) {
            allowDelegationPredicate = isAllowed ? Predicates.<ProfileRequestContext>alwaysTrue()
                    : Predicates.<ProfileRequestContext>alwaysFalse();
        } else {
            allowDelegationPredicate = Predicates.alwaysFalse();
        }
    }

    /**
     * Get whether produced assertions may be delegated.
     * 
     * @return whether produced assertions may be delegated
     */
    public boolean isAllowingDelegation() {
        return allowDelegationPredicate.apply(getProfileRequestContext());
    }
    
    
    /**
     * Get the predicate used to determine if produced assertions may be delegated.
     * 
     * @return predicate used to determine if produced assertions may be delegated
     */

    @Nonnull public Predicate<ProfileRequestContext> getAllowDelegation() {
        return allowDelegationPredicate;
    }

    /**
     * Set the predicate used to determine if produced assertions may be delegated.
     * 
     * @param  predicate used to determine if produced assertions may be delegated
     */

    public void setAllowDelegation(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        allowDelegationPredicate = Constraint.isNotNull(predicate, "Allow delegation predicate cannot be null");
    }

    /**
     * Get the limits on the total number of delegates that may be derived from the initial SAML token.
     * 
     * @return the limit on the total number of delegates that may be derived from the initial SAML token
     */
    @NonNegative public long getMaximumTokenDelegationChainLength() {
        return Constraint.isGreaterThanOrEqual(0,
                getIndirectProperty(maximumTokenDelegationChainLengthLookupStrategy, maximumTokenDelegationChainLength),
                "Delegation chain length must be greater than or equal to 0");
    }

    /**
     * Set the limits on the total number of delegates that may be derived from the initial SAML token.
     * 
     * @param length the limit on the total number of delegates that may be derived from the initial SAML token
     */
    public void setMaximumTokenDelegationChainLength(@NonNegative final long length) {
        maximumTokenDelegationChainLength = Constraint.isGreaterThanOrEqual(0, length,
                "Delegation chain length must be greater than or equal to 0");
    }
    
    /**
     * Set a lookup strategy for the {@link #maximumTokenDelegationChainLength} property.
     * 
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setMaximumTokenDelegationChainLengthLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Long> strategy) {
        maximumTokenDelegationChainLengthLookupStrategy = strategy;
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
     * Set a lookup strategy for the {@link #defaultAuthenticationMethods} property.
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

}
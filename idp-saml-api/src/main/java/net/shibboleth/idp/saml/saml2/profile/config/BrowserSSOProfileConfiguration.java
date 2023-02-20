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
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.profile.config.logic.ProxyAwareForceAuthnPredicate;
import net.shibboleth.idp.saml.saml2.profile.config.navigate.ProxyAwareAuthnContextComparisonLookupFunction;
import net.shibboleth.idp.saml.saml2.profile.config.navigate.ProxyAwareDefaultAuthenticationMethodsLookupFunction;
import net.shibboleth.profile.config.AttributeResolvingProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.SubjectLocality;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;

/** Configuration support for IdP and proxied SAML 2.0 Browser SSO. */
public class BrowserSSOProfileConfiguration extends AbstractSAML2AssertionProducingProfileConfiguration
        implements AuthenticationProfileConfiguration, AttributeResolvingProfileConfiguration,
            net.shibboleth.saml.saml2.profile.config.BrowserSSOProfileConfiguration{
    
    /** Default maximum delegation chain length. */
    @Nonnull public static final Long DEFAULT_DELEGATION_CHAIN_LENGTH = 1L;
    
    /** Whether attributes should be resolved in the course of the profile. */
    @Nonnull private Predicate<ProfileRequestContext> resolveAttributesPredicate;

    /** Whether responses to the authentication request should include an attribute statement. */
    @Nonnull private Predicate<ProfileRequestContext> includeAttributeStatementPredicate;

    /** Whether to ignore Scoping elements within AuthnRequest. */
    @Nonnull private Predicate<ProfileRequestContext> ignoreScoping;
    
    /** Whether to mandate forced authentication for the request. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;

    /** Whether to compare client and assertion addresses on inbound SSO. */
    @Nonnull private Predicate<ProfileRequestContext> checkAddressPredicate;

    /** Whether the response endpoint should be validated if the request is signed. */
    @Nonnull private Predicate<ProfileRequestContext> skipEndpointValidationWhenSignedPredicate;

    /** Lookup function to supply proxyCount property. */
    @Nonnull private Function<ProfileRequestContext,Integer> proxyCountLookupStrategy;

    /** Lookup function to supply proxy audiences. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> proxyAudiencesLookupStrategy;
    
    /** Whether authentication results should carry the proxied AuthnInstant. */
    @Nonnull private Predicate<ProfileRequestContext> proxiedAuthnInstantPredicate;

    /** 
     * The predicate used to determine whether to suppress {@link AuthenticatingAuthority} when possible.
     */
    @Nonnull private Predicate<ProfileRequestContext> suppressAuthenticatingAuthorityPredicate;
    
    /** Whether to require requests be signed. */
    @Nonnull private Predicate<ProfileRequestContext> requireSignedRequestsPredicate;

    /** Whether to require assertions be signed. */
    @Nonnull private Predicate<ProfileRequestContext> requireSignedAssertionsPredicate;

    /** Lookup function to supply maximum session lifetime. */
    @Nonnull private Function<ProfileRequestContext,Duration> maximumSPSessionLifetimeLookupStrategy;

    /** Lookup function to supply maximum time since inbound AuthnInstant. */
    @Nonnull private Function<ProfileRequestContext,Duration> maximumTimeSinceAuthnLookupStrategy;

    /** 
     * The predicate used to determine if produced assertions may be delegated.
     */
    @Nonnull private Predicate<ProfileRequestContext> allowDelegationPredicate;
    
    /** Lookup function to supply maximum delegation chain length. */
    @Nonnull private Function<ProfileRequestContext,Long> maximumTokenDelegationChainLengthLookupStrategy;

    /** Lookup function to supply the strategy function for translating SAML 2.0 AuthnContext data. */
    @Nonnull private Function<ProfileRequestContext,Function<AuthnContext,Collection<Principal>>>
        authnContextTranslationStrategyLookupStrategy;

    /** Lookup function to supply the strategy function for translating fully-generic data. */
    @Nonnull private Function<ProfileRequestContext,Function<ProfileRequestContext,Collection<Principal>>>
        authnContextTranslationStrategyExLookupStrategy;

    /** Lookup function for requested AC operator. */
    @Nonnull private Function<ProfileRequestContext,String> authnContextComparisonLookupStrategy;
    
    /** Lookup function to supply default authentication methods. */
    @Nonnull private Function<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>>
            defaultAuthenticationContextsLookupStrategy;
    
    /** Lookup function to supply authentication flows. */
    @Nonnull private Function<ProfileRequestContext,Set<String>> authenticationFlowsLookupStrategy;
    
    /** Lookup function to supply post authentication flows. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> postAuthenticationFlowsLookupStrategy;
    
    /** Lookup function to supply NameID formats. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> nameIDFormatPrecedenceLookupStrategy;

    /** Lookup function to supply SPNameQualifier in request. */
    @Nonnull private Function<ProfileRequestContext,String> spNameQualifierLookupStrategy;

    /** Lookup function to supply AttributeConsumingServiceIndex in request. */
    @Nonnull private Function<ProfileRequestContext,String> attributeIndexLookupStrategy;
    
    /** Lookup function to supply RequestedAttributes in request. */
    @Nonnull private Function<ProfileRequestContext,Collection<RequestedAttribute>> requestedAttributesLookupStrategy;

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
        setSignResponses(true);
        setEncryptAssertions(true);
        resolveAttributesPredicate = PredicateSupport.alwaysTrue();
        includeAttributeStatementPredicate = PredicateSupport.alwaysTrue();
        ignoreScoping = PredicateSupport.alwaysFalse();
        forceAuthnPredicate = new ProxyAwareForceAuthnPredicate();
        checkAddressPredicate = PredicateSupport.alwaysTrue();
        skipEndpointValidationWhenSignedPredicate = PredicateSupport.alwaysFalse();
        proxyCountLookupStrategy = FunctionSupport.constant(null);
        proxyAudiencesLookupStrategy = FunctionSupport.constant(null);
        proxiedAuthnInstantPredicate = PredicateSupport.alwaysTrue();
        suppressAuthenticatingAuthorityPredicate = PredicateSupport.alwaysFalse();
        requireSignedRequestsPredicate = PredicateSupport.alwaysFalse();
        requireSignedAssertionsPredicate = PredicateSupport.alwaysFalse();
        maximumSPSessionLifetimeLookupStrategy = FunctionSupport.constant(null);
        maximumTimeSinceAuthnLookupStrategy = FunctionSupport.constant(null);
        maximumTokenDelegationChainLengthLookupStrategy = FunctionSupport.constant(DEFAULT_DELEGATION_CHAIN_LENGTH);
        allowDelegationPredicate = PredicateSupport.alwaysFalse();
        authenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        postAuthenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        authnContextTranslationStrategyLookupStrategy = FunctionSupport.constant(null);
        authnContextTranslationStrategyExLookupStrategy = FunctionSupport.constant(null);
        authnContextComparisonLookupStrategy = new ProxyAwareAuthnContextComparisonLookupFunction();
        defaultAuthenticationContextsLookupStrategy = new ProxyAwareDefaultAuthenticationMethodsLookupFunction();
        nameIDFormatPrecedenceLookupStrategy = FunctionSupport.constant(null);
        spNameQualifierLookupStrategy = FunctionSupport.constant(null);
        attributeIndexLookupStrategy = FunctionSupport.constant(null);
        requestedAttributesLookupStrategy = FunctionSupport.constant(null);
    }
    
    /** {@inheritDoc} */
    public boolean isResolveAttributes(@Nullable final ProfileRequestContext profileRequestContext) {
        return resolveAttributesPredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether attributes should be resolved during the profile.
     * 
     * @param flag flag to set
     */
    public void setResolveAttributes(final boolean flag) {
        resolveAttributesPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set a condition to determine whether attributes should be resolved during the profile.
     * 
     * @param condition condition to set
     */
    public void setResolveAttributesPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        resolveAttributesPredicate = Constraint.isNotNull(condition, "Resolve attributes predicate cannot be null");
    }

    /**
     * Get whether responses to the authentication request should include an attribute statement.
     *
     * <p>Default is true</p>
     * 
     * @param profileRequestContext current profile request context
     *
     * @return whether responses to the authentication request should include an attribute statement
     */
    public boolean isIncludeAttributeStatement(@Nullable final ProfileRequestContext profileRequestContext) {
        return includeAttributeStatementPredicate.test(profileRequestContext);
    }

    /**
     * Set whether responses to the authentication request should include an attribute statement.
     *
     * @param flag flag to set
     */
    public void setIncludeAttributeStatement(final boolean flag) {
        includeAttributeStatementPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set a condition to determine whether responses to the authentication request should include an
     * attribute statement.
     *
     * @param condition  condition to set
     */
    public void setIncludeAttributeStatementPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        includeAttributeStatementPredicate = Constraint.isNotNull(condition,
                "Include attribute statement predicate cannot be null");
    }
    
    /**
     * Gets whether Scoping elements in requests should be ignored/omitted.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether Scoping elements in requests should be ignored/omitted
     * 
     * @since 4.0.0
     */
    public boolean isIgnoreScoping(@Nullable final ProfileRequestContext profileRequestContext) {
        return ignoreScoping.test(profileRequestContext);
    }
    
    /**
     * Sets whether Scoping elements in requests should be ignored/omitted.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.0.0
     */
    public void setIgnoreScoping(final boolean flag) {
        ignoreScoping = PredicateSupport.constant(flag);
    }
    
    /**
     * Sets a condition to determine whether Scoping elements in requests should be ignored/omitted.
     * 
     * @param condition condition to set
     * 
     * @since 4.0.0
     */
    public void setIgnoreScopingPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ignoreScoping = Constraint.isNotNull(condition, "Ignore Scoping condition cannot be null");
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
        forceAuthnPredicate = PredicateSupport.constant(flag);
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
     * Get whether the client's address must match the address in an inbound {@link SubjectLocality}
     * element during inbound SSO.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether to compare addresses
     * 
     * @since 4.0.0
     */
    public boolean isCheckAddress(@Nullable final ProfileRequestContext profileRequestContext) {
        return checkAddressPredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether the client's address must match the address in an inbound {@link SubjectLocality}
     * element during inbound SSO.
     * 
     * @param flag flag to set
     * 
     * @since 4.0.0
     */
    public void setCheckAddress(final boolean flag) {
        checkAddressPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set a condition to determine whether the client's address must match the address in an inbound
     * {@link SubjectLocality} element during inbound SSO.
     * 
     * @param condition condition to set
     * 
     * @since 4.0.0
     */
    public void setCheckAddressPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        checkAddressPredicate = Constraint.isNotNull(condition, "Address checking predicate cannot be null");
    }
    
    /**
     * Get condition to determine whether the response endpoint should be validated if the request is signed.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return condition
     * 
     * @since 4.0.0
     */
    public boolean isSkipEndpointValidationWhenSigned(@Nullable final ProfileRequestContext profileRequestContext) {
        return skipEndpointValidationWhenSignedPredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether the response endpoint should be validated if the request is signed.
     * 
     * @param flag flag to set
     * 
     * @since 3.4.0
     */
    public void setSkipEndpointValidationWhenSigned(final boolean flag) {
        skipEndpointValidationWhenSignedPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set condition to determine whether the response endpoint should be validated if the request is signed.
     * 
     * @param condition condition to set
     * 
     * @since 3.4.0
     */
    public void setSkipEndpointValidationWhenSignedPredicate(
            @Nonnull final Predicate<ProfileRequestContext> condition) {
        skipEndpointValidationWhenSignedPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }

    /**
     * Gets the maximum number of times an assertion may be proxied.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return maximum number of times an assertion may be proxied
     */
    @Nullable public Integer getProxyCount(@Nullable final ProfileRequestContext profileRequestContext) {
        final Integer count = proxyCountLookupStrategy.apply(profileRequestContext);
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        return count;
    }

    /**
     * Set the maximum number of times an assertion may be proxied.
     * 
     * @param count maximum number of times an assertion may be proxied
     */
    public void setProxyCount(@Nullable @NonNegative final Integer count) {
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        proxyCountLookupStrategy = FunctionSupport.constant(count);
    }

    /**
     * Set a lookup strategy for the maximum number of times an assertion may be proxied.
     *
     * @param strategy  lookup strategy
     */
    public void setProxyCountLookupStrategy(@Nonnull final Function<ProfileRequestContext,Integer> strategy) {
        proxyCountLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Gets the unmodifiable collection of audiences for a proxied assertion.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return audiences for a proxied assertion
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getProxyAudiences(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<String> audiences = proxyAudiencesLookupStrategy.apply(profileRequestContext);
        if (audiences != null) {
            return CollectionSupport.copyToSet(audiences);
        }
        return CollectionSupport.emptySet();
    }

    /**
     * Set the proxy audiences to be added to responses.
     * 
     * @param audiences proxy audiences to be added to responses
     */
    public void setProxyAudiences(@Nullable @NonnullElements final Collection<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            proxyAudiencesLookupStrategy = FunctionSupport.constant(null);
        } else {
            proxyAudiencesLookupStrategy = FunctionSupport.constant(
                    List.copyOf(StringSupport.normalizeStringCollection(audiences)));
        }
    }

    /**
     * Set a lookup strategy for the proxy audiences to be added to responses.
     *
     * @param strategy  lookup strategy
     */
    public void setProxyAudiencesLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        proxyAudiencesLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /**
     * Gets whether to suppress inclusion of {@link AuthenticatingAuthority} element.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff the element should be suppressed when possible
     * 
     * @since 4.2.0
     */
    public boolean isSuppressAuthenticatingAuthority(@Nullable final ProfileRequestContext profileRequestContext) {
        return suppressAuthenticatingAuthorityPredicate.test(profileRequestContext);
    }
    
    /**
     * Sets whether to suppress inclusion of {@link AuthenticatingAuthority} element.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.2.0
     */
    public void setSuppressAuthenticatingAuthority(final boolean flag) {
        suppressAuthenticatingAuthorityPredicate = PredicateSupport.constant(flag);
    }

    /**
     * Sets condition to determine whether to suppress inclusion of {@link AuthenticatingAuthority} element.
     * 
     * @param condition condition to set
     * 
     * @since 4.2.0
     */
    public void setSuppressAuthenticatingAuthorityPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        suppressAuthenticatingAuthorityPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }
    
    /**
     * Gets whether authentication results produced by use of this profile should carry the proxied
     * assertion's AuthnInstant, rather than the current time.
     * 
     * <p>Defaults to true.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether to proxy across the inbound AuthnInstant
     * 
     * @since 4.0.0
     */
    public boolean isProxiedAuthnInstant(@Nullable final ProfileRequestContext profileRequestContext) {
        return proxiedAuthnInstantPredicate.test(profileRequestContext);
    }
    
    /**
     * Sets whether authentication results produced by use of this profile should carry the proxied
     * assertion's AuthnInstant, rather than the current time.
     * 
     * @param flag flag to set
     * 
     * @since 4.0.0
     */
    public void setProxiedAuthnInstant(final boolean flag) {
        proxiedAuthnInstantPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Sets condition to determine whether authentication results produced by use of this profile should
     * carry the proxied assertion's AuthnInstant, rather than the current time.
     * 
     * @param condition condition to set
     * 
     * @since 4.0.0
     */
    public void setProxiedAuthnInstantPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        proxiedAuthnInstantPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }

    /**
     * Get whether to require signed requests.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether to require signed requests
     * 
     * @since 4.3.0
     */
    public boolean isRequireSignedRequests(@Nullable final ProfileRequestContext profileRequestContext) {
        return requireSignedRequestsPredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether to require signed requests.
     * 
     * @param flag flag to set
     * 
     * @since 4.3.0
     */
    public void setRequireSignedRequests(final boolean flag) {
        requireSignedRequestsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set a condition to determine whether to require signed requests.
     * 
     * @param condition condition to set
     * 
     * @since 4.3.0
     */
    public void setRequireSignedRequestsPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        requireSignedRequestsPredicate = Constraint.isNotNull(condition, "Signed requests predicate cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isRequireSignedAssertions(@Nullable final ProfileRequestContext profileRequestContext) {
        return requireSignedAssertionsPredicate.test(profileRequestContext);
    }
    
    /**
     * Set whether to require signed assertions.
     * 
     * @param flag flag to set
     * 
     * @since 5.0.0
     */
    public void setRequireSignedAssertions(final boolean flag) {
        requireSignedAssertionsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set a condition to determine whether to require signed assertions.
     * 
     * @param condition condition to set
     * 
     * @since 5.0.0
     */
    public void setRequireSignedAssertionsPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        requireSignedAssertionsPredicate = Constraint.isNotNull(condition,
                "Signed assertions predicate cannot be null");
    }
    
    /**
     * Get the maximum amount of time the service provider should maintain a session for the user
     * based on the authentication assertion. A null or 0 is interpreted as an unlimited lifetime.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return max lifetime of service provider should maintain a session
     */
    @Nullable public Duration getMaximumSPSessionLifetime(@Nullable final ProfileRequestContext profileRequestContext) {
        final Duration lifetime = maximumSPSessionLifetimeLookupStrategy.apply(profileRequestContext);
        Constraint.isFalse(lifetime != null && lifetime.isNegative(),
                "Maximum SP session lifetime must be greater than or equal to 0");
        return lifetime;
    }

    /**
     * Set the maximum amount of time the service provider should maintain a session for the user
     * based on the authentication assertion. A null or 0 is interpreted as an unlimited lifetime.
     * 
     * @param lifetime max lifetime of service provider should maintain a session
     */
    public void setMaximumSPSessionLifetime(@Nullable final Duration lifetime) {
        Constraint.isFalse(lifetime != null && lifetime.isNegative(),
                "Maximum SP session lifetime must be greater than or equal to 0");
        
        maximumSPSessionLifetimeLookupStrategy = FunctionSupport.constant(lifetime);
    }
    
    /**
     * Set a lookup strategy for the maximum amount of time the service provider should maintain a session for the user.
     * 
     * @param strategy  lookup strategy
     * 
     * @since 3.4.0
     */
    public void setMaximumSPSessionLifetimeLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Duration> strategy) {
        maximumSPSessionLifetimeLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /**
     * Get the maximum amount of time allowed to have elapsed since an incoming AuthnInstant.
     * 
     * <p>A null or 0 is interpreted as an unlimited amount.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return max time since inbound AuthnInstant
     * 
     * @since 4.0.0
     */
    @NonNegative @Nullable public Duration getMaximumTimeSinceAuthn(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Duration amount = maximumTimeSinceAuthnLookupStrategy.apply(profileRequestContext);
        Constraint.isFalse(amount != null && amount.isNegative(),
                "Maximum time since authentication must be greater than or equal to 0");
        return amount;
    }

    /**
     * Set the maximum amount of time allowed to have elapsed since an incoming AuthnInstant.
     * 
     * <p>A null or 0 is interpreted as an unlimited amount.</p>
     * 
     * @param amount max time to allow
     * 
     * @since 4.0.0
     */
    public void setMaximumTimeSinceAuthn(@Nullable final Duration amount) {
        Constraint.isFalse(amount != null && amount.isNegative(),
                "Maximum time since authentication must be greater than or equal to 0");
        
        maximumTimeSinceAuthnLookupStrategy = FunctionSupport.constant(amount);
    }
    
    /**
     * Set a lookup strategy for the maximum amount of time allowed to have elapsed since an incoming AuthnInstant.
     * 
     * @param strategy  lookup strategy
     * 
     * @since 4.0.0
     */
    public void setMaximumTimeSinceAuthnLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Duration> strategy) {
        maximumTimeSinceAuthnLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /**
     * Get the predicate used to determine if produced assertions may be delegated.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return predicate used to determine if produced assertions may be delegated
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    public boolean isAllowDelegation(@Nullable final ProfileRequestContext profileRequestContext) {
        return allowDelegationPredicate.test(profileRequestContext);
    }
    

    /**
     * Set whether produced assertions may be delegated.
     * 
     * @param  flag flag to set
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    public void setAllowDelegation(final boolean flag) {
        DeprecationSupport.warnOnce(ObjectType.CONFIGURATION, "allowDelegation", "relying-party.xml", null);
        allowDelegationPredicate = PredicateSupport.constant(flag);
    }    

    /**
     * Set the predicate used to determine if produced assertions may be delegated.
     * 
     * @param  predicate used to determine if produced assertions may be delegated
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    public void setAllowDelegationPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        DeprecationSupport.warnOnce(ObjectType.CONFIGURATION, "allowDelegationPredicate", "relying-party.xml", null);
        allowDelegationPredicate = Constraint.isNotNull(predicate, "Allow delegation predicate cannot be null");
    }

    /**
     * Get the limits on the total number of delegates that may be derived from the initial SAML token.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return the limit on the total number of delegates that may be derived from the initial SAML token
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    @NonNegative public long getMaximumTokenDelegationChainLength(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final Long len = maximumTokenDelegationChainLengthLookupStrategy.apply(profileRequestContext);
        Constraint.isNotNull(len, "Delegation chain length cannot be null");
        Constraint.isGreaterThanOrEqual(0, len, "Delegation chain length must be greater than or equal to 0");
        
        return len;
    }

    /**
     * Set the limits on the total number of delegates that may be derived from the initial SAML token.
     * 
     * @param length the limit on the total number of delegates that may be derived from the initial SAML token
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    public void setMaximumTokenDelegationChainLength(@NonNegative final long length) {
        DeprecationSupport.warnOnce(ObjectType.CONFIGURATION, "maximumTokenDelegationChainLength",
                "relying-party.xml", null);
        Constraint.isGreaterThanOrEqual(0, length, "Delegation chain length must be greater than or equal to 0");
        
        maximumTokenDelegationChainLengthLookupStrategy = FunctionSupport.constant(length);
    }
    
    /**
     * Set a lookup strategy for the limits on the total number of delegates that
     * may be derived from the initial SAML token.
     * 
     * @param strategy  lookup strategy
     * 
     * @since 3.4.0
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    public void setMaximumTokenDelegationChainLengthLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Long> strategy) {
        DeprecationSupport.warnOnce(ObjectType.CONFIGURATION, "maximumTokenDelegationChainLengthLookupStrategy",
                "relying-party.xml", null);
        maximumTokenDelegationChainLengthLookupStrategy =
                Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /**
     * Get the function to use to translate an inbound proxied SAML 2.0 {@link AuthnContext} into the appropriate
     * set of custom {@link Principal} objects to populate into the subject.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return translation function
     * 
     * @since 4.0.0
     */
    @Nullable public Function<AuthnContext,Collection<Principal>> getAuthnContextTranslationStrategy(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return authnContextTranslationStrategyLookupStrategy.apply(profileRequestContext);
    }

    /**
     * Set the function to use to translate an inbound proxied SAML 2.0 {@link AuthnContext} into the appropriate
     * set of custom {@link Principal} objects to populate into the subject.
     * 
     * @param strategy translation function
     * 
     * @since 4.0.0
     */
    public void setAuthnContextTranslationStrategy(
            @Nullable final Function<AuthnContext,Collection<Principal>> strategy) {
        authnContextTranslationStrategyLookupStrategy = FunctionSupport.constant(strategy);
    }

    /**
     * Set a lookup strategy for the function to use to translate an inbound proxied SAML 2.0 {@link AuthnContext}
     * into the appropriate set of custom {@link Principal} objects to populate into the subject.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setAuthnContextTranslationStrategyLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Function<AuthnContext,Collection<Principal>>> strategy) {
        authnContextTranslationStrategyLookupStrategy =
                Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Get the function to use to translate an inbound proxied response into the appropriate
     * set of custom {@link Principal} objects to populate into the subject.
     * 
     * <p>This differs from the original in that the input is the entire {@link ProfileRequestContext}
     * of the proxied authentication state rather than the SAML {@link AuthnContext} directly.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return translation function
     * 
     * @since 4.1.0
     */
    @Nullable public Function<ProfileRequestContext,Collection<Principal>> getAuthnContextTranslationStrategyEx(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return authnContextTranslationStrategyExLookupStrategy.apply(profileRequestContext);
    }

    /**
     * Set the function to use to translate an inbound proxied response into the appropriate
     * set of custom {@link Principal} objects to populate into the subject.
     * 
     * <p>This differs from the original in that the input is the entire {@link ProfileRequestContext}
     * of the proxied authentication state rather than the SAML {@link AuthnContext} directly.</p>
     * 
     * @param strategy translation function
     * 
     * @since 4.1.0
     */
    public void setAuthnContextTranslationStrategyEx(
            @Nullable final Function<ProfileRequestContext,Collection<Principal>> strategy) {
        authnContextTranslationStrategyExLookupStrategy = FunctionSupport.constant(strategy);
    }

    /**
     * Set a lookup strategy for the function to use to translate an inbound proxied response
     * into the appropriate set of custom {@link Principal} objects to populate into the subject.
     * 
     * <p>This differs from the original in that the input is the entire {@link ProfileRequestContext}
     * of the proxied authentication state rather than the SAML {@link AuthnContext} directly.</p>
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.1.0
     */
    public void setAuthnContextTranslationStrategyExLookupStrategy(
            @Nonnull
            final Function<ProfileRequestContext,Function<ProfileRequestContext,Collection<Principal>>> strategy) {
        authnContextTranslationStrategyExLookupStrategy =
                Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Get the comparison operator to use when issuing SAML requests containing requested context classes.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return comparison value or null
     * 
     * @since 4.0.0
     */
    @Nullable public AuthnContextComparisonTypeEnumeration getAuthnContextComparison(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final String comparison = authnContextComparisonLookupStrategy.apply(profileRequestContext);
        if (comparison != null) {
            return AuthnContextComparisonTypeEnumeration.valueOf(comparison.toUpperCase());
        }
        
        return null;
    }
    
    /**
     * Set the comparison operator to use when issuing SAML requests containing requested context classes.
     * 
     * @param comparison comparison value or null
     * 
     * @since 4.0.0
     */
    public void setAuthnContextComparison(@Nullable final AuthnContextComparisonTypeEnumeration comparison) {
        authnContextComparisonLookupStrategy =
                FunctionSupport.constant(comparison != null ? comparison.toString() : null);
    }

    /**
     * Set a lookup strategy for the comparison operator to use when issuing SAML requests containing
     * requested context classes.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setAuthnContextComparisonLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,String> strategy) {
        authnContextComparisonLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<AuthnContextClassRefPrincipal> methods =
                defaultAuthenticationContextsLookupStrategy.apply(profileRequestContext);
        if (methods != null) {
            return CollectionSupport.copyToList(methods);
        }
        return CollectionSupport.emptyList();
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
     * Set a lookup strategy for the authentication contexts to use, expressed as custom principals.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
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
            return CollectionSupport.copyToSet(flows);
        }
        return CollectionSupport.emptySet();
    }

    /**
     * Set the authentication flows to use.
     * 
     * @param flows   flow identifiers to use
     */
    public void setAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            authenticationFlowsLookupStrategy =
                    FunctionSupport.constant(
                            CollectionSupport.copyToSet(StringSupport.normalizeStringCollection(flows)));
        } else {
            authenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the authentication flows to use.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
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
            return CollectionSupport.copyToList(flows);
        }
        return CollectionSupport.emptyList();
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
     * 
     * @since 3.3.0
     */
    public void setPostAuthenticationFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        postAuthenticationFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Get the name identifier formats to use.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return formats to use
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getNameIDFormatPrecedence(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final Collection<String> formats = nameIDFormatPrecedenceLookupStrategy.apply(profileRequestContext);
        if (formats != null) {
            return CollectionSupport.copyToList(formats);
        }
        return CollectionSupport.emptyList();
    }

    /**
     * Set the name identifier formats to use.
     * 
     * @param formats   name identifier formats to use
     */
    public void setNameIDFormatPrecedence(@Nullable @NonnullElements final Collection<String> formats) {
        if (formats != null) {
            nameIDFormatPrecedenceLookupStrategy =
                    FunctionSupport.constant(
                            CollectionSupport.copyToList(StringSupport.normalizeStringCollection(formats)));
        } else {
            nameIDFormatPrecedenceLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the name identifier formats to use.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setNameIDFormatPrecedenceLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        nameIDFormatPrecedenceLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public String getSPNameQualifier(@Nullable final ProfileRequestContext profileRequestContext) {
        return spNameQualifierLookupStrategy.apply(profileRequestContext);
    }
    
    /**
     * Sets the SPNameQualifier to include in requests. 
     * 
     * @param qualifier the SPNameQualifier to include
     * 
     * @since 5.0.0
     */
    public void setSPNameQualifier(@Nullable final String qualifier) {
        spNameQualifierLookupStrategy = FunctionSupport.constant(qualifier);
    }
    
    /**
     * Sets a lookup strategy for the SPNameQualifier to include in requests. 
     * 
     * @param strategy lookup strategy
     * 
     * @since 5.0.0
     */
    public void setSPNameQualifierLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        spNameQualifierLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public String getAttributeIndex(@Nullable final ProfileRequestContext profileRequestContext) {
        return attributeIndexLookupStrategy.apply(profileRequestContext);
    }

    /**
     * Sets the AttributeConsumingServiceIndex to include in requests. 
     * 
     * @param index the index to include
     * 
     * @since 5.0.0
     */
    public void setAttributeIndex(@Nullable final String index) {
        attributeIndexLookupStrategy = FunctionSupport.constant(index);
    }
    
    /**
     * Sets a lookup strategy for the AttributeConsumingServiceIndex to include in requests. 
     * 
     * @param strategy lookup strategy
     * 
     * @since 5.0.0
     */
    public void setAttributeIndexLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        attributeIndexLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<RequestedAttribute> getRequestedAttributes(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<RequestedAttribute> attrs = requestedAttributesLookupStrategy.apply(profileRequestContext);
        if (attrs != null) {
            return CollectionSupport.copyToList(attrs);
        }
        return CollectionSupport.emptyList();
    }

    /**
     * Set the {@link RequestedAttribute} objects to include in request.
     * 
     * @param attrs   requested attributes to include
     * 
     * @since 5.0.0
     */
    public void setRequestedAttributes(@Nullable @NonnullElements final Collection<RequestedAttribute> attrs) {
        if (attrs != null) {
            requestedAttributesLookupStrategy = FunctionSupport.constant(CollectionSupport.copyToList(attrs));
        } else {
            requestedAttributesLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the name identifier formats to use.
     *
     * @param strategy  lookup strategy
     * 
     * @since 5.0.0
     */
    public void setRequestedAttributesLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<RequestedAttribute>> strategy) {
        requestedAttributesLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

}
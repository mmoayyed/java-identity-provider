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

package net.shibboleth.idp.admin;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.ext.saml2mdui.Description;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.InformationURL;
import org.opensaml.saml.ext.saml2mdui.PrivacyStatementURL;
import org.opensaml.saml.ext.saml2mdui.UIInfo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;

import net.shibboleth.idp.authn.principal.PrincipalServiceManager;
import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.LangBearingString;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A descriptor for an administrative flow.
 * 
 * <p>Administrative flows are essentially any feature intrinsic to the IdP itself and generally
 * not exposed to external systems using security mechanisms that would involve the more traditional
 * "relying party" machinery and security models. Examples include status reporting and service
 * management features, or user self-service features.
 * </p>
 * 
 * @since 3.3.0
 */
public class BasicAdministrativeFlowDescriptor extends AbstractProfileConfiguration
        implements AdministrativeFlowDescriptor {
    
    /** Logging ID. */
    @Nullable private String loggingId;
    
    /** Whether this flow supports non-browser clients. */
    @Nonnull private Predicate<ProfileRequestContext> supportsNonBrowserPredicate;

    /** Whether user authentication is required. */
    @Nonnull private Predicate<ProfileRequestContext> authenticatedPredicate;
    
    /** Expose user interface details. */
    @Nonnull private final UIInfo uiInfo;
    
    /** Lookup strategy for access control policy to apply. */
    @Nonnull private Function<ProfileRequestContext,String> policyNameLookupStrategy;

    /** Whether attributes should be resolved in the course of the flow. */
    @Nonnull private Predicate<ProfileRequestContext> resolveAttributesPredicate;
    
    /** Selects, and limits, the authentication flows to use for requests by supported principals. */
    @Nonnull private Function<ProfileRequestContext,Collection<Principal>>
            defaultAuthenticationMethodsLookupStrategy;

    /**
     * Auhentication methods provided by delimited strings, for post-initialization override via
     * {@link PrincipalServiceManager}.
     */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> stringBasedPrincipalsLookupStrategy;
    
    /** Filters the usable authentication flows. */
    @Nonnull private Function<ProfileRequestContext,Set<String>> authenticationFlowsLookupStrategy;

    /** Enables post-authentication interceptor flows. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> postAuthenticationFlowsLookupStrategy;
    
    /** Whether to mandate forced authentication for the request. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;

    /** Lookup function to supply proxyCount property. */
    @Nonnull private Function<ProfileRequestContext,Integer> proxyCountLookupStrategy;
    
    /** Builder factory for XMLObjects needed in UIInfo emulation. */
    @Nonnull private final XMLObjectBuilderFactory builderFactory;

    /** Access to principal services. */
    @Nullable private PrincipalServiceManager principalServiceManager;
    
    /**
     * Constructor.
     * 
     * @param id profile Id
     */
    public BasicAdministrativeFlowDescriptor(@Nonnull @NotEmpty @ParameterName(name="id") final String id) {
        super(id);
        
        supportsNonBrowserPredicate = Predicates.alwaysTrue();
        authenticatedPredicate = Predicates.alwaysFalse();
        policyNameLookupStrategy = FunctionSupport.constant(null);
        resolveAttributesPredicate = Predicates.alwaysFalse();
        forceAuthnPredicate = Predicates.alwaysFalse();
        
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        uiInfo = ((SAMLObjectBuilder<UIInfo>) builderFactory.<UIInfo>getBuilderOrThrow(
                UIInfo.DEFAULT_ELEMENT_NAME)).buildObject();
        
        defaultAuthenticationMethodsLookupStrategy = FunctionSupport.constant(null);
        stringBasedPrincipalsLookupStrategy = FunctionSupport.constant(null);
        authenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        postAuthenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        proxyCountLookupStrategy = FunctionSupport.constant(null);
    }
    
    /**
     * Sets a {@link PrincipalServiceManager} to use for string-based principal processing.
     * 
     * @param manager manager to set
     * 
     * @since 4.2.0
     */
    public void setPrincipalServiceManager(@Nullable final PrincipalServiceManager manager) {
        principalServiceManager = manager;
    }
    
    /** {@inheritDoc} */
    @Nullable public String getLoggingId() {
        return loggingId;
    }
    
    /**
     * Set a logging ID to use when auditing this profile.
     * 
     * @param id logging ID
     */
    public void setLoggingId(@Nullable final String id) {
        loggingId = StringSupport.trimOrNull(id);
    }
    
    /** {@inheritDoc} */
    public boolean isNonBrowserSupported(@Nullable final ProfileRequestContext profileRequestContext) {
        return supportsNonBrowserPredicate.test(profileRequestContext);
    }

    /**
     * Set whether this flow supports non-browser clients.
     * 
     * @param flag flag to set
     */
    public void setNonBrowserSupported(final boolean flag) {
        supportsNonBrowserPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }
    
    /**
     * Set condition to determine whether this flow supports non-browser clients.
     * 
     * @param condition condition to apply
     */
    public void setNonBrowserSupportedPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        supportsNonBrowserPredicate = Constraint.isNotNull(condition, "Non-browser support condition cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean isAuthenticated(@Nullable final ProfileRequestContext profileRequestContext) {
        return authenticatedPredicate.test(profileRequestContext);
    }

    /**
     * Set whether user authentication is required (default is false).
     * 
     * @param flag flag to set
     */
    public void setAuthenticated(final boolean flag) {
        authenticatedPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }
    
    /**
     * Set condition to determine whether user authentication is required (default is false).
     * 
     * @param condition condition to apply
     */
    public void setAuthenticatedPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        authenticatedPredicate = Constraint.isNotNull(condition, "Authentication condition cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull public UIInfo getUIInfo() {
        return uiInfo;
    }
    
    /**
     * Set the {@link DisplayName} objects to expose via {@link #getUIInfo()} via a utility class.
     *  
     * @param displayNames utility class collection of language-annotated strings
     */
    public void setDisplayNames(@Nonnull @NonnullElements final Collection<LangBearingString> displayNames) {
        uiInfo.getDisplayNames().clear();
        for (final LangBearingString s : displayNames) {
            final DisplayName displayName =
                    ((SAMLObjectBuilder<DisplayName>) builderFactory.<DisplayName>getBuilderOrThrow(
                            DisplayName.DEFAULT_ELEMENT_NAME)).buildObject();
            displayName.setValue(s.getValue());
            displayName.setXMLLang(s.getLang());
            uiInfo.getDisplayNames().add(displayName);
        }
    }

    /**
     * Set the {@link Description} objects to expose via {@link #getUIInfo()} via a utility class.
     *  
     * @param descriptions utility class collection of language-annotated strings
     */
    public void setDescriptions(@Nonnull @NonnullElements final Collection<LangBearingString> descriptions) {
        uiInfo.getDescriptions().clear();
        for (final LangBearingString s : descriptions) {
            final Description desc =
                    ((SAMLObjectBuilder<Description>) builderFactory.<Description>getBuilderOrThrow(
                            Description.DEFAULT_ELEMENT_NAME)).buildObject();
            desc.setValue(s.getValue());
            desc.setXMLLang(s.getLang());
            uiInfo.getDescriptions().add(desc);
        }
    }
    
    /**
     * Set the {@link org.opensaml.saml.ext.saml2mdui.Logo} objects to expose via {@link #getUIInfo()} via a
     * utility class.
     * 
     * @param logos utility class collection of logo metadata
     */
    public void setLogos(@Nonnull @NonnullElements final Collection<Logo> logos) {
        uiInfo.getLogos().clear();
        for (final Logo src : logos) {
            final org.opensaml.saml.ext.saml2mdui.Logo logo =
                    ((SAMLObjectBuilder<org.opensaml.saml.ext.saml2mdui.Logo>) 
                            builderFactory.<org.opensaml.saml.ext.saml2mdui.Logo>getBuilderOrThrow(
                                    org.opensaml.saml.ext.saml2mdui.Logo.DEFAULT_ELEMENT_NAME)).buildObject();
            logo.setURI(src.getValue());
            logo.setXMLLang(src.getLang());
            logo.setHeight(src.getHeight());
            logo.setWidth(src.getWidth());
            uiInfo.getLogos().add(logo);
        }
    }
    
    /**
     * Set the {@link InformationURL} objects to expose via {@link #getUIInfo()} via a utility class.
     *  
     * @param urls utility class collection of language-annotated strings
     */
    public void setInformationURLs(@Nonnull @NonnullElements final Collection<LangBearingString> urls) {
        uiInfo.getInformationURLs().clear();
        for (final LangBearingString s : urls) {
            final InformationURL url =
                    ((SAMLObjectBuilder<InformationURL>) builderFactory.<InformationURL>getBuilderOrThrow(
                            InformationURL.DEFAULT_ELEMENT_NAME)).buildObject();
            url.setURI(s.getValue());
            url.setXMLLang(s.getLang());
            uiInfo.getInformationURLs().add(url);
        }
    }

    /**
     * Set the {@link PrivacyStatementURL} objects to expose via {@link #getUIInfo()} via a utility class.
     *  
     * @param urls utility class collection of language-annotated strings
     */
    public void setPrivacyStatementURLs(@Nonnull @NonnullElements final Collection<LangBearingString> urls) {
        uiInfo.getPrivacyStatementURLs().clear();
        for (final LangBearingString s : urls) {
            final PrivacyStatementURL url =
                    ((SAMLObjectBuilder<PrivacyStatementURL>) builderFactory.<PrivacyStatementURL>getBuilderOrThrow(
                            PrivacyStatementURL.DEFAULT_ELEMENT_NAME)).buildObject();
            url.setURI(s.getValue());
            url.setXMLLang(s.getLang());
            uiInfo.getPrivacyStatementURLs().add(url);
        }
    }

    /** {@inheritDoc} */
    @Nullable public String getPolicyName(@Nullable final ProfileRequestContext profileRequestContext) {
        return policyNameLookupStrategy.apply(profileRequestContext);
    }  
    
    /**
     * Set an explicit access control policy name to apply.
     * 
     * @param name  policy name
     */
    public void setPolicyName(@Nullable final String name) {
        policyNameLookupStrategy = FunctionSupport.constant(StringSupport.trimOrNull(name));
    }

    /**
     * Set a lookup strategy to use to obtain the access control policy for this flow.
     * 
     * @param strategy  lookup strategy
     */
    public void setPolicyNameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        policyNameLookupStrategy = Constraint.isNotNull(strategy, "Policy lookup strategy cannot be null");
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
        resolveAttributesPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }
    
    /**
     * Set a condition to determine whether attributes should be resolved during the profile.
     *
     * @param condition  condition to set
     */
    public void setResolveAttributesPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        resolveAttributesPredicate = Constraint.isNotNull(condition, "Resolve attributes predicate cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getInboundInterceptorFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getOutboundInterceptorFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Nullable public SecurityConfiguration getSecurityConfiguration(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return null;
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        // Check for string-based representation first, then back off to native objects.
        
        if (principalServiceManager != null) {
            final Collection<String> stringBasedPrincipals =
                    stringBasedPrincipalsLookupStrategy.apply(profileRequestContext);
            if (stringBasedPrincipals != null && !stringBasedPrincipals.isEmpty()) {
                final List<Principal> principals = new ArrayList<>(stringBasedPrincipals.size());
                stringBasedPrincipals.forEach(v -> {
                    final Principal p = principalServiceManager.principalFromString(v);
                    if (p != null) {
                        principals.add(p);
                    }
                });
                return List.copyOf(principals);
            }
        }
        
        final Collection<Principal> methods = defaultAuthenticationMethodsLookupStrategy.apply(profileRequestContext);
        if (methods != null) {
            return List.copyOf(methods);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Set the default authentication methods to use, expressed as custom principals.
     * 
     * @param methods   default authentication methods to use
     */
    public void setDefaultAuthenticationMethods(@Nullable @NonnullElements final Collection<Principal> methods) {

        if (methods != null) {
            defaultAuthenticationMethodsLookupStrategy = FunctionSupport.constant(List.copyOf(methods));
        } else {
            defaultAuthenticationMethodsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the authentication methods to use, expressed as custom principals.
     *
     * @param strategy  lookup strategy
     */
    public void setDefaultAuthenticationMethodsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<Principal>> strategy) {
        defaultAuthenticationMethodsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Set the authentication methods to use, expressed as strings that will be
     * converted to principals during initialization.
     *
     * @param methods default authentication methods to use, expressed as strings
     * 
     * @since 4.2.0
     */
    public void setDefaultAuthenticationMethodsByString(
            @Nullable @NonnullElements final Collection<String> methods) {
        if (methods != null) {
            stringBasedPrincipalsLookupStrategy = FunctionSupport.constant(List.copyOf(methods));
        } else {
            stringBasedPrincipalsLookupStrategy = FunctionSupport.constant(null);
        }
    }
    
    /**
     * Set a lookup strategy for the authentication methods to use, expressed as strings that will be
     * converted to principals during initialization.
     *
     * @param strategy  lookup strategy
     * 
     * @since 4.2.0
     */
    public void setDefaultAuthenticationMethodsByStringLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        stringBasedPrincipalsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
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
    @Override public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof BasicAdministrativeFlowDescriptor) {
            return getId().equals(((BasicAdministrativeFlowDescriptor) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("flowId", getId())
                .toString();
    }

    /**
     * A wrapper class to construct logo objects for exposure by {@link UIInfo} interface.
     */
    public static class Logo extends LangBearingString {
        
        /** Logo height. */
        private final int height;

        /** Logo width. */
        private final int width;

        /**
         * Constructor.
         *
         * @param url logo URL
         * @param lang language
         * @param h logo height in pixels
         * @param w logo width in pixels
         */
        public Logo(@Nullable final String url, @Nullable @NotEmpty final String lang, @Positive final int h,
                @Positive final int w) {
            super(url, lang);
            
            height = Constraint.isGreaterThan(0, h, "Height must be greater than zero.");
            width = Constraint.isGreaterThan(0, w, "Width must be greater than zero.");
        }

        /**
         * Get logo height in pixels.
         * 
         * @return height
         */
        public int getHeight() {
            return height;
        }

        /**
         * Get logo width in pixels.
         * 
         * @return width
         */
        public int getWidth() {
            return width;
        }
    }

}
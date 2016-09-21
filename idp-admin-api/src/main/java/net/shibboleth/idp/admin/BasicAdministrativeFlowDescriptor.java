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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.LangBearingString;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.ext.saml2mdui.Description;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.InformationURL;
import org.opensaml.saml.ext.saml2mdui.PrivacyStatementURL;
import org.opensaml.saml.ext.saml2mdui.UIInfo;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
    private Predicate<ProfileRequestContext> supportsNonBrowserPredicate;

    /** Whether user authentication is required. */
    private Predicate<ProfileRequestContext> authenticatedPredicate;
    
    /** Expose user interface details. */
    @Nonnull private final UIInfo uiInfo;
    
    /** Lookup strategy for access control policy to apply. */
    @Nonnull private Function<ProfileRequestContext,String> policyNameLookupStrategy;

    /** Whether attributes should be resolved in the course of the flow. */
    @Nonnull private Predicate<ProfileRequestContext> resolveAttributesPredicate;
    
    /** Selects, and limits, the authentication flows to use for requests by supported principals. */
    @Nullable private Function<ProfileRequestContext,Collection<Principal>>
            defaultAuthenticationMethodsLookupStrategy;

    /** Filters the usable authentication flows. */
    @Nullable private Function<ProfileRequestContext,Set<String>> authenticationFlowsLookupStrategy;

    /** Enables post-authentication interceptor flows. */
    @Nullable private Function<ProfileRequestContext,Collection<String>> postAuthenticationFlowsLookupStrategy;
    
    /** Builder factory for XMLObjects needed in UIInfo emulation. */
    @Nonnull private final XMLObjectBuilderFactory builderFactory;
    
    /**
     * Constructor.
     * 
     * @param id profile Id
     */
    public BasicAdministrativeFlowDescriptor(@Nonnull @NotEmpty final String id) {
        super(id);
        
        supportsNonBrowserPredicate = Predicates.alwaysTrue();
        authenticatedPredicate = Predicates.alwaysFalse();
        policyNameLookupStrategy = FunctionSupport.constant(null);
        resolveAttributesPredicate = Predicates.alwaysFalse();
        
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        uiInfo = ((SAMLObjectBuilder<UIInfo>) builderFactory.<UIInfo>getBuilderOrThrow(
                UIInfo.DEFAULT_ELEMENT_NAME)).buildObject();
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
    public boolean isNonBrowserSupported() {
        return supportsNonBrowserPredicate.apply(getProfileRequestContext());
    }

    /**
     * Set whether this flow supports non-browser clients (default is true).
     * 
     * @param isSupported whether this flow supports non-browser clients
     */
    public void setNonBrowserSupported(final boolean isSupported) {
        supportsNonBrowserPredicate = isSupported ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
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
    public boolean isAuthenticated() {
        return authenticatedPredicate.apply(getProfileRequestContext());
    }
    
    /**
     * Set whether user authentication is required (default is false).
     * 
     * @param flag flag to set
     */
    public void setAuthenticated(final boolean flag) {
        authenticatedPredicate = flag ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
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
        for (final LangBearingString s : Collections2.filter(displayNames, Predicates.notNull())) {
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
        for (final LangBearingString s : Collections2.filter(descriptions, Predicates.notNull())) {
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
        for (final Logo src : Collections2.filter(logos, Predicates.notNull())) {
            final org.opensaml.saml.ext.saml2mdui.Logo logo =
                    ((SAMLObjectBuilder<org.opensaml.saml.ext.saml2mdui.Logo>) 
                            builderFactory.<org.opensaml.saml.ext.saml2mdui.Logo>getBuilderOrThrow(
                                    org.opensaml.saml.ext.saml2mdui.Logo.DEFAULT_ELEMENT_NAME)).buildObject();
            logo.setURL(src.getValue());
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
        for (final LangBearingString s : Collections2.filter(urls, Predicates.notNull())) {
            final InformationURL url =
                    ((SAMLObjectBuilder<InformationURL>) builderFactory.<InformationURL>getBuilderOrThrow(
                            InformationURL.DEFAULT_ELEMENT_NAME)).buildObject();
            url.setValue(s.getValue());
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
        for (final LangBearingString s : Collections2.filter(urls, Predicates.notNull())) {
            final PrivacyStatementURL url =
                    ((SAMLObjectBuilder<PrivacyStatementURL>) builderFactory.<PrivacyStatementURL>getBuilderOrThrow(
                            PrivacyStatementURL.DEFAULT_ELEMENT_NAME)).buildObject();
            url.setValue(s.getValue());
            url.setXMLLang(s.getLang());
            uiInfo.getPrivacyStatementURLs().add(url);
        }
    }

    /** {@inheritDoc} */
    @Nullable public String getPolicyName() {
        return getIndirectProperty(policyNameLookupStrategy, null);
    }  
    
    /**
     * Set a lookup strategy to use to obtain the access control policy for this flow.
     * 
     * @param strategy  lookup strategy
     */
    public void setPolicyNameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        policyNameLookupStrategy = Constraint.isNotNull(strategy, "Policy lookup strategy cannot be null");
    }

    /**
     * Set an explicit access control policy name to apply.
     * 
     * @param name  policy name
     */
    public void setPolicyName(@Nonnull @NotEmpty final String name) {
        policyNameLookupStrategy = FunctionSupport.constant(
                Constraint.isNotNull(StringSupport.trimOrNull(name), "Policy name cannot be null or empty"));
    }

    /** {@inheritDoc} */
    public boolean resolveAttributes() {
        return resolveAttributesPredicate.apply(getProfileRequestContext());
    }

    /**
     * Set whether attributes should be resolved during the profile.
     *
     * @param flag  flag to set
     */
    public void setResolveAttributes(final boolean flag) {
        resolveAttributesPredicate = flag ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
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
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getInboundInterceptorFlows() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getOutboundInterceptorFlows() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Nullable public SecurityConfiguration getSecurityConfiguration() {
        return null;
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods() {
        return ImmutableList.<Principal>copyOf(getIndirectProperty(defaultAuthenticationMethodsLookupStrategy,
                Collections.<Principal>emptyList()));
    }
    
    /**
     * Set the default authentication methods to use, expressed as custom principals.
     * 
     * @param methods   default authentication methods to use
     */
    public void setDefaultAuthenticationMethods(
            @Nullable @NonnullElements final Collection<Principal> methods) {

        if (methods != null) {
            defaultAuthenticationMethodsLookupStrategy = FunctionSupport.constant(
                    (Collection<Principal>) new ArrayList<>(Collections2.filter(methods, Predicates.notNull())));
        } else {
            defaultAuthenticationMethodsLookupStrategy = null;
        }
    }

    /**
     * Set a lookup strategy for the authentication methods to use, expressed as custom principals.
     *
     * @param strategy  lookup strategy
     */
    public void setDefaultAuthenticationMethodsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<Principal>> strategy) {
        defaultAuthenticationMethodsLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getAuthenticationFlows() {
        return ImmutableSet.<String>copyOf(getIndirectProperty(authenticationFlowsLookupStrategy,
                Collections.<String>emptySet()));
    }

    /**
     * Set the authentication flows to use.
     * 
     * @param flows   flow identifiers to use
     */
    public void setAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {

        if (flows != null) {
            authenticationFlowsLookupStrategy = FunctionSupport.<ProfileRequestContext,Set<String>>constant(
                    new HashSet<>(StringSupport.normalizeStringCollection(flows)));
        } else {
            authenticationFlowsLookupStrategy = null;
        }
    }

    /**
     * Set a lookup strategy for the authentication flows to use.
     *
     * @param strategy  lookup strategy
     */
    public void setAuthenticationFlowsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Set<String>> strategy) {
        authenticationFlowsLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getPostAuthenticationFlows() {
        return ImmutableList.<String>copyOf(getIndirectProperty(postAuthenticationFlowsLookupStrategy,
                Collections.<String>emptyList()));
    }
    
    /**
     * Set the ordered collection of post-authentication interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setPostAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {

        if (flows != null) {
            postAuthenticationFlowsLookupStrategy = FunctionSupport.<ProfileRequestContext,Collection<String>>constant(
                    new ArrayList<>(StringSupport.normalizeStringCollection(flows)));
        } else {
            postAuthenticationFlowsLookupStrategy = null;
        }
    }

    /**
     * Set a lookup strategy for the post-authentication interceptor flows to enable.
     *
     * @param strategy  lookup strategy
     */
    public void setPostAuthenticationFlowsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<String>> strategy) {
        postAuthenticationFlowsLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getNameIDFormatPrecedence() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
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
            
            height = (int) Constraint.isGreaterThan(0, h, "Height must be greater than zero.");
            width = (int) Constraint.isGreaterThan(0, w, "Width must be greater than zero.");
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

    /** {@inheritDoc} */
    
}
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
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
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
    
    /** Whether this flow supports non-browser clients. */
    private Predicate<ProfileRequestContext> supportsNonBrowserPredicate;
    
    /** Whether access to flow should be recorded in audit log. */
    private Predicate<ProfileRequestContext> auditedPredicate;

    /** Whether user authentication is required. */
    private Predicate<ProfileRequestContext> authenticatedPredicate;
    
    /** Whether to populate the context tree with pseudo-"relying-party" tree. */
    private Predicate<ProfileRequestContext> contextDecoratedPredicate;
    
    /** Expose user interface details. */
    private UIInfo uiInfo;
    
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
    
    /**
     * Constructor.
     * 
     * @param profileId the profile identifier
     */
    public BasicAdministrativeFlowDescriptor(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        
        supportsNonBrowserPredicate = Predicates.alwaysTrue();
        auditedPredicate = Predicates.alwaysTrue();
        authenticatedPredicate = Predicates.alwaysFalse();
        contextDecoratedPredicate = Predicates.alwaysFalse();
        policyNameLookupStrategy = FunctionSupport.constant(null);
        resolveAttributesPredicate = Predicates.alwaysFalse();
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
    public boolean isAudited() {
        return auditedPredicate.apply(getProfileRequestContext());
    }
    
    /**
     * Set whether access to flow should be recorded in audit log (default is true).
     * 
     * @param flag flag to set
     */
    public void setAudited(final boolean flag) {
        auditedPredicate = flag ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
    }
    
    /**
     * Set condition to determine whether access to flow should be recorded in audit log.
     * 
     * @param condition condition to apply
     */
    public void setAuditedPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        auditedPredicate = Constraint.isNotNull(condition, "Auditing condition cannot be null");
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
    public boolean isContextDecorated() {
        return contextDecoratedPredicate.apply(getProfileRequestContext());
    }
    
    /**
     * Set whether to decorate the profile request context tree with emulated relying party and user interface
     * contexts to support e.g., login interfaces (default is false).
     * 
     * @param flag flag to set
     */
    public void setContextDecorated(final boolean flag) {
        contextDecoratedPredicate = flag ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
    }
    
    /**
     * Set condition to determine whether to decorate the profile request context tree with emulated
     * relying party and user interface contexts to support e.g., login interfaces.
     * 
     * @param condition condition to apply
     */
    public void setContextDecoratedPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        contextDecoratedPredicate = Constraint.isNotNull(condition, "Authentication condition cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public UIInfo getUIInfo() {
        return uiInfo;
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

}
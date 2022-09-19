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

package net.shibboleth.idp.authn.testing;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicates;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Mock implementation of {@link AuthenticationProfileConfiguration}. */
public class MockAuthenticationProfileConfiguration extends AbstractProfileConfiguration
        implements AuthenticationProfileConfiguration {

    /** Selects, and limits, the authentication methods to use for requests. */
    @Nonnull @NonnullElements private List<Principal> defaultAuthenticationMethods;

    /** Filters the usable authentication flows. */
    @Nonnull @NonnullElements private Set<String> authenticationFlows;

    /** Enables post-authentication interceptor flows. */
    @Nonnull @NonnullElements private List<String> postAuthenticationFlows;

    /** Precedence of name identifier formats to use for requests. */
    @Nonnull @NonnullElements private List<String> nameIDFormatPrecedence;
    
    /** ForceAuthn predicate. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;
    
    /** Proxy count. */
    @Nonnull private Integer proxyCount;
    
    /**
     * Constructor.
     * 
     * @param id ID of this profile
     * @param methods default authentication methods to use
     */
    public MockAuthenticationProfileConfiguration(@Nonnull @NotEmpty final String id,
            @Nonnull @NonnullElements final List<Principal> methods) {
        this(id, methods, Collections.emptySet(), Collections.emptyList());
    }

    /**
     * Constructor.
     * 
     * @param id ID of this profile
     * @param methods default authentication methods to use
     * @param flows ...
     * @param formats name identifier formats to use
     */
    public MockAuthenticationProfileConfiguration(@Nonnull @NotEmpty final String id,
            @Nonnull @NonnullElements final List<Principal> methods,
            @Nonnull @NonnullElements final Collection<String> flows,
            @Nonnull @NonnullElements final List<String> formats) {
        super(id);
        setSecurityConfiguration(new SecurityConfiguration());
        setDefaultAuthenticationMethods(methods);
        setAuthenticationFlows(flows);
        setNameIDFormatPrecedence(formats);
        forceAuthnPredicate = Predicates.alwaysFalse();
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return defaultAuthenticationMethods;
    }
    
    /**
     * Set the default authentication methods to use, expressed as custom principals.
     * 
     * @param methods   default authentication methods to use
     */
    public void setDefaultAuthenticationMethods(@Nonnull @NonnullElements final List<Principal> methods) {
        defaultAuthenticationMethods = List.copyOf(Constraint.isNotNull(methods, "List of methods cannot be null"));
    }
    
    /**
     * Get the name identifier formats to use.
     * 
     * @param profileRequestContext profile request context
     * @return formats to use
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getNameIDFormatPrecedence(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return nameIDFormatPrecedence;
    }

    /**
     * Set the name identifier formats to use.
     * 
     * @param formats   name identifier formats to use
     */
    public void setNameIDFormatPrecedence(@Nonnull @NonnullElements final List<String> formats) {
        Constraint.isNotNull(formats, "List of formats cannot be null");
        
        nameIDFormatPrecedence = List.copyOf(StringSupport.normalizeStringCollection(formats));
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Set<String> getAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return authenticationFlows;
    }

    /**
     * Set the authentication flows to use.
     * 
     * @param flows   flow identifiers to use
     */
    public void setAuthenticationFlows(@Nonnull @NonnullElements final Collection<String> flows) {
        Constraint.isNotNull(flows, "Collection of flows cannot be null");
        
        authenticationFlows = Set.copyOf(StringSupport.normalizeStringCollection(flows));
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getPostAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return postAuthenticationFlows;
    }

    /**
     * Set the ordered collection of post-authentication interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setPostAuthenticationFlows(@Nonnull @NonnullElements final Collection<String> flows) {
        Constraint.isNotNull(flows, "Collection of flows cannot be null");
        
        postAuthenticationFlows = List.copyOf(StringSupport.normalizeStringCollection(flows));
    }

    /** {@inheritDoc} */
    public boolean isForceAuthn(ProfileRequestContext profileRequestContext) {
        return forceAuthnPredicate.test(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Nullable @NonNegative public Integer getProxyCount(@Nullable final ProfileRequestContext profileRequestContext) {
        return proxyCount;
    }
    
    /**
     * Set proxy count.
     * 
     * @param count the count
     */
    public void setProxyCount(@Nullable @NonNegative final Integer count) {
        if (count != null) {
            proxyCount = Constraint.isGreaterThanOrEqual(0, count, "Proxy count cannot be negative");
        } else {
            proxyCount = null;
        }
    }

}
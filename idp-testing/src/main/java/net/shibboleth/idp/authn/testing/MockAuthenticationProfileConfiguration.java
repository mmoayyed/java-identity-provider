/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.xmlsec.config.BasicXMLSecurityConfiguration;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.profile.config.AbstractInterceptorAwareProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.StringSupport;

/** Mock implementation of {@link AuthenticationProfileConfiguration}. */
public class MockAuthenticationProfileConfiguration extends AbstractInterceptorAwareProfileConfiguration
        implements AuthenticationProfileConfiguration {

    /** Selects, and limits, the authentication methods to use for requests. */
    @Nonnull private List<Principal> defaultAuthenticationMethods = CollectionSupport.emptyList();

    /** Filters the usable authentication flows. */
    @Nonnull private Set<String> authenticationFlows = CollectionSupport.emptySet();

    /** Enables post-authentication interceptor flows. */
    @Nonnull private List<String> postAuthenticationFlows = CollectionSupport.emptyList();

    /** Precedence of name identifier formats to use for requests. */
    @Nonnull private List<String> nameIDFormatPrecedence = CollectionSupport.emptyList();
    
    /** ForceAuthn predicate. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;
    
    /** Proxy count. */
    private Integer proxyCount;
    
    /**
     * Constructor.
     * 
     * @param id ID of this profile
     * @param methods default authentication methods to use
     */
    public MockAuthenticationProfileConfiguration(@Nonnull @NotEmpty final String id,
            @Nonnull final List<Principal> methods) {
        this(id, methods, CollectionSupport.emptySet(), CollectionSupport.emptyList());
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
            @Nonnull final List<Principal> methods,
            @Nonnull final Collection<String> flows,
            @Nonnull final List<String> formats) {
        super(id);
        setSecurityConfiguration(new BasicXMLSecurityConfiguration());
        setDefaultAuthenticationMethods(methods);
        setAuthenticationFlows(flows);
        setNameIDFormatPrecedence(formats);
        forceAuthnPredicate = PredicateSupport.alwaysFalse();
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return defaultAuthenticationMethods;
    }
    
    /**
     * Set the default authentication methods to use, expressed as custom principals.
     * 
     * @param methods   default authentication methods to use
     */
    public void setDefaultAuthenticationMethods(@Nonnull final List<Principal> methods) {
        defaultAuthenticationMethods = CollectionSupport.copyToList(
                Constraint.isNotNull(methods, "List of methods cannot be null"));
    }
    
    /**
     * Get the name identifier formats to use.
     * 
     * @param profileRequestContext profile request context
     * @return formats to use
     */
    @Nonnull @NotLive @Unmodifiable public List<String> getNameIDFormatPrecedence(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return nameIDFormatPrecedence;
    }

    /**
     * Set the name identifier formats to use.
     * 
     * @param formats   name identifier formats to use
     */
    public void setNameIDFormatPrecedence(@Nonnull final List<String> formats) {
        Constraint.isNotNull(formats, "List of formats cannot be null");
        
        nameIDFormatPrecedence = CollectionSupport.copyToList(StringSupport.normalizeStringCollection(formats));
    }

    /** {@inheritDoc} */
    @Nonnull @NotLive @Unmodifiable public Set<String> getAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return authenticationFlows;
    }

    /**
     * Set the authentication flows to use.
     * 
     * @param flows   flow identifiers to use
     */
    public void setAuthenticationFlows(@Nonnull final Collection<String> flows) {
        Constraint.isNotNull(flows, "Collection of flows cannot be null");
        
        authenticationFlows = CollectionSupport.copyToSet(StringSupport.normalizeStringCollection(flows));
    }

    /** {@inheritDoc} */
    @Nonnull @NotLive @Unmodifiable public List<String> getPostAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return postAuthenticationFlows;
    }

    /**
     * Set the ordered collection of post-authentication interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setPostAuthenticationFlows(@Nonnull final Collection<String> flows) {
        Constraint.isNotNull(flows, "Collection of flows cannot be null");
        
        postAuthenticationFlows = CollectionSupport.copyToList(StringSupport.normalizeStringCollection(flows));
    }

    /** {@inheritDoc} */
    public boolean isForceAuthn(@Nullable final ProfileRequestContext profileRequestContext) {
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
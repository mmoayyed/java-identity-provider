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

package net.shibboleth.idp.saml.profile.config.saml2;

import java.security.Principal;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.shibboleth.idp.profile.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Configuration support for SAML 2 Browser SSO. */
public class BrowserSSOProfileConfiguration extends AbstractSAML2ProfileConfiguration
        implements AuthenticationProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/saml2/sso/browser";

    /** Whether responses to the authentication request should include an attribute statement. Default value: true */
    private boolean includeAttributeStatement;

    /** Whether the response endpoint should be validated if the request is signed. */
    private boolean skipEndpointValidationWhenSigned;
    
    /**
     * The maximum amount of time, in milliseconds, the service provider should maintain a session for the user. A value
     * of 0 (the default) indicates no cap is put on the SP's session lifetime.
     */
    @Duration @NonNegative private long maximumSPSessionLifetime;

    /** Whether produced assertions may be delegated. Default value: false */
    private boolean allowingDelegation;

    /** Selects, and limits, the authentication contexts to use for requests. */
    @Nonnull @NonnullElements private List<AuthnContextClassRefPrincipal> defaultAuthenticationContexts;

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
        
        includeAttributeStatement = true;
        skipEndpointValidationWhenSigned = false;
        maximumSPSessionLifetime = 0;
        allowingDelegation = false;
    }

    /**
     * Get whether responses to the authentication request should include an attribute statement.
     * 
     * @return whether responses to the authentication request should include an attribute statement
     */
    public boolean includeAttributeStatement() {
        return includeAttributeStatement;
    }

    /**
     * Set whether responses to the authentication request should include an attribute statement.
     * 
     * @param include whether responses to the authentication request should include an attribute statement
     */
    public void setIncludeAttributeStatement(final boolean include) {
        includeAttributeStatement = include;
    }

    /**
     * Get whether the response endpoint should be validated if the request is signed.
     * 
     * @return whether the response endpoint should be validated if the request is signed
     */
    public boolean skipEndpointValidationWhenSigned() {
        return skipEndpointValidationWhenSigned;
    }

    /**
     * Set whether the response endpoint should be validated if the request is signed.
     * 
     * @param skip whether the response endpoint should be validated if the request is signed
     */
    public void setSkipEndpointValidationWhenSigned(final boolean skip) {
        skipEndpointValidationWhenSigned = skip;
    }
    
    /**
     * Get the maximum amount of time, in milliseconds, the service provider should maintain a session for the user
     * based on the authentication assertion. A value of 0 is interpreted as an unlimited lifetime.
     * 
     * @return max lifetime of service provider should maintain a session
     */
    @NonNegative public long getMaximumSPSessionLifetime() {
        return maximumSPSessionLifetime;
    }

    /**
     * Set the maximum amount of time, in milliseconds, the service provider should maintain a session for the user
     * based on the authentication assertion. A value of 0 is interpreted as an unlimited lifetime.
     * 
     * @param lifetime max lifetime of service provider should maintain a session
     */
    public void setMaximumSPSessionLifetime(@Duration @NonNegative final long lifetime) {
            maximumSPSessionLifetime = Constraint.isGreaterThanOrEqual(0, lifetime,
                    "Maximum SP session lifetime must be greater than or equal to 0");
    }

    /**
     * Get whether produced assertions may be delegated.
     * 
     * @return whether produced assertions may be delegated
     */
    public boolean isAllowingDelegation() {
        return allowingDelegation;
    }

    /**
     * Set whether produced assertions may be delegated.
     * 
     * @param isAllowed whether produced assertions may be delegated
     */
    public void setAllowingDelegation(final boolean isAllowed) {
        allowingDelegation = isAllowed;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<Principal> getDefaultAuthenticationMethods() {
        return ImmutableList.<Principal>copyOf(defaultAuthenticationContexts);
    }
    
    /**
     * Set the default authentication contexts to use, expressed as custom principals.
     * 
     * @param contexts   default authentication contexts to use
     */
    public void setDefaultAuthenticationMethods(
            @Nonnull @NonnullElements final List<AuthnContextClassRefPrincipal> contexts) {
        Constraint.isNotNull(contexts, "List of contexts cannot be null");
        
        defaultAuthenticationContexts = Lists.newArrayList(Collections2.filter(contexts, Predicates.notNull()));
    }
    
}
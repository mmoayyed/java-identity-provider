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

package net.shibboleth.idp.saml.saml2.profile.config;

import java.security.Principal;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.profile.config.AttributeResolvingProfileConfiguration;
import net.shibboleth.shared.annotation.ConfigurationSetting;
import net.shibboleth.shared.annotation.constraint.NonNegative;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;

/**
 * Configuration support for IdP and proxied SAML 2.0 Browser SSO.
 * 
 * <p>Adds settings specific issuer role for SAML 2.0, along with special features
 * needed for proxying.</p>
 */
public interface BrowserSSOProfileConfiguration
        extends net.shibboleth.saml.saml2.profile.config.BrowserSSOProfileConfiguration,
            net.shibboleth.idp.saml.profile.config.BrowserSSOProfileConfiguration,
            AuthenticationProfileConfiguration, AttributeResolvingProfileConfiguration {
    
    /** Default maximum delegation chain length. */
    @Nonnull static final Long DEFAULT_DELEGATION_CHAIN_LENGTH = 1L;
        
    /**
     * Gets whether Scoping elements in requests should be ignored/omitted.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether Scoping elements in requests should be ignored/omitted
     * 
     * @since 4.0.0
     */
    @ConfigurationSetting(name="ignoreScoping")
    boolean isIgnoreScoping(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Get condition to determine whether the response endpoint should be validated if the request is signed.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return condition
     * 
     * @since 4.0.0
     */
    @ConfigurationSetting(name="skipEndpointValidationWhenSigned")
    boolean isSkipEndpointValidationWhenSigned(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Gets whether to randomize/perturb the FriendlyName attribute when encoding SAML 2.0 Attributes to
     * enable probing of invalid behavior by relying parties.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff the FriendlyName should be randomized
     * 
     * @since 5.1.0
     */
    @ConfigurationSetting(name="randomizeFriendlyName")
    boolean isRandomizeFriendlyName(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Gets the unmodifiable collection of audiences for a proxied assertion.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return audiences for a proxied assertion
     */
    @ConfigurationSetting(name="proxyAudiences")
    @Nonnull @NotLive @Unmodifiable Set<String> getProxyAudiences(
            @Nullable final ProfileRequestContext profileRequestContext);
    
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
    @ConfigurationSetting(name="suppressAuthenticatingAuthority")
    boolean isSuppressAuthenticatingAuthority(@Nullable final ProfileRequestContext profileRequestContext);
        
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
    @ConfigurationSetting(name="proxiedAuthnInstant")
    boolean isProxiedAuthnInstant(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get whether to require signed requests.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether to require signed requests
     * 
     * @since 4.3.0
     */
    @ConfigurationSetting(name="requireSignedRequests")
    boolean isRequireSignedRequests(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Get the maximum amount of time the service provider should maintain a session for the user
     * based on the authentication assertion. A null or 0 is interpreted as an unlimited lifetime.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return max lifetime of service provider should maintain a session
     */
    @ConfigurationSetting(name="maximumSPSessionLifetime")
    @Nullable Duration getMaximumSPSessionLifetime(@Nullable final ProfileRequestContext profileRequestContext);
    
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
    boolean isAllowDelegation(@Nullable final ProfileRequestContext profileRequestContext);
    
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
    @NonNegative long getMaximumTokenDelegationChainLength(@Nullable final ProfileRequestContext profileRequestContext);
    
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
    @ConfigurationSetting(name="authnContextTranslationStrategy")
    @Nullable Function<AuthnContext,Collection<Principal>> getAuthnContextTranslationStrategy(
            @Nullable final ProfileRequestContext profileRequestContext);

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
    @ConfigurationSetting(name="authnContextTranslationStrategyEx")
    @Nullable Function<ProfileRequestContext,Collection<Principal>> getAuthnContextTranslationStrategyEx(
            @Nullable final ProfileRequestContext profileRequestContext);

}
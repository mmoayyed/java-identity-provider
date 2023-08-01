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

package net.shibboleth.idp.saml.profile.config;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.profile.config.AttributeResolvingProfileConfiguration;
import net.shibboleth.saml.profile.config.SAMLAssertionProducingProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

/**
 * Profile configuration for IdP SAML Browser SSO profiles.
 * 
 * <p>Adds settings specific to the issuer role that are cross-version.</p>
 */
public interface BrowserSSOProfileConfiguration extends AuthenticationProfileConfiguration,
        AttributeResolvingProfileConfiguration, SAMLAssertionProducingProfileConfiguration {

    /**
     * Get whether responses to the authentication request should include an attribute statement.
     *
     * <p>Default is true</p>
     * 
     * @param profileRequestContext current profile request context
     *
     * @return whether responses to the authentication request should include an attribute statement
     */
    boolean isIncludeAttributeStatement(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get the name identifier formats to use.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return the formats to use
     */
    @Nonnull @NotLive @Unmodifiable public List<String> getNameIDFormatPrecedence(
            @Nullable final ProfileRequestContext profileRequestContext);

}
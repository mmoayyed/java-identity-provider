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

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.shared.annotation.ConfigurationSetting;

/**
 * Configuration support for SAML 2.0 Attribute Query profile.
 * 
 * <p>Adds settings specific issuer role for SAML 2.0.
 * 
 * @since 5.1.0
 */
public interface AttributeQueryProfileConfiguration
        extends net.shibboleth.saml.saml2.profile.config.AttributeQueryProfileConfiguration {

    /**
     * Gets whether to randomize/perturb the FriendlyName attribute when encoding SAML 2.0 Attributes to
     * enable probing of invalid behavior by relying parties.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff the FriendlyName should be randomized
     */
    @ConfigurationSetting(name="randomizeFriendlyName")
    boolean isRandomizeFriendlyName(@Nullable final ProfileRequestContext profileRequestContext);

}
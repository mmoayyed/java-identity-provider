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

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.shared.annotation.ConfigurationSetting;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

/** Configuration support for IdP SAML 2.0 ECP profile. */
public interface ECPProfileConfiguration extends BrowserSSOProfileConfiguration, 
        net.shibboleth.saml.saml2.profile.config.ECPProfileConfiguration {

    /**
     * Get the set of local events to handle without a SOAP fault.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  truly local events
     * 
     * @since 3.3.0
     */
    @ConfigurationSetting(name="localEvents")
    @Nonnull @NotLive @Unmodifiable Set<String> getLocalEvents(
            @Nullable final ProfileRequestContext profileRequestContext);
    
}
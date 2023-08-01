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

package net.shibboleth.idp.admin;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.ext.saml2mdui.UIInfo;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.profile.FlowDescriptor;
import net.shibboleth.profile.config.AttributeResolvingProfileConfiguration;

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
public interface AdministrativeFlowDescriptor
        extends FlowDescriptor, AuthenticationProfileConfiguration, AttributeResolvingProfileConfiguration {
    
    /**
     * Get a logging ID to use when auditing this profile.
     * 
     * @return logging ID
     */
    @Nullable String getLoggingId();

    /**
     * Get whether this flow supports non-browser clients (default is true).
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether this flow supports non-browser clients
     */
    boolean isNonBrowserSupported(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Get whether user authentication is required (default is false).
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return whether user authentication is required
     */
    boolean isAuthenticated(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Get the user interface details for this profile.
     * 
     * @return user interface details
     */
    @Nullable UIInfo getUIInfo();
    
    /**
     * Get the access control policy for this flow.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return name of access control policy
     */
    @Nullable String getPolicyName(@Nullable final ProfileRequestContext profileRequestContext);

    /** {@inheritDoc} */
    @Override
    default boolean isLocal() {
        return true;
    }
}
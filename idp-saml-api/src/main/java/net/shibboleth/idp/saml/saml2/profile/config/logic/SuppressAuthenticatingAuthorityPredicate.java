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

package net.shibboleth.idp.saml.saml2.profile.config.logic;

import javax.annotation.Nullable;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.logic.AbstractRelyingPartyPredicate;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * A predicate that evaluates a {@link ProfileRequestContext} and extracts the effective
 * setting of {@link BrowserSSOProfileConfiguration#isSuppressAuthenticatingAuthority(ProfileRequestContext)}.
 * 
 * <p>Defaults to false.</p>
 * 
 * @since 4.2.0
 */
public class SuppressAuthenticatingAuthorityPredicate extends AbstractRelyingPartyPredicate {
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        final RelyingPartyContext rpc = getRelyingPartyContext(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc instanceof BrowserSSOProfileConfiguration sso) {
                return sso.isSuppressAuthenticatingAuthority(input);
            }
        }
        
        return false;
    }

}
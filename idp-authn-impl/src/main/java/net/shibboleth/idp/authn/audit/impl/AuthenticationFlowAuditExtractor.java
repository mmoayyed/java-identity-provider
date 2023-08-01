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

package net.shibboleth.idp.authn.audit.impl;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;

/** {@link Function} that returns the authentication flow ID used to satisfy a request. */
public class AuthenticationFlowAuditExtractor implements Function<ProfileRequestContext,String> {

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {

        assert input != null;
        final AuthenticationContext authnCtx = input.getSubcontext(AuthenticationContext.class);
        AuthenticationResult result = null;
        if (authnCtx != null) {
            result = authnCtx.getAuthenticationResult();
        }
        if (result != null) {
            return result.getAuthenticationFlowId();
        }
        
        return null;
    }
    
}
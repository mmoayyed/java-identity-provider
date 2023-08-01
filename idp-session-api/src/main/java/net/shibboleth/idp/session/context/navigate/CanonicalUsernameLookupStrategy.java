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

package net.shibboleth.idp.session.context.navigate;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;

/**
 * Function that returns a principal name from one of two places: a {@link SubjectCanonicalizationContext} child
 * of the root context or a {@link SessionContext}.
 */
public class CanonicalUsernameLookupStrategy implements Function<ProfileRequestContext, String> {

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        
        if (input != null) {
            final SubjectCanonicalizationContext c14nContext =
                    input.getSubcontext(SubjectCanonicalizationContext.class);
            if (c14nContext != null && c14nContext.getPrincipalName() != null) {
                return c14nContext.getPrincipalName();
            }
            
            final SessionContext sessionContext = input.getSubcontext(SessionContext.class);
            if (sessionContext != null) {
                final IdPSession idpSession = sessionContext.getIdPSession();
                if (idpSession != null) {
                    return idpSession.getPrincipalName();
                }
            }
        }
        return null;
    }

}
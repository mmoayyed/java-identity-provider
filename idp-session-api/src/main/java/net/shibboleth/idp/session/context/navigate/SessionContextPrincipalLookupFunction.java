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

import javax.annotation.Nullable;

import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;

import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;

/** A function that returns the principal name from the session inside a {@link SessionContext}. */
public class SessionContextPrincipalLookupFunction implements ContextDataLookupFunction<SessionContext,String> {

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final SessionContext input) {
        
        if (input != null) {
            final IdPSession idpSession = input.getIdPSession();
            if (idpSession != null) {
                return idpSession.getPrincipalName();
            }
        }
        return null;
    }

}
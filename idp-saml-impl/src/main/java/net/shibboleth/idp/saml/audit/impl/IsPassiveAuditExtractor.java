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

package net.shibboleth.idp.saml.audit.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnRequest;

import net.shibboleth.shared.logic.Constraint;

/** {@link Function} that returns the IsPassive attribute from an {@link AuthnRequest}. */
public class IsPassiveAuditExtractor implements Function<ProfileRequestContext,Boolean> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,AuthnRequest> requestLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public IsPassiveAuditExtractor(@Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        requestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public Boolean apply(@Nullable final ProfileRequestContext input) {
        final AuthnRequest request = requestLookupStrategy.apply(input);
        if (request != null) {
            return request.isPassive();
        }
        
        return null;
    }

}
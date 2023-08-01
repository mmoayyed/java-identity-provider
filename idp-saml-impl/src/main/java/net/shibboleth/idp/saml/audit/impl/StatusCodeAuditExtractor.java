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
import javax.xml.namespace.QName;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml2.core.StatusResponseType;

import net.shibboleth.shared.logic.Constraint;

/** {@link Function} that returns the StatusCode from a response. */
public class StatusCodeAuditExtractor implements Function<ProfileRequestContext,String> {

    /** Lookup strategy for message to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLObject> responseLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public StatusCodeAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        responseLookupStrategy = Constraint.isNotNull(strategy, "Response lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        final SAMLObject response = responseLookupStrategy.apply(input);
        if (response != null) {
            if (response instanceof Response r) {
                final org.opensaml.saml.saml1.core.Status status = r.getStatus();
                final org.opensaml.saml.saml1.core.StatusCode sc = status != null ? status.getStatusCode() : null;
                if (sc != null && sc.getValue() != null) {
                    final QName q = sc.getValue();
                    assert q != null;
                    return q.getLocalPart();
                }
            } else if (response instanceof StatusResponseType srt) {
                final org.opensaml.saml.saml2.core.Status status = srt.getStatus();
                final org.opensaml.saml.saml2.core.StatusCode sc = status != null ? status.getStatusCode() : null;
                if (sc != null) {
                    return sc.getValue();
                }
            }
        }
        
        return null;
    }

}
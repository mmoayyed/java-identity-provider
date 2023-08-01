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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.ProxyRestriction;

import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;

/**
 * {@link Function} that returns {@link ProxyRestriction#getAudiences()}.
 * 
 * @since 4.2.0
 */
public class ProxyAudienceAuditExtractor extends AbstractProxyRestrictionAuditExtractor<Collection<String>> {
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public ProxyAudienceAuditExtractor(@Nonnull final Function<ProfileRequestContext,SAMLObject> strategy) {
        super(strategy);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable @Unmodifiable @NotLive protected Collection<String> doApply(@Nullable final ProxyRestriction condition) {
        if (condition != null) {
            final List<Audience> audiences = condition.getAudiences();
            assert audiences != null;
            return audiences.stream()
                    .map(Audience::getURI)
                    .collect(Collectors.toUnmodifiableList());
        }
        
        return null;
    }
    
}
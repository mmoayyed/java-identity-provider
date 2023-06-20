/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.saml2.profile.config.navigate;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.navigate.AbstractRelyingPartyLookupFunction;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * A function that returns the allowable proxy count and audiences to include in assertions,
 * based on the results of lookup functions for local configuration merged with upstream
 * proxy restrictions to compute a final result in accordance with the standard.
 */
public class ProxyRestrictionLookupFunction extends AbstractRelyingPartyLookupFunction<Pair<Integer,Set<String>>> {

    /** SubjectContext lookup strategy. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> subjectContextLookupStrategy;
    
    /** Constructor. */
    public ProxyRestrictionLookupFunction() {
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }
    
    /**
     * Set the lookup strategy to locate the {@link SubjectContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }
    
// Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Nonnull public Pair<Integer,Set<String>> apply(@Nullable final ProfileRequestContext input) {
                
        // The proxy count is normally set to the minimum of local policy and upstream - 1, but
        // null values have to be taken into account, and 0 is the minimum.
        
        Integer proxyCount = null;

        final Set<String> audiences = new HashSet<>();
        
        final RelyingPartyContext rpc = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc instanceof BrowserSSOProfileConfiguration sso) {
                proxyCount = sso.getProxyCount(input);
                final Set<String> configAudiences = sso.getProxyAudiences(input);
                if (configAudiences != null && !configAudiences.isEmpty()) {
                    audiences.addAll(configAudiences);
                }
            }
        }

        // At this point the local configuration is derived, and lacking any upstream constraint, that applies.
        
        final SubjectContext sc = subjectContextLookupStrategy.apply(input);
        final Set<ProxyAuthenticationPrincipal> proxieds =
                sc == null ? CollectionSupport.emptySet()
                    : sc.getSubjects().stream()
                        .map(s -> s.getPrincipals(ProxyAuthenticationPrincipal.class))
                        .flatMap(Set::stream)
                        .collect(Collectors.toUnmodifiableSet());

        if (proxieds.isEmpty()) {
            return new Pair<>(proxyCount, Set.copyOf(audiences));
        }
        
        for (final ProxyAuthenticationPrincipal p : proxieds) {
            
            // Given upstream audiences, we either initialize an empty local set to that set,
            // or intersect the non-empty local set against upstream.
            
            final Set<String> upstreamAudiences = p.getAudiences();
            if (upstreamAudiences != null && !upstreamAudiences.isEmpty()) {
                if (audiences.isEmpty()) {
                    audiences.addAll(upstreamAudiences);
                } else {
                    audiences.retainAll(upstreamAudiences);
                    
                    // If the interaction is empty, we have disallowed proxying by finding no common
                    // audiences, and can immediately exit signaling no proxying.
                    
                    if (audiences.isEmpty()) {
                        return new Pair<>(0, CollectionSupport.emptySet());
                    }
                }
            }

            // Given a non-null upstream count, we reduce the local value if necessary, or possibly
            // set it for the first time. The max expression just turns -1 back into 0.
            
            final Integer upstreamCount = p.getProxyCount();
            if (upstreamCount != null) {
                if (proxyCount != null) {
                    proxyCount = Integer.min(proxyCount, Integer.max(0, upstreamCount - 1));
                } else {
                    proxyCount = Integer.max(0, upstreamCount - 1);
                }
            }
        }
        
        return new Pair<>(proxyCount, CollectionSupport.copyToSet(audiences));
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON
    
}
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

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

/**
 * Implements a set of default logic for determining the custom principals to derive the
 * {@link org.opensaml.saml.saml2.core.RequestedAuthnContext} from.
 * 
 * <p>This operates in two different scenarios: ordinary use and proxy SAML authentication use, detectable
 * by whether the input context is parent-less (the former), or the child of an {@link AuthenticationContext}.</p>
 * 
 * <p>In normal use, the value returned is empty.</p>
 * 
 * <p>In proxy use, the value returned is empty unless the parent context itself contains a child context carrying
 * particular values. In other words, the proxy default is "passthrough" of the values.</p>
 * 
 * @since 4.0.0
 */
public class ProxyAwareDefaultAuthenticationMethodsLookupFunction
        implements Function<ProfileRequestContext,Collection<AuthnContextClassRefPrincipal>> {
    
    /** Mappings to transform proxied Principals. */
    @Nonnull @NonnullElements private Map<Principal,Collection<Principal>> principalMappings;
    
    /** Constructor. */
    public ProxyAwareDefaultAuthenticationMethodsLookupFunction() {
        principalMappings = Collections.emptyMap();
    }
    
    /**
     * Sets the mappings from input/proxied Principals to zero or more equivalent values to use.
     * 
     * <p>Any values not mapped will be assumed to be passed through.</p>
     * 
     * @param mappings {@link Principal} mappings
     */
    public void setMappings(@Nullable @NonnullElements final Map<Principal,Collection<Principal>> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            principalMappings = Collections.emptyMap();
            return;
        }
        
        principalMappings = new HashMap<>(mappings.size());
        mappings.forEach((k, v) -> principalMappings.put(k, List.copyOf(v)));
    }
    
    /** {@inheritDoc} */
    @Nullable public Collection<AuthnContextClassRefPrincipal> apply(@Nullable final ProfileRequestContext input) {
        if (input != null && input.getParent() instanceof AuthenticationContext) {
            final RequestedPrincipalContext rpc = input.getParent().getSubcontext(RequestedPrincipalContext.class);
            if (rpc != null) {
                // Returns a transformed collection of the original principals, replacing any elements
                // found in the multimap with the corresponding (possibly empty) set of replacements.
                return rpc.getRequestedPrincipals().stream()
                        .map(p -> {
                            if (principalMappings.containsKey(p)) {
                                return principalMappings.get(p);
                            }
                            return Collections.singletonList(p);
                        })
                        .flatMap(Collection::stream)
                        .filter(AuthnContextClassRefPrincipal.class::isInstance)
                        .map(AuthnContextClassRefPrincipal.class::cast)
                        .collect(Collectors.toUnmodifiableList());
            }
        }
        
        return Collections.emptyList();
    }

}
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

package net.shibboleth.idp.saml.profile.config.navigate;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;

/**
 * Implements a set of default logic for determining the {@link org.opensaml.saml.saml2.core.RequestedAuthnContext}
 * operator to use. 
 * 
 * <p>This operates in two different scenarios: ordinary use and proxy SAML authentication use, detectable
 * by whether the input context is parent-less (the former), or the child of an {@link AuthenticationContext}.</p>
 * 
 * <p>In normal use, the value returned is null.</p>
 * 
 * <p>In proxy use, the value returned is null unless the parent context itself contains a child context carrying
 * a particular value. In other words, the proxy default is "passthrough" of the value.</p>
 * 
 * @since 4.0.0
 */
public class ProxyAwareAuthnContextComparisonLookupFunction implements Function<ProfileRequestContext,String> {
    
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext input) {
        if (input != null && input.getParent() instanceof AuthenticationContext) {
            final RequestedPrincipalContext rpc = input.getParent().getSubcontext(RequestedPrincipalContext.class);
            if (rpc != null) {
                return rpc.getOperator();
            }
        }
        
        return null;
    }

}
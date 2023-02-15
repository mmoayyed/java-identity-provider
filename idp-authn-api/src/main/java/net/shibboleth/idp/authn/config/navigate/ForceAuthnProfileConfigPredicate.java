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

package net.shibboleth.idp.authn.config.navigate;

import javax.annotation.Nullable;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.logic.AbstractRelyingPartyPredicate;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * A predicate that evaluates a {@link ProfileRequestContext} and determines whether forced
 * authentication should be set based on the associated {@link AuthenticationProfileConfiguration}.
 * 
 * @since 3.4.0
 */
public class ForceAuthnProfileConfigPredicate extends AbstractRelyingPartyPredicate {
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        final RelyingPartyContext rpc = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc instanceof AuthenticationProfileConfiguration) {
                return ((AuthenticationProfileConfiguration) pc).isForceAuthn(input);
            }
        }
        
        return false;
    }

}
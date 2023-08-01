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

package net.shibboleth.idp.saml.profile.config.logic;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;

/**
 * Implements a set of default logic for determining whether ForceAuthn should be applied.
 * 
 * <p>This operates in two different scenarios: ordinary use and proxy SAML authentication use, detectable
 * by whether the input context is parent-less (the former), or the child of an {@link AuthenticationContext}.</p>
 * 
 * <p>In normal use, the value returned is false, requiring it to be explicitly superceded.</p>
 * 
 * <p>In proxy use, the value returned is false unless the parent context itself indicates the use of forced
 * authentication, which was itself established in most cases from this function running previously or
 * being overridden by a default. In other words, the proxy default is "passthrough" of the value.</p>
 * 
 * @since 4.0.0
 */
public class ProxyAwareForceAuthnPredicate implements Predicate<ProfileRequestContext> {
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
     
        if (input != null) {
            final BaseContext parent = input.getParent();
            if (parent instanceof AuthenticationContext) {
                return ((AuthenticationContext) parent).isForceAuthn();
            }
        }
        
        return false;
    }

}
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

package net.shibboleth.idp.profile.config.navigate;

import java.util.Collection;

import javax.annotation.Nullable;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.navigate.AbstractRelyingPartyLookupFunction;
import net.shibboleth.idp.profile.config.InterceptorAwareProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * A function that returns {@link InterceptorAwareProfileConfiguration#getOutboundInterceptorFlows}() if such a
 * profile is available from a {@link RelyingPartyContext} obtained via a lookup function, by default a child of the
 * {@link ProfileRequestContext}.
 * 
 * <p>If a specific setting is unavailable, no values are returned.</p>
 */
public class OutboundFlowsLookupFunction extends AbstractRelyingPartyLookupFunction<Collection<String>> {

    /** {@inheritDoc} */
    @Nullable @NotLive @Unmodifiable public Collection<String> apply(@Nullable final ProfileRequestContext input) {
        final RelyingPartyContext rpc = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpc != null) {
            final ProfileConfiguration pc = rpc.getProfileConfig();
            if (pc instanceof InterceptorAwareProfileConfiguration ipc) {
                return ipc.getOutboundInterceptorFlows(input);
            }
        }
        
        return CollectionSupport.emptyList();
    }

}
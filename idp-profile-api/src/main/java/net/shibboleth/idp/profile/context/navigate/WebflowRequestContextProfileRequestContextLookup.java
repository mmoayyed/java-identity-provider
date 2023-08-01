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

package net.shibboleth.idp.profile.context.navigate;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;

/**
 * A {@link Function} that extracts the {@link ProfileRequestContext} from the current Webflow conversation. It is
 * expected that the {@link ProfileRequestContext} will be bound to the conversation attribute identified by
 * {@link ProfileRequestContext#BINDING_KEY}.
 */
public class WebflowRequestContextProfileRequestContextLookup implements
        Function<RequestContext,ProfileRequestContext> {

    /** {@inheritDoc} */
    @Nullable public ProfileRequestContext apply(@Nullable final RequestContext requestContext) {
        if (requestContext != null) {
            final Object ctx = requestContext.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
            if (ctx instanceof ProfileRequestContext) {
                return (ProfileRequestContext) ctx;
            }
        }
        
        return null;
    }
    
}
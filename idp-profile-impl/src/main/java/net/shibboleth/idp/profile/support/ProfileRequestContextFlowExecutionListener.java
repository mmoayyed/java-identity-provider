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

package net.shibboleth.idp.profile.support;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.webflow.definition.StateDefinition;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.ServletRequest;

/**
 * Exposes the {@link org.opensaml.profile.context.ProfileRequestContext} in a request attribute to make it
 * accessible outside the Webflow execution pipeline. The PRC is stored under the key
 * {@link ProfileRequestContext#BINDING_KEY}.
 *
 * @author Marvin S. Addison
 */
public class ProfileRequestContextFlowExecutionListener implements FlowExecutionListener {

    /** Logger instance. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProfileRequestContextFlowExecutionListener.class);

    @Override
    public void stateEntered(final RequestContext context, final StateDefinition previousState,
            final StateDefinition newState) {
        if (previousState != null && previousState.getId().startsWith("Initialize")) {
            assert context != null;
            final ProfileRequestContext prc = getProfileRequestContext(context);
            final ServletRequest request = getRequest(context);
            if (prc != null && request != null) {
                log.trace("Exposing ProfileRequestContext in servlet request");
                request.setAttribute(ProfileRequestContext.BINDING_KEY, prc);
            }
        }
    }

    @Override
    public void resuming(final RequestContext context) {
        assert context != null;
        final ProfileRequestContext prc = getProfileRequestContext(context);
        final ServletRequest request = getRequest(context);
        if (prc != null && request != null) {
            log.trace("Updating ProfileRequestContext in servlet request");
            request.setAttribute(ProfileRequestContext.BINDING_KEY, prc);
        }
    }

    /**
     * Get the profile request context bound to conversation scope.
     * 
     * @param context Spring request context
     * 
     * @return the bound profile request context, or null
     */
    @Nullable private ProfileRequestContext getProfileRequestContext(@Nonnull final RequestContext context) {
        final Object prc = context.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
        if (prc instanceof ProfileRequestContext) {
            return (ProfileRequestContext) prc;
        }
        return null;
    }

    /**
     * Get the servlet request.
     * 
     * @param context Spring request context
     * 
     * @return servlet request, or null
     */
    @Nullable private ServletRequest getRequest(@Nonnull final RequestContext context) {
        final Object o = context.getExternalContext().getNativeRequest();
        if (o instanceof ServletRequest) {
            return (ServletRequest) o;
        }
        return null;
    }

}
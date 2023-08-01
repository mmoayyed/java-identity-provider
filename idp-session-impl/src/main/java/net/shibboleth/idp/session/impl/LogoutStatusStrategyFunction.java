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

package net.shibboleth.idp.session.impl;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.StatusCode;

import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;

/**
 * A strategy function for determining the status of a logout based on the content of
 * a {@link LogoutContext}.
 * 
 * <p>It signals an error if the context still contains any {@link IdPSession} objects,
 * indicating a logout was cancelled.</p>
 */
public class LogoutStatusStrategyFunction implements Function<ProfileRequestContext,List<String>> {
    
    /** Lookup strategy for context. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextLookupStrategy;
    
    /** Constructor. */
    public LogoutStatusStrategyFunction() {
        logoutContextLookupStrategy = new ChildContextLookup<>(LogoutContext.class);
    }

    /**
     * Set the lookup strategy for the LogoutContext to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        
        logoutContextLookupStrategy = Constraint.isNotNull(strategy,
                "LogoutContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable @Unmodifiable @NotLive public List<String> apply(@Nullable final ProfileRequestContext input) {
        
        final LogoutContext logoutCtx = logoutContextLookupStrategy.apply(input);
        if (logoutCtx != null) {
            if (!logoutCtx.getIdPSessions().isEmpty()) {
                return CollectionSupport.listOf(StatusCode.RESPONDER, StatusCode.REQUEST_DENIED);
            }
        }
        
        return CollectionSupport.emptyList();
    }

}
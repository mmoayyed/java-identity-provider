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

package net.shibboleth.idp.saml.session.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.criterion.BestMatchLocationCriterion;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;

import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.shared.logic.Constraint;

/**
 * A strategy function for establishing an appropriate {@link BestMatchLocationCriterion}
 * based on the {@link AssertionConsumerService} location used to initiate a {@link SAML2SPSession}.
 * 
 * <p>Used during SAML 2.0 logout propagation to "fuzz" the determination of the best logout
 * endpoint to use based on the original endpoint used.</p>
 * 
 * <p>Returns null if not applicable or the ACS is unknown.</p>
 */
public class BestMatchLocationLookupStrategy implements Function<ProfileRequestContext,BestMatchLocationCriterion> {
    
    /** Lookup strategy for context. */
    @Nonnull private Function<ProfileRequestContext,LogoutPropagationContext> logoutPropagationContextLookupStrategy;
    
    /** Constructor. */
    public BestMatchLocationLookupStrategy() {
        logoutPropagationContextLookupStrategy = new ChildContextLookup<>(LogoutPropagationContext.class);
    }

    /**
     * Set the lookup strategy for the {@link LogoutPropagationContext} to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutPropagationContext> strategy) {
        
        logoutPropagationContextLookupStrategy = Constraint.isNotNull(strategy,
                "LogoutPropagationContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public BestMatchLocationCriterion apply(@Nullable final ProfileRequestContext input) {
        
        final LogoutPropagationContext propCtx = logoutPropagationContextLookupStrategy.apply(input);
        final SPSession session = propCtx != null ? propCtx.getSession() : null;
        if (session != null && session instanceof SAML2SPSession) {
            final String acsLocation = ((SAML2SPSession) session).getACSLocation();
            if (acsLocation != null) {
                return new BestMatchLocationCriterion(acsLocation);
            }
        }
        
        return null;
    }

}
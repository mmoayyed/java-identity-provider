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

package net.shibboleth.idp.authn.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * An action that creates and populates a {@link MultiFactorAuthenticationContext} with the set of
 * transition rules to use for coordinating activity, and with any active "factors" found,
 * if an active result from the MFA flow is present in the {@link AuthenticationContext}.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @post <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getSubcontext(
 *  MultiFactorAuthenticationContext.class) != null</pre>
 */
public class PopulateMultiFactorAuthenticationContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateMultiFactorAuthenticationContext.class);
    
    /** Map of login "factors" (flows) and the transition rules to run after them. */
    @Nonnull @NonnullElements private Map<String,MultiFactorAuthenticationTransition> transitionMap;
    
    /** Lookup/creation function for the context to populate. */
    @Nonnull
    private Function<ProfileRequestContext,MultiFactorAuthenticationContext> multiFactorContextCreationStrategy;
    
    /** Lookup strategy for active "factors" that may already be usable. */
    @Nullable private Function<ProfileRequestContext,Collection<AuthenticationResult>> activeResultLookupStrategy;
    
    /** Constructor. */
    PopulateMultiFactorAuthenticationContext() {
        transitionMap = Collections.emptyMap();
        multiFactorContextCreationStrategy = Functions.compose(
                new ChildContextLookup(MultiFactorAuthenticationContext.class, true),
                new ChildContextLookup(AuthenticationContext.class));
    }
    
    /**
     * Set the map of transitions to apply.
     * 
     * @param map map of transition logic
     */
    public void setTransitionMap(@Nonnull @NonnullElements final Map<String,MultiFactorAuthenticationTransition> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(map, "Map cannot be null");
        
        transitionMap = new HashMap<>(map);
    }
    
    /**
     * Set the lookup/creation strategy to use for the context to populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setMultiFactorContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,MultiFactorAuthenticationContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        multiFactorContextCreationStrategy = Constraint.isNotNull(strategy,
                "MultiFactorAuthenticationContext creation strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy for any active "factors" that may be reusable.
     * 
     * <p>The default strategy is to look for an active {@link AuthenticationResult} of the flow
     * currently being attempted, and check within it for {@link AuthenticationResultPrincipal} objects.</p>
     * 
     * @param strategy lookup strategy
     */
    public void setActiveResultLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<AuthenticationResult>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        activeResultLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final MultiFactorAuthenticationContext mfaCtx = multiFactorContextCreationStrategy.apply(profileRequestContext);
        if (mfaCtx == null) {
            log.error("{} Unable to create/access MultiFactorAuthenticationContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
        }
        
        mfaCtx.setTransitionMap(transitionMap);
        
        if (activeResultLookupStrategy != null) {
            final Collection<AuthenticationResult> results = activeResultLookupStrategy.apply(profileRequestContext);
            int count = 0;
            long now = System.currentTimeMillis();
            if (results != null && !results.isEmpty()) {
                for (final AuthenticationResult result : results) {
                    final AuthenticationFlowDescriptor descriptor =
                            authenticationContext.getAvailableFlows().get(result.getAuthenticationFlowId());
                    if (descriptor != null) {
                        if (result.getAuthenticationInstant() + descriptor.getLifetime() <= now) {
                            mfaCtx.getActiveResults().put(descriptor.getId(), result);
                            ++count;
                        } else {
                            log.debug("{} Result from login flow {} has expired", getLogPrefix(), descriptor.getId());
                        }
                    } else {
                        log.warn("{} Ignoring active result from unconfigured login flow {}", getLogPrefix(),
                                result.getAuthenticationFlowId());
                    }
                }
            }
            log.debug("{} {} active result(s) extracted for possible reuse", getLogPrefix(), count);
        } else {
            log.debug("{} No lookup strategy provided, no active results will be made available", getLogPrefix());
        }
    }
    
}
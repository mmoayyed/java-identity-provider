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

package net.shibboleth.idp.session.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An authentication action that populates a {@link AuthenticationContext} with the active
 * {@link AuthenticationResult} objects found in a {@link SessionContext} that is a direct
 * child of the {@link ProfileRequestContext}.
 * 
 * <p>Only results from flows in the "potentialFlows" collection in the {@link AuthenticationContext}
 * are extracted, which prevents cross-contamination between SPs that have differing rules established
 * for which flows are to be active, because the potentialFlows collection is filtered by that
 * criterion.</p>
 * 
 * <p>If {@link AuthenticationContext#getHintedName()} is null, then it is populated with the
 * principal name from the session.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post AuthenticationContext is modified as above.
 */
public class ExtractActiveAuthenticationResults extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractActiveAuthenticationResults.class);

    /** Lookup function for SessionContext. */
    @Nonnull private Function<ProfileRequestContext,SessionContext> sessionContextLookupStrategy;
    
    /** Session to copy results from. */
    @Nullable private IdPSession session;
    
    /** Constructor. */
    public ExtractActiveAuthenticationResults() {
        sessionContextLookupStrategy = new ChildContextLookup<>(SessionContext.class);
    }
    
    /**
     * Set the lookup strategy for the SessionContext to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setSessionContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SessionContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        sessionContextLookupStrategy = Constraint.isNotNull(strategy,
                "SessionContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        final SessionContext ctx = sessionContextLookupStrategy.apply(profileRequestContext);
        if (ctx != null) {
            session = ctx.getIdPSession();
            if (session != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (authenticationContext.getHintedName() == null) {
            authenticationContext.setHintedName(session.getPrincipalName());
        }
        
        final Instant now = Instant.now();
        final Duration maxAge = authenticationContext.getMaxAge();
        
        final List<AuthenticationResult> actives = new ArrayList<>();
        for (final AuthenticationResult result : session.getAuthenticationResults()) {
            final AuthenticationFlowDescriptor descriptor =
                    authenticationContext.getPotentialFlows().get(result.getAuthenticationFlowId());
            if (descriptor == null) {
                log.debug("{} Authentication result {} has no corresponding flow descriptor, considering inactive", 
                        getLogPrefix(), result.getAuthenticationFlowId());
                continue;
            }
            
            if (descriptor.isResultActive(result)) {
                if (maxAge != null && result.getAuthenticationInstant().plus(maxAge).isBefore(now)) {
                    log.debug("{} Authentication result {} exceeds maxAge setting, skipping it", getLogPrefix(),
                            result.getAuthenticationFlowId());
                    continue;
                }
                
                log.debug("{} Authentication result {} is active, copying from session", getLogPrefix(),
                        result.getAuthenticationFlowId());
                actives.add(result);
            } else {
                log.debug("{} Authentication result {} is inactive, skipping it", getLogPrefix(),
                        result.getAuthenticationFlowId());
            }
        }
        
        if (actives.isEmpty()) {
            log.debug("{} No active authentication results, SSO will not be possible", getLogPrefix());
        }
        
        authenticationContext.setActiveResults(actives);
    }
    
}
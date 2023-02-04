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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

import com.google.common.base.Predicates;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.servlet.HttpServletSupport;

/**
 * A profile action that populates a {@link SessionContext} with an active, valid
 * {@link IdPSession}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post As above, and the session will be bound to the client's address if the underlying
 *  {@link SessionResolver} is configured to do so.
 */
public class PopulateSessionContext extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateSessionContext.class);
    
    /** Session resolver. */
    @NonnullAfterInit private SessionResolver sessionResolver;

    /** Creation/lookup function for SessionContext. */
    @Nonnull private Function<ProfileRequestContext,SessionContext> sessionContextCreationStrategy;
    
    /** Function to return {@link CriteriaSet} to give to session resolver. */
    @Nonnull private Function<ProfileRequestContext,CriteriaSet> sessionResolverCriteriaStrategy;
    
    /** Function to override source of address to bind session. */
    @Nullable private Function<ProfileRequestContext,String> addressLookupStrategy;
        
    /** Constructor. */
    public PopulateSessionContext() {
        sessionContextCreationStrategy = new ChildContextLookup<>(SessionContext.class, true);
        sessionResolverCriteriaStrategy = prc -> new CriteriaSet(new HttpServletRequestCriterion());
    }
    
    /**
     * Set the {@link SessionResolver} to use.
     * 
     * @param resolver  session resolver to use
     */
    public void setSessionResolver(@Nonnull final SessionResolver resolver) {
        checkSetterPreconditions();
        sessionResolver = Constraint.isNotNull(resolver, "SessionResolver cannot be null");
    }
    
    /**
     * Set the creation/lookup strategy for the SessionContext to populate.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setSessionContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,SessionContext> strategy) {
        checkSetterPreconditions();
        sessionContextCreationStrategy = Constraint.isNotNull(strategy,
                "SessionContext creation strategy cannot be null");
    }
    
    /**
     * Set an optional lookup strategy to obtain the address to which to bind the session.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.2.0
     */
    public void setAddressLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        addressLookupStrategy = strategy;
    }
    
    /**
     * Set the strategy for building the {@link CriteriaSet} to feed into the {@link SessionResolver}.
     * 
     * @param strategy  building strategy
     */
    public void setSessionResolverCriteriaStrategy(
            @Nonnull final Function<ProfileRequestContext,CriteriaSet> strategy) {
        sessionResolverCriteriaStrategy = Constraint.isNotNull(strategy,
                "SessionResolver CriteriaSet strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (!getActivationCondition().equals(Predicates.alwaysFalse()) && sessionResolver == null) {
            throw new ComponentInitializationException("SessionResolver cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        IdPSession session = null;
        try {
            session = sessionResolver.resolveSingle(sessionResolverCriteriaStrategy.apply(profileRequestContext));
            if (session == null) {
                log.debug("{} No session found for client", getLogPrefix());
                return;
            } else if (!session.checkTimeout()) {
                log.info("{} Session {} no longer valid due to inactivity", getLogPrefix(), session.getId());
                return;
            }
            
            String addr = null;
            if (addressLookupStrategy != null) {
                addr = addressLookupStrategy.apply(profileRequestContext);
            } else {
                final HttpServletRequest request = getHttpServletRequest();
                addr = request != null ? HttpServletSupport.getRemoteAddr(request) : null;
            }
            if (addr != null) {
                if (!session.checkAddress(addr)) {
                    return;
                }
            } else {
                log.info("{} No client address available, skipping address check for session {}", getLogPrefix(),
                        session.getId());
            }
            
            final SessionContext sessionCtx = sessionContextCreationStrategy.apply(profileRequestContext);
            if (sessionCtx == null) {
                log.error("{} Unable to create or locate SessionContext", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
                return;
            }
            
            sessionCtx.setIdPSession(session);
            
        } catch (final ResolverException e) {
            log.error("{} Error resolving a session for the active client", getLogPrefix(), e);
        } catch (final SessionException e) {
            log.error("{} Error during timeout or address checking for session {}",getLogPrefix(), session.getId(), e);
        }
    }

}
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
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.idp.session.criterion.HttpServletRequestCriterion;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.net.HttpServletSupport;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Profile action that resolves an active session from the profile request, and records it,
 * populating the associated {@link SPSession} objects into a {@link LogoutContext}.
 * 
 * <p>A {@link SubjectContext} and {@link SessionContext} are also populated.</p>
 * 
 * <p>Each {@link SPSession} is also assigned a unique number and inserted into the map
 * returned by {@link LogoutContext#getKeyedSessionMap()}.</p> 
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_PROFILE_CTX}
 * @event {@link org.opensaml.profile.action.EventIds#IO_ERROR}
 * @post If a {@link IdPSession} was found, then a {@link SubjectContext} and {@link LogoutContext} will be populated.
 */
public class ProcessLogout extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessLogout.class);
    
    /** Session resolver. */
    @NonnullAfterInit private SessionResolver sessionResolver;

    /** Creation/lookup function for SubjectContext. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> subjectContextCreationStrategy;

    /** Creation/lookup function for SessionContext. */
    @Nonnull private Function<ProfileRequestContext,SessionContext> sessionContextCreationStrategy;

    /** Creation/lookup function for LogoutContext. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextCreationStrategy;
    
    /** Function to return {@link CriteriaSet} to give to session resolver. */
    @Nonnull private Function<ProfileRequestContext,CriteriaSet> sessionResolverCriteriaStrategy;
    
    /** Function to override source of address to bind session. */
    @Nullable private Function<ProfileRequestContext,String> addressLookupStrategy;
    
    /** Constructor. */
    public ProcessLogout() {
        subjectContextCreationStrategy = new ChildContextLookup<>(SubjectContext.class, true);
        sessionContextCreationStrategy = new ChildContextLookup<>(SessionContext.class, true);
        logoutContextCreationStrategy = new ChildContextLookup<>(LogoutContext.class, true);
        
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
     * Set the creation/lookup strategy for the SubjectContext to populate.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setSubjectContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        checkSetterPreconditions();
        subjectContextCreationStrategy = Constraint.isNotNull(strategy,
                "SubjectContext creation strategy cannot be null");
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
     * Set the creation/lookup strategy for the LogoutContext to populate.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setLogoutContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        checkSetterPreconditions();
        logoutContextCreationStrategy = Constraint.isNotNull(strategy,
                "LogoutContext creation strategy cannot be null");
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

    /**
     * Set an optional lookup strategy to obtain the address to which to validate the session.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.2.0
     */
    public void setAddressLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        addressLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (!getActivationCondition().equals(Predicates.alwaysFalse())) {
            if (sessionResolver == null) {
                throw new ComponentInitializationException("SessionResolver cannot be null");
            }
        }
    }

// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            final IdPSession session =
                    sessionResolver.resolveSingle(sessionResolverCriteriaStrategy.apply(profileRequestContext));
            if (session == null) {
                log.info("{} No active session found matching current request", getLogPrefix());
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
                try {
                    if (!session.checkAddress(addr)) {
                        return;
                    }
                } catch (final SessionException e) {
                    log.error("{} Error validating session against client address", getLogPrefix(), e);
                    ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
                    return;
                } 
            } else {
                log.info("{} No client address available, skipping address check for session",
                        getLogPrefix());
            }

            final SubjectContext subjectCtx = subjectContextCreationStrategy.apply(profileRequestContext);
            if (subjectCtx != null) {
                subjectCtx.setPrincipalName(session.getPrincipalName());
            }

            final SessionContext sessionCtx = sessionContextCreationStrategy.apply(profileRequestContext);
            if (sessionCtx != null) {
                sessionCtx.setIdPSession(session);
            }

            final LogoutContext logoutCtx = logoutContextCreationStrategy.apply(profileRequestContext);
            if (logoutCtx == null) {
                log.error("{} Unable to create or locate LogoutContext", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
                return;
            }
            
            logoutCtx.getIdPSessions().add(session);
            
            int count = 1;
            for (final SPSession spSession : session.getSPSessions()) {
                logoutCtx.getSessionMap().put(spSession.getId(), spSession);
                logoutCtx.getKeyedSessionMap().put(Integer.toString(count++), spSession);
            }
                
        } catch (final ResolverException e) {
            log.error("{} Error resolving matching session(s)", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON
    
}
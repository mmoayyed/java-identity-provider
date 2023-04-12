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

import com.google.common.base.Predicates;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Profile action that destroys any {@link IdPSession}s found in a {@link LogoutContext}.
 * 
 * <p>If a {@link SessionContext} is found, the corresponding session is also unbound
 * from the client and the {@link SessionContext} is removed.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#IO_ERROR}
 * @post The sessions are removed from the session manager.
 * @post The sessions are removed from the {@link LogoutContext}.
 * @post The {@link SessionContext} is removed if it matched one of the sessions destroyed.
 * 
 * @since 4.0.0
 */
public class DestroySessions extends AbstractProfileAction {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DestroySessions.class);
    
    /** Session resolver. */
    @NonnullAfterInit private SessionManager sessionManager;

    /** Lookup function for SessionContext. */
    @Nonnull private Function<ProfileRequestContext,SessionContext> sessionContextLookupStrategy;

    /** Lookup function for LogoutContext. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextLookupStrategy;
    
    /** SessionContext to access. */
    @Nullable private SessionContext sessionContext;

    /** LogoutContext to access. */
    @NonnullBeforeExec private LogoutContext logoutContext;
    
    /** Constructor. */
    public DestroySessions() {
        sessionContextLookupStrategy = new ChildContextLookup<>(SessionContext.class);
        logoutContextLookupStrategy = new ChildContextLookup<>(LogoutContext.class);
    }
    
    /**
     * Set the {@link SessionManager} to use.
     * 
     * @param manager  session manager to use
     */
    public void setSessionManager(@Nonnull final SessionManager manager) {
        checkSetterPreconditions();
        sessionManager = Constraint.isNotNull(manager, "SessionManager cannot be null");
    }
    
    /**
     * Set the lookup strategy for the SessionContext to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setSessionContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SessionContext> strategy) {
        checkSetterPreconditions();
        sessionContextLookupStrategy = Constraint.isNotNull(strategy,
                "SessionContext lookup strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy for the LogoutContext to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        checkSetterPreconditions();
        logoutContextLookupStrategy = Constraint.isNotNull(strategy,
                "LogoutContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (!getActivationCondition().equals(Predicates.alwaysFalse())) {
            if (sessionManager == null) {
                throw new ComponentInitializationException("SessionManager cannot be null");
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        logoutContext = logoutContextLookupStrategy.apply(profileRequestContext);
        if (logoutContext == null || logoutContext.getIdPSessions().isEmpty()) {
            log.debug("{} No LogoutContext or IdPSessions found, nothing to do", getLogPrefix());
            return false;
        }
        
        sessionContext = sessionContextLookupStrategy.apply(profileRequestContext);
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        SessionContext sc = sessionContext;
        for (final IdPSession session : logoutContext.getIdPSessions()) {
            assert session!= null;
            final IdPSession idpSession = sc != null ? sc.getIdPSession() : null;
            log.debug("{} Attempting destruction of session {}", getLogPrefix(), session.getId());
            
            final boolean unbind = idpSession != null ? idpSession.equals(session) : false;
            if (unbind) {
                assert sc != null;
                sc.removeFromParent();
                sc = sessionContext = null;
            }
            
            try {
                final String id = session.getId();
                assert id != null;
                sessionManager.destroySession(id, unbind);
            } catch (final SessionException e) {
                log.error("{} Error destroying session", getLogPrefix(), e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
        }
        
        logoutContext.getIdPSessions().clear();
    }
    
}
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

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An authentication action that establishes a record of the {@link net.shibboleth.idp.authn.AuthenticationResult}
 * in an {@link IdPSession} for the client, either by updating an existing session or creating a new one.
 * 
 * <p>A new {@link net.shibboleth.idp.authn.AuthenticationResult} may be added to the session, or the last activity
 * time of an existing one updated. A new one will only be added if the authentication context indicates that the
 * result is "cacheable".</p>
 * 
 * <p>An existing session is identified via a {@link SessionContext} attached to the {@link ProfileRequestContext}.
 * If a new session is created, it will be placed into a {@link SessionContext}, creating it if necessary, with the
 * principal name coming from a {@link SubjectContext}.</p>
 * 
 * <p>An error interacting with the session layer will result in an {@link EventIds#IO_ERROR} event.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * @post If AuthenticationContext.getAuthenticationResult() != null and
 * SubjectContext.getPrincipalName() != null then the steps above are performed,
 * and ProfileRequestContext.getSubcontext(SessionContext.class).getIdPSession() != null
 */
public class UpdateSessionWithAuthenticationResult extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(UpdateSessionWithAuthenticationResult.class);

    /** SessionManager. */
    @NonnullAfterInit private SessionManager sessionManager;

    /** Lookup/creation function for SessionContext. */
    @Nonnull private Function<ProfileRequestContext,SessionContext> sessionContextCreationStrategy;

    /** Lookup function for SubjectContext. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> subjectContextLookupStrategy;
    
    /** Existing or newly created SessionContext. */
    @NonnullBeforeExec private SessionContext sessionCtx;

    /** Existing SubjectContext. */
    @NonnullBeforeExec private SubjectContext subjectCtx;
    
    /** Constructor. */
    public UpdateSessionWithAuthenticationResult() {
        sessionContextCreationStrategy = new ChildContextLookup<>(SessionContext.class, true);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
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
     * Set the lookup/creation strategy for the SessionContext to update.
     * 
     * @param strategy  creation/lookup strategy
     */
    public void setSessionContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,SessionContext> strategy) {
        checkSetterPreconditions();
        sessionContextCreationStrategy = Constraint.isNotNull(strategy,
                "SessionContext lookup/creation strategy cannot be null");
    }
    
    /**
     * Set the lookup strategy for the SubjectContext to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        checkSetterPreconditions();
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (!PredicateSupport.isAlwaysFalse(getActivationCondition())) {
            if (sessionManager == null) {
                throw new ComponentInitializationException("SessionManager cannot be null");
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (super.doPreExecute(profileRequestContext, authenticationContext)
                && authenticationContext.getAuthenticationResult() != null) {
            subjectCtx = subjectContextLookupStrategy.apply(profileRequestContext);
            sessionCtx = sessionContextCreationStrategy.apply(profileRequestContext);
            if (sessionCtx == null) {
                log.error("{} SessionContext creation failed", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
                return false;
            }
            
            // We can only do work if a session exists or a non-empty SubjectContext exists.
            return sessionCtx.getIdPSession() != null || (subjectCtx != null && subjectCtx.getPrincipalName() != null);
        }
        
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final IdPSession session = sessionCtx.getIdPSession();
        if (session != null) {
            try {
                updateIdPSession(authenticationContext, session);
            } catch (final SessionException e) {
                log.error("{} Error updating session {}", getLogPrefix(), session.getId(), e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
        } else {
            try {
                createIdPSession(authenticationContext);
            } catch (final SessionException e) {
                final String principalName = subjectCtx.getPrincipalName();
                log.error("{} Error creating session for principal {}", getLogPrefix(), principalName, e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
        }
    }

    /**
     * Update an existing session.
     * 
     * <p>If the result is the product of an attempted flow, then it's added to the session.
     * If reused, its last activity time is updated.</p>
     * 
     * @param authenticationContext current authentication context
     * @param session session to update
     * @throws SessionException if an error occurs updating the session
     */
    private void updateIdPSession(@Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final IdPSession session) throws SessionException {
        
        final AuthenticationResult ar = authenticationContext.getAuthenticationResult();
        assert ar != null;
        if (authenticationContext.getAttemptedFlow() != null) {
            if (authenticationContext.isResultCacheable()) {
                log.debug("{} Adding new AuthenticationResult for flow {} to existing session {}", getLogPrefix(),
                        ar.getAuthenticationFlowId(), session.getId());
                session.addAuthenticationResult(ar);
            }
        } else {
            log.debug("{} Updating activity time on reused AuthenticationResult for flow {} in existing session {}",
                    getLogPrefix(), ar.getAuthenticationFlowId(),
                    session.getId());
            session.updateAuthenticationResultActivity(ar);
        }
    }
    
    /**
     * Create a new session and populate the SessionContext.
     * 
     * @param authenticationContext current authentication context
     * @throws SessionException if an error occurs creating the session
     */
    private void createIdPSession(@Nonnull final AuthenticationContext authenticationContext)
            throws SessionException {

        final SubjectContext sbc = subjectCtx;
        assert sbc != null && isPreExecuteCalled();
        final String principalName = sbc.getPrincipalName();
        assert principalName != null;
        log.debug("{} Creating new session for principal {}", getLogPrefix(), principalName);
        
        sessionCtx.setIdPSession(sessionManager.createSession(principalName));
        if (authenticationContext.isResultCacheable()) {
            final AuthenticationResult ar = authenticationContext.getAuthenticationResult();
            assert ar != null;
            final IdPSession idPSession = sessionCtx.getIdPSession();
            assert idPSession != null;
            idPSession.addAuthenticationResult(ar);
        }
    }
}
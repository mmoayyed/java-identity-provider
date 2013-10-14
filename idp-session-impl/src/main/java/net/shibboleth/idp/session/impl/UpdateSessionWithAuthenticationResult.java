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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * An authentication action that establishes a record of the {@link AuthenticationResult} in an
 * {@link IdPSession} for the client, either by updating an existing session or creating a new
 * one.
 * 
 * <p>A new {@link AuthenticationResult} may be added to the session, or the last activity time
 * of an existing one updated.</p>
 * 
 * <p>An existing session is identified via a {@link SessionContext} attached to the
 * {@link ProfileRequestContext}. If a new session is created, it will be placed into a
 * {@link SessionContext}, creating it if necessary.</p>
 * 
 * <p>An error interacting with the session layer will result in an {@link EventIds#IO_ERROR}
 * event.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If AuthenticationContext.getAuthenticationResult() != null and
 * AuthenticationContext.getCanonicalPrincipalName() != null then the steps above are performed,
 * and ProfileRequestContext.getSubcontext(SessionContext.class, false).getIdPSession() != null
 */
public class UpdateSessionWithAuthenticationResult extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(UpdateSessionWithAuthenticationResult.class);

    /** SessionManager. */
    @NonnullAfterInit private SessionManager sessionManager;

    /** Existing or newly created SessionContext. */
    @Nullable private SessionContext sessionCtx;
    
    /** Flag to turn action on or off. */
    private boolean enabled;
    
    /** Constructor. */
    public UpdateSessionWithAuthenticationResult() {
        enabled = true;
    }
    
    /**
     * Set the {@link SessionManager} to use.
     * 
     * @param manager  session manager to use
     */
    public void setSessionManager(@Nonnull final SessionManager manager) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        sessionManager = Constraint.isNotNull(manager, "SessionManager cannot be null");
    }
    
    /**
     * Set whether the action should run or not.
     * 
     * @param flag flag to set
     */
    public void setEnabled(final boolean flag) {
        enabled = flag;
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (enabled && sessionManager == null) {
            throw new ComponentInitializationException("SessionManager cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (enabled && !Strings.isNullOrEmpty(authenticationContext.getCanonicalPrincipalName())
                && authenticationContext.getAuthenticationResult() != null) {
            
            sessionCtx = profileRequestContext.getSubcontext(SessionContext.class, true);
            return true;
        }
        
        return false;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        IdPSession session = sessionCtx.getIdPSession();
        if (session != null) {
            try {
                updateIdPSession(authenticationContext, session);
            } catch (SessionException e) {
                log.error(getLogPrefix() + " Error updating session " + session.getId(), e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
        } else {
            try {
                createIdPSession(authenticationContext);
            } catch (SessionException e) {
                log.error(getLogPrefix() + " Error creating session for "
                        + authenticationContext.getCanonicalPrincipalName(), e);
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
        
        if (authenticationContext.getAttemptedFlow() != null) {
            session.addAuthenticationResult(authenticationContext.getAuthenticationResult());
        } else {
            session.updateAuthenticationResultActivity(authenticationContext.getAuthenticationResult());
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
        
        sessionCtx.setIdPSession(sessionManager.createSession(authenticationContext.getCanonicalPrincipalName()));
        sessionCtx.getIdPSession().addAuthenticationResult(authenticationContext.getAuthenticationResult());
    }
}
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

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
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

/**
 * An authentication action that destroys a pre-existing session and clears {@link AuthenticationContext}
 * and {@link SessionContext} state such that no trace of its impact on the contexts remains.
 * 
 * <p>An error interacting with the session layer will result in an {@link EventIds#IO_ERROR}
 * event.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 * @pre <pre>ProfileRequestContext.getSubcontext(SessionContext.class, false).getIdPSession() != null</pre>
 * @post <pre>ProfileRequestContext.getSubcontext(SessionContext.class, false).getIdPSession() == null</pre>
 * @post <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getCanonicalPrincipalName()
 *  == null</pre>
 * @post <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getActiveResults().isEmpty()</pre>
 */
public class InvalidateSession extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InvalidateSession.class);

    /** SessionManager. */
    @NonnullAfterInit private SessionManager sessionManager;

    /** SessionContext to operate on. */
    @Nullable private SessionContext sessionCtx;
    
    /**
     * Set the {@link SessionManager} to use.
     * 
     * @param manager  session manager to use
     */
    public void setSessionManager(@Nonnull final SessionManager manager) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        sessionManager = Constraint.isNotNull(manager, "SessionManager cannot be null");
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (sessionManager == null) {
            throw new ComponentInitializationException("SessionManager cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        sessionCtx = profileRequestContext.getSubcontext(SessionContext.class, false);
        if (sessionCtx == null || sessionCtx.getIdPSession() == null) {
            log.warn("{} No IdPSession found in SessionContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        log.info("{} Identity switch detected, destroying original session {} for principal {}",
                getLogPrefix(), sessionCtx.getIdPSession().getId(), sessionCtx.getIdPSession().getPrincipalName());
        
        try {
            sessionManager.destroySession(sessionCtx.getIdPSession().getId());
        } catch (SessionException e) {
            log.error(getLogPrefix() + " Error destroying session " + sessionCtx.getIdPSession().getId(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
        
        // Establish context state as if the original session didn't exist.
        sessionCtx.setIdPSession(null);
        authenticationContext
            .setCanonicalPrincipalName(null)
            .setActiveResults(Collections.<AuthenticationResult>emptyList());
    }

}
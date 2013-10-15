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
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
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
 * An authentication action that checks for a mismatch between an existing session's identity and
 * the result of a newly canonicalized subject (from a {@link SubjectCanonicalizationContext}).
 * 
 * <p>On a mismatch, it destroys a pre-existing session and clears {@link AuthenticationContext}
 * and {@link SessionContext} state such that no trace of its impact on the contexts remains.
 * 
 * <p>An error interacting with the session layer will result in an {@link EventIds#IO_ERROR}
 * event.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 * @post If an identity switch is detected, SessionContext.getIdPSession() == null
 *  && AuthenticationContext.getActiveResults().isEmpty()
 */
public class InvalidateSessionOnIdentitySwitch extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InvalidateSessionOnIdentitySwitch.class);

    /** SessionManager. */
    @NonnullAfterInit private SessionManager sessionManager;

    /** SessionContext to operate on. */
    @Nullable private SessionContext sessionCtx;
    
    /** A newly established principal name to check. */
    @Nullable private String newPrincipalName;
    
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
            log.debug("{} No previous session found, nothing to do", getLogPrefix());
            return false;
        }
        
        SubjectCanonicalizationContext c14n =
                profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false);
        if (c14n == null || c14n.getPrincipalName() == null) {
            log.debug("{} Reusing identity from session, nothing to do", getLogPrefix());
            return false;
        }

        newPrincipalName = c14n.getPrincipalName();
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (sessionCtx.getIdPSession().getPrincipalName().equals(newPrincipalName)) {
            log.debug("{} Identities from session and new authentication result match, nothing to do");
            return;
        }
        
        log.info("{} Identity switch to {} detected, destroying original session {} for principal {}",
                getLogPrefix(), newPrincipalName, sessionCtx.getIdPSession().getId(),
                sessionCtx.getIdPSession().getPrincipalName());
        
        try {
            sessionManager.destroySession(sessionCtx.getIdPSession().getId());
        } catch (SessionException e) {
            log.error(getLogPrefix() + " Error destroying session " + sessionCtx.getIdPSession().getId(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
        
        // Establish context state as if the original session didn't exist.
        sessionCtx.setIdPSession(null);
        authenticationContext.setActiveResults(Collections.<AuthenticationResult>emptyList());
    }

}
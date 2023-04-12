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
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionManager;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;


/**
 * An authentication action that checks for a mismatch between an existing session's identity and
 * the result of a newly canonicalized subject (from a {@link SubjectCanonicalizationContext}).
 * 
 * <p>On a mismatch it destroys a pre-existing session and clears {@link AuthenticationContext}
 * and {@link SessionContext} state such that no trace of its impact on the contexts remains, and
 * signals the event.</p>
 * 
 * <p>An error interacting with the session layer will result in an {@link EventIds#IO_ERROR} event.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 * @event {@link AuthnEventIds#IDENTITY_SWITCH}
 * @post If an identity switch is detected, SessionContext.getIdPSession() == null
 *  &amp;&amp; AuthenticationContext.getActiveResults().isEmpty()
 */
public class DetectIdentitySwitch extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DetectIdentitySwitch.class);

    /** SessionManager. */
    @NonnullAfterInit private SessionManager sessionManager;

    /** Lookup function for SessionContext. */
    @Nonnull private Function<ProfileRequestContext,SessionContext> sessionContextLookupStrategy;

    /** Lookup function for SubjectCanonicalizationContext. */
    @Nonnull private Function<ProfileRequestContext,SubjectCanonicalizationContext> c14nContextLookupStrategy;
    
    /** SessionContext to operate on. */
    @NonnullBeforeExec private SessionContext sessionCtx;
    
    /** A newly established principal name to check. */
    @NonnullBeforeExec private String newPrincipalName;
    
    /** Constructor. */
    public DetectIdentitySwitch() {
        sessionContextLookupStrategy = new ChildContextLookup<>(SessionContext.class);
        c14nContextLookupStrategy = new ChildContextLookup<>(SubjectCanonicalizationContext.class);
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
     * Set the lookup strategy for the SubjectCanonicalizationContext to access.
     * 
     * @param strategy  lookup strategy
     */
    public void setSubjectCanonicalizationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectCanonicalizationContext> strategy) {
        checkSetterPreconditions();
        c14nContextLookupStrategy = Constraint.isNotNull(strategy,
                "SubjectCanonicalizationContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (sessionManager == null) {
            throw new ComponentInitializationException("SessionManager cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        final SessionContext ctx =  sessionCtx = sessionContextLookupStrategy.apply(profileRequestContext);
        if (ctx == null || ctx.getIdPSession() == null) {
            log.debug("{} No previous session found, nothing to do", getLogPrefix());
            return false;
        }
        
        final SubjectCanonicalizationContext c14n = c14nContextLookupStrategy.apply(profileRequestContext);
        if (c14n == null || c14n.getPrincipalName() == null) {
            log.debug("{} Reusing identity from session, nothing to do", getLogPrefix());
            return false;
        }

        newPrincipalName = c14n.getPrincipalName();
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final IdPSession idpSession = sessionCtx.getIdPSession();
        assert idpSession != null;
        if (idpSession.getPrincipalName().equals(newPrincipalName)) {
            log.debug("{} Identities from session and new authentication result match, nothing to do", getLogPrefix());
            return;
        }
        
        log.info("{} Identity switch to {} detected, destroying original session {} for principal {}",
                getLogPrefix(), newPrincipalName, idpSession.getId(),
                idpSession.getPrincipalName());
        
        try {
            final String id = idpSession.getId();
            assert id != null;
            sessionManager.destroySession(id, true);
        } catch (final SessionException e) {
            log.error("{} Error destroying session {}", getLogPrefix(), idpSession.getId(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
        
        // Establish context state as if the original session didn't exist.
        sessionCtx.setIdPSession(null);
        authenticationContext.setActiveResults(CollectionSupport.<AuthenticationResult>emptyList());
        
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.IDENTITY_SWITCH);
    }

}
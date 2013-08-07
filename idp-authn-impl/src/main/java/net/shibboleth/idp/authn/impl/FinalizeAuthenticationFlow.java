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

import java.security.Principal;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.CloneablePrincipal;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An authentication action that runs at the end of a completed authentication flow and
 * finalizes the content of the {@link AuthenticationResult} produced.
 * 
 * <p>The {@link Subject} is extended with a copy of any {@CloneablePrincipal} objects returned
 * by {@link AuthenticationFlowDescriptor#getSupportedPrincipals()}, and fed into the
 * {@link SubjectCanonicalizer} returned by {@link AuthenticationFlowDescriptor#getSubjectCanonicalizer()}.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#IDENTITY_SWITCH} if the c14n result does not match
 * {@link AuthenticationContext#getCanonicalPrincipalName()}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS} - if the c14n step fails
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If AuthenticationContext.getAuthenticationResult() != null,
 * then the steps above are performed.
 * @post AuthenticationContext.getAuthenticationResult() is updated.
 */
public class FinalizeAuthenticationFlow extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(FinalizeAuthenticationFlow.class);
    
    /** Authentication flow to finalize. */
    private AuthenticationFlowDescriptor attemptedFlow;
    
    /** Result of flow. */
    private AuthenticationResult authenticationResult;
    
    /** Principal currently active from session. */
    private String principalFromSession;
    
    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        attemptedFlow = authenticationContext.getAttemptedFlow();
        authenticationResult = authenticationContext.getAuthenticationResult();
        
        if (attemptedFlow != null && authenticationResult != null) {
            principalFromSession = authenticationContext.getCanonicalPrincipalName();
            return true;
        }
        
        return false;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        
        log.debug("{} adding additional Principal objects from flow descriptor '{}' to authenticated subject",
                getLogPrefix(), attemptedFlow.getId());
        
        for (Principal p : attemptedFlow.getSupportedPrincipals()) {
            if (p instanceof CloneablePrincipal) {
                try {
                    authenticationResult.getSubject().getPrincipals().add(((CloneablePrincipal) p).clone());
                } catch (CloneNotSupportedException e) {
                    log.error(getLogPrefix() + " error cloning principal for addition to authenticated subject", e);
                }
            }
        }
        
        log.debug("{} performing subject canonicalization using {}", getLogPrefix(),
                attemptedFlow.getSubjectCanonicalizer().getId());
        try {
            authenticationResult.setCanonicalPrincipalName(
                    attemptedFlow.getSubjectCanonicalizer().canonicalize(authenticationResult.getSubject()));
        } catch (SubjectCanonicalizationException e) {
            log.error(getLogPrefix() + " error canonicalizing subject", e);
            authenticationContext.setLoginException(e);
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return;
        }
        
        log.debug("{} result of subject canonicalization: '{}'", getLogPrefix(),
                authenticationResult.getCanonicalPrincipalName());
        
        if (principalFromSession != null) {
            if (!principalFromSession.equals(authenticationResult.getCanonicalPrincipalName())) {
                log.warn("{} detected identity switch from '{}' to '{}'", getLogPrefix(), principalFromSession,
                        authenticationResult.getCanonicalPrincipalName());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.IDENTITY_SWITCH);
                return;
            }
        } else {
            authenticationContext.setCanonicalPrincipalName(authenticationResult.getCanonicalPrincipalName());
        }
    }

}
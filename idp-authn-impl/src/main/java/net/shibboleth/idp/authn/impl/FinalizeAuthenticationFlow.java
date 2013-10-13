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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;


/**
 * An authentication action that runs at the end of a completed authentication flow and
 * finalizes the content of the {@link AuthenticationResult} it produced.
 * 
 * <p>The principal name obtained from a {@link SubjectCanonicalizationContext} child of the
 * {@link ProfileRequestContext} is transferred into the {@link AuthenticationResult}.</p>
 * 
 * <p>If the {@link AuthenticationContext} contains a pre-existing principal name, it is compared
 * to the value placed in the result, possibly resulting in an {@link AuthnEventIds#IDENTITY_SWITCH}
 * event. Otherwise, the principal name is copied into the {@link AuthenticationContext}.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#IDENTITY_SWITCH}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If AuthenticationContext.getAuthenticationResult() != null,
 * then the steps above are performed.
 */
public class FinalizeAuthenticationFlow extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(FinalizeAuthenticationFlow.class);
    
    /** SubjectCanonicalizationContext to operate on. */
    @Nullable private SubjectCanonicalizationContext scContext;
    
    /** Result of flow. */
    @Nullable private AuthenticationResult authenticationResult;
    
    /** Principal currently active from session. */
    @Nullable private String principalFromSession;
    
    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        authenticationResult = authenticationContext.getAuthenticationResult();
        if (authenticationResult == null) {
            return false;
        }
                
        scContext = profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false);
        if (scContext == null || Strings.isNullOrEmpty(scContext.getPrincipalName())) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        principalFromSession = authenticationContext.getCanonicalPrincipalName();
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        
        log.debug("{} result of subject canonicalization: '{}'", getLogPrefix(), scContext.getPrincipalName());
        
        if (principalFromSession != null) {
            if (!principalFromSession.equals(scContext.getPrincipalName())) {
                log.warn("{} detected identity switch from '{}' to '{}'", getLogPrefix(), principalFromSession,
                        scContext.getPrincipalName());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.IDENTITY_SWITCH);
            }
        } else {
            authenticationContext.setCanonicalPrincipalName(scContext.getPrincipalName());
        }
    }

}
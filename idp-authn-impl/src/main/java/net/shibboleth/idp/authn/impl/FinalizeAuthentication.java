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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.session.context.SessionContext;

import org.opensaml.profile.context.ProfileRequestContext;


/**
 * An authentication action that runs after a completed authentication flow (or the reuse
 * of an active result) and transfers information from other contexts into a {@link SubjectContext}
 * child of the {@link ProfileRequestContext}.
 * 
 * <p>The context is populated based on the presence of a canonical principal name in either
 * a {@link SubjectCanonicalizationContext} or {@link SessionContext}, and also includes
 * the completed {@link AuthenticationResult} and any other active results found in the
 * {@link AuthenticationContext}.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If SubjectCanonicalizationContext.getCanonicalPrincipalName() != null
 * || SessionContext.getIdPSession() != null
 * then ProfileRequestContext.getSubcontext(SubjectContext.class, false) != null 
 * @post AuthenticationContext.setCompletionInstant() was called
 */
public class FinalizeAuthentication extends AbstractAuthenticationAction {

    /** The principal name extracted from the context tree. */
    @Nullable private String canonicalPrincipalName;
    
    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        SubjectCanonicalizationContext c14nCtx =
                profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, false);
        if (c14nCtx != null) {
            canonicalPrincipalName = c14nCtx.getPrincipalName();
        }
        
        if (canonicalPrincipalName == null) {
            SessionContext sessionCtx = profileRequestContext.getSubcontext(SessionContext.class, false);
            if (sessionCtx != null && sessionCtx.getIdPSession() != null) {
                canonicalPrincipalName = sessionCtx.getIdPSession().getPrincipalName();
            }
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (canonicalPrincipalName != null) {
            SubjectContext sc = profileRequestContext.getSubcontext(SubjectContext.class, true);
            sc.setPrincipalName(canonicalPrincipalName);

            Map scResults = sc.getAuthenticationResults();
            scResults.putAll(authenticationContext.getActiveResults());
            
            AuthenticationResult latest = authenticationContext.getAuthenticationResult();
            if (latest != null && !scResults.containsKey(latest.getAuthenticationFlowId())) {
                    scResults.put(latest.getAuthenticationFlowId(), latest);
            }
        }
        
        authenticationContext.setCompletionInstant();
    }

}
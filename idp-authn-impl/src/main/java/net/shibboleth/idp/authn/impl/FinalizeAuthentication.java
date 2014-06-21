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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.session.context.SessionContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An authentication action that runs after a completed authentication flow (or the reuse
 * of an active result) and transfers information from other contexts into a {@link SubjectContext}
 * child of the {@link ProfileRequestContext}.
 * 
 * <p>The action also cross-checks {@link RequestedPrincipalContext#getMatchingPrincipal()}, if set,
 * against the {@link AuthenticationResult} to ensure that the result produced actually satisfies the
 * request. This is redundant when reusing active results, but is necessary to prevent a flow from running
 * that can return different results and having it produce a result that doesn't actually satisfy the
 * request. Such a flow would be buggy, but this guards against a mistake from leaving the subsystem.</p>
 * 
 * <p>The context is populated based on the presence of a canonical principal name in either
 * a {@link SubjectCanonicalizationContext} or {@link SessionContext}, and also includes
 * the completed {@link AuthenticationResult} and any other active results found in the
 * {@link AuthenticationContext}.</p>
 * 
 * <p>Any {@link SubjectCanonicalizationContext} found will be removed.</p>
 * 
 * <p>If a {@link SubjectContext} already exists, then this action will validate that
 * the same principal name is represented by it, and signal a mismatch otherwise. This
 * is used in protocols that indicate normatively what the authenticated identity is
 * required to be.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_SUBJECT_CTX}
 * @event {@link AuthnEventIds#REQUEST_UNSUPPORTED}
 * 
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class) != null</pre>
 * 
 * @post If SubjectCanonicalizationContext.getCanonicalPrincipalName() != null
 * || SessionContext.getIdPSession() != null
 * then ProfileRequestContext.getSubcontext(SubjectContext.class) != null 
 * @post AuthenticationContext.setCompletionInstant() was called
 * @post <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class) == null</pre>
 */
public class FinalizeAuthentication extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FinalizeAuthentication.class);
    
    /** The principal name extracted from the context tree. */
    @Nullable private String canonicalPrincipalName;
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final SubjectCanonicalizationContext c14nCtx =
                profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class);
        if (c14nCtx != null) {
            canonicalPrincipalName = c14nCtx.getPrincipalName();
            profileRequestContext.removeSubcontext(c14nCtx);
        }
        
        if (canonicalPrincipalName == null) {
            final SessionContext sessionCtx = profileRequestContext.getSubcontext(SessionContext.class);
            if (sessionCtx != null && sessionCtx.getIdPSession() != null) {
                canonicalPrincipalName = sessionCtx.getIdPSession().getPrincipalName();
            }
        }
        
        // Check for a requested Principal and make sure it's in the result.
        final RequestedPrincipalContext requestedPrincipalCtx =
                authenticationContext.getSubcontext(RequestedPrincipalContext.class);
        if (requestedPrincipalCtx != null) {
            final Principal match = requestedPrincipalCtx.getMatchingPrincipal();
            if (match != null) {
                final AuthenticationResult latest = authenticationContext.getAuthenticationResult();
                if (latest == null || !latest.getSupportedPrincipals(match.getClass()).contains(match)) {
                    log.warn("{} Authentication result for flow {} did not satisfy the requested Principal {}",
                            getLogPrefix(), latest != null ? latest.getAuthenticationFlowId() : "(none)", match);
                    ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
                    return false;
                }
            }
        }
        
        return super.doPreExecute(profileRequestContext, authenticationContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (canonicalPrincipalName != null) {
            final SubjectContext sc = profileRequestContext.getSubcontext(SubjectContext.class, true);
            
            // Check for an existing value.
            if (sc.getPrincipalName() != null && !canonicalPrincipalName.equals(sc.getPrincipalName())) {
                log.warn("{} Result of authentication ({}) does not match existing subject in context ({})",
                        getLogPrefix(), canonicalPrincipalName, sc.getPrincipalName());
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_SUBJECT_CTX);
                return;
            }
            
            sc.setPrincipalName(canonicalPrincipalName);

            final Map scResults = sc.getAuthenticationResults();
            scResults.putAll(authenticationContext.getActiveResults());
            
            final AuthenticationResult latest = authenticationContext.getAuthenticationResult();
            if (latest != null && !scResults.containsKey(latest.getAuthenticationFlowId())) {
                scResults.put(latest.getAuthenticationFlowId(), latest);
            }
        }
        
        authenticationContext.setCompletionInstant();
    }

}
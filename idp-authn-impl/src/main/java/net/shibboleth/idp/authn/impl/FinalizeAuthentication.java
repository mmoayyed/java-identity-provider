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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
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
 * <p>The action enforces any constraints on proxying that may be present in the result against
 * the intended use based on the {@link RelyingPartyContext}.</p>
 * 
 * <p>The action also cross-checks {@link RequestedPrincipalContext#getMatchingPrincipal()}, if set,
 * against the {@link AuthenticationResult} to ensure that the result produced actually satisfies the
 * request. This is redundant when reusing active results, but is necessary to prevent a flow from running
 * that can return different results and having it produce a result that doesn't actually satisfy the
 * request. Such a flow would be buggy, but this guards against a mistake from leaving the subsystem.</p>
 * 
 * <p>If no matching Principal is established, or if the match is no longer valid, the request is
 * evaluated in conjunction with the {@link AuthenticationResult} to establish a Principal that
 * does satisfy the request and it is recorded via
 * {@link RequestedPrincipalContext#setMatchingPrincipal(Principal)}.</p>
 * 
 * <p>The context is populated based on the presence of a canonical principal name in either
 * a {@link SubjectCanonicalizationContext} or {@link SessionContext}, and also includes
 * the completed {@link AuthenticationResult} and any other active results found in the
 * {@link AuthenticationContext}.</p>
 * 
 * <p>Any {@link SubjectCanonicalizationContext} found will be removed.</p>
 * 
 * <p>If {@link AuthenticationContext#getRequiredName()} is set, then this action will validate that
 * the same principal name is represented by it, and signal a mismatch otherwise. This is used in
 * protocols that indicate normatively what the authenticated identity is required to be.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_SUBJECT}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
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
    
    // Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }

        final SubjectCanonicalizationContext c14nCtx =
                profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class);
        if (c14nCtx != null && c14nCtx.getPrincipalName() != null) {
            canonicalPrincipalName = c14nCtx.getPrincipalName();
            profileRequestContext.removeSubcontext(c14nCtx);
            log.debug("{} Canonical principal name was established as '{}'", getLogPrefix(), canonicalPrincipalName);
        } else if (canonicalPrincipalName == null) {
            final SessionContext sessionCtx = profileRequestContext.getSubcontext(SessionContext.class);
            if (sessionCtx != null && sessionCtx.getIdPSession() != null) {
                canonicalPrincipalName = sessionCtx.getIdPSession().getPrincipalName();
                log.debug("{} Canonical principal name established from session as '{}'", getLogPrefix(),
                        canonicalPrincipalName);
            }
        }

        final AuthenticationResult latest = authenticationContext.getAuthenticationResult();
        if (latest == null) {
            log.warn("{} Authentication result missing from context?", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return false;
        }
        
        final AuthenticationFlowDescriptor flowDescriptor =
                authenticationContext.getAvailableFlows().get(latest.getAuthenticationFlowId());
        if (flowDescriptor.isProxyRestrictionsEnforced() &&
                !checkProxyRestrictions(profileRequestContext, latest.getSubject())) {
            return false;
        }
        
        // Check for requested Principal criteria and make sure the result accomodates the criteria.
        // This is required because flow selection is based (generally) on statically-defined information
        // and the actual result produced may be a subset (and therefore could be an inadequate subset).
        final RequestedPrincipalContext requestedPrincipalCtx =
                authenticationContext.getSubcontext(RequestedPrincipalContext.class);
        if (requestedPrincipalCtx != null && requestedPrincipalCtx.getOperator() != null) {
            
            // If a matching principal is set, re-verify it. Normally this will work.
            final Principal match = requestedPrincipalCtx.getMatchingPrincipal();
            if (match != null) {
                if (!latest.getSupportedPrincipals(match.getClass()).contains(match)) {
                    log.debug("{} Authentication result lacks originally projected matching principal '{}',"
                            + " reevaluating", getLogPrefix(), match.getName());
                    requestedPrincipalCtx.setMatchingPrincipal(null);
                }
            }
            
            // It didn't work, so we have to run the machinery over the request principals and
            // evaluate the result more fully.
            requestedPrincipalCtx.setMatchingPrincipal(
                    findMatchingPrincipal(authenticationContext, requestedPrincipalCtx));

            // If it's still null, then the result didn't meet our needs.
            if (requestedPrincipalCtx.getMatchingPrincipal() == null) {
                log.warn("{} Authentication result for flow {} did not satisfy the request", getLogPrefix(),
                        latest.getAuthenticationFlowId());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
                return false;
            }
            
        } else {
            log.debug("{} Request did not have explicit authentication requirements, result is accepted",
                    getLogPrefix());
        }
        
        return true;
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
    
        if (canonicalPrincipalName != null) {
            if (authenticationContext.getRequiredName() != null &&
                    !canonicalPrincipalName.equals(authenticationContext.getRequiredName())) {
                log.warn("{} Result of authentication ({}) does not match existing subject in context ({})",
                        getLogPrefix(), canonicalPrincipalName, authenticationContext.getRequiredName());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT);
                return;
            }
            
            final SubjectContext sc = profileRequestContext.getSubcontext(SubjectContext.class, true);
            sc.setPrincipalName(canonicalPrincipalName);
            
            log.info("{} Principal {} authenticated", getLogPrefix(), canonicalPrincipalName);
    
            final Map<String,AuthenticationResult> scResults = sc.getAuthenticationResults();
            scResults.putAll(authenticationContext.getActiveResults());
            
            final AuthenticationResult latest = authenticationContext.getAuthenticationResult();
            if (latest != null) {
                scResults.put(latest.getAuthenticationFlowId(), latest);
            }
        }
        
        authenticationContext.setCompletionInstant();
    }

    /**
     * Evaluate request criteria and the {@link AuthenticationResult} to locate a {@link Principal} in the
     * result that satisfies the request criteria.
     * 
     * <p>If a weighting map is supplied, the {@link Principal} returned is the one that both satisfies
     * the request and is highest weighted according to the underlying flow descriptor.</p>
     * 
     * @param authenticationContext authentication context
     * @param requestedPrincipalCtx request criteria
     * 
     * @return matching Principal, or null
     */
    @Nullable protected Principal findMatchingPrincipal(@Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final RequestedPrincipalContext requestedPrincipalCtx) {
                
        // Maintain a list of each Principal that matches the request.
        final ArrayList<Principal> matches = new ArrayList<>();
        
        for (final Principal p : requestedPrincipalCtx.getRequestedPrincipals()) {
            log.debug("{} Checking result for compatibility with operator '{}' and principal '{}'",
                    getLogPrefix(), requestedPrincipalCtx.getOperator(), p.getName());
            final PrincipalEvalPredicateFactory factory =
                    requestedPrincipalCtx.getPrincipalEvalPredicateFactoryRegistry().lookup(
                            p.getClass(), requestedPrincipalCtx.getOperator());
            if (factory != null) {
                final PrincipalEvalPredicate predicate = factory.getPredicate(p);
    
                // For unweighted results, we'd just apply the predicate to the AuthenticationResult, but
                // we won't be able to honor weighting, so we have to walk the supported principals one
                // at a time, wrap it to apply the predicate, and then record it if it succeeds.
                
                matches.clear();
                for (final Principal candidate
                        : authenticationContext.getAuthenticationResult().getSupportedPrincipals(p.getClass())) {
                    if (predicate.test(new PrincipalSupportingComponent() {
                        @SuppressWarnings("unchecked")
                        public <T extends Principal> Set<T> getSupportedPrincipals(final Class<T> c) {
                            return Collections.<T>singleton((T) candidate);
                        }
                    })) {
                        log.debug("{} Principal '{}' in authentication result satisfies request for principal '{}'",
                                getLogPrefix(), candidate.getName(), p.getName());
                        matches.add(candidate);
                    }
                }
                
                // The first non-empty match set satisfies the request, so that's what we use.
                // That honors the precedence order of the input criteria.
                if (!matches.isEmpty()) {
                    break;
                }
            } else {
                log.warn("{} Configuration does not support requested principal evaluation with "
                        + "operator '{}' and type '{}'", getLogPrefix(), requestedPrincipalCtx.getOperator(),
                        p.getClass());
            }
        }
        
        if (matches.isEmpty()) {
            return null;
        }
        
        final AuthenticationFlowDescriptor flowDescriptor = authenticationContext.getAvailableFlows().get(
                authenticationContext.getAuthenticationResult().getAuthenticationFlowId());
        return flowDescriptor.getHighestWeighted(matches);
    }

    /**
     * Check for proxy restrictions and evaluate them against the request.
     * 
     * @param profileRequestContext current profile request context
     * @param subject the authentication result's subject
     * 
     * @return true iff processing should continue
     */
    private boolean checkProxyRestrictions(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final Subject subject) {
        
        final Set<ProxyAuthenticationPrincipal> proxieds = subject.getPrincipals(ProxyAuthenticationPrincipal.class);
        if (proxieds == null || proxieds.isEmpty()) {
            return true;
        }
        
        for (final ProxyAuthenticationPrincipal proxied : proxieds) {
            if (!proxied.test(profileRequestContext)) {
                log.warn("{} Result contained a proxy restriction disallowing use", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
                return false;
            }
        }
        
        return true;
    }

}
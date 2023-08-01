/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.ExternalAuthenticationContext;
import net.shibboleth.idp.consent.context.ConsentManagementContext;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.logic.Constraint;

/**
 * Implementation of the {@link ExternalAuthentication} API that handles moving information in and out
 * of request attributes.
 */
public class ExternalAuthenticationImpl extends ExternalAuthentication {
    
    /** Lookup function for relying party context. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Track whether we were invoked from within another login flow. */
    private final boolean extendedFlow;

    /** Constructor. */
    public ExternalAuthenticationImpl() {
        this(false);
    }

    /**
     * Constructor.
     * 
     * @param extended called as extended flow from another login flow
     */
    public ExternalAuthenticationImpl(final boolean extended) {
        extendedFlow = extended;
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }

    /**
     * Set lookup strategy for relying party context.
     * 
     * @param strategy  lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doStart(@Nonnull final HttpServletRequest request,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ExternalAuthenticationContext externalAuthenticationContext)
                    throws ExternalAuthenticationException {
        super.doStart(request, profileRequestContext, externalAuthenticationContext);
        
        final AuthenticationContext authnContext = profileRequestContext.getSubcontext(AuthenticationContext.class);
        if (authnContext == null) {
            throw new ExternalAuthenticationException("No AuthenticationContext found");
        } else if (authnContext.getAttemptedFlow() == null) {
            throw new ExternalAuthenticationException("No attempted authentication flow set");
        }
        
        request.setAttribute(EXTENDED_FLOW_PARAM, extendedFlow);
        
        request.setAttribute(PASSIVE_AUTHN_PARAM, authnContext.isPassive());
        request.setAttribute(FORCE_AUTHN_PARAM, authnContext.isForceAuthn());
                
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx != null) {
            request.setAttribute(RELYING_PARTY_PARAM, rpCtx.getRelyingPartyId());
        }
    }

 // Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    protected void doFinish(@Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ExternalAuthenticationContext extContext)
                    throws ExternalAuthenticationException, IOException {
        
        if (extContext.getFlowExecutionUrl() == null) {
            throw new ExternalAuthenticationException("No flow execution URL found to return control");
        }
        
        Object attr = request.getAttribute(SUBJECT_KEY);
        if (attr != null && attr instanceof Subject) {
            extContext.setSubject((Subject) attr);
        } else {
            attr = request.getAttribute(PRINCIPAL_KEY);
            if (attr != null && attr instanceof Principal) {
                extContext.setPrincipal((Principal) attr);
            } else {
                attr = request.getAttribute(PRINCIPAL_NAME_KEY);
                if (attr != null && attr instanceof String) {
                    extContext.setPrincipalName((String) attr);
                }
            }
        }
        
        attr = request.getAttribute(AUTHENTICATION_INSTANT_KEY);
        if (attr != null && attr instanceof Instant) {
            extContext.setAuthnInstant((Instant) attr);
        }
        
        attr = request.getAttribute(AUTHENTICATING_AUTHORITIES_KEY);
        if (attr != null && attr instanceof Collection<?>) {
            extContext.getAuthenticatingAuthorities().addAll((Collection<String>) attr);
        }
        
        attr = request.getAttribute(ATTRIBUTES_KEY);
        if (attr != null && attr instanceof Collection<?>) {
            final AttributeContext ac = extContext.ensureSubcontext(AttributeContext.class);
            ac.setUnfilteredIdPAttributes(
                    (Collection<IdPAttribute>) attr);
            ac.setIdPAttributes(
                    (Collection<IdPAttribute>) attr);
        }
        
        attr = request.getAttribute(AUTHENTICATION_ERROR_KEY);
        if (attr != null && attr instanceof String) {
            extContext.setAuthnError((String) attr);
        }
        
        attr = request.getAttribute(AUTHENTICATION_EXCEPTION_KEY);
        if (attr != null && attr instanceof Exception) {
            extContext.setAuthnException((Exception) attr);
        }
        
        attr = request.getAttribute(DONOTCACHE_KEY);
        if (attr != null && attr instanceof Boolean) {
            extContext.setDoNotCache((Boolean) attr);
        }

        attr = request.getAttribute(PREVIOUSRESULT_KEY);
        if (attr != null && attr instanceof Boolean) {
            extContext.setPreviousResult((Boolean) attr);
        }
        
        attr = request.getAttribute(REVOKECONSENT_KEY);
        if (attr != null && attr instanceof Boolean && ((Boolean) attr).booleanValue()) {
            final ConsentManagementContext consentCtx =
                    profileRequestContext.ensureSubcontext(ConsentManagementContext.class);
            consentCtx.setRevokeConsent(true);
        }
        
        response.sendRedirect(extContext.getFlowExecutionUrl());
    }
// Checkstyle: CyclomaticComplexity|MethodLength OFF
        
}

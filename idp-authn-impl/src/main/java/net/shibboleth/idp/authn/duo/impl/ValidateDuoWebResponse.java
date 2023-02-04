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

package net.shibboleth.idp.authn.duo.impl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import com.duosecurity.duoweb.DuoWebException;

import net.shibboleth.idp.authn.AuthnAuditFields;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.duo.DuoIntegration;
import net.shibboleth.idp.authn.duo.DuoPrincipal;
import net.shibboleth.idp.authn.impl.AbstractAuditingValidationAction;
import net.shibboleth.idp.profile.IdPAuditFields;
import net.shibboleth.idp.session.context.navigate.CanonicalUsernameLookupStrategy;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.ServletRequest;

/**
 * An action that validates a DuoWeb response message and produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} or records error state.
 * 
 * <p>The username to cross-check comes from a lookup strategy, by default a {@link CanonicalUsernameLookupStrategy}
 * that returns a username produced by an earlier authentication flow, and on success the same name is populated into
 * a {@link SubjectCanonicalizationContext} as a pre-established result for the login flow.</p>
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @post ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class).getPrincipalName() != null
 * 
 * @since 3.3.0
 */
public class ValidateDuoWebResponse extends AbstractAuditingValidationAction {

    /** Signed response parameter name. */
    @Nonnull @NotEmpty public static final String RESPONSE_PARAM = "sig_response";

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.duo";
        
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateDuoWebResponse.class);
    
    /** Lookp strategy for Duo integration. */
    @Nonnull private Function<ProfileRequestContext,DuoIntegration> duoIntegrationLookupStrategy;
    
    /** Lookup strategy for username to match against Duo identity. */
    @Nonnull private Function<ProfileRequestContext,String> usernameLookupStrategy;
    
    /** Duo integration to use. */
    @Nullable private DuoIntegration duoIntegration;
    
    /** Attempted username. */
    @Nullable @NotEmpty private String username;
    
    /** Signed response string. */
    @Nullable @NotEmpty private String signedResponse;
    
    /** Constructor. */
    public ValidateDuoWebResponse() {
        duoIntegrationLookupStrategy = FunctionSupport.constant(null);
        usernameLookupStrategy = new CanonicalUsernameLookupStrategy();
        setMetricName(DEFAULT_METRIC_NAME);
    }

    /**
     * Set DuoIntegration lookup strategy to use.
     * 
     * @param strategy lookup strategy
     */
    public void setDuoIntegrationLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,DuoIntegration> strategy) {
        checkSetterPreconditions();
        duoIntegrationLookupStrategy = Constraint.isNotNull(strategy, "DuoIntegration lookup strategy cannot be null");
    }

    /**
     * Set DuoIntegration details to use directly.
     * 
     * @param duo Duo integration details
     */
    public void setDuoIntegration(@Nonnull final DuoIntegration duo) {
        checkSetterPreconditions();
        Constraint.isNotNull(duo, "DuoIntegration cannot be null");
        duoIntegrationLookupStrategy = FunctionSupport.constant(duo);
    }
    
    /**
     * Set the lookup strategy to use for the username to match against Duo identity.
     * 
     * @param strategy lookup strategy
     */
    public void setUsernameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        usernameLookupStrategy = Constraint.isNotNull(strategy, "Username lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        duoIntegration = duoIntegrationLookupStrategy.apply(profileRequestContext);
        if (duoIntegration == null) {
            log.warn("{} No DuoIntegration returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        username = usernameLookupStrategy.apply(profileRequestContext);
        if (username == null) {
            log.warn("{} No principal name available to cross-check Duo result", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        final ServletRequest servletRequest = getHttpServletRequest();
        if (servletRequest == null) {
            log.error("{} No ServletRequest available", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            return false;
        }
        
        signedResponse = servletRequest.getParameter(RESPONSE_PARAM);
        if (signedResponse == null || signedResponse.isEmpty()) {
            log.warn("{} No signed Duo response in the request", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            recordFailure(profileRequestContext);
            return false;
        }
                        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        log.trace("{} Signed Duo response is: {}", getLogPrefix(), signedResponse);
                
        final String usernameFromDuo;
        try {
            usernameFromDuo = DuoSupport.validateSignedResponseToken(duoIntegration, signedResponse);
        } catch (final InvalidKeyException | NoSuchAlgorithmException | DuoWebException | IOException e) {
            log.warn("{} Error validating signed Duo response for username '{}'", getLogPrefix(), username, e);
            handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure(profileRequestContext);
            return;
        }
        
        if (!username.equals(usernameFromDuo)) {
            log.warn("{} Username '{}' from Duo response does not match previously established username '{}'",
                    getLogPrefix(), usernameFromDuo, username);
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.INVALID_CREDENTIALS,
                    AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure(profileRequestContext);
        } else {
            log.info("{} Duo authentication succeeded for '{}'", getLogPrefix(), usernameFromDuo);
            recordSuccess(profileRequestContext);
            buildAuthenticationResult(profileRequestContext, authenticationContext);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected Subject populateSubject(@Nonnull final Subject subject) {
        subject.getPrincipals().add(new DuoPrincipal(username));
        subject.getPrincipals().addAll(duoIntegration.getSupportedPrincipals(Principal.class));
        return subject;
    }

    /** {@inheritDoc} */
    @Override
    protected void buildAuthenticationResult(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        super.buildAuthenticationResult(profileRequestContext, authenticationContext);
        
        // Bypass c14n. We already operate on a canonical name, so just re-confirm it.
        profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName(username);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Map<String,String> getAuditFields(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (username != null) {
            if (duoIntegration != null) {
                return Map.of(AuthnAuditFields.DUO_CLIENT_ID, duoIntegration.getIntegrationKey(),
                        IdPAuditFields.USERNAME, username);
            } else {
                return Collections.singletonMap(IdPAuditFields.USERNAME, username);
            }
        } else if (duoIntegration != null) {
            return Collections.singletonMap(AuthnAuditFields.DUO_CLIENT_ID, duoIntegration.getIntegrationKey());
        }
        
        return super.getAuditFields(profileRequestContext);
    }
    
}

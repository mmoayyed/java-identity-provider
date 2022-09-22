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

import java.security.Principal;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.duosecurity.duoweb.DuoWebException;

import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.duo.DuoAuthAPI;
import net.shibboleth.idp.authn.duo.DuoIntegration;
import net.shibboleth.idp.authn.duo.DuoPrincipal;
import net.shibboleth.idp.authn.duo.context.DuoAuthenticationContext;
import net.shibboleth.idp.session.context.navigate.CanonicalUsernameLookupStrategy;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

/**
 * An action that checks for a {@link DuoAuthenticationContext} and directly produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} based on that identity by authenticating against the Duo
 * AuthAPI.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#AUTHN_EXCEPTION}
 * @event {@link AuthnEventIds#ACCOUNT_LOCKED}
 * @event {@link AuthnEventIds#ACCOUNT_WARNING}
 * @event {@link AuthnEventIds#ACCOUNT_ERROR}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @pre
 * 
 *      <pre>
 *      ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null
 *      </pre>
 * 
 * @post If AuthenticationContext.getSubcontext(DuoAuthenticationContext.class) != null, then an
 *       {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext} on a
 *       successful login. On a failed login, the
 *       {@link AbstractValidationAction#handleError(ProfileRequestContext, AuthenticationContext, String, String)}
 *       method is called.
 */
public class ValidateDuoAuthAPI extends AbstractValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.duo";

    /** Class logger. */
    @Nonnull @NotEmpty private final Logger log = LoggerFactory.getLogger(ValidateDuoAuthAPI.class);

    /** Lookp strategy for Duo integration. */
    @Nonnull private Function<ProfileRequestContext,DuoIntegration> duoIntegrationLookupStrategy;

    /** Lookup strategy for username to match against Duo identity. */
    @Nonnull private Function<ProfileRequestContext,String> usernameLookupStrategy;

    /** Implementation of Duo AuthApi /auth endpoint. */
    @Nonnull private DuoAuthAuthenticator authAuthenticator;

    /** Implementation of Duo AuthApi /preauth enpoint. */
    @Nonnull private DuoPreauthAuthenticator preauthAuthenticator;

    /** DuoApi context for tokens. **/
    @Nonnull @NotEmpty private DuoAuthenticationContext duoContext;

    /** Duo integration to use. */
    @Nullable private DuoIntegration duoIntegration;

    /** Attempted username. */
    @Nullable @NotEmpty private String username;

    /** Constructor. */
    public ValidateDuoAuthAPI() {
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
    public void setUsernameLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        usernameLookupStrategy = Constraint.isNotNull(strategy, "Username lookup strategy cannot be null");
    }

    /**
     * Set the {@link DuoAuthAuthenticator}.
     * 
     * @param authenticator a Duo AuthAPI /auth endpoint implementation
     */
    public void setAuthAuthenticator(@Nonnull final DuoAuthAuthenticator authenticator) {
        checkSetterPreconditions();
        authAuthenticator = Constraint.isNotNull(authenticator, "DuoAuthAuthenticator cannot be null");
    }

    /**
     * Set the {@link DuoPreauthAuthenticator}.
     * 
     * @param authenticator a Duo AuthAPI /preauth endpoint implementation
     */
    public void setPreauthAuthenticator(@Nonnull final DuoPreauthAuthenticator authenticator) {
        checkSetterPreconditions();
        preauthAuthenticator = Constraint.isNotNull(authenticator, "DuoPreauthAuthenticator cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (authAuthenticator == null) {
            throw new ComponentInitializationException("DuoAuthAuthenticator cannot be null");
        }

        if (preauthAuthenticator == null) {
            throw new ComponentInitializationException("DuoPreauthAuthenticator cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
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
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        duoContext = authenticationContext.getSubcontext(DuoAuthenticationContext.class);
        if (duoContext == null) {
            log.info("{} No DuoAuthenticationContext available", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "No DuoAuthenticationContext context available",
                    AuthnEventIds.INVALID_AUTHN_CTX);
            recordFailure(profileRequestContext);
            return false;
        } else if (duoContext.getFactor() == null) {
            log.info("{} No factor set in DuoAuthenticationContext", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "No Duo factor set in DuoAuthenticationContext",
                    AuthnEventIds.REQUEST_UNSUPPORTED);
            recordFailure(profileRequestContext);
            return false;
        }

        duoContext.setUsername(username);

        return true;
    }

    /** {@inheritDoc} */
    // CheckStyle: CyclomaticComplexity|MethodLength|ReturnCount OFF
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        log.trace("{} Attempting Duo AuthAPI authentication", getLogPrefix());

        try {
            // Duo AuthAPI pre-authentication
            final DuoPreauthResponse preAuthResponse = preauthAuthenticator.authenticate(duoContext, duoIntegration);
            if (preAuthResponse == null) {
                log.info("{} No Duo AuthAPI preauthentication response", getLogPrefix());
                throw new DuoWebException("No preauthentication response");
            }

            final String preAuthResult = preAuthResponse.getResult();

            if (DuoAuthAPI.DUO_PREAUTH_RESULT_ALLOW.equals(preAuthResult)) {
                // User in bypass mode; treat as authenticated.
                log.info("{} Duo pre-authentication (bypass) succeeded for '{}'", getLogPrefix(), username);
                recordSuccess(profileRequestContext);
                buildAuthenticationResult(profileRequestContext, authenticationContext);
                return;
            }

            if (!DuoAuthAPI.DUO_PREAUTH_RESULT_AUTH.equals(preAuthResult)) {
                // Either deny or enroll.
                log.info("{} Duo pre-authentication failed for '{}': {}", getLogPrefix(), username,
                        preAuthResponse.getStatusMessage());
                handleError(profileRequestContext, authenticationContext,
                        String.format("%s:%s:%s", preAuthResult, username, preAuthResponse.getStatusMessage()),
                        AuthnEventIds.ACCOUNT_ERROR);
                recordFailure(profileRequestContext);
                return;
            }
            
            // Validate device ID specified against the enrolled set.
            if (duoContext.getDeviceID() != null && !DuoAuthAPI.DUO_DEVICE_AUTO.equals(duoContext.getDeviceID())) {
                boolean found = false;
                for (final DuoDevice device : preAuthResponse.getDevices()) {
                    if (duoContext.getDeviceID().equals(device.getDevice())) {
                        found = true;
                        break;
                    } else if (duoContext.getDeviceID().equals(device.getName())) {
                        log.debug("{} Remapped device ID based on device name ({}) for '{}'", getLogPrefix(),
                                device.getName(), username);
                        duoContext.setDeviceID(device.getDevice());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    log.info("{} Duo authentication failed for '{}': non-existent device ID ({})", getLogPrefix(),
                            username, duoContext.getDeviceID());
                    handleError(profileRequestContext, authenticationContext, AuthnEventIds.INVALID_CREDENTIALS,
                            AuthnEventIds.INVALID_CREDENTIALS);
                    recordFailure(profileRequestContext);
                    return;
                }
            }

            // Duo AuthAPI authentication
            final DuoAuthResponse authenticationResponse = authAuthenticator.authenticate(duoContext, duoIntegration);
            if (authenticationResponse == null) {
                log.info("{} No Duo AuthAPI authentication response", getLogPrefix());
                throw new DuoWebException("No authentication response");
            }

            final String authResult = authenticationResponse.getResult();
            if (DuoAuthAPI.DUO_AUTH_RESULT_ALLOW.equals(authResult)) {
                log.info("{} Duo authentication succeeded for '{}' (Factor: {}, Device: {})", getLogPrefix(), username,
                        duoContext.getFactor(), duoContext.getDeviceID());
                recordSuccess(profileRequestContext);
                buildAuthenticationResult(profileRequestContext, authenticationContext);
            } else if (DuoAuthAPI.DUO_AUTH_RESULT_DENY.equals(authResult)) {
                log.info("{} Duo authentication failed for '{}'", getLogPrefix(), username);
                handleError(profileRequestContext, authenticationContext, authenticationResponse.getStatus(),
                        AuthnEventIds.INVALID_CREDENTIALS);
                recordFailure(profileRequestContext);
            } else {
                throw new DuoWebException("Unexpected authentication response");
            }
        } catch (final DuoWebException e) {
            log.error("{} Duo AuthAPI access failed for '{}'", getLogPrefix(), username, e);
            handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
            recordFailure(profileRequestContext);
        }
    }
    // CheckStyle: CyclomaticComplexity|MethodLength|ReturnCount OFF

    /** {@inheritDoc} */
    @Override protected Subject populateSubject(@Nonnull final Subject subject) {
        subject.getPrincipals().add(new DuoPrincipal(username));
        subject.getPrincipals().addAll(duoIntegration.getSupportedPrincipals(Principal.class));
        return subject;
    }

    /** {@inheritDoc} */
    @Override protected void buildAuthenticationResult(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        super.buildAuthenticationResult(profileRequestContext, authenticationContext);

        // Bypass c14n. We already operate on a canonical name, so just re-confirm it.
        profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName(username);
    }

}
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
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.LDAPResponseContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.ldaptive.Credential;
import org.ldaptive.LdapException;
import org.ldaptive.auth.AccountState;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResultCode;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.jaas.LdapPrincipal;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that checks for a {@link UsernamePasswordContext} and directly produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} based on that identity by authenticating against an LDAP.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>
 * ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getAttemptedFlow() != null
 * </pre>
 * @post If AuthenticationContext.getSubcontext(UsernamePasswordContext.class, false) != null, then an
 *       {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext} on a
 *       successful login. On a failed login, the
 *       {@link AbstractValidationAction#handleError(ProfileRequestContext, AuthenticationContext, String, String)}
 *       method is called.
 */
public class ValidateUsernamePasswordAgainstLDAP extends AbstractValidationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateUsernamePasswordAgainstLDAP.class);

    /** UsernamePasswordContext containing the credentials to validate. */
    @Nullable private UsernamePasswordContext upContext;

    /** LDAP authenticator. */
    @Nonnull private Authenticator authenticator;

    /** Attributes to return from authentication. */
    @Nullable private String[] returnAttributes;

    /** Authentication response associated with the login. */
    @Nullable private AuthenticationResponse response;

    /**
     * Returns the authenticator.
     * 
     * @return authenticator
     */
    @NonnullAfterInit public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * Sets the authenticator.
     * 
     * @param auth to authenticate with
     */
    public void setAuthenticator(@Nonnull final Authenticator auth) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        authenticator = Constraint.isNotNull(auth, "Authenticator cannot be null");
    }

    /**
     * Returns the return attributes.
     * 
     * @return attribute names
     */
    @Nullable public String[] getReturnAttributes() {
        return returnAttributes;
    }

    /**
     * Sets the return attributes.
     * 
     * @param attributes attribute names
     */
    public void setReturnAttributes(@Nullable final String... attributes) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        returnAttributes = attributes;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (authenticator == null) {
            throw new ComponentInitializationException("Authenticator cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        if (authenticationContext.getAttemptedFlow() == null) {
            log.debug("{} no attempted flow within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        upContext = authenticationContext.getSubcontext(UsernamePasswordContext.class, false);
        if (upContext == null) {
            log.debug("{} no UsernameContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        if (upContext.getUsername() == null || upContext.getPassword() == null) {
            log.debug("{} no username or password available within UsernamePasswordContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        return super.doPreExecute(profileRequestContext, authenticationContext);
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        try {
            log.debug("{} attempting to authenticate user {}", getLogPrefix(), upContext.getUsername());
            final AuthenticationRequest request =
                    new AuthenticationRequest(upContext.getUsername(), new Credential(upContext.getPassword()),
                            returnAttributes);
            response = authenticator.authenticate(request);
            log.trace("{} authentication response {}", getLogPrefix(), response);
            if (response.getResult()) {
                log.debug("{} login by '{}' succeeded", getLogPrefix(), upContext.getUsername());
                authenticationContext.getSubcontext(LDAPResponseContext.class, true)
                        .setAuthenticationResponse(response);
                if (response.getAccountState() != null) {
                    handleWarning(
                            profileRequestContext,
                            authenticationContext,
                            String.format("%s:%s:%s", "ACCOUNT_WARNING", response.getResultCode(),
                                    response.getMessage()), AuthnEventIds.ACCOUNT_WARNING);
                }
                buildAuthenticationResult(profileRequestContext, authenticationContext);
            } else {
                log.debug("{} login by '{}' failed", getLogPrefix(), upContext.getUsername());
                authenticationContext.getSubcontext(LDAPResponseContext.class, true)
                        .setAuthenticationResponse(response);
                if (AuthenticationResultCode.DN_RESOLUTION_FAILURE == response.getAuthenticationResultCode()
                        || AuthenticationResultCode.INVALID_CREDENTIAL == response.getAuthenticationResultCode()) {
                    handleError(profileRequestContext, authenticationContext,
                            String.format("%s:%s", response.getAuthenticationResultCode(), response.getMessage()),
                            AuthnEventIds.INVALID_CREDENTIALS);
                } else if (response.getAccountState() != null) {
                    final AccountState state = response.getAccountState();
                    handleError(profileRequestContext, authenticationContext, String.format("%s:%s:%s",
                            state.getError(), response.getResultCode(), response.getMessage()),
                            AuthnEventIds.ACCOUNT_ERROR);
                } else {
                    handleError(profileRequestContext, authenticationContext, String.format("%s:%s",
                            response.getResultCode(), response.getMessage()), AuthnEventIds.INVALID_CREDENTIALS);
                }
            }
        } catch (LdapException e) {
            log.warn(getLogPrefix() + " login by '" + upContext.getUsername() + "' produced exception", e);
            handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) throws AuthenticationException {
        subject.getPrincipals().add(new UsernamePrincipal(upContext.getUsername()));
        subject.getPrincipals().add(new LdapPrincipal(upContext.getUsername(), response.getLdapEntry()));
        return subject;
    }

}

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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.apache.velocity.VelocityContext;
import org.ldaptive.Credential;
import org.ldaptive.LdapException;
import org.ldaptive.ResultCode;
import org.ldaptive.auth.AccountState;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResultCode;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.User;
import org.ldaptive.jaas.LdapPrincipal;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AbstractUsernamePasswordCredentialValidator;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.LDAPResponseContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A password validator that authenticates against LDAP natively.
 * 
 * @since 4.0.0
 */
@ThreadSafeAfterInit
public class LDAPCredentialValidator extends AbstractUsernamePasswordCredentialValidator {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LDAPCredentialValidator.class);

    /** LDAP authenticator. */
    @NonnullAfterInit private Authenticator authenticator;

    /** Attributes to return from authentication. */
    @Nullable private String[] returnAttributes;
    
    /** Optional strategy for obtaining/transforming the password. */
    @Nullable private Function<ProfileRequestContext,char[]> passwordLookupStrategy;
    
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
        checkSetterPreconditions();
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
        checkSetterPreconditions();
        returnAttributes = attributes;
    }
    
    /**
     * Set a strategy function to produce the password to bind with.
     * 
     * @param strategy strategy function
     */
    public void setPasswordLookupStrategy(@Nullable final Function<ProfileRequestContext,char[]> strategy) {
        checkSetterPreconditions();
        passwordLookupStrategy = strategy;
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
    protected void doDestroy() {
        if (authenticator != null) {
            authenticator.close();
        }
        super.doDestroy();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final UsernamePasswordContext usernamePasswordContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {
        
        final String username = usernamePasswordContext.getTransformedUsername();
        
        log.debug("{} Attempting to authenticate user {}", getLogPrefix(), username);
        final VelocityContext context = new VelocityContext();
        context.put("usernamePasswordContext", usernamePasswordContext);
        final char[] password;
        if (passwordLookupStrategy != null) {
            password = passwordLookupStrategy.apply(profileRequestContext);
        } else {
            final String ctxPassword = usernamePasswordContext.getPassword();
            assert ctxPassword != null;
            password = ctxPassword.toCharArray();
        }
        final AuthenticationRequest request = new AuthenticationRequest(
          new User(username, context), new Credential(password), returnAttributes);
        final AuthenticationResponse response;
        try {
            // authenticator should only throw for communication errors
            response = authenticator.authenticate(request);
        } catch (final LdapException e) {
            log.error("{} Error attempting LDAP authentication for '{}'", getLogPrefix(), username, e);
            if (errorHandler != null) {
                errorHandler.handleError(
                    profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
            }
            throw e;
        }

        log.debug("{} Authentication response {}", getLogPrefix(), response);
        authenticationContext.ensureSubcontext(LDAPResponseContext.class).setAuthenticationResponse(response);
        if (response.isSuccess()) {
            log.info("{} Login by '{}' succeeded", getLogPrefix(), username);
            if (response.getAccountState() != null) {
                final AccountState.Error error = response.getAccountState().getError();
                if (warningHandler != null) {
                    warningHandler.handleWarning(
                      profileRequestContext,
                      authenticationContext,
                      String.format("%s:%s:%s", error != null ? error : "ACCOUNT_WARNING",
                        response.getResultCode(), response.getDiagnosticMessage()),
                      AuthnEventIds.ACCOUNT_WARNING);
                }
            }
            return populateSubject(usernamePasswordContext, response);
        }

        String eventToSignal;
        LdapException authException;
        if (AuthenticationResultCode.DN_RESOLUTION_FAILURE == response.getAuthenticationResultCode()
                || AuthenticationResultCode.INVALID_CREDENTIAL == response.getAuthenticationResultCode()) {
            eventToSignal = AuthnEventIds.INVALID_CREDENTIALS;
            authException = new LdapException(
              String.format("%s:%s", response.getAuthenticationResultCode(), response.getDiagnosticMessage()));
        } else if (response.getAccountState() != null) {
            final AccountState state = response.getAccountState();
            eventToSignal = AuthnEventIds.ACCOUNT_ERROR;
            authException = new LdapException(
                response.getResultCode(),
                String.format("%s:%s:%s", state.getError(), response.getResultCode(), response.getDiagnosticMessage()));
        } else if (response.getResultCode() == ResultCode.INVALID_CREDENTIALS) {
            eventToSignal = AuthnEventIds.INVALID_CREDENTIALS;
            authException = new LdapException(
                response.getResultCode(),
                String.format("%s:%s", response.getResultCode(), response.getDiagnosticMessage()));
        } else {
            eventToSignal = AuthnEventIds.AUTHN_EXCEPTION;
            authException = new LdapException(
                response.getResultCode(),
                String.format("%s:%s", response.getResultCode(), response.getDiagnosticMessage()));
        }

        log.info("{} Login by '{}' failed", getLogPrefix(), username, authException);
        if (errorHandler != null) {
            errorHandler.handleError(profileRequestContext, authenticationContext, authException, eventToSignal);
        }
        throw authException;
    }

    /**
     * Builds a new {@link Subject} populated with the necessary data.
     * 
     * @param usernamePasswordContext input context
     * @param ldapResponse LDAP response data
     * 
     * @return the subject to return
     */
    @Nonnull protected Subject populateSubject(@Nonnull final UsernamePasswordContext usernamePasswordContext,
            @Nonnull final AuthenticationResponse ldapResponse) {
        
        final Subject subject = new Subject();
        subject.getPrincipals().add(
                new LdapPrincipal(usernamePasswordContext.getTransformedUsername(), ldapResponse.getLdapEntry()));
        return super.populateSubject(subject, usernamePasswordContext);
    }

}

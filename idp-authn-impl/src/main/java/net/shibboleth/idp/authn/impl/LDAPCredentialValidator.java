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
import javax.annotation.concurrent.ThreadSafe;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AbstractUsernamePasswordCredentialValidator;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.LDAPResponseContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

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
import org.slf4j.LoggerFactory;

/**
 * A password validator that authenticates against LDAP natively.
 * 
 * @since 4.0.0
 */
@ThreadSafe
public class LDAPCredentialValidator extends AbstractUsernamePasswordCredentialValidator {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LDAPCredentialValidator.class);

    /** LDAP authenticator. */
    @Nonnull private Authenticator authenticator;

    /** Attributes to return from authentication. */
    @Nullable private String[] returnAttributes;
    
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

// Checkstyle: CyclomaticComplexity OFF    
    /** {@inheritDoc} */
    @Override
    @Nullable protected Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final UsernamePasswordContext usernamePasswordContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {
        
        final String username = usernamePasswordContext.getTransformedUsername();
        
        String eventToSignal = AuthnEventIds.INVALID_CREDENTIALS;
        
        // The error handling is squonky. We log at info to generically record the failure.
        // Known conditions are not explicitly logged but are wrapped with an exception and
        // reported out to the caller. Last ditch, an exception is logged on warn and then
        // reported out.
        
        try {
            log.debug("{} Attempting to authenticate user {}", getLogPrefix(), username);
            final VelocityContext context = new VelocityContext();
            context.put("usernamePasswordContext", usernamePasswordContext);
            final AuthenticationRequest request =
                    new AuthenticationRequest(new User(username, context),
                            new Credential(usernamePasswordContext.getPassword()), returnAttributes);
            final AuthenticationResponse response = authenticator.authenticate(request);
            log.trace("{} Authentication response {}", getLogPrefix(), response);
            if (response.getResult()) {
                log.info("{} Login by '{}' succeeded", getLogPrefix(), username);
                authenticationContext.getSubcontext(
                        LDAPResponseContext.class, true).setAuthenticationResponse(response);
                if (response.getAccountState() != null) {
                    final AccountState.Error error = response.getAccountState().getError();
                    if (warningHandler != null) {
                        warningHandler.handleWarning(
                                profileRequestContext,
                                authenticationContext,
                                String.format("%s:%s:%s", error != null ? error : "ACCOUNT_WARNING",
                                        response.getResultCode(), response.getMessage()),
                                AuthnEventIds.ACCOUNT_WARNING);
                    }
                }
                return populateSubject(usernamePasswordContext, response);
            } else {
                log.info("{} Login by '{}' failed", getLogPrefix(), username);
                authenticationContext.getSubcontext(
                        LDAPResponseContext.class, true).setAuthenticationResponse(response);
                if (AuthenticationResultCode.DN_RESOLUTION_FAILURE == response.getAuthenticationResultCode()
                        || AuthenticationResultCode.INVALID_CREDENTIAL == response.getAuthenticationResultCode()) {
                    throw new LdapException(
                            String.format("%s:%s", response.getAuthenticationResultCode(), response.getMessage()));
                } else if (response.getAccountState() != null) {
                    final AccountState state = response.getAccountState();
                    eventToSignal = AuthnEventIds.ACCOUNT_ERROR;
                    throw new LdapException(
                            String.format("%s:%s:%s", state.getError(), response.getResultCode(), response.getMessage())
                            );
                } else if (response.getResultCode() == ResultCode.INVALID_CREDENTIALS) {
                    throw new LdapException(String.format("%s:%s", response.getResultCode(), response.getMessage()));
                } else {
                    eventToSignal = AuthnEventIds.AUTHN_EXCEPTION;
                    final LdapException e =
                            new LdapException(response.getMessage(), response.getResultCode(), response.getMatchedDn(),
                            response.getControls(), response.getReferralURLs(), response.getMessageId());
                    log.warn("{} Login by {} produced exception", getLogPrefix(), username, e);
                    throw e;
                }
            }
        } catch (final LdapException e) {
            if (errorHandler != null) {
                errorHandler.handleError(profileRequestContext, authenticationContext, e, eventToSignal);
            }
            throw e;
        }
    }
// Checkstyle: CyclomaticComplexity ON

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
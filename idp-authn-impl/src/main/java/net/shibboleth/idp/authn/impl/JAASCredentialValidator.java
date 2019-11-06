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

import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import net.shibboleth.idp.authn.AbstractUsernamePasswordCredentialValidator;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A password validator that authenticates against JAAS.
 * 
 * <p>Support for complex chaining of JAAS modules remains supported but should be
 * avoided in favor of the new support for chaining validators in most cases.</p>
 * 
 * @since 4.0.0
 */
@ThreadSafe
public class JAASCredentialValidator extends AbstractUsernamePasswordCredentialValidator {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(JAASCredentialValidator.class);
    
    /** Type of JAAS Configuration to instantiate. */
    @Nullable private String loginConfigType;
    
    /** Type-specific configuration parameters. */
    @Nullable private Configuration.Parameters loginConfigParameters;
    
    /** Application name(s) in JAAS configuration to use. */
    @Nonnull private Collection<Pair<String,Subject>> loginConfigurations;
    
    /** Strategy function to dynamically derive the login config(s) to use. */
    @Nullable private Function<ProfileRequestContext,Collection<Pair<String,Subject>>> loginConfigStrategy;
    
    /** Constructor. */
    public JAASCredentialValidator() {
        // For compatibility with V2.
        loginConfigurations = Collections.singletonList(new Pair<String,Subject>("ShibUserPassAuth", null));
    }
    
    /**
     * Get the type of JAAS {@link Configuration} to use. 
     * 
     * @return the type of JAAS configuration to use
     */
    @Nullable public String getLoginConfigType() {
        return loginConfigType;
    }

    /**
     * Set the type of JAAS {@link Configuration} to use.
     * 
     * @param type the type of JAAS configuration to use
     */
    public void setLoginConfigType(@Nullable final String type) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        loginConfigType = StringSupport.trimOrNull(type);
    }

    /**
     * Get the type-specific parameters of the JAAS {@link Configuration} to use.
     * 
     * @return the JAAS configuration parameters to use
     */
    @Nullable public Configuration.Parameters getLoginConfigParameters() {
        return loginConfigParameters;
    }

    /**
     * Set the type-specific parameters of the JAAS {@link Configuration} to use.
     * 
     * @param params the JAAS configuration parameters to use
     */
    public void setLoginConfigParameters(@Nullable final Configuration.Parameters params) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        loginConfigParameters = params;
    }

    /**
     * Set the JAAS application name(s) to use, along with an optional collection of custom principals to
     * apply to the result.
     * 
     * @param configs list of JAAS application names and custom principals to use
     */
    public void setLoginConfigurations(@Nullable final Collection<Pair<String,Collection<Principal>>> configs) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        if (configs != null) {
            loginConfigurations = new ArrayList<>(configs.size());
            for (final Pair<String,Collection<Principal>> config : configs) {
                final String trimmed = StringSupport.trimOrNull(config.getFirst());
                if (trimmed != null) {
                    if (config.getSecond() == null || config.getSecond().isEmpty()) {
                        loginConfigurations.add(new Pair<String,Subject>(trimmed, null));
                    } else {
                        final Subject subject = new Subject();
                        subject.getPrincipals().addAll(config.getSecond());
                        loginConfigurations.add(new Pair<>(trimmed, subject));
                    }
                }
            }
        }
    }

    /**
     * Set the JAAS application name(s) to use.
     * 
     * @param names list of JAAS application names to use
     */
    public void setLoginConfigNames(@Nullable @NonnullElements final Collection<String> names) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        if (names != null) {
            loginConfigurations = new ArrayList<>(names.size());
            for (final String name : names) {
                final String trimmed = StringSupport.trimOrNull(name);
                if (trimmed != null) {
                    loginConfigurations.add(new Pair<String,Subject>(trimmed,null));
                }
            }
        }
    }
    
    /**
     * Set the strategy function to use to obtain the JAAS application configuration(s) to use.
     * 
     * @param strategy strategy function
     */
    public void setLoginConfigStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<Pair<String,Subject>>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        loginConfigStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final UsernamePasswordContext usernamePasswordContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {

        final RequestedPrincipalContext requestedPrincipalCtx =
                authenticationContext.getSubcontext(RequestedPrincipalContext.class);
        
        final Collection<Pair<String,Subject>> configs;
        if (loginConfigStrategy != null) {
            configs = loginConfigStrategy.apply(profileRequestContext);
        } else {
            configs = loginConfigurations;
        }

        Exception caughtException = null;
        
        for (final Pair<String,Subject> loginConfig : configs) {
            
            if (!isAcceptable(requestedPrincipalCtx, loginConfig.getSecond(), loginConfig.getFirst())) {
                continue;
            }

            final String currentLoginConfigName = loginConfig.getFirst();

            try {
                log.debug("{} Attempting to authenticate user '{}' via '{}'", getLogPrefix(),
                        usernamePasswordContext.getTransformedUsername(), currentLoginConfigName);
                final Subject subject = authenticate(currentLoginConfigName, usernamePasswordContext);
                log.info("{} Login by '{}' via '{}' succeeded", getLogPrefix(),
                        usernamePasswordContext.getTransformedUsername(), currentLoginConfigName);
                return populateSubject(subject, loginConfig.getSecond(), usernamePasswordContext);
            } catch (final LoginException e){ 
                log.info("{} Login by '{}' via '{}' failed", getLogPrefix(),
                        usernamePasswordContext.getTransformedUsername(), currentLoginConfigName, e);
                if (errorHandler != null) {
                    errorHandler.handleError(profileRequestContext, authenticationContext, e,
                            AuthnEventIds.INVALID_CREDENTIALS);
                }
                caughtException = e;
            } catch (final Exception e) {
                log.warn("{} Login by '{}' via '{}' produced exception", getLogPrefix(),
                        usernamePasswordContext.getTransformedUsername(), currentLoginConfigName, e);
                if (errorHandler != null) {
                    errorHandler.handleError(profileRequestContext, authenticationContext, e,
                            AuthnEventIds.AUTHN_EXCEPTION);
                }
                caughtException = e;
            }
        }
        
        if (caughtException == null) {
            log.info("{} No JAAS application configurations are available or acceptable for use", getLogPrefix());
            return null;
        }
        
        throw caughtException;
    }
        
    /**
     * Create a JAAS configuration and attempt a login with it.
     * 
     * @param loginConfigName the application name to use
     * @param usernamePasswordContext input context
     * 
     * @return the JAAS result
     * 
     * @throws LoginException if the JAAS login process fails
     * @throws NoSuchAlgorithmException if a JAAS configuration cannot be created
     */
    @Nonnull private Subject authenticate(@Nonnull @NotEmpty final String loginConfigName,
            @Nonnull final UsernamePasswordContext usernamePasswordContext)
                    throws LoginException, NoSuchAlgorithmException {
        
        final javax.security.auth.login.LoginContext jaasLoginCtx;
        
        if (getLoginConfigType() != null) {
            log.debug("{} Using custom JAAS configuration type {} with parameters of type {}", getLogPrefix(),
                    getLoginConfigType(), getLoginConfigParameters().getClass().getName());
            final Configuration loginConfig =
                    Configuration.getInstance(getLoginConfigType(), getLoginConfigParameters());
            jaasLoginCtx = new javax.security.auth.login.LoginContext(loginConfigName, null,
                    new SimpleCallbackHandler(usernamePasswordContext), loginConfig);
        } else {
            log.debug("{} Using system JAAS configuration", getLogPrefix());
            jaasLoginCtx = new javax.security.auth.login.LoginContext(loginConfigName, null,
                    new SimpleCallbackHandler(usernamePasswordContext));
        }

        jaasLoginCtx.login();
        
        return jaasLoginCtx.getSubject();
    }

    /**
     * Finish decorating the result.
     * 
     * @param subject the JAAS result
     * @param derivedSubject container for additional principals
     * @param usernamePasswordContext input context
     * 
     * @return final result
     */
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject,
            @Nullable final Subject derivedSubject, @Nonnull final UsernamePasswordContext usernamePasswordContext) {

        if (derivedSubject != null) {
            subject.getPrincipals().addAll(derivedSubject.getPrincipals());
        }
        return super.populateSubject(subject, usernamePasswordContext);
    }
        
    /**
     * A callback handler that provides name and password data to a JAAS login process,
     * along with other miscellany.
     * 
     * This handler supports {@link NameCallback}, {@link PasswordCallback}, and {@link LanguageCallback}.
     */
    protected class SimpleCallbackHandler implements CallbackHandler {

        /** Context for call. */
        @Nonnull private final UsernamePasswordContext context;
        
        /**
         * Constructor.
         *
         * @param usernamePasswordContext input context
         */
        public SimpleCallbackHandler(@Nonnull final UsernamePasswordContext usernamePasswordContext) {
            context = usernamePasswordContext;
        }
        
        /**
         * Handle a callback.
         * 
         * @param callbacks The list of callbacks to process.
         * 
         * @throws UnsupportedCallbackException If callbacks has a callback other than {@link NameCallback} or
         *             {@link PasswordCallback}.
         */
        public void handle(final Callback[] callbacks) throws UnsupportedCallbackException {

            if (callbacks == null || callbacks.length == 0) {
                return;
            }

            for (final Callback cb : callbacks) {
                if (cb instanceof NameCallback) {
                    final NameCallback ncb = (NameCallback) cb;
                    ncb.setName(context.getTransformedUsername());
                } else if (cb instanceof PasswordCallback) {
                    final PasswordCallback pcb = (PasswordCallback) cb;
                    pcb.setPassword(context.getPassword().toCharArray());
                }
            }
        }
    }

}
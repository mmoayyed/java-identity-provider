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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import net.shibboleth.idp.authn.AbstractUsernamePasswordValidationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * An action that checks for a {@link UsernamePasswordContext} and directly produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} based on that identity by invoking a JAAS configuration.
 * 
 * <p>Various optional properties are supported to control the JAAS configuration process.</p>
 *  
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(UsernamePasswordContext.class) != null, then
 * an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext} on a
 * successful login. On a failed login, the
 * {@link AbstractValidationAction#handleError(ProfileRequestContext, AuthenticationContext, Exception, String)}
 * method is called.
 */
public class ValidateUsernamePasswordAgainstJAAS extends AbstractUsernamePasswordValidationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateUsernamePasswordAgainstJAAS.class);
    
    /** Type of JAAS Configuration to instantiate. */
    @Nullable private String loginConfigType;
    
    /** Type-specific configuration parameters. */
    @Nullable private Configuration.Parameters loginConfigParameters;
    
    /** Application name(s) in JAAS configuration to use. */
    @Nonnull @NonnullElements private Collection< Pair< String,Collection<Principal> > > loginConfigurations;
    
    /** Strategy function to dynamically derive the login config name(s) to use. */
    @Nullable
    private Function< ProfileRequestContext,Collection< Pair< String,Collection<Principal> > > > loginConfigStrategy;
    
    /** Tracks any Principals derived from the login configuration to add to the Subject. */
    @Nullable @NonnullElements private Collection<Principal> derivedPrincipals;
    
    /** Constructor. */
    public ValidateUsernamePasswordAgainstJAAS() {
        // For compatibility with V2.
        loginConfigurations = Collections.singletonList(
                new Pair<String,Collection<Principal>>("ShibUserPassAuth", Collections.<Principal>emptyList()));
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
     * Set the JAAS application name(s) to use.
     * 
     * @param names list of JAAS application names to use
     */
    public void setLoginConfigurations(
            @Nonnull @NonnullElements final Collection< Pair< String,Collection<Principal> > > names) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(names, "Configuration list cannot be null");

        loginConfigurations = new ArrayList<>(names);
    }

    /**
     * Set the JAAS application name(s) to use.
     * 
     * @param names list of JAAS application names to use
     */
    public void setLoginConfigNames(@Nonnull @NonnullElements final Collection<String> names) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(names, "Configuration name list cannot be null");

        loginConfigurations = new ArrayList<>(names.size());
        for (final String name : names) {
            final String trimmed = StringSupport.trimOrNull(name);
            if (trimmed != null) {
                loginConfigurations.add(
                        new Pair<String,Collection<Principal>>(trimmed,Collections.<Principal>emptyList()));
            }
        }
    }
    
    /**
     * Set the strategy function to use to obtain the JAAS application name(s) to use.
     * 
     * @param strategy strategy function
     */
    public void setLoginConfigStrategy(@Nullable
            final Function< ProfileRequestContext,Collection< Pair< String,Collection<Principal> > > > strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        loginConfigStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Collection< Pair< String,Collection<Principal> > > configs;
        if (loginConfigStrategy != null) {
            configs = loginConfigStrategy.apply(profileRequestContext);
        } else {
            configs = loginConfigurations;
        }
        
        if (configs.isEmpty()) {
            log.warn("{} No JAAS application configurations are available for use", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "RequestUnsupported",
                    AuthnEventIds.REQUEST_UNSUPPORTED);
            return;
        }
        
        for (final Pair< String,Collection<Principal> > loginConfig : loginConfigurations) {
            try {
                log.debug("{} Attempting to authenticate user '{}' via '{}'", getLogPrefix(),
                        getUsernamePasswordContext().getUsername(), loginConfig.getFirst());
                authenticate(loginConfig.getFirst());
                log.info("{} Login by '{}' succeeded", getLogPrefix(), getUsernamePasswordContext().getUsername());
                derivedPrincipals = loginConfig.getSecond();
                buildAuthenticationResult(profileRequestContext, authenticationContext);
                ActionSupport.buildProceedEvent(profileRequestContext);
                return;
            } catch (final LoginException e){ 
                log.info("{} Login by '{}' failed", getLogPrefix(), getUsernamePasswordContext().getUsername(), e);
                handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.INVALID_CREDENTIALS);
            } catch (final Exception e) {
                log.warn("{} Login by '{}' produced exception", getLogPrefix(),
                        getUsernamePasswordContext().getUsername(), e);
                handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
            }
        }
    }
    
    /**
     * Create a JAAS configuration and attempt a login with it.
     * 
     * @param loginConfigName the application name to use
     * 
     * @throws LoginException if the JAAS login process fails
     * @throws NoSuchAlgorithmException if a JAAS configuration cannot be created
     */
    private void authenticate(@Nonnull @NotEmpty final String loginConfigName)
            throws LoginException, NoSuchAlgorithmException {
        
        final javax.security.auth.login.LoginContext jaasLoginCtx;
        
        if (getLoginConfigType() != null) {
            log.debug("{} Using custom JAAS configuration type {} with parameters of type {}", getLogPrefix(),
                    getLoginConfigType(), getLoginConfigParameters().getClass().getName());
            final Configuration loginConfig =
                    Configuration.getInstance(getLoginConfigType(), getLoginConfigParameters());
            jaasLoginCtx = new javax.security.auth.login.LoginContext(loginConfigName, getSubject(),
                    new SimpleCallbackHandler(), loginConfig);
        } else {
            log.debug("{} Using system JAAS configuration", getLogPrefix());
            jaasLoginCtx = new javax.security.auth.login.LoginContext(loginConfigName, getSubject(),
                    new SimpleCallbackHandler());
        }

        jaasLoginCtx.login();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        
        final Subject theSubject = super.populateSubject(subject);
        if (derivedPrincipals != null && !derivedPrincipals.isEmpty()) {
            theSubject.getPrincipals().addAll(derivedPrincipals);
        }
        return theSubject;
    }
    
    
    /**
     * A callback handler that provides static name and password data to a JAAS login process.
     * 
     * This handler only supports {@link NameCallback} and {@link PasswordCallback}.
     */
    protected class SimpleCallbackHandler implements CallbackHandler {

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

            for (Callback cb : callbacks) {
                if (cb instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) cb;
                    ncb.setName(getUsernamePasswordContext().getUsername());
                } else if (cb instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback) cb;
                    pcb.setPassword(getUsernamePasswordContext().getPassword().toCharArray());
                }
            }
        }
    }
}

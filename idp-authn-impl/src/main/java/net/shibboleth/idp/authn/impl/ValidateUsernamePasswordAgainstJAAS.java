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
import java.util.Set;

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
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
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
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#REQUEST_UNSUPPORTED}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(UsernamePasswordContext.class) != null, then
 * an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext} on a
 * successful login. On a failed login, the
 * {@link AbstractValidationAction#handleError(ProfileRequestContext, AuthenticationContext, Exception, String)}
 * method is called.
 */
public class ValidateUsernamePasswordAgainstJAAS extends AbstractUsernamePasswordValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn"; 
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateUsernamePasswordAgainstJAAS.class);
    
    /** Type of JAAS Configuration to instantiate. */
    @Nullable private String loginConfigType;
    
    /** Type-specific configuration parameters. */
    @Nullable private Configuration.Parameters loginConfigParameters;
    
    /** Application name(s) in JAAS configuration to use. */
    @Nonnull private Collection<Pair<String,Subject>> loginConfigurations;
    
    /** Strategy function to dynamically derive the login config(s) to use. */
    @Nullable
    private Function<ProfileRequestContext,Collection<Pair<String,Subject>>> loginConfigStrategy;
    
    /** Saved off context. */
    @Nullable private RequestedPrincipalContext requestedPrincipalCtx;
    
    /** Tracks any principals derived from the login configuration to add to the Subject. */
    @Nullable private Subject derivedSubject;
    
    /** Tracker for current login config for reporting. */
    @Nullable private String currentLoginConfigName;
    
    /** Constructor. */
    public ValidateUsernamePasswordAgainstJAAS() {
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
                        loginConfigurations.add(new Pair<String,Subject>(trimmed, subject));
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
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        requestedPrincipalCtx = authenticationContext.getSubcontext(RequestedPrincipalContext.class);
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final Collection<Pair<String,Subject>> configs;
        if (loginConfigStrategy != null) {
            configs = loginConfigStrategy.apply(profileRequestContext);
        } else {
            configs = loginConfigurations;
        }
        
        boolean eventSignaled = false;
        
        for (final Pair<String,Subject> loginConfig : configs) {
            
            if (!isAcceptable(authenticationContext, loginConfig.getFirst(), loginConfig.getSecond())) {
                continue;
            }
            
            try {
                currentLoginConfigName = loginConfig.getFirst();
                log.debug("{} Attempting to authenticate user '{}' via '{}'", getLogPrefix(),
                        getUsernamePasswordContext().getUsername(), currentLoginConfigName);
                authenticate(currentLoginConfigName);
                log.info("{} Login by '{}' succeeded", getLogPrefix(), getUsernamePasswordContext().getUsername());
                recordSuccess();
                derivedSubject = loginConfig.getSecond();
                buildAuthenticationResult(profileRequestContext, authenticationContext);
                ActionSupport.buildProceedEvent(profileRequestContext);
                return;
            } catch (final LoginException e){ 
                log.info("{} Login by '{}' failed", getLogPrefix(), getUsernamePasswordContext().getUsername(), e);
                handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.INVALID_CREDENTIALS);
                recordFailure();
                eventSignaled = true;
            } catch (final Exception e) {
                log.warn("{} Login by '{}' produced exception", getLogPrefix(),
                        getUsernamePasswordContext().getUsername(), e);
                handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
                recordFailure();
                eventSignaled = true;
            }
        }
    
        if (!eventSignaled) {
            log.warn("{} No JAAS application configurations are available or acceptable for use", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "RequestUnsupported",
                    AuthnEventIds.REQUEST_UNSUPPORTED);
        }
    }
    
    /**
     * Checks a particular JAAS configuration and principal collection for suitability.
     * 
     * @param authenticationContext the authentication context
     * @param configName name of JAAS config
     * @param subject collection of custom principals to check, embedded in a subject
     * 
     * @return true iff the request does not specify requirements or the principal collection is empty
     *  or the combination is acceptable
     */
    private boolean isAcceptable(@Nonnull final AuthenticationContext authenticationContext,
            @Nonnull @NotEmpty final String configName, @Nullable final Subject subject) {
        
        if (subject != null && requestedPrincipalCtx != null && requestedPrincipalCtx.getOperator() != null) {
            log.debug("{} Request contains principal requirements, evaluating JAAS config '{}' for compatibility",
                    getLogPrefix(), configName);
            for (final Principal p : requestedPrincipalCtx.getRequestedPrincipals()) {
                final PrincipalEvalPredicateFactory factory =
                        requestedPrincipalCtx.getPrincipalEvalPredicateFactoryRegistry().lookup(
                                p.getClass(), requestedPrincipalCtx.getOperator());
                if (factory != null) {
                    final PrincipalEvalPredicate predicate = factory.getPredicate(p);
                    final PrincipalSupportingComponent wrapper = new PrincipalSupportingComponent() {
                        public <T extends Principal> Set<T> getSupportedPrincipals(Class<T> c) {
                            return subject.getPrincipals(c);
                        }
                    };
                    if (predicate.apply(wrapper)) {
                        log.debug("{} JAAS config '{}' compatible with principal type '{}' and operator '{}'",
                                getLogPrefix(), configName, p.getClass(), requestedPrincipalCtx.getOperator());
                        requestedPrincipalCtx.setMatchingPrincipal(predicate.getMatchingPrincipal());
                        return true;
                    } else {
                        log.debug("{} JAAS config '{}' not compatible with principal type '{}' and operator '{}'",
                                getLogPrefix(), configName, p.getClass(), requestedPrincipalCtx.getOperator());
                    }
                } else {
                    log.debug("{} No comparison logic registered for principal type '{}' and operator '{}'",
                            getLogPrefix(), p.getClass(), requestedPrincipalCtx.getOperator());
                }
            }
            
            log.debug("{} Skipping JAAS config '{}', not compatible with request's principal requirements",
                    getLogPrefix(), configName);
            return false;
        }
        
        return true;
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
        if (derivedSubject != null) {
            theSubject.getPrincipals().addAll(derivedSubject.getPrincipals());
        }
        return theSubject;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String getMetricName() {
        return super.getMetricName() + '.' + currentLoginConfigName;
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

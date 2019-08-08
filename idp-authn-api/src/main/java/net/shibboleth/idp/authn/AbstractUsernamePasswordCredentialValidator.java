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

package net.shibboleth.idp.authn;

import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.principal.PasswordPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract {@link CredentialValidator} that checks for a {@link UsernamePasswordContext} and delegates
 * to subclasses to produce an {@link net.shibboleth.idp.authn.AuthenticationResult}.
 * 
 * @since 4.0.0
 */
public abstract class AbstractUsernamePasswordCredentialValidator extends AbstractIdentifiedInitializableComponent
        implements CredentialValidator {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.password"; 
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractUsernamePasswordCredentialValidator.class);

    /** Lookup strategy for UP context. */
    @Nonnull private Function<AuthenticationContext,UsernamePasswordContext> usernamePasswordContextLookupStrategy;
    
    /** Whether to save the password in the Java Subject's private credentials. */
    private boolean savePasswordToCredentialSet;
    
    /** Whether to remove the {@link UsernamePasswordContext} after successful validation. */
    private boolean removeContextAfterValidation;
    
    /** A regular expression to apply for acceptance testing. */
    @Nullable private Pattern matchExpression;
    
    /** Cached log prefix. */
    @Nullable private String logPrefix;
    
    /** Constructor. */
    public AbstractUsernamePasswordCredentialValidator() {
        usernamePasswordContextLookupStrategy = new ChildContextLookup<>(UsernamePasswordContext.class);
        removeContextAfterValidation = true;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setId(final String id) {
        super.setId(id);
    }
    
    /**
     * Set the lookup strategy to locate the {@link UsernamePasswordContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setUsernamePasswordContextLookupStrategy(
            @Nonnull final Function<AuthenticationContext,UsernamePasswordContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        usernamePasswordContextLookupStrategy = Constraint.isNotNull(strategy,
                "UsernamePasswordContextLookupStrategy cannot be null");
    }
    
    /**
     * Get whether to save the password in the private credential set.
     * 
     * @return whether to save the password in the private credential set
     */
    public boolean savePasswordToCredentialSet() {
        return savePasswordToCredentialSet;
    }
    
    /**
     * Set whether to save the password in the private credential set.
     * 
     * @param flag  flag to set
     */
    public void setSavePasswordToCredentialSet(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        savePasswordToCredentialSet = flag;
    }

    /**
     * Get whether to remove the {@link UsernamePasswordContext} after it's
     * successfully validated.
     * 
     * <p>Defaults to true</p>
     * 
     * @return whether to remove the context after successful validation
     */
    public boolean removeContextAfterValidation() {
        return removeContextAfterValidation;
    }
    
    /**
     * Set whether to remove the {@link UsernamePasswordContext} after it's
     * successfully validated.
     * 
     * @param flag  flag to set
     */
    public void setRemoveContextAfterValidation(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        removeContextAfterValidation = flag;
    }

    /**
     * Set a matching expression to apply to the username for acceptance. 
     * 
     * @param expression a matching expression
     */
    public void setMatchExpression(@Nullable final Pattern expression) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        matchExpression = expression;
    }
    
    /** {@inheritDoc} */
    @Override
    public Subject validate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {
        
        final UsernamePasswordContext upContext = getUsernamePasswordContext(authenticationContext);
        if (upContext == null) {
            log.info("{} No UsernamePasswordContext available", getLogPrefix());
            if (errorHandler != null) {
                errorHandler.handleError(profileRequestContext, authenticationContext, (String) null,
                        AuthnEventIds.NO_CREDENTIALS);
            }
            throw new LoginException(AuthnEventIds.NO_CREDENTIALS);
        } else if (upContext.getUsername() == null) {
            log.info("{} No username available within UsernamePasswordContext", getLogPrefix());
            if (errorHandler != null) {
                errorHandler.handleError(profileRequestContext, authenticationContext, (String) null,
                        AuthnEventIds.NO_CREDENTIALS);
            }
            throw new LoginException(AuthnEventIds.NO_CREDENTIALS);
        } else if (upContext.getPassword() == null) {
            log.info("{} No password available within UsernamePasswordContext", getLogPrefix());
            if (errorHandler != null) {
                errorHandler.handleError(profileRequestContext, authenticationContext, (String) null,
                        AuthnEventIds.INVALID_CREDENTIALS);
            }
            throw new LoginException(AuthnEventIds.INVALID_CREDENTIALS);
        }
        
        if (matchExpression != null && !matchExpression.matcher(upContext.getUsername()).matches()) {
            log.debug("{} Username '{}' did not match expression", getLogPrefix(), upContext.getUsername());
            return null;
        }
                
        return doValidate(profileRequestContext, authenticationContext, upContext, warningHandler, errorHandler);
    }

    /**
     * Override method for subclasses to use to perform the actual validation.
     * 
     * @param profileRequestContext profile request context
     * @param authenticationContext authentication context
     * @param usernamePasswordContext the username/password to validate
     * @param warningHandler optional warning handler interface
     * @param errorHandler optional error handler interface
     * 
     * @return the validated result, or null if inapplicable
     * 
     * @throws Exception if an error occurs
     */
    @Nullable protected abstract Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final UsernamePasswordContext usernamePasswordContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception;

    /**
     * Get the {@link UsernamePasswordContext} to validate. 
     * 
     * @param authenticationContext parent context
     * 
     * @return context to validate
     */
    @Nullable protected UsernamePasswordContext getUsernamePasswordContext(
            @Nonnull final AuthenticationContext authenticationContext) {
        return usernamePasswordContextLookupStrategy.apply(authenticationContext);
    }

    /**
     * Decorate the subject with "standard" content from the validation
     * and clean up as instructed.
     * 
     * @param subject the subject being returned
     * @param usernamePasswordContext the username/password validated
     * 
     * @return the decorated subject
     */
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject,
            @Nonnull final UsernamePasswordContext usernamePasswordContext) {
        subject.getPrincipals().add(new UsernamePrincipal(usernamePasswordContext.getUsername()));
        if (savePasswordToCredentialSet) {
            subject.getPrivateCredentials().add(new PasswordPrincipal(usernamePasswordContext.getPassword()));
        }
        
        if (removeContextAfterValidation) {
            usernamePasswordContext.getParent().removeSubcontext(usernamePasswordContext);
            usernamePasswordContext.setPassword(null);
        }
        
        return subject;
    }

    /**
     * Return a prefix for logging messages for this component.
     * 
     * @return a string for insertion at the beginning of any log messages
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        if (logPrefix == null) {
            logPrefix = "Credential Validator " + (getId() != null ? getId() : "(unknown)") + ":";
        }
        return logPrefix;
    }
    
}
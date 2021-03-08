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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.principal.PasswordPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract {@link CredentialValidator} that checks for a {@link UsernamePasswordContext} and delegates
 * to subclasses to produce a result.
 * 
 * @since 4.0.0
 */
@ThreadSafeAfterInit
public abstract class AbstractUsernamePasswordCredentialValidator extends AbstractCredentialValidator {

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
    
    /** Match patterns and replacement strings to apply prior to use. */
    @Nonnull @NonnullElements private List<Pair<Pattern,String>> transforms;

    /** Convert username to uppercase prior to transforms? */
    private boolean uppercase;
    
    /** Convert username to lowercase prior to transforms? */
    private boolean lowercase;
    
    /** Trim username prior to transforms? */
    private boolean trim;
    
    /** Constructor. */
    public AbstractUsernamePasswordCredentialValidator() {
        usernamePasswordContextLookupStrategy = new ChildContextLookup<>(UsernamePasswordContext.class);

        transforms = Collections.emptyList();
        
        uppercase = false;
        lowercase = false;
        trim = false;
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
     * 
     * @deprecated
     */
    @Deprecated(since="4.1.0", forRemoval=true)
    public boolean removeContextAfterValidation() {
        return removeContextAfterValidation;
    }
    
    /**
     * Set whether to remove the {@link UsernamePasswordContext} after it's
     * successfully validated.
     * 
     * @param flag  flag to set
     * 
     * @deprecated
     */
    @Deprecated(since="4.1.0", forRemoval=true)
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
        
        if (expression != null && !expression.pattern().isEmpty()) {
            matchExpression = expression;
        } else {
            matchExpression = null;
        }
    }
    
    /**
     * A collection of regular expression and replacement pairs.
     * 
     * @param newTransforms collection of replacement transforms
     */
    public void setTransforms(@Nullable @NonnullElements final Collection<Pair<String,String>> newTransforms) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        if (newTransforms != null) {
            transforms = new ArrayList<>();
            for (final Pair<String,String> p : newTransforms) {
                final Pattern pattern = Pattern.compile(StringSupport.trimOrNull(p.getFirst()));
                transforms.add(new Pair<>(pattern, Constraint.isNotNull(
                        StringSupport.trimOrNull(p.getSecond()), "Replacement expression cannot be null")));
            }
        } else {
            transforms = Collections.emptyList();
        }
    }

    /**
     * Controls conversion to uppercase prior to applying any transforms.
     * 
     * @param flag  uppercase flag
     */
    public void setUppercase(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        uppercase = flag;
    }

    /**
     * Controls conversion to lowercase prior to applying any transforms.
     * 
     * @param flag lowercase flag
     */
    public void setLowercase(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        lowercase = flag;
    }
    
    /**
     * Controls whitespace trimming prior to applying any transforms.
     * 
     * @param flag trim flag
     */
    public void setTrim(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        trim = flag;
    }
    
    /** {@inheritDoc} */
    @Override
    protected Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final UsernamePasswordContext upContext = usernamePasswordContextLookupStrategy.apply(authenticationContext);
        if (upContext == null) {
            log.info("{} No UsernamePasswordContext available", getLogPrefix());
            if (errorHandler != null) {
                errorHandler.handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                        AuthnEventIds.NO_CREDENTIALS);
            }
            throw new LoginException(AuthnEventIds.NO_CREDENTIALS);
        } else if (upContext.getUsername() == null) {
            log.info("{} No username available within UsernamePasswordContext", getLogPrefix());
            if (errorHandler != null) {
                errorHandler.handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                        AuthnEventIds.NO_CREDENTIALS);
            }
            throw new LoginException(AuthnEventIds.NO_CREDENTIALS);
        } else if (upContext.getPassword() == null) {
            log.info("{} No password available within UsernamePasswordContext", getLogPrefix());
            if (errorHandler != null) {
                errorHandler.handleError(profileRequestContext, authenticationContext,
                        AuthnEventIds.INVALID_CREDENTIALS, AuthnEventIds.INVALID_CREDENTIALS);
            }
            throw new LoginException(AuthnEventIds.INVALID_CREDENTIALS);
        }
        
        upContext.setTransformedUsername(applyTransforms(upContext.getUsername()));
        
        if (matchExpression != null && !matchExpression.matcher(upContext.getTransformedUsername()).matches()) {
            log.debug("{} Username '{}' did not match expression", getLogPrefix(), upContext.getTransformedUsername());
            return null;
        }
                
        return doValidate(profileRequestContext, authenticationContext, upContext, warningHandler, errorHandler);
    }

    /**
     * Override method for subclasses to use to perform the actual validation.
     * 
     * <p>Any configured transforms will have been applied to populate the context with a transformed
     * username prior to this method call.</p>
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
        subject.getPrincipals().add(new UsernamePrincipal(usernamePasswordContext.getTransformedUsername()));
        if (savePasswordToCredentialSet) {
            subject.getPrivateCredentials().add(new PasswordPrincipal(usernamePasswordContext.getPassword()));
        }

        // This is migrating out to the validation action, leaving code here for now but we won't use it.
        if (removeContextAfterValidation) {
            usernamePasswordContext.getParent().removeSubcontext(usernamePasswordContext);
            usernamePasswordContext.setPassword(null);
        }
        
        return super.populateSubject(subject);
    }
    
    /**
     * Apply any configured regular expression replacements to an input value and return the result.
     * 
     * @param input the input string
     * 
     * @return  the result of applying the expressions
     */
    @Nonnull @NotEmpty protected String applyTransforms(@Nonnull @NotEmpty final String input) {
        
        String s = input;
        
        if (trim) {
            log.trace("{} Trimming whitespace of input string '{}'", getLogPrefix(), s);
            s = s.trim();
        }
        
        if (lowercase) {
            log.trace("{} Converting input string '{}' to lowercase", getLogPrefix(), s);
            s = s.toLowerCase();
        } else if (uppercase) {
            log.trace("{} Converting input string '{}' to uppercase", getLogPrefix(), s);
            s = s.toUpperCase();
        }
        
        if (transforms.isEmpty()) {
            return s;
        }
        
        for (final Pair<Pattern,String> p : transforms) {            
            final Matcher m = p.getFirst().matcher(s);
            log.trace("{} Applying replacement expression '{}' against input '{}'", getLogPrefix(),
                    p.getFirst().pattern(), s);
            s = m.replaceAll(p.getSecond());
            log.trace("{} Result of replacement is '{}'", getLogPrefix(), s);
        }
        
        return s;
    }

}
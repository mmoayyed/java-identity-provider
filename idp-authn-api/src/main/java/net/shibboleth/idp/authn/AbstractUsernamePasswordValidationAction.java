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

import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.idp.authn.principal.PasswordPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract action that checks for a {@link UsernamePasswordContext} and produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} based on that identity by invoking
 * a subclass method.
 *  
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @post If AuthenticationContext.getSubcontext(UsernamePasswordContext.class) != null, then
 * an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext} on a
 * successful login. On a failed login, the
 * {@link AbstractValidationAction#handleError(ProfileRequestContext, AuthenticationContext, Exception, String)}
 * method is called.
 */
public abstract class AbstractUsernamePasswordValidationAction extends AbstractValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.password"; 
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractUsernamePasswordValidationAction.class);

    /** Whether to save the password in the Java Subject's private credentials. */
    private boolean savePasswordToCredentialSet;
    
    /** Whether to remove the {@link UsernamePasswordContext} after successful validation. */
    private boolean removeContextAfterValidation;
    
    /** A regular expression to apply for acceptance testing. */
    @Nullable private Pattern matchExpression;
    
    /** UsernamePasswordContext containing the credentials to validate. */
    @Nullable private UsernamePasswordContext upContext;
    
    /** Constructor. */
    public AbstractUsernamePasswordValidationAction() {
        removeContextAfterValidation = true;
        setMetricName(DEFAULT_METRIC_NAME);
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
     * @since 3.3.0
     */
    public boolean removeContextAfterValidation() {
        return removeContextAfterValidation;
    }
    
    /**
     * Set whether to remove the {@link UsernamePasswordContext} after it's
     * successfully validated.
     * 
     * @param flag  flag to set
     * 
     * @since 3.3.0
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
    
    /**
     * Get the {@link UsernamePasswordContext} to validate. 
     * 
     * @return context to validate
     */
    @Nullable public UsernamePasswordContext getUsernamePasswordContext() {
        return upContext;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
                
        upContext = authenticationContext.getSubcontext(UsernamePasswordContext.class);
        if (upContext == null) {
            log.info("{} No UsernamePasswordContext available within authentication context", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "NoCredentials", AuthnEventIds.NO_CREDENTIALS);
            recordFailure();
            return false;
        } else if (upContext.getUsername() == null) {
            log.info("{} No username available within UsernamePasswordContext", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "NoCredentials", AuthnEventIds.NO_CREDENTIALS);
            recordFailure();
            return false;
        } else if (upContext.getPassword() == null) {
            log.info("{} No password available within UsernamePasswordContext", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, "InvalidCredentials",
                    AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure();
            return false;
        }
        
        if (matchExpression != null && !matchExpression.matcher(upContext.getUsername()).matches()) {
            log.debug("{} Username '{}' did not match expression", getLogPrefix(), upContext.getUsername());
            handleError(profileRequestContext, authenticationContext, "InvalidCredentials",
                    AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure();
            return false;
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        subject.getPrincipals().add(new UsernamePrincipal(upContext.getUsername()));
        if (savePasswordToCredentialSet) {
            subject.getPrivateCredentials().add(new PasswordPrincipal(upContext.getPassword()));
        }
        
        if (removeContextAfterValidation) {
            upContext.getParent().removeSubcontext(upContext);
            upContext.setPassword(null);
            upContext = null;
        }
        
        return subject;
    }
    
}
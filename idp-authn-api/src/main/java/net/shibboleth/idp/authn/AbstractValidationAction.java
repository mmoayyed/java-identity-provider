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

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * A base class for authentication related actions that validate credentials and produce an
 * {@link AuthenticationResult}.
 */
public abstract class AbstractValidationAction extends AbstractAuthenticationAction
    implements PrincipalSupportingComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractValidationAction.class);
    
    /** Basis for {@link AuthenticationResult}. */
    @Nonnull private final Subject authenticatedSubject;
    
    /** Error messages indicating an unknown username. */
    @Nonnull @NonnullElements private Collection<String> unknownUsernameErrors;

    /** Error messages indicating an invalid password. */
    @Nonnull @NonnullElements private Collection<String> invalidPasswordErrors;

    /** Error messages indicating an expired password. */
    @Nonnull @NonnullElements private Collection<String> expiredPasswordErrors;

    /** Error messages indicating a locked account. */
    @Nonnull @NonnullElements private Collection<String> accountLockedErrors;

    /** Error messages indicating a disabled account. */
    @Nonnull @NonnullElements private Collection<String> accountDisabledErrors;
    
    /** Constructor. */
    public AbstractValidationAction() {
        super();

        authenticatedSubject = new Subject();
        unknownUsernameErrors = Collections.emptyList();
        invalidPasswordErrors = Collections.emptyList();
        expiredPasswordErrors = Collections.emptyList();
        accountLockedErrors = Collections.emptyList();
        accountDisabledErrors = Collections.emptyList();
    }

    /**
     * Get the error messages indicating an unknown username.
     * 
     * @return the "unknown username" error messages
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<String> getUnknownUsernameErrors() {
        return ImmutableList.copyOf(unknownUsernameErrors);
    }
    
    /**
     * Set the error messages indicating an unknown username.
     * 
     * @param messages the "unknown username" error messages to set
     */
    public void setUnknownUsernameErrors(@Nonnull @NonnullElements final Collection<String> messages) {
        unknownUsernameErrors = Lists.newArrayList(Collections2.filter(messages, Predicates.notNull()));
    }
    
    /**
     * Get the error messages indicating an invalid password.
     * 
     * @return the "invalid password" error messages
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<String> getInvalidPasswordErrors() {
        return ImmutableList.copyOf(invalidPasswordErrors);
    }
    
    /**
     * Sets the error messages indicating an invalid password.
     * 
     * @param messages the "invalid password" error messages to set
     */
    public void setInvalidPasswordErrors(@Nonnull @NonnullElements final Collection<String> messages) {
        invalidPasswordErrors = Lists.newArrayList(Collections2.filter(messages, Predicates.notNull()));
    }
    
    /**
     * Get the error messages indicating an expired password.
     * 
     * @return the "expired password" error messages
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<String> getExpiredPasswordErrors() {
        return ImmutableList.copyOf(expiredPasswordErrors);
    }

    /**
     * Sets the error messages indicating an expired password.
     * 
     * @param messages the "expired password" error messages to set
     */
    public void setExpiredPasswordErrors(@Nonnull @NonnullElements final Collection<String> messages) {
        expiredPasswordErrors = Lists.newArrayList(Collections2.filter(messages, Predicates.notNull()));
    }
        
    /**
     * Get the error messages indicating a locked account.
     * 
     * @return the "account locked" error messages
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<String> getAccountLockedErrors() {
        return ImmutableList.copyOf(accountLockedErrors);
    }

    /**
     * Sets the error messages indicating a locked account.
     * 
     * @param messages the "account locked" error messages to set
     */
    public void setAccountLockedErrors(@Nonnull @NonnullElements final Collection<String> messages) {
        accountLockedErrors = Lists.newArrayList(Collections2.filter(messages, Predicates.notNull()));
    }
    
    /**
     * Get the error messages indicating a disabled account.
     * 
     * @return the "account disabled" error messages
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<String> getAccountDisabledErrors() {
        return ImmutableList.copyOf(accountDisabledErrors);
    }

    /**
     * Sets the error messages indicating a disabled account.
     * 
     * @param messages the "account disabled" error messages to set
     */
    public void setAccountDisabledErrors(@Nonnull @NonnullElements final Collection<String> messages) {
        accountDisabledErrors = Lists.newArrayList(Collections2.filter(messages, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable public <T extends Principal> Set<T> getSupportedPrincipals(
            @Nonnull final Class<T> c) {
        return authenticatedSubject.getPrincipals(c);
    }
    
    /**
     * Set supported non-user-specific principals that the action will include in the subjects
     * it generates.
     * 
     * @param <T> a type of principal to add, if not generic
     * @param principals supported principals to include
     */
    public <T extends Principal> void setSupportedPrincipals(@Nonnull @NonnullElements final Collection<T> principals) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(principals, "Principal collection cannot be null.");
        
        authenticatedSubject.getPrincipals().clear();
        authenticatedSubject.getPrincipals().addAll(Collections2.filter(principals, Predicates.notNull()));
    }

    /**
     * Copy the principals from the {@link AuthenticationFlowDescriptor#getSupportedPrincipals()} method
     * as the basis for the subjects generated by this action.
     * 
     * <p>This is a shortcut method for handling the simple case in which all of the principals defined for a flow
     * apply to the action. More complex flows in which different validation steps may produce different principals
     * should explicitly define the appropriate subset to be generated by each action by using the other setter.</p>
     * 
     * @param descriptor the flow descriptor to copy from
     */
    public void setSupportedPrincipals(@Nonnull AuthenticationFlowDescriptor descriptor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(descriptor, "Authentication flow descriptor cannot be null.");
        
        authenticatedSubject.getPrincipals().clear();
        authenticatedSubject.getPrincipals().addAll(descriptor.getSupportedPrincipals());
    }
    
    /**
     * Get the subject to be produced by successful execution of this action.
     * 
     * @return  the subject meant as the result of this action
     */
    @Nonnull protected Subject getSubject() {
        return authenticatedSubject;
    }

    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        
        // If the request mandates particular principals, evaluate this validating component to see if it
        // can produce a matching principal. This skips validators chained together in flows that aren't
        // able to satisfy the request.
        final RequestedPrincipalContext rpCtx =
                authenticationContext.getSubcontext(RequestedPrincipalContext.class, false);
        if (rpCtx != null) {
            log.debug("{} Request contains principal requirements, evaluating for compatibility", getLogPrefix());
            for (Principal p : rpCtx.getRequestedPrincipals()) {
                final PrincipalEvalPredicateFactory factory =
                        authenticationContext.getPrincipalEvalPredicateFactoryRegistry().lookup(
                                p.getClass(), rpCtx.getOperator());
                if (factory != null) {
                    if (factory.getPredicate(p).apply(this)) {
                        log.debug("{} Compatible with principal type '{}' and operator '{}'", getLogPrefix(),
                                p.getClass(), rpCtx.getOperator());
                        return true;
                    } else {
                        log.debug("{} Not compatible with principal type '{}' and operator '{}'", getLogPrefix(),
                                p.getClass(), rpCtx.getOperator());
                    }
                } else {
                    log.debug("{} No comparison logic registered for principal type '{}' and operator '{}'",
                            getLogPrefix(), p.getClass(), rpCtx.getOperator());
                }
            }
            
            log.info("{} Skipping validator, not compatible with request's principal requirements", getLogPrefix());
            return false;
        }
        
        return true;
    }
    
    /**
     * Normally called upon successful completion of credential validation, calls the {@link #populateSubject(Subject)}
     * abstract method, stores an {@AuthenticationResult} in the {@link AuthenticationContext}, and attaches a
     * {@link SubjectCanonicalizationContext} to the {@link ProfileRequestContext} in preparation for c14n to occur.
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * 
     * @throws AuthenticationException thrown if there is a problem performing the authentication action
     */
    protected void buildAuthenticationResult(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        
        AuthenticationResult result = new AuthenticationResult(authenticationContext.getAttemptedFlow().getId(),
                populateSubject(authenticatedSubject));
        authenticationContext.setAuthenticationResult(result);
        profileRequestContext.addSubcontext(new SubjectCanonicalizationContext(result.getSubject()), true);
    }

    /**
     * Subclasses must override this method to complete the population of the {@link Subject} with
     * {@link Principal} and credential information based on the validation they perform.
     * 
     * <p>Typically this will include attaching a {@link UsernamePrincipal}, but this is not a requirement
     * if other components are suitably overridden.</p>
     * 
     * @param subject subject to populate
     * @return  the input subject
     * @throws AuthenticationException  if an error occurs
     */
    @Nonnull protected abstract Subject populateSubject(@Nonnull final Subject subject) throws AuthenticationException;
    
    /**
     * Adds an exception encountered during the action to an {@link AuthenticationErrorContext}, creating one if
     * necessary, beneath the {@link AuthenticationContext}, and uses the supplied event as the result of the action.
     * 
     * <p>The exception is matched against the various error message collections to determine whether to also set
     * one of the {@link AuthenticationErrorContext} flags to indicate a more specific error type.</p>
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @param e the exception to process
     * @param eventId the event to "return" via an {@link EventContext}
     */
    protected void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull final Exception e,
            @Nonnull @NotEmpty final String eventId) {
        
        AuthenticationErrorContext errorCtx =
                authenticationContext.getSubcontext(AuthenticationErrorContext.class, true);
        errorCtx.addException(e);
        
        ActionSupport.buildEvent(profileRequestContext, eventId);

        for (String m : unknownUsernameErrors) {
            if (e.getMessage().contains(m)) {
                errorCtx.setUnknownUsername(true);
                return;
            }
        }

        for (String m : invalidPasswordErrors) {
            if (e.getMessage().contains(m)) {
                errorCtx.setInvalidPassword(true);
                return;
            }
        }

        for (String m : expiredPasswordErrors) {
            if (e.getMessage().contains(m)) {
                errorCtx.setExpiredPassword(true);
                return;
            }
        }


        for (String m : accountDisabledErrors) {
            if (e.getMessage().contains(m)) {
                errorCtx.setAccountDisabled(true);
                return;
            }
        }
        
        for (String m : accountLockedErrors) {
            if (e.getMessage().contains(m)) {
                errorCtx.setAccountLocked(true);
                return;
            }
        }
    }
    
}
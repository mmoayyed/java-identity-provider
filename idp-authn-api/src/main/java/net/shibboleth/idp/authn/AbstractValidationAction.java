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
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.AuthenticationWarningContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * A base class for authentication related actions that validate credentials and produce an
 * {@link AuthenticationResult}.
 * 
 * @event {@link AuthnEventIds#REQUEST_UNSUPPORTED}
 */
public abstract class AbstractValidationAction extends AbstractAuthenticationAction
    implements PrincipalSupportingComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractValidationAction.class);
    
    /** Basis for {@link AuthenticationResult}. */
    @Nonnull private final Subject authenticatedSubject;
    
    /** Track whether custom principals have been explicitly set (including the empty set). */
    private boolean principalsAdded;
    
    /** Indicates whether to clear any existing {@link AuthenticationErrorContext} before execution. */
    private boolean clearErrorContext;
    
    /** Error messages associated with a specific error condition token. */
    @Nonnull @NonnullElements private Map<String,Collection<String>> classifiedMessages;
    
    /** Predicate to apply when setting AuthenticationResult cacheability. */
    @Nullable private Predicate<ProfileRequestContext> resultCachingPredicate;
    
    /** Constructor. */
    public AbstractValidationAction() {
        super();

        authenticatedSubject = new Subject();
        clearErrorContext = true;
        classifiedMessages = Collections.emptyMap();
    }

    /**
     * Get the error messages classified by specific error conditions.
     * 
     * @return classified error message map
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Map<String,Collection<String>> getClassifiedErrors() {
        return ImmutableMap.copyOf(classifiedMessages);
    }
    
    /**
     * Set the error messages indicating an unknown username.
     * 
     * @param messages the "unknown username" error messages to set
     */
    public void setClassifiedMessages(@Nonnull @NonnullElements final Map<String,Collection<String>> messages) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(messages, "Map of classified messages cannot be null");
        
        classifiedMessages = Maps.newHashMap();
        for (Map.Entry<String, Collection<String>> entry : messages.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty()
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                classifiedMessages.put(entry.getKey(),
                        ImmutableList.copyOf(Collections2.filter(entry.getValue(), Predicates.notNull())));
            }
        }
    }

    /**
     * Get predicate to apply to determine cacheability of {@link AuthenticationResult}.
     * 
     * @return predicate to apply, or null
     */
    @Nullable public Predicate<ProfileRequestContext> getResultCachingPredicate() {
        return resultCachingPredicate;
    }

    /**
     * Set predicate to apply to determine cacheability of {@link AuthenticationResult}.
     * 
     * @param predicate predicate to apply, or null
     */
    public void setResultCachingPredicate(@Nullable final Predicate<ProfileRequestContext> predicate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        resultCachingPredicate = predicate;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public <T extends Principal> Set<T> getSupportedPrincipals(
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
        
        principalsAdded = true;
        authenticatedSubject.getPrincipals().clear();
        authenticatedSubject.getPrincipals().addAll(Collections2.filter(principals, Predicates.notNull()));
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
                    PrincipalEvalPredicate predicate = factory.getPredicate(p);
                    if (predicate.apply(this)) {
                        log.debug("{} Compatible with principal type '{}' and operator '{}'", getLogPrefix(),
                                p.getClass(), rpCtx.getOperator());
                        rpCtx.setMatchingPrincipal(predicate.getMatchingPrincipal());
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
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
            return false;
        }
        
        if (clearErrorContext) {
            authenticationContext.removeSubcontext(AuthenticationErrorContext.class);
        }
        
        return true;
    }
    
    /**
     * Normally called upon successful completion of credential validation, calls the {@link #populateSubject(Subject)}
     * abstract method, stores an {@link AuthenticationResult} in the {@link AuthenticationContext}, and attaches a
     * {@link SubjectCanonicalizationContext} to the {@link ProfileRequestContext} in preparation for c14n to occur.
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * 
     * @throws AuthenticationException thrown if there is a problem performing the authentication action
     */
    protected void buildAuthenticationResult(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        
        if (!principalsAdded && authenticationContext.getAttemptedFlow() != null) {
            log.debug("{} Adding custom Principal(s) defined on underlying flow descriptor", getLogPrefix());
            authenticatedSubject.getPrincipals().addAll(
                    authenticationContext.getAttemptedFlow().getSupportedPrincipals());
        }
        
        AuthenticationResult result = new AuthenticationResult(authenticationContext.getAttemptedFlow().getId(),
                populateSubject(authenticatedSubject));
        authenticationContext.setAuthenticationResult(result);
        
        // Override cacheability if a predicate is installed.
        if (authenticationContext.isResultCacheable() && resultCachingPredicate != null) {
            authenticationContext.setResultCacheable(resultCachingPredicate.apply(profileRequestContext));
            log.info("{} Predicate indicates authentication result {} be cacheable in a session", getLogPrefix(),
                    authenticationContext.isResultCacheable() ? "will" : "will not");
        }
        
        // Transfer the subject to a new c14n context.
        profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, true).setSubject(result.getSubject());
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
     * @param eventId the event to "return" via an {@link org.opensaml.profile.context.EventContext}
     */
    protected void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull final Exception e,
            @Nonnull @NotEmpty final String eventId) {

        AuthenticationErrorContext errorCtx =
                authenticationContext.getSubcontext(AuthenticationErrorContext.class, true);
        errorCtx.addException(e);

        handleError(profileRequestContext, authenticationContext, e.getMessage(), eventId);
    }
    
    /**
     * Adds a message encountered during the action to an {@link AuthenticationErrorContext}, creating one if
     * necessary, beneath the {@link AuthenticationContext}, and uses the supplied event as the result of the action.
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @param message to process
     * @param eventId the event to "return" via an {@link org.opensaml.profile.context.EventContext}
     */
    protected void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull @NotEmpty final String message,
            @Nonnull @NotEmpty final String eventId) {
        
        AuthenticationErrorContext errorCtx =
                authenticationContext.getSubcontext(AuthenticationErrorContext.class, true);
        
        ActionSupport.buildEvent(profileRequestContext, eventId);

        MessageChecker checker = new MessageChecker(message);
        
        for (Map.Entry<String, Collection<String>> entry : classifiedMessages.entrySet()) {
            if (Iterables.any(entry.getValue(), checker)) {
                errorCtx.getClassifiedErrors().add(entry.getKey());
            }
        }
    }
    
    /**
     * Adds a message encountered during the action to an {@link AuthenticationWarningContext}, creating one if
     * necessary, beneath the {@link AuthenticationContext}, and uses the supplied event as the result of the action.
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @param message to process
     * @param eventId the event to "return" via an {@link org.opensaml.profile.context.EventContext}
     */
    protected void handleWarning(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull @NotEmpty final String message,
            @Nonnull @NotEmpty final String eventId) {
        
        AuthenticationWarningContext warningCtx =
                authenticationContext.getSubcontext(AuthenticationWarningContext.class, true);
        
        ActionSupport.buildEvent(profileRequestContext, eventId);

        MessageChecker checker = new MessageChecker(message);

        for (Map.Entry<String, Collection<String>> entry : classifiedMessages.entrySet()) {
            if (Iterables.any(entry.getValue(), checker)) {
                warningCtx.getClassifiedWarnings().add(entry.getKey());
            }
        }
    }
    
    /**
     * A predicate that examines a message to see if it contains a particular String.
     */
    private class MessageChecker implements Predicate<String> {

        /** Message to operate on. */
        @Nonnull @NotEmpty private final String s;
        
        /**
         * Constructor.
         *
         * @param msg to operate on
         */
        public MessageChecker(@Nonnull @NotEmpty final String msg) {
            Constraint.isNotNull(Strings.isNullOrEmpty(msg), "Message cannot be null or empty");
            s = msg;
        }
        
        /** {@inheritDoc} */
        public boolean apply(String input) {
            return s.contains(input);
        }
    }
    
}
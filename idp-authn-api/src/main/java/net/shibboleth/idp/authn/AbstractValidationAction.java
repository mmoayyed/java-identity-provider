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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.AuthenticationErrorContext;
import net.shibboleth.idp.authn.context.AuthenticationWarningContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.metrics.MetricsSupport;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * A base class for authentication related actions that validate credentials and produce an
 * {@link AuthenticationResult}.
 * 
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 * @event {@link AuthnEventIds#REQUEST_UNSUPPORTED}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 */
public abstract class AbstractValidationAction extends AbstractAuthenticationAction
            implements PrincipalSupportingComponent {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.validation"; 
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractValidationAction.class);
    
    /** Base name of metrics. */
    @Nonnull @NotEmpty private String metricName;
    
    /** Basis for {@link AuthenticationResult}. */
    @Nonnull private final Subject authenticatedSubject;
    
    /** Whether to inject the authentication flow's default custom principals into the subject. */
    private boolean addDefaultPrincipals;
    
    /** Indicates whether to clear any existing {@link AuthenticationErrorContext} before execution. */
    private boolean clearErrorContext;
    
    /** A cleanup hook to execute after successful validation. */
    @Nullable private Consumer<ProfileRequestContext> cleanupHook;
    
    /** Error messages associated with a specific error condition token. */
    @Nonnull @NonnullElements private Map<String,Collection<String>> classifiedMessages;
    
    /** Predicate to apply when setting AuthenticationResult cacheability. */
    @Nullable private Predicate<ProfileRequestContext> resultCachingPredicate;

    /** Function used to obtain the requester ID. */
    @Nullable private Function<ProfileRequestContext,String> requesterLookupStrategy;

    /** Function used to obtain the responder ID. */
    @Nullable private Function<ProfileRequestContext,String> responderLookupStrategy;
    
    /** Constructor. */
    public AbstractValidationAction() {
        addDefaultPrincipals = true;
        authenticatedSubject = new Subject();
        clearErrorContext = true;
        classifiedMessages = Collections.emptyMap();
        requesterLookupStrategy = new RelyingPartyIdLookupFunction();
        responderLookupStrategy = new ResponderIdLookupFunction();
        
        setMetricName(DEFAULT_METRIC_NAME);
    }
    
    /**
     * Get the base name to use for metrics reported.
     * 
     * @return root for name of metrics
     * 
     * @since 3.3.0
     */
    @Nonnull @NotEmpty public String getMetricName() {
        return metricName;
    }
    
    /**
     * Set the base name to use for metrics reported.
     * 
     * @param name root for name of metrics
     * 
     * @since 3.3.0
     */
    public void setMetricName(@Nonnull @NotEmpty final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metricName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Metric name cannot be null or empty");
    }
    
    /**
     * Get whether to inject the authentication flow's default custom principals into the subject.
     * 
     * <p>This is the default behavior, and works for static flows in which the principal set can
     * be statically determined from the flow.</p>
     * 
     * @return whether to inject the authentication flow's default custom principals into the subject
     */
    public boolean addDefaultPrincipals() {
        return addDefaultPrincipals;
    }
    
    /**
     * Set whether to inject the authentication flow's default custom principals into the subject.
     * 
     * @param flag flag to set
     */
    public void setAddDefaultPrincipals(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        addDefaultPrincipals = flag;
    }
    
    /**
     * Get the error messages classified by specific error conditions.
     * 
     * @return classified error message map
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Map<String,Collection<String>> getClassifiedErrors() {
        // For now this is using the older wrapper approach to guarding a live map to maintain the map insertion order.
        return Collections.unmodifiableMap(classifiedMessages);
    }
    
    /**
     * Set the error messages indicating an unknown username.
     * 
     * @param messages the "unknown username" error messages to set
     */
    public void setClassifiedMessages(@Nonnull @NonnullElements final Map<String,Collection<String>> messages) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(messages, "Map of classified messages cannot be null");
        
        classifiedMessages = new LinkedHashMap<>();
        for (final Map.Entry<String, Collection<String>> entry : messages.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty()
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                classifiedMessages.put(entry.getKey(), List.copyOf(entry.getValue()));
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
    
    /**
     * Get the cleanup hook to execute after successful validation.
     * 
     * @return cleanup hook
     * 
     * @since 4.1.0
     */
    @Nullable public Consumer<ProfileRequestContext> getCleanupHook() {
        return cleanupHook;
    }
    
    /**
     * Set the cleanup hook to execute after successful validation.
     * 
     * @param hook cleanup hook
     * 
     * @since 4.1.0
     */
    public void setCleanupHook(@Nullable final Consumer<ProfileRequestContext> hook) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        cleanupHook = hook;
    }
    
    /**
     * Get the strategy used to locate the requester ID for canonicalization.
     * 
     * @return lookup strategy
     * 
     * @since 4.0.0
     */
    @Nullable public Function<ProfileRequestContext,String> getRequesterLookupStrategy() {
        return requesterLookupStrategy;
    }

    /**
     * Set the strategy used to locate the requester ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setRequesterLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        requesterLookupStrategy = strategy;
    }
    
    /**
     * Get the strategy used to locate the responder ID for canonicalization.
     * 
     * @return lookup strategy
     * 
     * @since 4.0.0
     */
    @Nullable public Function<ProfileRequestContext,String> getResponderLookupStrategy() {
        return responderLookupStrategy;
    }

    /**
     * Set the strategy used to locate the responder ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setResponderLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responderLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @Unmodifiable @NotLive public <T extends Principal> Set<T> getSupportedPrincipals(
            @Nonnull final Class<T> c) {
        return getSubject().getPrincipals(c);
    }
    
    /**
     * Set supported non-user-specific principals that the action will include in the subjects
     * it generates, in place of any default principals from the flow.
     * 
     * <p>Setting to a null or empty collection will maintain the default behavior of relying on the flow.</p>
     * 
     * @param principals supported principals to include
     */
    public void setSupportedPrincipals(@Nullable @NonnullElements final Collection<Principal> principals) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        getSubject().getPrincipals().clear();
        
        if (principals != null && !principals.isEmpty()) {
            getSubject().getPrincipals().addAll(Set.copyOf(principals));
        }
    }
 
    /**
     * Get the subject to be produced by successful execution of this action.
     * 
     * @return  the subject meant as the result of this action
     */
    @Nonnull protected Subject getSubject() {
        return authenticatedSubject;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        } else if (authenticationContext.getAttemptedFlow() == null) {
            log.info("{} No attempted flow within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return false;
        }

        if (clearErrorContext) {
            authenticationContext.removeSubcontext(AuthenticationErrorContext.class);
        }        
        
        // If the request mandates particular principals, evaluate this validating component to see if it
        // can produce a matching principal. This skips validators chained together in flows that aren't
        // able to satisfy the request. This step only applies if the validator has been injected with
        // specific principals, otherwise the flow's capabilities have already been examined.
        final RequestedPrincipalContext rpCtx = authenticationContext.getSubcontext(RequestedPrincipalContext.class);
        if (rpCtx != null && rpCtx.getOperator() != null && !getSubject().getPrincipals().isEmpty()) {
            log.debug("{} Request contains principal requirements, evaluating for compatibility", getLogPrefix());
            for (final Principal p : rpCtx.getRequestedPrincipals()) {
                final PrincipalEvalPredicateFactory factory =
                        rpCtx.getPrincipalEvalPredicateFactoryRegistry().lookup(p.getClass(), rpCtx.getOperator());
                if (factory != null) {
                    final PrincipalEvalPredicate predicate = factory.getPredicate(p);
                    if (predicate.test(this)) {
                        log.debug("{} Compatible with principal type '{}' and operator '{}'", getLogPrefix(),
                                p.getClass(), rpCtx.getOperator());
                        rpCtx.setMatchingPrincipal(predicate.getMatchingPrincipal());
                        return true;
                    }
                    log.debug("{} Not compatible with principal type '{}' and operator '{}'", getLogPrefix(),
                            p.getClass(), rpCtx.getOperator());
                } else {
                    log.debug("{} No comparison logic registered for principal type '{}' and operator '{}'",
                            getLogPrefix(), p.getClass(), rpCtx.getOperator());
                }
            }
            
            log.info("{} Skipping validator, not compatible with request's principal requirements", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.REQUEST_UNSUPPORTED);
            return false;
        }
        
        if (authenticationContext.getFixedEventLookupStrategy() != null) {
            final String fixedEvent = authenticationContext.getFixedEventLookupStrategy().apply(profileRequestContext);
            if (fixedEvent != null) {
                log.info("{} Signaling fixed event: {}", getLogPrefix(), fixedEvent);
                ActionSupport.buildEvent(profileRequestContext, fixedEvent);
                return false;
            }
        }
    
        return true;
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * Normally called upon successful completion of credential validation, calls the {@link #populateSubject(Subject)}
     * abstract method, stores an {@link AuthenticationResult} in the {@link AuthenticationContext}, and attaches a
     * {@link SubjectCanonicalizationContext} to the {@link ProfileRequestContext} in preparation for c14n to occur.
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     */
    protected void buildAuthenticationResult(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (addDefaultPrincipals) {
            log.debug("{} Adding custom Principal(s) defined on underlying flow descriptor", getLogPrefix());
            getSubject().getPrincipals().addAll(authenticationContext.getAttemptedFlow().getSupportedPrincipals());
        }
        
        final AuthenticationResult result =
                authenticationContext.getAttemptedFlow().newAuthenticationResult(populateSubject(getSubject()));
        authenticationContext.setAuthenticationResult(result);
        
        // Override cacheability if a predicate is installed.
        if (authenticationContext.isResultCacheable() && resultCachingPredicate != null) {
            authenticationContext.setResultCacheable(resultCachingPredicate.test(profileRequestContext));
            log.info("{} Predicate indicates authentication result {} be cacheable in a session", getLogPrefix(),
                    authenticationContext.isResultCacheable() ? "will" : "will not");
        }
        
        // Transfer the subject to a new c14n context.
        final SubjectCanonicalizationContext c14n = new SubjectCanonicalizationContext();
        c14n.setSubject(result.getSubject());
        if (requesterLookupStrategy != null) {
            c14n.setRequesterId(requesterLookupStrategy.apply(profileRequestContext));
        }
        if (responderLookupStrategy != null) {
            c14n.setResponderId(responderLookupStrategy.apply(profileRequestContext));
        }
        authenticationContext.getParent().addSubcontext(c14n, true);
    }
    
    /**
     * Subclasses must override this method to complete the population of the {@link Subject} with
     * {@link Principal} and credential information based on the validation they perform.
     * 
     * <p>Typically this will include attaching a {@link net.shibboleth.idp.authn.principal.UsernamePrincipal},
     * but this is not a requirement if other components are suitably overridden.</p>
     * 
     * @param subject subject to populate
     * @return  the input subject
     */
    @Nonnull protected abstract Subject populateSubject(@Nonnull final Subject subject);
    
    /**
     * Record a successful authentication attempt against the configured counter. Records
     * nothing if the metrics registry is not installed into the runtime.
     * 
     * @since 3.3.0
     * 
     * @deprecated
     */
    @Deprecated(since="4.1.0", forRemoval=true)
    protected void recordSuccess() {
        if (MetricsSupport.getMetricRegistry() != null) {
            MetricsSupport.getMetricRegistry().counter(getMetricName() + ".successes").inc();
        }
    }
    
    /**
     * Record a failed authentication attempt against the configured counter. Records
     * nothing if the metrics registry is not installed into the runtime.
     * 
     * @since 3.3.0
     * 
     * @deprecated
     */
    @Deprecated(since="4.1.0", forRemoval=true)
    protected void recordFailure() {
        if (MetricsSupport.getMetricRegistry() != null) {
            MetricsSupport.getMetricRegistry().counter(getMetricName() + ".failures").inc();
        }
    }
    
    /**
     * Record a successful authentication attempt against the configured counter. Records
     * nothing if the metrics registry is not installed into the runtime.
     * 
     * @param profileRequestContext profile request context
     * 
     * @since 4.1.0
     */
    protected void recordSuccess(@Nonnull final ProfileRequestContext profileRequestContext) {
        recordSuccess();
        if (cleanupHook != null) {
            cleanupHook.accept(profileRequestContext);
        }
    }
    
    /**
     * Record a failed authentication attempt against the configured counter. Records
     * nothing if the metrics registry is not installed into the runtime.
     * 
     * @param profileRequestContext profile request context
     * 
     * @since 4.1.0
     */
    protected void recordFailure(@Nonnull final ProfileRequestContext profileRequestContext) {
        recordFailure();
    }
    
    /**
     * Adds an exception encountered during the action to an {@link AuthenticationErrorContext}, creating one if
     * necessary, beneath the {@link AuthenticationContext}.
     * 
     * <p>The exception message is evaluated as a potential match as a "classified" error and if matched,
     * the classification label is attached to the {@link AuthenticationErrorContext} and used as the
     * resulting event for the action.
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @param e the exception to process
     * @param eventId the event to "return" via an {@link org.opensaml.profile.context.EventContext} if
     *  the exception message is not classified
     */
    protected void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull final Exception e,
            @Nonnull @NotEmpty final String eventId) {

        authenticationContext.getSubcontext(AuthenticationErrorContext.class, true).getExceptions().add(e);

        handleError(profileRequestContext, authenticationContext, e.getMessage(), eventId);
    }
    
    /**
     * Evaluates a message as a potential match as a "classified" error and if matched, the classification
     * label is attached to an {@link AuthenticationErrorContext} and used as the resulting event for the action.
     * 
     * <p>If no match, the supplied eventId is used as the result.</p>
     * 
     * <p>If multiple matches, the first matching label is used as the result, but each match is added to the
     * context.</p>
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @param message to process
     * @param eventId the event to "return" via an {@link org.opensaml.profile.context.EventContext} if
     *  the message is not classified
     */
    protected void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
            @Nonnull @NotEmpty final String eventId) {

        boolean eventSet = false;

        if (!Strings.isNullOrEmpty(message)) {
            final MessageChecker checker = new MessageChecker(message);
            
            for (final Map.Entry<String, Collection<String>> entry : classifiedMessages.entrySet()) {
                if (Iterables.any(entry.getValue(), checker::test)) {
                    authenticationContext.getSubcontext(AuthenticationErrorContext.class,
                            true).getClassifiedErrors().add(entry.getKey());
                    if (!eventSet) {
                        eventSet = true;
                        ActionSupport.buildEvent(profileRequestContext, entry.getKey());
                    }
                }
            }
        }
        
        if (!eventSet) {
            ActionSupport.buildEvent(profileRequestContext, eventId);
        }
    }
    
    /**
     * Evaluates a message as a potential match as a "classified" warning and if matched, the classification
     * label is attached to an {@link AuthenticationWarningContext} and used as the resulting event for the action.
     * 
     * <p>If no match, the supplied eventId is used as the result.</p>
     * 
     * <p>If multiple matches, the first matching label is used as the result, but each match is added to the
     * context.</p>
     * 
     * @param profileRequestContext the current profile request context
     * @param authenticationContext the current authentication context
     * @param message to process
     * @param eventId the event to "return" via an {@link org.opensaml.profile.context.EventContext} if
     *  the message is not classified
     */
    protected void handleWarning(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
            @Nonnull @NotEmpty final String eventId) {
        
        boolean eventSet = false;
        
        if (!Strings.isNullOrEmpty(message)) {
            final MessageChecker checker = new MessageChecker(message);
            
            for (final Map.Entry<String, Collection<String>> entry : classifiedMessages.entrySet()) {
                if (Iterables.any(entry.getValue(), checker::test)) {
                    authenticationContext.getSubcontext(AuthenticationWarningContext.class,
                            true).getClassifiedWarnings().add(entry.getKey());
                    if (!eventSet) {
                        eventSet = true;
                        ActionSupport.buildEvent(profileRequestContext, entry.getKey());
                    }
                }
            }
        }
        
        if (!eventSet) {
            ActionSupport.buildEvent(profileRequestContext, eventId);
        }
    }
    
    /** A predicate that examines a message to see if it contains a particular String. */
    private class MessageChecker implements Predicate<String> {

        /** Message to operate on. */
        @Nonnull @NotEmpty private final String s;
        
        /**
         * Constructor.
         *
         * @param msg to operate on
         */
        public MessageChecker(@Nonnull @NotEmpty final String msg) {
            Constraint.isFalse(Strings.isNullOrEmpty(msg), "Message cannot be null or empty");
            s = msg;
        }
        
        /** {@inheritDoc} */
        public boolean test(final String input) {
            return s.contains(input);
        }
    }
    
}
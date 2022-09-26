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

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * An authentication action that completes MFA by producing a final {@link AuthenticationResult}
 * out of whatever constituent parts and pieces exist, by means of an overridable function,
 * storing it in the {@link AuthenticationContext} and preparing a fresh {@link SubjectCanonicalizationContext}
 * to operate on.
 * 
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getSubcontext(
 *      MultiFactorAuthenticationContext.class) != null</pre>
 * @post <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAuthenticationResult() != null</pre>
 * @post <pre>ProfileRequestContext.getSubcontext(SubjectCanonicalizationContext.class) != null</pre>
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 */
public class FinalizeMultiFactorAuthentication extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FinalizeMultiFactorAuthentication.class);

    /** Lookup function for the context to evaluate. */
    @Nonnull
    private Function<ProfileRequestContext,MultiFactorAuthenticationContext> multiFactorContextLookupStrategy;
    
    /** Strategy function to produce a final, merged result. */
    @NonnullAfterInit private Function<ProfileRequestContext,AuthenticationResult> resultMergingStrategy;
    
    /** Predicate to apply when setting AuthenticationResult cacheability. */
    @Nullable private Predicate<ProfileRequestContext> resultCachingPredicate;

    /** Function used to obtain the requester ID. */
    @Nullable private Function<ProfileRequestContext,String> requesterLookupStrategy;

    /** Function used to obtain the responder ID. */
    @Nullable private Function<ProfileRequestContext,String> responderLookupStrategy;

    /** A subordinate {@link MultiFactorAuthenticationContext}, if any. */
    @Nullable private MultiFactorAuthenticationContext mfaContext;

    /** Constructor. */
    public FinalizeMultiFactorAuthentication() {
        multiFactorContextLookupStrategy =
                new ChildContextLookup<>(MultiFactorAuthenticationContext.class).compose(
                        new ChildContextLookup<>(AuthenticationContext.class));
                
        requesterLookupStrategy = new RelyingPartyIdLookupFunction();
        responderLookupStrategy = new ResponderIdLookupFunction();
    }

    /**
     * Set the lookup strategy to use for the context to evaluate.
     * 
     * @param strategy lookup strategy
     */
    public void setMultiFactorContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,MultiFactorAuthenticationContext> strategy) {
        checkSetterPreconditions();
        multiFactorContextLookupStrategy = Constraint.isNotNull(strategy,
                "MultiFactorAuthenticationContext lookup strategy cannot be null");
    }
    
    /**
     * Set the result merging strategy to use.
     * 
     * @param strategy result merging strategy
     */
    public void setResultMergingStrategy(
            @Nullable final Function<ProfileRequestContext,AuthenticationResult> strategy) {
        checkSetterPreconditions();
        resultMergingStrategy = strategy;
    }

    /**
     * Set predicate to apply to determine cacheability of {@link AuthenticationResult}.
     * 
     * @param predicate predicate to apply, or null
     */
    public void setResultCachingPredicate(@Nullable final Predicate<ProfileRequestContext> predicate) {
        checkSetterPreconditions();
        resultCachingPredicate = predicate;
    }

    /**
     * Set the strategy used to locate the requester ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setRequesterLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        requesterLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate the responder ID for canonicalization.
     * 
     * @param strategy lookup strategy
     */
    public void setResponderLookupStrategy(
            @Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        responderLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (resultMergingStrategy == null) {
            resultMergingStrategy = new DefaultResultMergingStrategy();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        mfaContext = multiFactorContextLookupStrategy.apply(profileRequestContext);
        if (mfaContext == null) {
            log.error("{} No MultiFactorAuthenticationContext found by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        log.debug("{} MFA complete, producing merged result", getLogPrefix());
        final AuthenticationResult result = resultMergingStrategy.apply(profileRequestContext);
        if (result == null) {
            log.warn("{} Unable to produce merged AuthenticationResult", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return;
        }
        
        authenticationContext.setAuthenticationResult(result);
        
        final BiConsumer<ProfileRequestContext,Subject> decorator =
                authenticationContext.getAttemptedFlow().getSubjectDecorator();
        if (decorator != null) {
            decorator.accept(profileRequestContext, result.getSubject());
        }
        
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
        profileRequestContext.addSubcontext(c14n, true);
    }

    /**
     * Default merging strategy to combine individual {@link AuthenticationResult} objects into a
     * single result.
     * 
     * <p>The default strategy searches for a {@link MultiFactorAuthenticationContext} child of an
     * {@link AuthenticationContext} child of the input context, and combines all of the {@link Subject}
     * content from {@link MultiFactorAuthenticationContext#getActiveResults()} into a single result.</p>
     * 
     * <p>It assigns the flow ID based on {@link AuthenticationContext#getAttemptedFlow()}, and also preserves
     * the original result objects in wrapper principals within the new result.</p>
     */
    public static class DefaultResultMergingStrategy implements Function<ProfileRequestContext,AuthenticationResult> {

        /** {@inheritDoc} */
        @Nullable public AuthenticationResult apply(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final AuthenticationContext authnContext = input.getSubcontext(AuthenticationContext.class);
                if (authnContext != null) {
                    final MultiFactorAuthenticationContext mfaContext =
                            authnContext.getSubcontext(MultiFactorAuthenticationContext.class);
                    if (mfaContext != null) {
                        final Collection<AuthenticationResult> results = mfaContext.getActiveResults().values();
                        if (!results.isEmpty()) {
                            final Subject subject = new Subject();
                            for (final AuthenticationResult result : results) {
                                subject.getPrincipals().add(new AuthenticationResultPrincipal(result));
                                subject.getPrincipals().addAll(result.getSubject().getPrincipals());
                                subject.getPublicCredentials().addAll(result.getSubject().getPublicCredentials());
                                subject.getPrivateCredentials().addAll(result.getSubject().getPrivateCredentials());
                            }
                            final AuthenticationResult merged = new AuthenticationResult(
                                    mfaContext.getAuthenticationFlowDescriptor().getId(), subject);
                            return merged;
                        }
                    }
                }
            }
            
            return null;
        }
        
    }
    
}
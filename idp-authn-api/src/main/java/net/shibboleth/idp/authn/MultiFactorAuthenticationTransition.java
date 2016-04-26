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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * A ruleset for managing the transition out of an authentication factor during the multi-factor authn flow.
 * 
 * <p>After each factor is successfully completed, this object supplies rules for determining whether additional
 * factors are required, how to combine {@link Subject}s produced by different factors when a flow completes,
 * and what flow should execute next.</p>
 * 
 * @since 3.3.0
 */
public class MultiFactorAuthenticationTransition {

    /** Determines whether authentication has completed or not. */
    @Nonnull private Predicate<ProfileRequestContext> completionCondition;
        
    /** A function that produces a merged {@link AuthenticationResult}. */
    @Nonnull private Function<ProfileRequestContext,AuthenticationResult> resultMergingStrategy;

    /** A function that determines the next flow to execute. */
    @Nonnull private Function<ProfileRequestContext,String> nextFlowStrategy;
    
    /** Constructor. */
    public MultiFactorAuthenticationTransition() {
        completionCondition = new DefaultCompletionCondition();
        resultMergingStrategy = new DefaultResultMergingStrategy();
        setNextFlow(null);
    }
    
    /**
     * Get the condition to check to determine whether authentication is complete.
     * 
     * <p>The default condition simply evaluates whether the {@link AuthenticationResult} satisfies
     * the request.</p>
     * 
     * @return condition to check
     */
    @Nonnull public Predicate<ProfileRequestContext> getCompletionCondition() {
        return completionCondition;
    }
    
    /**
     * Set the condition to check to determine whether authentication is complete.
     * 
     * @param condition condition to check
     */
    public void setCompletionCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        completionCondition = Constraint.isNotNull(condition, "Completion condition cannot be null");
    }

    /**
     * Get the function to run to merge previous and freshly produced {@link AuthenticationResult} objects into
     * a single result.
     * 
     * <p>The default function simply merges the {@link Principal} collections together and labels it with the
     * flow ID of the most recent result.</p>
     * 
     * @return result merging function
     */
    @Nonnull public Function<ProfileRequestContext,AuthenticationResult> getResultMergingStrategy() {
        return resultMergingStrategy;
    }
    
    /**
     * Set the function to run to merge previous and freshly produced {@link AuthenticationResult} objects into
     * a single result.
     * 
     * @param strategy result merging function
     */
    public void setResultMergingStrategy(@Nonnull final Function<ProfileRequestContext,AuthenticationResult> strategy) {
        resultMergingStrategy = Constraint.isNotNull(strategy, "Result merging strategy cannot be null");
    }
    
    /**
     * Get the function to run to determine the next subflow to run.
     * 
     * <p>The flow ID must be fully-qualified; that is, for the typical case that a login subflow needs to be
     * run, the "authn/" prefix must be included.</p>
     * 
     * @return flow determination strategy
     */
    @Nonnull public Function<ProfileRequestContext,String> getNextFlowStrategy() {
        return nextFlowStrategy;
    }
    
    /**
     * Set the function to run to determine the next subflow to run.
     * 
     * @param strategy flow determination strategy
     */
    public void setNextFlowStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        nextFlowStrategy = Constraint.isNotNull(strategy, "Flow determination strategy cannot be null");
    }
    
    /**
     * Set the next flow to run instead of using a strategy function.
     * 
     * <p>The flow ID must be fully-qualified; that is, for the typical case that a login subflow needs to be
     * run, the "authn/" prefix must be included.</p>

     * @param flowId fully-qualified flow ID to run
     */
    public void setNextFlow(@Nullable @NotEmpty final String flowId) {
        nextFlowStrategy = FunctionSupport.constant(StringSupport.trimOrNull(flowId));
    }

    /**
     * Default condition to apply to determine whether MFA flow has completed.
     * 
     * <p>The default condition searches for a {@link MultiFactorAuthenticationContext} child of
     * an {@link AuthenticationContext} child of the input context, and evaluates the
     * {@link MultiFactorAuthenticationContext#getMergedAuthenticationResult()} property for suitability
     * to satisfy the request based on supported custom {@link Principal} objects.</p>
     */
    public class DefaultCompletionCondition implements Predicate<ProfileRequestContext> {

        /** {@inheritDoc} */
        public boolean apply(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final AuthenticationContext authnContext = input.getSubcontext(AuthenticationContext.class);
                if (authnContext != null) {
                    final MultiFactorAuthenticationContext mfaContext =
                            authnContext.getSubcontext(MultiFactorAuthenticationContext.class);
                    if (mfaContext != null) {
                        if (mfaContext.getMergedAuthenticationResult() != null) {
                            return authnContext.isAcceptable(mfaContext.getMergedAuthenticationResult());
                        }
                    }
                }
            }
            
            return false;
        }
    }
    
    /**
     * Default merging strategy to combine individual {@link AuthenticationResult} objects into a
     * single result.
     * 
     * <p>If only a single result is found, then it's returned directly.</p>
     * 
     * <p>When there are multiple, the default strategy searches for a {@link MultiFactorAuthenticationContext}
     * child of an {@link AuthenticationContext} child of the input context, and combines all of the {@link Subject}
     * content from {@link MultiFactorAuthenticationContext#getAuthenticationResults()} into a single result.</p>
     * 
     * <p>It assigns the flow ID based on {@link AuthenticationContext#getAttemptedFlow()}, and also preserves
     * the original result objects in wrapper principals within the new result.</p>
     */
    public class DefaultResultMergingStrategy implements Function<ProfileRequestContext,AuthenticationResult> {

        /** {@inheritDoc} */
        @Nullable public AuthenticationResult apply(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final AuthenticationContext authnContext = input.getSubcontext(AuthenticationContext.class);
                if (authnContext != null) {
                    final MultiFactorAuthenticationContext mfaContext =
                            authnContext.getSubcontext(MultiFactorAuthenticationContext.class);
                    if (mfaContext != null) {
                        final Collection<AuthenticationResult> results = mfaContext.getActiveResults().values();
                        if (results.size() == 1) {
                            return results.iterator().next();
                        } else if (results.size() > 1) {
                            final Subject subject = new Subject();
                            for (final AuthenticationResult result : results) {
                                subject.getPrincipals().add(new AuthenticationResultPrincipal(result));
                                subject.getPrincipals().addAll(result.getSubject().getPrincipals());
                                subject.getPublicCredentials().addAll(result.getSubject().getPublicCredentials());
                                subject.getPrivateCredentials().addAll(result.getSubject().getPrivateCredentials());
                            }
                            final AuthenticationResult merged =
                                    new AuthenticationResult(authnContext.getAttemptedFlow().getId(), subject);
                            return merged;
                        }
                    }
                }
            }
            
            return null;
        }
        
    }
}
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

package net.shibboleth.idp.saml.profile.config;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.AbstractInterceptorAwareProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/** Base class for SAML profile configurations. */
public abstract class AbstractSAMLProfileConfiguration extends AbstractInterceptorAwareProfileConfiguration implements
        SAMLProfileConfiguration {
    
    /** Default assertion lifetime. */
    @Nonnull public static final Duration DEFAULT_ASSERTION_LIFETIME = Duration.ofMinutes(5);
    
    /** Predicate used to determine if the generated request should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signRequestsPredicate;

    /** Predicate used to determine if the generated response should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signResponsesPredicate;

    /** Predicate used to determine if the generated assertion should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signAssertionsPredicate;

    /** Controls whether to include a NotBefore attribute in the Conditions of generated assertions. */
    @Nonnull private Predicate<ProfileRequestContext> includeNotBeforePredicate;

    /** Lookup function to supply assertionLifetime property. */
    @Nonnull private Function<ProfileRequestContext,Duration> assertionLifetimeLookupStrategy;

    /** Lookup function to supply assertionAudiences property. */
    @Nonnull private Function<ProfileRequestContext,Set<String>> assertionAudiencesLookupStrategy;

    /**
     * Constructor.
     * 
     * @param profileId ID of the communication profile
     */
    public AbstractSAMLProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);

        signRequestsPredicate = PredicateSupport.alwaysFalse();
        signResponsesPredicate = PredicateSupport.alwaysFalse();
        signAssertionsPredicate = PredicateSupport.alwaysFalse();
        includeNotBeforePredicate = PredicateSupport.alwaysTrue();
        assertionLifetimeLookupStrategy = FunctionSupport.constant(DEFAULT_ASSERTION_LIFETIME);
        assertionAudiencesLookupStrategy = FunctionSupport.constant(null);
    }
    
    /** {@inheritDoc} */
    public boolean isSignAssertions(@Nullable final ProfileRequestContext profileRequestContext) {
        return signAssertionsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether generated assertions should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignAssertions(final boolean flag) {
        signAssertionsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if generated assertions should be signed.
     * 
     * @param predicate predicate used to determine if generated assertions should be signed
     * 
     * @since 4.0.0
     */
    public void setSignAssertionsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signAssertionsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isSignRequests(@Nullable final ProfileRequestContext profileRequestContext) {
        return signRequestsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether generated requests should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignRequests(final boolean flag) {
        signRequestsPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if generated requests should be signed.
     * 
     * @param predicate predicate used to determine if generated requests should be signed
     * 
     * @since 4.0.0
     */
    public void setSignRequestsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signRequestsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isSignResponses(@Nullable final ProfileRequestContext profileRequestContext) {
        return signResponsesPredicate.test(profileRequestContext);
    }

    /**
     * Set whether generated responses should be signed.
     * 
     * @param flag flag to set
     */
    public void setSignResponses(final boolean flag) {
        signResponsesPredicate = PredicateSupport.constant(flag);
    }
    
    /**
     * Set the predicate used to determine if generated responses should be signed.
     * 
     * @param predicate predicate used to determine if generated responses should be signed
     * 
     * @since 4.0.0
     */
    public void setSignResponsesPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signResponsesPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull public Duration getAssertionLifetime(@Nullable final ProfileRequestContext profileRequestContext) {
        final Duration lifetime = assertionLifetimeLookupStrategy.apply(profileRequestContext);
        Constraint.isNotNull(lifetime, "Assertion lifetime cannot be null");
        Constraint.isFalse(lifetime.isNegative() || lifetime.isZero(), "Assertion lifetime must be greater than 0");
        return lifetime;
    }

    /**
     * Set the lifetime of an assertion.
     * 
     * @param lifetime lifetime of an assertion
     */
    public void setAssertionLifetime(@Nonnull final Duration lifetime) {
        Constraint.isNotNull(lifetime, "Assertion lifetime cannot be null");
        Constraint.isFalse(lifetime.isNegative() || lifetime.isZero(), "Assertion lifetime must be greater than 0");
        
        assertionLifetimeLookupStrategy = FunctionSupport.constant(lifetime);
    }

    /**
     * Set a lookup strategy for the lifetime of an assertion.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setAssertionLifetimeLookupStrategy(@Nonnull final Function<ProfileRequestContext,Duration> strategy) {
        assertionLifetimeLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**{@inheritDoc} */
    public boolean isIncludeConditionsNotBefore(@Nullable final ProfileRequestContext profileRequestContext) {
        return includeNotBeforePredicate.test(profileRequestContext);
    }

    /**
     * Set whether to include a NotBefore attribute in the Conditions of generated assertions.
     * 
     * @param flag flag to set
     */
    public void setIncludeConditionsNotBefore(final boolean flag) {
        includeNotBeforePredicate = PredicateSupport.constant(flag);
    }

    /**
     * Set a condition to determine whether to include a NotBefore attribute in the Conditions of
     * generated assertions.
     *
     * @param condition  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setIncludeConditionsNotBeforePredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        includeNotBeforePredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive public Set<String> getAdditionalAudiencesForAssertion(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final Set<String> audiences = assertionAudiencesLookupStrategy.apply(profileRequestContext);
        if (audiences != null) {
            return CollectionSupport.copyToSet(audiences);
        }
        return CollectionSupport.emptySet();
    }

    /**
     * Set the set of audiences, in addition to the relying party(ies) to which the IdP is issuing the assertion, with
     * which an assertion may be shared.
     * 
     * @param audiences the additional audiences
     */
    public void setAdditionalAudiencesForAssertion(@Nullable @NonnullElements final Collection<String> audiences) {

        if (audiences == null || audiences.isEmpty()) {
            assertionAudiencesLookupStrategy = FunctionSupport.constant(null);
        } else {
            assertionAudiencesLookupStrategy = FunctionSupport.constant(
                    Set.copyOf(StringSupport.normalizeStringCollection(audiences)));
        }
    }

    /**
     * Set a lookup strategy for the set of audiences, in addition to the relying party(ies) to which the IdP
     * is issuing the assertion, with which an assertion may be shared.
     *
     * @param strategy  lookup strategy
     * 
     * @since 4.0.0
     */
    public void setAdditionalAudiencesForAssertionLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Set<String>> strategy) {
        assertionAudiencesLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

}
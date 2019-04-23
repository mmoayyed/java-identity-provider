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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.AbstractConditionalProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

/** Base class for SAML profile configurations. */
public abstract class AbstractSAMLProfileConfiguration extends AbstractConditionalProfileConfiguration implements
        SAMLProfileConfiguration {
    
    /** Default assertion lifetime. */
    @Nonnull public static final Duration DEFAULT_ASSERTION_LIFETIME = Duration.ofMinutes(5);

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSAMLProfileConfiguration.class);
    
    /** Predicate used to determine if the generated request should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signRequestsPredicate;

    /** Predicate used to determine if the generated response should be signed. Default returns true. */
    @Nonnull private Predicate<ProfileRequestContext> signResponsesPredicate;

    /** Predicate used to determine if the generated assertion should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signAssertionsPredicate;

    /** Controls whether to include a NotBefore attribute in the Conditions of generated assertions. */
    @Nonnull private Predicate<ProfileRequestContext> includeNotBeforePredicate;

    /** Lookup function to supply assertionLifetime property. */
    @Nonnull private Function<ProfileRequestContext,Duration> assertionLifetimeLookupStrategy;

    /** Lookup function to supply assertionAudiences property. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> assertionAudiencesLookupStrategy;

    /**
     * Constructor.
     * 
     * @param profileId ID of the communication profile
     */
    public AbstractSAMLProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);

        signRequestsPredicate = Predicates.alwaysFalse();
        signResponsesPredicate = Predicates.alwaysFalse();
        signAssertionsPredicate = Predicates.alwaysFalse();
        includeNotBeforePredicate = Predicates.alwaysTrue();
        assertionLifetimeLookupStrategy = FunctionSupport.constant(DEFAULT_ASSERTION_LIFETIME);
        assertionAudiencesLookupStrategy = FunctionSupport.constant(null);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getInboundInterceptorFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final List<String> flows = super.getInboundInterceptorFlows(profileRequestContext);
        if (flows.isEmpty()) {
            log.warn("Inbound interceptor collection is empty, this disables default inbound message security checks");
        }
        return flows;
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
        signAssertionsPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
        signRequestsPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
        signResponsesPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
        includeNotBeforePredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
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
        return CollectionSupport.buildImmutableSet(assertionAudiencesLookupStrategy.apply(profileRequestContext));
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
            final Set<String> assertionAudiences = new HashSet<>();
            for (final String audience : audiences) {
                final String trimmedAudience = StringSupport.trimOrNull(audience);
                if (trimmedAudience != null) {
                    assertionAudiences.add(trimmedAudience);
                }
            }
            assertionAudiencesLookupStrategy = FunctionSupport.constant(assertionAudiences);
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
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        assertionAudiencesLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

}
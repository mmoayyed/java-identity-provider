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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

/** Base class for SAML profile configurations. */
public abstract class AbstractSAMLProfileConfiguration extends AbstractProfileConfiguration implements
        SAMLProfileConfiguration {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSAMLProfileConfiguration.class);
    
    /** Predicate used to determine if the generated request should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signRequestsPredicate;

    /** Predicate used to determine if the generated response should be signed. Default returns true. */
    @Nonnull private Predicate<ProfileRequestContext> signResponsesPredicate;

    /** Predicate used to determine if the generated assertion should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signAssertionsPredicate;

    /** Controls whether to include a NotBefore attribute in the Conditions of generated assertions. */
    @Nullable private Predicate<ProfileRequestContext> includeNotBeforePredicate;

    /** Lookup function to supply {@link #assertionLifetime} property. */
    @Nullable private Function<ProfileRequestContext,Long> assertionLifetimeLookupStrategy;

    /** Lifetime of an assertion in milliseconds. Default value: 5 minutes */
    @Positive @Duration private long assertionLifetime;

    /** Lookup function to supply {@link #assertionAudiences} property. */
    @Nullable private Function<ProfileRequestContext,Collection<String>> assertionAudiencesLookupStrategy;

    /** Additional audiences to which an assertion may be released. Default value: empty */
    @Nonnull @NonnullElements private Set<String> assertionAudiences;

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
        assertionLifetime = 5 * 60 * 1000;
        assertionAudiences = Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getInboundInterceptorFlows() {
        
        final List<String> flows = super.getInboundInterceptorFlows();
        if (flows.isEmpty()) {
            log.warn("Inbound interceptor collection is empty, this disables default inbound message security checks");
        }
        return flows;
    }
    
    /** {@inheritDoc} */
    @Override @Nonnull public Predicate<ProfileRequestContext> getSignAssertions() {
        return signAssertionsPredicate;
    }

    /**
     * Set the predicate used to determine if generated assertions should be signed.
     * 
     * @param predicate predicate used to determine if generated assertions should be signed
     */
    public void setSignAssertions(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signAssertionsPredicate =
                Constraint.isNotNull(predicate, "Predicate to determine if assertions should be signed cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Predicate<ProfileRequestContext> getSignRequests() {
        return signRequestsPredicate;
    }

    /**
     * Set the predicate used to determine if generated requests should be signed.
     * 
     * @param predicate predicate used to determine if generated requests should be signed
     */
    public void setSignRequests(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signRequestsPredicate =
                Constraint.isNotNull(predicate,
                        "Predicate to determine if requests should be signed cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Predicate<ProfileRequestContext> getSignResponses() {
        return signResponsesPredicate;
    }

    /**
     * Set the predicate used to determine if generated responses should be signed.
     * 
     * @param predicate predicate used to determine if generated responses should be signed
     */
    public void setSignResponses(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signResponsesPredicate =
                Constraint.isNotNull(predicate, "Predicate to determine if responses should be signed cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Positive @Duration public long getAssertionLifetime() {
        return Constraint.isGreaterThan(0, getIndirectProperty(assertionLifetimeLookupStrategy, assertionLifetime),
                "Assertion lifetime must be greater than 0");
    }

    /**
     * Set the lifetime of an assertion.
     * 
     * @param lifetime lifetime of an assertion in milliseconds
     */
    @Duration public void setAssertionLifetime(@Positive @Duration final long lifetime) {
        assertionLifetime = Constraint.isGreaterThan(0, lifetime, "Assertion lifetime must be greater than 0");
    }

    /**
     * Set a lookup strategy for the {@link #assertionLifetime} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setAssertionLifetimeLookupStrategy(@Nullable final Function<ProfileRequestContext,Long> strategy) {
        assertionLifetimeLookupStrategy = strategy;
    }

    /**{@inheritDoc} */
    @Override public boolean includeConditionsNotBefore() {
        return includeNotBeforePredicate.apply(getProfileRequestContext());
    }

    /**
     * Set whether to include a NotBefore attribute in the Conditions of generated assertions.
     * 
     * @param include whether to include a NotBefore attribute in the Conditions of generated assertions
     */
    public void setIncludeConditionsNotBefore(final boolean include) {
        includeNotBeforePredicate = include ? Predicates.<ProfileRequestContext>alwaysTrue()
                : Predicates.<ProfileRequestContext>alwaysFalse();
    }

    /**
     * Get a condition to determine whether to include a NotBefore attribute in the Conditions of
     * generated assertions.
     *
     * @return a condition to evaluate
     * 
     * @since 3.3.0
     */
    @Nonnull public Predicate<ProfileRequestContext> getIncludeConditionsNotBeforePredicate() {
        return includeNotBeforePredicate;
    }

    /**
     * Set a condition to determine whether to include a NotBefore attribute in the Conditions of
     * generated assertions.
     *
     * @param condition  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setIncludeConditionsNotBeforePredicate(@Nullable final Predicate<ProfileRequestContext> condition) {
        includeNotBeforePredicate = Constraint.isNotNull(condition, "NotBefore predicate cannot be null");
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements @NotLive public Set<String> getAdditionalAudiencesForAssertion() {
        return ImmutableSet.copyOf(getIndirectProperty(assertionAudiencesLookupStrategy, assertionAudiences));
    }

    /**
     * Set the set of audiences, in addition to the relying party(ies) to which the IdP is issuing the assertion, with
     * which an assertion may be shared.
     * 
     * @param audiences the additional audiences
     * 
     * @deprecated
     */
    @Deprecated
    public void setAdditionalAudienceForAssertion(@Nonnull @NonnullElements final Collection<String> audiences) {
        LoggerFactory.getLogger(AbstractSAMLProfileConfiguration.class).warn(
                "Use of deprecated property name 'additionalAudienceForAssertion', please correct to "
                    + "'additionalAudiencesForAssertion'");
        setAdditionalAudiencesForAssertion(audiences);
    }

    /**
     * Set the set of audiences, in addition to the relying party(ies) to which the IdP is issuing the assertion, with
     * which an assertion may be shared.
     * 
     * @param audiences the additional audiences
     */
    public void setAdditionalAudiencesForAssertion(@Nullable @NonnullElements final Collection<String> audiences) {

        if (audiences == null || audiences.isEmpty()) {
            assertionAudiences = Collections.emptySet();
        } else {
            assertionAudiences = new HashSet<>();
            for (final String audience : audiences) {
                final String trimmedAudience = StringSupport.trimOrNull(audience);
                if (trimmedAudience != null) {
                    assertionAudiences.add(trimmedAudience);
                }
            }
        }
    }

    /**
     * Set a lookup strategy for the {@link #assertionAudiences} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setAssertionAudiencesLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<String>> strategy) {
        assertionAudiencesLookupStrategy = strategy;
    }

}
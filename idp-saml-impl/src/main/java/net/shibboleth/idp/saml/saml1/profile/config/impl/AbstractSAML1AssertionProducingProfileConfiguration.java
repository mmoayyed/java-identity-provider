/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.saml1.profile.config.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.saml.profile.config.SAMLAssertionProducingProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/** Base class for IdP SAML 1.x profile configurations that produce assertions. */
public abstract class AbstractSAML1AssertionProducingProfileConfiguration
        extends AbstractSAML1ArtifactAwareProfileConfiguration
        implements SAMLAssertionProducingProfileConfiguration {

    /** Predicate used to determine whether to sign assertions. */
    @Nonnull private Predicate<ProfileRequestContext> signAssertionsPredicate;

    /**
     * Predicate used to determine whether to include a NotBefore attribute in the
     * Conditions of generated assertions.
     */
    @Nonnull private Predicate<ProfileRequestContext> includeNotBeforePredicate;
    
    /** Lookup function to supply assertionLifetime property. */
    @Nonnull private Function<ProfileRequestContext,Duration> assertionLifetimeLookupStrategy;

    /** Lookup function to supply assertionAudiences property. */
    @Nonnull private Function<ProfileRequestContext,Set<String>> additionalAudiencesLookupStrategy;    

    /**
     * Constructor.
     * 
     * @param profileId ID of the communication profile, never null or empty
     */
    public AbstractSAML1AssertionProducingProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);

        signAssertionsPredicate = PredicateSupport.alwaysFalse();
        includeNotBeforePredicate = PredicateSupport.alwaysTrue();
        assertionLifetimeLookupStrategy =
                FunctionSupport.constant(SAMLAssertionProducingProfileConfiguration.DEFAULT_ASSERTION_LIFETIME);
        additionalAudiencesLookupStrategy = FunctionSupport.constant(null);
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
     */
    public void setSignAssertionsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signAssertionsPredicate = Constraint.isNotNull(predicate, "Condition cannot be null");
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
     */
    public void setIncludeConditionsNotBeforePredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        includeNotBeforePredicate = Constraint.isNotNull(condition, "Condition cannot be null");
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
     */
    public void setAssertionLifetimeLookupStrategy(@Nonnull final Function<ProfileRequestContext,Duration> strategy) {
        assertionLifetimeLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Nonnull @Unmodifiable @NotLive public Set<String> getAssertionAudiences(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final Set<String> audiences = additionalAudiencesLookupStrategy.apply(profileRequestContext);
        if (audiences != null) {
            return CollectionSupport.copyToSet(audiences);
        }
        return CollectionSupport.emptySet();
    }

    /**
     * Deprecated, replacement is {@link #setAssertionAudiences(Collection)}.
     * 
     * @param audiences the additional audiences
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    public void setAdditionalAudiencesForAssertion(@Nullable final Collection<String> audiences) {
        DeprecationSupport.warn(ObjectType.METHOD, "setAdditionalAudiencesForAssertion", "relying-party.xml",
                "setAssertionAudiences");
        setAssertionAudiences(audiences);
    }

    /**
     * Deprecated, replacement is {@link #setAssertionAudiencesLookupStrategy(Function)}.
     * 
     * @param strategy lookup strategy
     * 
     * @deprecated
     */
    @Deprecated(since="5.0.0", forRemoval=true)
    public void setAdditionalAudiencesForAssertionLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Set<String>> strategy) {
        DeprecationSupport.warn(ObjectType.METHOD, "setAdditionalAudiencesForAssertionLookupStrategy",
                "relying-party.xml", "setAssertionAudiences");
        setAssertionAudiencesLookupStrategy(strategy);
    }
    
    /**
     * Set the set of audiences, in addition to the relying party(ies) to which the IdP is issuing the assertion, with
     * which an assertion may be shared.
     * 
     * @param audiences the additional audiences
     */
    public void setAssertionAudiences(@Nullable final Collection<String> audiences) {

        if (audiences == null || audiences.isEmpty()) {
            additionalAudiencesLookupStrategy = FunctionSupport.constant(null);
        } else {
            additionalAudiencesLookupStrategy = FunctionSupport.constant(
                    Set.copyOf(StringSupport.normalizeStringCollection(audiences)));
        }
    }

    /**
     * Set a lookup strategy for the set of audiences, in addition to the relying party(ies) to which the IdP
     * is issuing the assertion, with which an assertion may be shared.
     *
     * @param strategy  lookup strategy
     */
    public void setAssertionAudiencesLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Set<String>> strategy) {
        additionalAudiencesLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
}
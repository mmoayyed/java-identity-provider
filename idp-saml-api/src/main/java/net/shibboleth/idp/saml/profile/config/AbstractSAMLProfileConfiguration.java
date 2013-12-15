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
import java.util.Set;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

/** Base class for SAML profile configurations. */
public abstract class AbstractSAMLProfileConfiguration
        extends AbstractProfileConfiguration implements SAMLProfileConfiguration {

    /** Predicate used to determine if the received request should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signedRequestsPredicate;

    /** Predicate used to determine if the generated response should be signed. Default returns true. */
    @Nonnull private Predicate<ProfileRequestContext> signResponsesPredicate;

    /** Predicate used to determine if the generated assertion should be signed. Default returns false. */
    @Nonnull private Predicate<ProfileRequestContext> signAssertionsPredicate;

    /** Lifetime of an assertion in milliseconds. Default value: 5 minutes */
    @Positive @Duration private long assertionLifetime;

    /** Additional audiences to which an assertion may be released. Default value: empty */
    @Nonnull @NonnullElements private Set<String> assertionAudiences;

    /**
     * Constructor.
     * 
     * @param profileId ID of the the communication profile
     */
    public AbstractSAMLProfileConfiguration(@Nonnull @NotEmpty final String profileId) {
        super(profileId);
        
        signedRequestsPredicate = Predicates.alwaysFalse();
        signResponsesPredicate = Predicates.alwaysTrue();
        signAssertionsPredicate = Predicates.alwaysFalse();
        assertionLifetime = 5 * 60 * 1000;
        assertionAudiences = Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Nonnull public Predicate<ProfileRequestContext> getSignAssertionsPredicate() {
        return signAssertionsPredicate;
    }

    /**
     * Set the predicate used to determine if the generated assertion should be signed.
     * 
     * @param predicate predicate used to determine if the generated assertion should be signed
     */
    public void setSignAssertionsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signAssertionsPredicate =
                Constraint.isNotNull(predicate, "Predicate to determine if assertions should be signed cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull public Predicate<ProfileRequestContext> getSignedRequestsPredicate() {
        return signedRequestsPredicate;
    }

    /**
     * Set the predicate used to determine if the received request should be signed.
     * 
     * @param predicate predicate used to determine if the received request should be signed
     */
    public void setSignedRequestsPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signedRequestsPredicate =
                Constraint.isNotNull(predicate,
                        "Predicate to determine if received requests should be signed cannot be null");
    }

    /** {@inheritDoc} */
    public Predicate<ProfileRequestContext> getSignResponsesPredicate() {
        return signResponsesPredicate;
    }

    /**
     * Set the predicate used to determine if the generated response should be signed.
     * 
     * @param predicate predicate used to determine if the generated response should be signed
     */
    public void setSignResponsesPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        signResponsesPredicate =
                Constraint.isNotNull(predicate, "Predicate to determine if responses should be signed cannot be null");
    }

    /** {@inheritDoc} */
    @Positive public long getAssertionLifetime() {
        return assertionLifetime;
    }

    /**
     * Set the lifetime of an assertion.
     * 
     * @param lifetime lifetime of an assertion in milliseconds
     */
    public void setAssertionLifetime(@Positive @Duration final long lifetime) {
        assertionLifetime = Constraint.isGreaterThan(0, lifetime, "Assertion lifetime must be greater than 0");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive public Set<String> getAdditionalAudiencesForAssertion() {
        return ImmutableSet.copyOf(assertionAudiences);
    }

    /**
     * Set the set of audiences, in addition to the relying party(ies) to which the IdP is issuing the assertion, with
     * which an assertion may be shared.
     * 
     * @param audiences the additional audiences
     */
    public void setAdditionalAudienceForAssertion(@Nonnull @NonnullElements final Collection<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            assertionAudiences = Collections.emptySet();
            return;
        }

        final HashSet<String> newAudiences = new HashSet<>();
        String trimmedAudience;
        for (String audience : audiences) {
            trimmedAudience = StringSupport.trimOrNull(audience);
            if (trimmedAudience != null) {
                newAudiences.add(trimmedAudience);
            }
        }
    }
    
}
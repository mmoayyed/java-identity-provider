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

package net.shibboleth.idp.saml.relyingparty;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.AbstractProfileConfiguration;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.StaticResponseEvaluableCriterion;

//TODO fix name so that 'SAML' is 'Saml'

/** Base class for SAML profile configurations. */
public abstract class AbstractSAMLProfileConfiguration extends AbstractProfileConfiguration {

    /** Criteria used to determine if the received assertion should be signed. Default criteria always returns false. */
    private EvaluableCriterion<ProfileRequestContext> signedRequestsCriteria;

    /** Criteria used to determine if the generated response should be signed. Default criteria always returns true. */
    private EvaluableCriterion<ProfileRequestContext> signResponsesCriteria;

    /** Criteria used to determine if the generated assertion should be signed. Default criteria always returns false. */
    private EvaluableCriterion<ProfileRequestContext> signAssertionsCriteria;

    /** Lifetime of an assertion in milliseconds. Default value: 5 minutes */
    private long assertionLifetime;

    /** Additional audiences to which an assertion may be released. Default value: empty */
    private Set<String> assertionAudiences;

    /**
     * Constructor.
     * 
     * @param profileId ID of the the communication profile, never null or empty
     */
    public AbstractSAMLProfileConfiguration(String profileId) {
        super(profileId);
        signedRequestsCriteria = StaticResponseEvaluableCriterion.FALSE_RESPONSE;
        signResponsesCriteria = StaticResponseEvaluableCriterion.TRUE_RESPONSE;
        signAssertionsCriteria = StaticResponseEvaluableCriterion.FALSE_RESPONSE;
        assertionLifetime = 5 * 60 * 1000;
        assertionAudiences = Collections.emptySet();
    }

    /**
     * Gets the criteria used to determine if the generated assertion should be signed.
     * 
     * @return criteria used to determine if the generated assertion should be signed, never null
     */
    public EvaluableCriterion<ProfileRequestContext> getSignAssertionsCriteria() {
        return signAssertionsCriteria;
    }

    /**
     * Sets the criteria used to determine if the generated assertion should be signed.
     * 
     * @param criteria criteria used to determine if the generated assertion should be signed, never null
     */
    public void setSignAssertionsCriteria(EvaluableCriterion<ProfileRequestContext> criteria) {
        signAssertionsCriteria =
                Assert.isNotNull(criteria, "Criteria to determine if assertions should be signed can not be null");
    }

    /**
     * Gets the criteria used to determine if the received assertion should be signed.
     * 
     * @return criteria used to determine if the received assertion should be signed, never null
     */
    public EvaluableCriterion<ProfileRequestContext> getSignedRequestsCriteria() {
        return signedRequestsCriteria;
    }

    /**
     * Sets the criteria used to determine if the received assertion should be signed.
     * 
     * @param criteria criteria used to determine if the received assertion should be signed, never null
     */
    public void setSignedRequestsCriteria(EvaluableCriterion<ProfileRequestContext> criteria) {
        signedRequestsCriteria =
                Assert.isNotNull(criteria,
                        "Criteria to determine if received requests should be signed can not be null");
    }

    /**
     * Gets the criteria used to determine if the generated response should be signed.
     * 
     * @return criteria used to determine if the generated response should be signed, never null
     */
    public EvaluableCriterion<ProfileRequestContext> getSignResponsesCriteria() {
        return signResponsesCriteria;
    }

    /**
     * Sets the criteria used to determine if the generated response should be signed.
     * 
     * @param criteria criteria used to determine if the generated response should be signed, never null
     */
    public void setSignResponsesCriteria(EvaluableCriterion<ProfileRequestContext> criteria) {
        signResponsesCriteria =
                Assert.isNotNull(criteria, "Criteria to determine if responses should be signed can not be null");
    }

    /**
     * Gets the lifetime of an assertion in milliseconds.
     * 
     * @return lifetime of an assertion in milliseconds, always positive
     */
    public long getAssertionLifetime() {
        return assertionLifetime;
    }

    /**
     * Sets the lifetime of an assertion.
     * 
     * @param lifetime lifetime of an assertion in milliseconds, must be greater than 0
     */
    public void setAssertionLifetime(final long lifetime) {
        assertionLifetime = Assert.isGreaterThan(0, lifetime, "Assertion lifetime must be greater than 0");
    }

    /**
     * Gets an unmodifiable set of audiences, in addition to the relying party(ies) to which the IdP is issuing the
     * assertion, with which an assertion may be shared.
     * 
     * @return additional audiences to which an assertion may be shared
     */
    public Set<String> getAdditionalAudiencesForAssertion() {
        return assertionAudiences;
    }

    /**
     * Sets the set of audiences, in addition to the relying party(ies) to which the IdP is issuing the assertion, with
     * which an assertion may be shared.
     * 
     * @param audiences the additional audiences
     */
    public void setAdditionalAudienceForAssertion(final Collection<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            assertionAudiences = Collections.emptySet();
            return;
        }

        final HashSet<String> newAudiences = new HashSet<String>();
        String trimmedAudience;
        for (String audience : audiences) {
            trimmedAudience = StringSupport.trimOrNull(audience);
            if (trimmedAudience != null) {
                newAudiences.add(trimmedAudience);
            }
        }

        if (newAudiences == null || newAudiences.isEmpty()) {
            assertionAudiences = Collections.emptySet();
        } else {
            assertionAudiences = Collections.unmodifiableSet(newAudiences);
        }
    }
}
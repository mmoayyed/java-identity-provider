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

package net.shibboleth.idp.saml.relyingparty.idwsf;

import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.relyingparty.saml2.SsoProfileConfiguration;

import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.util.Assert;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.StaticResponseEvaluableCriterion;

/** Configuration for constrained Liberty IDWSF SSOS requests. */
public class SsosProfileConfiguration extends SsoProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/liberty/ssos";

    /** Maximum number of times a given token is allowed to have been delegated. Default value: 0 */
    private int maximumTokenDelegationChainLength;

    /** Criterion used to determine if a token may be delegated to a relying party. */
    private EvaluableCriterion<ProfileRequestContext<RequestAbstractType, Response>> delegationCriterion;

    /** Constructor. */
    public SsosProfileConfiguration() {
        this(PROFILE_ID);
        delegationCriterion = StaticResponseEvaluableCriterion.FALSE_RESPONSE;
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected SsosProfileConfiguration(final String profileId) {
        super(profileId);
        maximumTokenDelegationChainLength = 0;
    }

    /**
     * Get the maximum number of times a given token is allowed to have been delegated.
     * 
     * @return maximum number of times a given token is allowed to have been delegated
     */
    public int getMaximumTokenDelegationChainLength() {
        return maximumTokenDelegationChainLength;
    }

    /**
     * Set the maximum number of times a given token is allowed to have been delegated.
     * 
     * @param length maximum number of times a given token is allowed to have been delegated
     */
    public void setMaximumTokenDelegationChainLength(final int length) {
        maximumTokenDelegationChainLength = length;
    }

    /**
     * Gets criterion used to determine if a token may be delegated to a relying party.
     * 
     * @return criterion used to determine if a token may be delegated to a relying party, never null
     */
    public EvaluableCriterion<ProfileRequestContext<RequestAbstractType, Response>> getDelegationCriterion() {
        return delegationCriterion;
    }

    /**
     * Sets the criterion used to determine if a token may be delegated to a relying party.
     * 
     * @param criterion criterion used to determine if a token may be delegated to a relying party, never null
     */
    public void setDelegationCriterion(
            final EvaluableCriterion<ProfileRequestContext<RequestAbstractType, Response>> criterion) {
        delegationCriterion = Assert.isNotNull(criterion, "Delegation criterion can not be null");;
    }
}
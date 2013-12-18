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

package net.shibboleth.idp.saml.profile.config.idwsf;

import javax.annotation.Nonnull;

import net.shibboleth.idp.saml.profile.config.saml2.BrowserSSOProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.Response;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/** Configuration support for the Liberty ID-WSF SSOS profile. */
public class SSOSProfileConfiguration extends BrowserSSOProfileConfiguration {

    /** ID for this profile configuration. */
    @Nonnull @NotEmpty public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/liberty/ssos";

    /** Maximum number of times a given token is allowed to have been delegated. Default value: 0 */
    @NonNegative private long maximumTokenDelegationChainLength;

    /** Predicate used to determine if a token may be delegated to a relying party. */
    @Nonnull private Predicate<ProfileRequestContext<RequestAbstractType, Response>> delegationPredicate;

    /** Constructor. */
    public SSOSProfileConfiguration() {
        this(PROFILE_ID);
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected SSOSProfileConfiguration(final String profileId) {
        super(profileId);
        
        maximumTokenDelegationChainLength = 0;
        delegationPredicate = Predicates.alwaysFalse();
    }

    /**
     * Get the maximum number of times a given token is allowed to have been delegated.
     * 
     * @return maximum number of times a given token is allowed to have been delegated
     */
    public long getMaximumTokenDelegationChainLength() {
        return maximumTokenDelegationChainLength;
    }

    /**
     * Set the maximum number of times a given token is allowed to have been delegated.
     * 
     * @param length maximum number of times a given token is allowed to have been delegated
     */
    public void setMaximumTokenDelegationChainLength(final long length) {
        maximumTokenDelegationChainLength = Constraint.isGreaterThanOrEqual(0, length,
                "Delegation chain length must be greater than or equal to 0");
    }

    /**
     * Gets predicate used to determine if a token may be delegated to a relying party.
     * 
     * @return predicate used to determine if a token may be delegated to a relying party
     */
    @Nonnull public Predicate<ProfileRequestContext<RequestAbstractType, Response>> getDelegationPredicate() {
        return delegationPredicate;
    }

    /**
     * Sets the predicate used to determine if a token may be delegated to a relying party.
     * 
     * @param predicate predicate used to determine if a token may be delegated to a relying party
     */
    public void setDelegationPredicate(
            @Nonnull final Predicate<ProfileRequestContext<RequestAbstractType, Response>> predicate) {
        delegationPredicate = Constraint.isNotNull(predicate, "Delegation predicate cannot be null");
    }
    
}
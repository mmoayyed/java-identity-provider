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

package net.shibboleth.idp.saml.saml2.profile.delegation;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicate;

/**
 * A predicate which determines whether issuance of a delegated 
 * SAML 2 {@link org.opensaml.saml.saml2.core.Assertion} is active.
 */
public class IssuingDelegatedAssertionPredicate implements Predicate<ProfileRequestContext> {

    /** {@inheritDoc} */
    public boolean apply(@Nullable ProfileRequestContext input) {
        if (input == null) {
            return false;
        }
        DelegationContext delegationContext = input.getSubcontext(DelegationContext.class);
        if (delegationContext == null) {
            return false;
        }
        return delegationContext.isIssuingDelegatedAssertion();
    }

}

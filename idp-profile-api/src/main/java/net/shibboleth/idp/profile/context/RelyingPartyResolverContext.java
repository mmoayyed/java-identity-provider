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

package net.shibboleth.idp.profile.context;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * A {@link BaseContext} which holds working instructions for the {@link RelyingPartyConfigurationResolver}
 * to use in lieu of fixing it to take pluggable criteria.
 * 
 * @since 4.2.0
 */
public final class RelyingPartyResolverContext extends BaseContext {

    /** How to determine verified status. */
    @Nullable private Predicate<ProfileRequestContext> verificationCondition;

    /**
     * Get the condition to apply to determine whether the relying party is verified.
     * 
     * @return condition
     */
    @Nullable public Predicate<ProfileRequestContext> getVerificationPredicate() {
        return verificationCondition;
    }

    /**
     * Set the condition to apply to determine whether the relying party is verified.
     * 
     * @param condition condition
     * 
     * @return this context
     */
    @Nonnull public RelyingPartyResolverContext setVerificationPredicate(
            @Nullable final Predicate<ProfileRequestContext> condition) {
        verificationCondition = condition;
        
        return this;
    }
    
}
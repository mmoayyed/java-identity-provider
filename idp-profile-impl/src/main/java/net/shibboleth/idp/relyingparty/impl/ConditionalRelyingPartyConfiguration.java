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

package net.shibboleth.idp.relyingparty.impl;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.Predicate;

/**
 * A {@link RelyingPartyConfiguration} that contains a {@link Predicate} that indicates whether it should be active for
 * a given {@link ProfileRequestContext}.
 */
public class ConditionalRelyingPartyConfiguration extends RelyingPartyConfiguration {

    /** Predicate that must be true for this configuration to be active for a given request. */
    @Nonnull private final Predicate<ProfileRequestContext> activationCondition;

    /**
     * Constructor.
     * 
     * @param configurationId unique ID for this configuration
     * @param responderId the ID by which the responder is known by this relying party
     * @param configurations communication profile configurations for this relying party
     * @param condition criteria that must be met in order for this relying party configuration to apply to a given
     *            profile request
     */
    public ConditionalRelyingPartyConfiguration(@Nonnull @NotEmpty final String configurationId,
            @Nonnull @NotEmpty final String responderId,
            @Nonnull @NonnullElements final Collection<? extends ProfileConfiguration> configurations,
            @Nonnull final Predicate<ProfileRequestContext> condition) {
        super(configurationId, responderId, configurations);

        activationCondition = Constraint.isNotNull(condition,
                "Relying partying configuration activation condition cannot be null");
    }

    /**
     * Get the predicate that must be met for this configuration to be active for a given request.
     * 
     * @return criteria that must be met for this configuration to be active for a given request
     */
    @Nonnull public Predicate<ProfileRequestContext> getActivationCondition() {
        return activationCondition;
    }
}
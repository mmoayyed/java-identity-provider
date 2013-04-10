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
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.Predicate;

/**
 * A {@link RelyingPartyConfiguration} that contains a {@link Predicate} that indicates whether it should be active for
 * a given {@link ProfileRequestContext}.
 */
public class ActivatedRelyingPartyConfiguration extends RelyingPartyConfiguration {

    /** Criterion that must be met for this configuration to be active for a given request. */
    private final Predicate<ProfileRequestContext> activationCriteria;

    /**
     * Constructor.
     * 
     * @param configurationId unique ID for this configuration
     * @param responderId the ID by which the responder is known by this relying party
     * @param configurations communication profile configurations for this relying party
     * @param criteria criteria that must be met in order for this relying party configuration to apply to a given
     *            profile request
     */
    public ActivatedRelyingPartyConfiguration(@Nonnull @NotEmpty final String configurationId,
            @Nonnull @NotEmpty final String responderId,
            @Nullable @NullableElements final Collection<? extends ProfileConfiguration> configurations,
            @Nonnull final Predicate<ProfileRequestContext> criteria) {
        super(configurationId, responderId, configurations);

        activationCriteria = Constraint.isNotNull(criteria, "Relying partying configuration criteria can not be null");
    }

    /**
     * Gets the criteria that must be met for this configuration to be active for a given request.
     * 
     * @return criteria that must be met for this configuration to be active for a given request, never null
     */
    @Nonnull public Predicate<ProfileRequestContext> getActivationCriteria() {
        return activationCriteria;
    }
}

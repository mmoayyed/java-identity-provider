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

package net.shibboleth.idp.relyingparty;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.collect.ImmutableMap.Builder;

/** The configuration that applies to given relying party. */
public class RelyingPartyConfiguration {

    /** Unique identifier for this configuration. */
    private final String id;

    /** The entity ID of the IdP. */
    private final String responderEntityId;

    /** Registered and usable communication profile configurations for this relying party. */
    private final Map<String, ProfileConfiguration> profileConfigurations;

    /**
     * Constructor.
     * 
     * @param configurationId unique ID for this configuration
     * @param responderId the ID by which the responder is known by this relying party
     * @param configurations communication profile configurations for this relying party
     */
    public RelyingPartyConfiguration(@Nonnull @NotEmpty final String configurationId,
            @Nonnull @NotEmpty final String responderId,
            @Nullable @NullableElements final Collection<? extends ProfileConfiguration> configurations) {
        id =
                Constraint.isNotNull(StringSupport.trimOrNull(configurationId),
                        "Relying party configuration ID can not be null or empty");

        responderEntityId =
                Constraint.isNotNull(StringSupport.trimOrNull(responderId), "Responder entity ID can not be null");

        if (configurations == null || configurations.isEmpty()) {
            profileConfigurations = Collections.emptyMap();
            return;
        }

        Builder<String, ProfileConfiguration> configBuilder = new Builder<String, ProfileConfiguration>();
        for (ProfileConfiguration config : configurations) {
            if (config != null) {
                final String trimmedId =
                        Constraint
                                .isNotNull(StringSupport.trimOrNull(config.getProfileId()),
                                        "ID of profile configuration class " + config.getClass().getName()
                                                + " can not be null");
                configBuilder.put(trimmedId, config);
            }
        }
        profileConfigurations = configBuilder.build();
    }

    /**
     * Gets the unique ID of this configuration.
     * 
     * @return unique ID of this configuration, never null or empty
     */
    @Nonnull @NotEmpty public String getConfigurationId() {
        return id;
    }

    /**
     * Gets the ID of the entity responding to requests.
     * 
     * @return ID of the entity responding to requests
     */
    @Nonnull @NotEmpty public String getResponderEntityId() {
        return responderEntityId;
    }

    /**
     * Gets the unmodifiable set of profile configurations for this relying party.
     * 
     * @return unmodifiable set of profile configurations for this relying party, never null
     */
    @Nonnull @NonnullElements @Unmodifiable public Map<String, ProfileConfiguration> getProfileConfigurations() {
        return profileConfigurations;
    }

    /**
     * Gets the profile configuration, for the relying party, for the given profile. This is a convenience method and is
     * equivalent to calling {@link Map#get(Object)} on the return of {@link #getProfileConfigurations()}. This map
     * contains no null entries, keys, or values.
     * 
     * @param profileId the ID of the profile
     * 
     * @return the configuration for the profile or null if the profile ID was null or empty or there is no
     *         configuration for the given profile
     */
    @Nullable public ProfileConfiguration getProfileConfiguration(@Nullable final String profileId) {
        final String trimmedId = StringSupport.trimOrNull(profileId);
        if (trimmedId == null) {
            return null;
        }

        return profileConfigurations.get(trimmedId);
    }
}
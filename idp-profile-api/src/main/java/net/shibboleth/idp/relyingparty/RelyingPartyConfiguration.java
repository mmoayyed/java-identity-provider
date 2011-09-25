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
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.criteria.EvaluableCriterion;

/** The configuration that applies to given relying party. */
public class RelyingPartyConfiguration {

    /** Unique identifier for this configuration. */
    private final String id;

    /** Criterion that must be met for this configuration to be active for a given request. */
    private final EvaluableCriterion<ProfileRequestContext> activationCriteria;

    /** Registered and usable communication profile configurations for this relying party. */
    private final Map<String, ProfileConfiguration> profileConfigurations;

    /**
     * Constructor.
     * 
     * @param configurationId unique ID for this configuration
     * @param criteria criteria that must be met in order for this relying party configuration to apply to a given
     *            profile request, never null
     * @param configurations communication profile configurations for this relying party, may be null or empty
     */
    public RelyingPartyConfiguration(final String configurationId,
            final EvaluableCriterion<ProfileRequestContext> criteria,
            final Collection<ProfileConfiguration> configurations) {
        id =
                Assert.isNotNull(StringSupport.trimOrNull(configurationId),
                        "Relying party configuration ID can not be null or empty");

        activationCriteria = Assert.isNotNull(criteria, "Relying partying configuration criteria can not be null");;

        if (configurations == null || configurations.isEmpty()) {
            profileConfigurations = Collections.emptyMap();
            return;
        }

        final HashMap<String, ProfileConfiguration> configMap = new HashMap<String, ProfileConfiguration>();
        for (ProfileConfiguration config : configurations) {
            if (config != null) {
                final String trimmedId =
                        Assert.isNotNull(StringSupport.trimOrNull(config.getProfileId()), "ID of profile class "
                                + config.getClass().getName() + " can not be null");
                configMap.put(trimmedId, config);
            }
        }

        if (configMap.size() == 0) {
            profileConfigurations = Collections.emptyMap();
        } else {
            profileConfigurations = Collections.unmodifiableMap(configMap);
        }
    }

    /**
     * Gets the unique ID of this configuration.
     * 
     * @return unique ID of this configuration, never null or empty
     */
    public String getConfigurationId() {
        return id;
    }

    /**
     * Gets the criteria that must be met for this configuration to be active for a given request.
     * 
     * @return criteria that must be met for this configuration to be active for a given request, never null
     */
    public EvaluableCriterion<ProfileRequestContext> getActivationCriteria() {
        return activationCriteria;
    }

    /**
     * Gets the unmodifiable set of profile configurations for this relying party.
     * 
     * @return unmodifiable set of profile configurations for this relying party, never null
     */
    public Map<String, ProfileConfiguration> getProfileConfigurations() {
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
    public ProfileConfiguration getProfileConfiguration(String profileId) {
        final String trimmedId = StringSupport.trimOrNull(profileId);
        if (trimmedId == null) {
            return null;
        }

        return profileConfigurations.get(trimmedId);
    }
}
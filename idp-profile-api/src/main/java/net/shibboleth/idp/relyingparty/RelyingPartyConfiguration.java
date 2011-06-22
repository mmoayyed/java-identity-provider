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
import org.opensaml.xml.security.EvaluableCriteria;

/** The configuration that applies to given relying party. */
public class RelyingPartyConfiguration {

    /** Criteria that must be met in order for this relying party configuration to apply to a given profile request. */
    private final EvaluableCriteria<ProfileRequestContext> requirementCriteria;

    /** Registered and usable communication profile configurations for this relying party. */
    private final Map<String, ProfileConfiguration> profileConfigurations;

    /**
     * Constructor.
     * 
     * @param criteria criteria that must be met in order for this relying party configuration to apply to a given
     *            profile request, never null
     * @param configurations communication profile configurations for this relying party, may be null or empty
     */
    public RelyingPartyConfiguration(final EvaluableCriteria<ProfileRequestContext> criteria,
            final Collection<ProfileConfiguration> configurations) {
        Assert.isNotNull(criteria, "Relying partying configuration criteria can not be null");
        requirementCriteria = criteria;

        if (configurations == null || configurations.isEmpty()) {
            profileConfigurations = Collections.emptyMap();
            return;
        }

        String trimmedProfileId;
        final HashMap<String, ProfileConfiguration> configMap = new HashMap<String, ProfileConfiguration>();
        for (ProfileConfiguration config : configurations) {
            if (config != null) {
                trimmedProfileId = StringSupport.trimOrNull(config.getProfileId());
                Assert.isNotNull(trimmedProfileId, "ID of profile class " + config.getClass().getName()
                        + " can not be null");
                configMap.put(trimmedProfileId, config);
            }
        }

        if (configMap.size() == 0) {
            profileConfigurations = Collections.emptyMap();
        } else {
            profileConfigurations = Collections.unmodifiableMap(configMap);
        }
    }

    /**
     * Gets the criteria that must be met in order for this relying party configuration to apply to a given profile
     * request.
     * 
     * @return criteria that must be met in order for this relying party configuration to apply to a given profile
     *         request
     */
    public EvaluableCriteria<ProfileRequestContext> getRequirementCriteria() {
        return requirementCriteria;
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
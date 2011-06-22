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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.xml.security.Resolver;
import org.opensaml.xml.security.SecurityException;

/**
 * Retrieves a per-relying party configuration for a given profile request based on the request. Also gives access to an
 * "anonymous" relying party configuration which is used when nothing is known about the relying party.
 */
public class RelyingPartyConfigurationResolver implements Resolver<RelyingPartyConfiguration, ProfileRequestContext> {

    /** Configuration used with anonymous relying parties. */
    private final RelyingPartyConfiguration anonymousConfiguration;

    /** Registered relying party configurations. */
    private final List<RelyingPartyConfiguration> configurations;

    /**
     * Constructor.
     * 
     * @param anonymous configuration to use for an anonymous relying party, may be null
     * @param configs configurations, in order of preference, for relying parties, may be null or contain null entries
     */
    public RelyingPartyConfigurationResolver(final RelyingPartyConfiguration anonymous,
            final List<RelyingPartyConfiguration> configs) {
        anonymousConfiguration = anonymous;

        if (configs == null || configs.isEmpty()) {
            configurations = Collections.emptyList();
            return;
        }

        final ArrayList<RelyingPartyConfiguration> configList = new ArrayList<RelyingPartyConfiguration>();
        for (RelyingPartyConfiguration config : configs) {
            if (config != null) {
                configList.add(config);
            }
        }

        if (configList.isEmpty()) {
            configurations = Collections.emptyList();
        } else {
            configurations = Collections.unmodifiableList(configList);
        }
    }

    /**
     * Gets the configuration used with anonymous relying parties.
     * 
     * @return configuration used with anonymous relying parties
     */
    public RelyingPartyConfiguration getAnonymousConfiguration() {
        return anonymousConfiguration;
    }

    /** {@inheritDoc} */
    public Iterable<RelyingPartyConfiguration> resolve(final ProfileRequestContext context) throws SecurityException {
        final ArrayList<RelyingPartyConfiguration> matches = new ArrayList<RelyingPartyConfiguration>();

        for (RelyingPartyConfiguration configuration : configurations) {

            if (configuration.getRequirementCriteria().evaluate(context) == Boolean.TRUE) {
                matches.add(configuration);
            }
        }

        return matches;
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration resolveSingle(final ProfileRequestContext context) throws SecurityException {
        for (RelyingPartyConfiguration configuration : configurations) {

            if (configuration.getRequirementCriteria().evaluate(context) == Boolean.TRUE) {
                return configuration;
            }
        }

        return null;
    }
}
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

import net.shibboleth.idp.AbstractComponent;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.xml.security.Resolver;
import org.opensaml.xml.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves a per-relying party configuration for a given profile request based on the request. Also gives access to an
 * "anonymous" relying party configuration which is used when nothing is known about the relying party.
 * 
 * Note, this resolver does not permit more than one {@link RelyingPartyConfiguration} with the same ID.
 */
public class RelyingPartyConfigurationResolver extends AbstractComponent implements
        Resolver<RelyingPartyConfiguration, ProfileRequestContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RelyingPartyConfigurationResolver.class);

    /** Configuration used with anonymous relying parties. */
    private final RelyingPartyConfiguration anonymousRpConfiguration;

    /** Registered relying party configurations. */
    private final List<RelyingPartyConfiguration> rpConfigurations;

    /**
     * Constructor.
     * 
     * @param id unique identifier for this component
     * @param anonymous configuration to use for an anonymous relying party, may be null
     * @param configs configurations, in order of preference, for relying parties, may be null or contain null entries
     */
    public RelyingPartyConfigurationResolver(final String id, final RelyingPartyConfiguration anonymous,
            final List<RelyingPartyConfiguration> configs) {
        super(id);

        anonymousRpConfiguration = anonymous;

        if (configs == null || configs.isEmpty()) {
            rpConfigurations = Collections.emptyList();
            return;
        }

        final ArrayList<String> configIds = new ArrayList<String>();
        final ArrayList<RelyingPartyConfiguration> configList = new ArrayList<RelyingPartyConfiguration>();
        for (RelyingPartyConfiguration config : configs) {
            if (config != null) {
                if (configIds.contains(config.getConfigurationId())) {
                    throw new IllegalArgumentException("Relying party configuration with ID "
                            + config.getConfigurationId() + " already exists.");
                }
                configList.add(config);
                configIds.add(config.getConfigurationId());
            }
        }

        if (configList.isEmpty()) {
            rpConfigurations = Collections.emptyList();
        } else {
            rpConfigurations = Collections.unmodifiableList(configList);
        }
    }

    /**
     * Gets the configuration used with anonymous relying parties.
     * 
     * @return configuration used with anonymous relying parties, may be null
     */
    public RelyingPartyConfiguration getAnonymousRelyingPartyConfiguration() {
        return anonymousRpConfiguration;
    }

    /**
     * Gets the unmodifiable list of available relying party configurations.
     * 
     * @return unmodifiable list of available relying party configurations, never null and never contains null entries
     */
    public List<RelyingPartyConfiguration> getRelyingPartyConfigurations() {
        return rpConfigurations;
    }

    /** {@inheritDoc} */
    public Iterable<RelyingPartyConfiguration> resolve(final ProfileRequestContext context) throws SecurityException {
        if (context == null) {
            return Collections.emptyList();
        }

        log.debug("Resolving relying party configurations for profile request {}", context.getId());

        final ArrayList<RelyingPartyConfiguration> matches = new ArrayList<RelyingPartyConfiguration>();

        for (RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getConfigurationId(), context.getId());
            if (configuration.getRequirementCriteria().evaluate(context) == Boolean.TRUE) {
                log.debug("Relying party configuration {} is applicable to profile request {}",
                        configuration.getConfigurationId(), context.getId());
                matches.add(configuration);
            } else {
                log.debug("Relying party configuration {} is not applicable to profile request {}",
                        configuration.getConfigurationId(), context.getId());
            }
        }

        return matches;
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration resolveSingle(final ProfileRequestContext context) throws SecurityException {
        if (context == null) {
            return null;
        }

        log.debug("Resolving relying party configuration for profile request {}", context.getId());
        for (RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getConfigurationId(), context.getId());
            if (configuration.getRequirementCriteria().evaluate(context) == Boolean.TRUE) {
                log.debug("Relying party configuration {} is applicable to profile request {}",
                        configuration.getConfigurationId(), context.getId());
                return configuration;
            } else {
                log.debug("Relying party configuration {} is not applicable to profile request {}",
                        configuration.getConfigurationId(), context.getId());
            }
        }

        log.debug("No relying party configurations are applicable to profile request {}", context.getId());
        return null;
    }
}
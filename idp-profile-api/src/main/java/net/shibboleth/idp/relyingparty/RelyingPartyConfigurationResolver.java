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

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.AbstractIdentifiedInitializableComponent;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.criteria.EvaluationException;
import org.opensaml.util.resolver.Resolver;
import org.opensaml.util.resolver.ResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves a per-relying party configuration for a given profile request based on the request. Also gives access to an
 * "anonymous" relying party configuration which is used when nothing is known about the relying party.
 * 
 * Note, this resolver does not permit more than one {@link RelyingPartyConfiguration} with the same ID.
 */
public class RelyingPartyConfigurationResolver extends AbstractIdentifiedInitializableComponent implements
        Resolver<RelyingPartyConfiguration, ProfileRequestContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RelyingPartyConfigurationResolver.class);

    /** Configuration used with anonymous relying parties. */
    private RelyingPartyConfiguration anonymousRpConfiguration;

    /** Registered relying party configurations. */
    private List<RelyingPartyConfiguration> rpConfigurations = Collections.emptyList();

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /**
     * Gets the relying party configuration used for anonymous relying parties.
     * 
     * @return relying party configuration used for anonymous relying parties, may be null
     */
    public RelyingPartyConfiguration getAnonymousRelyingPartyConfiguration() {
        return anonymousRpConfiguration;
    }

    /**
     * Sets the relying party configuration used for anonymous relying parties.
     * 
     * This property may not be changed after the resolver is initialized.
     * 
     * @param config relying party configuration used for anonymous relying parties
     */
    public synchronized void setAnonymousRelyingPartyConfiguration(RelyingPartyConfiguration config) {
        if (isInitialized()) {
            return;
        }

        anonymousRpConfiguration = config;
    }

    /**
     * Gets the unmodifiable list of registered relying party configurations.
     * 
     * @return unmodifiable list of registered relying party configurations, never null nor containing null entries
     */
    public List<RelyingPartyConfiguration> getRelyingPartyConfigurations() {
        return rpConfigurations;
    }

    /**
     * Sets the list of registered relying party configurations.
     * 
     * This property may not be changed after the resolver is initialized.
     * 
     * @param configs list of registered relying party configurations
     */
    public synchronized void setRelyingPartyConfigurations(List<RelyingPartyConfiguration> configs) {
        if (isInitialized()) {
            return;
        }

        ArrayList<RelyingPartyConfiguration> checkedConfigs =
                CollectionSupport.addNonNull(configs, new ArrayList<RelyingPartyConfiguration>());
        if (checkedConfigs.isEmpty()) {
            rpConfigurations = Collections.emptyList();
        } else {
            rpConfigurations = Collections.unmodifiableList(checkedConfigs);
        }
    }

    /** {@inheritDoc} */
    public Iterable<RelyingPartyConfiguration> resolve(final ProfileRequestContext context) throws ResolverException {
        if (context == null) {
            return Collections.emptyList();
        }

        log.debug("Resolving relying party configurations for profile request {}", context.getId());

        final ArrayList<RelyingPartyConfiguration> matches = new ArrayList<RelyingPartyConfiguration>();

        for (RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getConfigurationId(), context.getId());
            try {
                if (configuration.getActivationCriteria().evaluate(context) == Boolean.TRUE) {
                    log.debug("Relying party configuration {} is applicable to profile request {}",
                            configuration.getConfigurationId(), context.getId());
                    matches.add(configuration);
                } else {
                    log.debug("Relying party configuration {} is not applicable to profile request {}",
                            configuration.getConfigurationId(), context.getId());
                }
            } catch (EvaluationException e) {
                log.warn("Error evaluating relying party configuration criteria", e);
            }
        }

        return matches;
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration resolveSingle(final ProfileRequestContext context) throws ResolverException {
        if (context == null) {
            return null;
        }

        log.debug("Resolving relying party configuration for profile request {}", context.getId());
        for (RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getConfigurationId(), context.getId());
            try {
                if (configuration.getActivationCriteria().evaluate(context) == Boolean.TRUE) {
                    log.debug("Relying party configuration {} is applicable to profile request {}",
                            configuration.getConfigurationId(), context.getId());
                    return configuration;
                } else {
                    log.debug("Relying party configuration {} is not applicable to profile request {}",
                            configuration.getConfigurationId(), context.getId());
                }
            } catch (EvaluationException e) {
                log.warn("Error evaluating relying party configuration criteria", e);
            }
        }

        log.debug("No relying party configurations are applicable to profile request {}", context.getId());
        return null;
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final ArrayList<String> configIds = new ArrayList<String>();
        for (RelyingPartyConfiguration config : rpConfigurations) {
            if (configIds.contains(config.getConfigurationId())) {
                throw new ComponentInitializationException("Multiple replying party configurations with ID "
                        + config.getConfigurationId() + " detected.  Configuration IDs must be unique.");
            }
            configIds.add(config.getConfigurationId());
        }

    }
}
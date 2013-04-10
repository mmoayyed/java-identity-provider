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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Retrieves a per-relying party configuration for a given profile request based on the request.
 * 
 * Note, this resolver does not permit more than one {@link ActivatedRelyingPartyConfiguration} with the same ID.
 */
public class ActivatedRelyingPartyConfigurationResolver extends AbstractIdentifiableInitializableComponent implements
        Resolver<ActivatedRelyingPartyConfiguration, ProfileRequestContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ActivatedRelyingPartyConfigurationResolver.class);

    /** Registered relying party configurations. */
    private List<ActivatedRelyingPartyConfiguration> rpConfigurations = Collections.emptyList();

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /**
     * Gets the unmodifiable list of registered relying party configurations.
     * 
     * @return unmodifiable list of registered relying party configurations, never null nor containing null entries
     */
    public List<ActivatedRelyingPartyConfiguration> getRelyingPartyConfigurations() {
        return rpConfigurations;
    }

    /**
     * Sets the list of registered relying party configurations.
     * 
     * This property may not be changed after the resolver is initialized.
     * 
     * @param configs list of registered relying party configurations
     */
    public synchronized void setRelyingPartyConfigurations(List<ActivatedRelyingPartyConfiguration> configs) {
        if (isInitialized()) {
            return;
        }

        rpConfigurations =
                ImmutableList.<ActivatedRelyingPartyConfiguration> builder()
                        .addAll(Iterables.filter(configs, Predicates.notNull())).build();

    }

    /** {@inheritDoc} */
    public Iterable<ActivatedRelyingPartyConfiguration> resolve(final ProfileRequestContext context)
            throws ResolverException {
        if (context == null) {
            return Collections.emptyList();
        }

        log.debug("Resolving relying party configurations for profile request {}", context.getId());

        final ArrayList<ActivatedRelyingPartyConfiguration> matches =
                new ArrayList<ActivatedRelyingPartyConfiguration>();

        for (ActivatedRelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getConfigurationId(), context.getId());
            if (configuration.getActivationCriteria().apply(context)) {
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
    public ActivatedRelyingPartyConfiguration resolveSingle(final ProfileRequestContext context)
            throws ResolverException {
        if (context == null) {
            return null;
        }

        log.debug("Resolving relying party configuration for profile request {}", context.getId());
        for (ActivatedRelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getConfigurationId(), context.getId());
            if (configuration.getActivationCriteria().apply(context)) {
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

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final ArrayList<String> configIds = new ArrayList<String>();
        for (ActivatedRelyingPartyConfiguration config : rpConfigurations) {
            if (configIds.contains(config.getConfigurationId())) {
                throw new ComponentInitializationException("Multiple replying party configurations with ID "
                        + config.getConfigurationId() + " detected.  Configuration IDs must be unique.");
            }
            configIds.add(config.getConfigurationId());
        }
    }
}
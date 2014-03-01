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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Retrieves a per-relying party configuration for a given profile request based on the request context.
 * 
 * <p>
 * Note that this resolver does not permit more than one {@link RelyingPartyConfiguration} with the same ID.
 * </p>
 */
public class DefaultRelyingPartyConfigurationResolver extends AbstractIdentifiableInitializableComponent implements
        RelyingPartyConfigurationResolver {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DefaultRelyingPartyConfigurationResolver.class);

    /** Registered relying party configurations. */
    private Collection<RelyingPartyConfiguration> rpConfigurations;

    /** Constructor. */
    public DefaultRelyingPartyConfigurationResolver() {
        rpConfigurations = Collections.emptyList();
    }

    /**
     * Get an unmodifiable list of registered relying party configurations.
     * 
     * @return unmodifiable list of registered relying party configurations
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Collection<RelyingPartyConfiguration>
            getRelyingPartyConfigurations() {
        return ImmutableList.copyOf(rpConfigurations);
    }

    /**
     * Set the registered relying party configurations.
     * 
     * This property may not be changed after the resolver is initialized.
     * 
     * @param configs list of registered relying party configurations
     */
    public void setRelyingPartyConfigurations(
            @Nonnull @NonnullElements final Collection<RelyingPartyConfiguration> configs) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(configs, "RelyingPartyConfiguration collection cannot be null");

        rpConfigurations = Lists.newArrayList(Collections2.filter(configs, Predicates.notNull()));
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final HashSet<String> configIds = Sets.newHashSetWithExpectedSize(rpConfigurations.size());
        for (final RelyingPartyConfiguration config : rpConfigurations) {
            if (configIds.contains(config.getId())) {
                throw new ComponentInitializationException("Multiple replying party configurations with ID "
                        + config.getId() + " detected. Configuration IDs must be unique.");
            }
            configIds.add(config.getId());
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements public Iterable<RelyingPartyConfiguration> resolve(
            @Nullable final ProfileRequestContext context) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        if (context == null) {
            return Collections.emptyList();
        }

        log.debug("Resolving relying party configurations for profile request {}", context.getId());

        final ArrayList<RelyingPartyConfiguration> matches = Lists.newArrayList();

        for (final RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getId(), context.getId());
            if (configuration.apply(context)) {
                log.debug("Relying party configuration {} is applicable to profile request {}", configuration.getId(),
                        context.getId());
                matches.add(configuration);
            } else {
                log.debug("Relying party configuration {} is not applicable to profile request {}",
                        configuration.getId(), context.getId());
            }
        }

        return matches;
    }

    /** {@inheritDoc} */
    @Override @Nullable public RelyingPartyConfiguration resolveSingle(@Nullable final ProfileRequestContext context)
            throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        if (context == null) {
            return null;
        }

        log.debug("Resolving relying party configuration for profile request {}", context.getId());
        for (RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable to profile request {}",
                    configuration.getId(), context.getId());
            if (configuration.apply(context)) {
                log.debug("Relying party configuration {} is applicable to profile request {}", configuration.getId(),
                        context.getId());
                return configuration;
            } else {
                log.debug("Relying party configuration {} is not applicable to profile request {}",
                        configuration.getId(), context.getId());
            }
        }

        log.debug("No relying party configurations are applicable to profile request {}", context.getId());
        return null;
    }

}
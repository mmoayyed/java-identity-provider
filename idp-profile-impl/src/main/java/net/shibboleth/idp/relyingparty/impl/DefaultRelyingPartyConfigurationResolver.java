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
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.config.SecurityConfiguration;
import org.opensaml.security.credential.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import net.shibboleth.idp.profile.logic.VerifiedProfilePredicate;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.RelyingPartyResolverCredentialHolder;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.resolver.ResolverException;

/**
 * Retrieves a per-relying party configuration for a given profile request based on the request context.
 * 
 * <p>
 * Note that this resolver does not permit more than one {@link RelyingPartyConfiguration} with the same ID.
 * </p>
 */
public class DefaultRelyingPartyConfigurationResolver extends AbstractIdentifiableInitializableComponent
        implements RelyingPartyConfigurationResolver {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DefaultRelyingPartyConfigurationResolver.class);

    /** Registered relying party configurations. */
    @Nonnull private List<RelyingPartyConfiguration> rpConfigurations;

    /** Unverified relying party configuration, used if the request is unverified. */
    @Nullable private RelyingPartyConfiguration unverifiedConfiguration;

    /** Default relying party configuration, used if no other verified configuration matches. */
    @NonnullAfterInit private RelyingPartyConfiguration defaultConfiguration;

    /** The predicate which decides if this request is "verified". */
    @NonnullAfterInit private Predicate<ProfileRequestContext> verificationPredicate;
    
    /** A global default security configuration. */
    @Nullable private SecurityConfiguration defaultSecurityConfiguration;
    
    /** The global list of all configured signing credentials. */
    @Nullable private List<Credential> signingCredentials;
    
    /** The global list of all configured encryption credentials. */
    @Nullable private List<Credential> encryptionCredentials;

    /** Constructor. */
    public DefaultRelyingPartyConfigurationResolver() {
        rpConfigurations = Collections.emptyList();
        verificationPredicate = new VerifiedProfilePredicate();
        signingCredentials = Collections.emptyList();
        encryptionCredentials = Collections.emptyList();
    }
    
    /**
     * Get an unmodifiable list of verified relying party configurations.
     * 
     * @return unmodifiable list of verified relying party configurations
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<RelyingPartyConfiguration>
            getRelyingPartyConfigurations() {
        return rpConfigurations;
    }

    /**
     * Set the verified relying party configurations.
     * 
     * @param configs list of verified relying party configurations
     */
    public void setRelyingPartyConfigurations(@Nonnull @NonnullElements final List<RelyingPartyConfiguration> configs) {
        checkSetterPreconditions();
        rpConfigurations = List.copyOf(Constraint.isNotNull(configs, "RelyingPartyConfiguration list cannot be null"));
    }

    /**
     * Get the {@link RelyingPartyConfiguration} to use if no other verified configuration is acceptable.
     * 
     * @return default verified configuration
     */
    @NonnullAfterInit public RelyingPartyConfiguration getDefaultConfiguration() {
        return defaultConfiguration;
    }

    /**
     * Set the {@link RelyingPartyConfiguration} to use if no other verified configuration is acceptable.
     * 
     * @param configuration default verified configuration
     */
    public void setDefaultConfiguration(@Nonnull final RelyingPartyConfiguration configuration) {
        checkSetterPreconditions();
        defaultConfiguration = Constraint.isNotNull(configuration, "Default RP configuration cannot be null");
    }

    /**
     * Get the {@link RelyingPartyConfiguration} to use if the configuration is found to be "unverified"
     * (via use of {@link #getVerificationPredicate()}).
     * 
     * @return unverified configuration
     */
    @NonnullAfterInit public RelyingPartyConfiguration getUnverifiedConfiguration() {
        return unverifiedConfiguration;
    }

    /**
     * Set the {@link RelyingPartyConfiguration} to use if the configuration is found to be "unverified"
     * (via use of {@link #getVerificationPredicate()}).
     * 
     * @param configuration unverified configuration
     */
    public void setUnverifiedConfiguration(@Nonnull final RelyingPartyConfiguration configuration) {
        checkSetterPreconditions();
        unverifiedConfiguration = Constraint.isNotNull(configuration, "Unverified RP configuration cannot be null");
    }

    /**
     * Get the definition of what a verified request is.
     * 
     * @return predicate for determination whether request is verified
     */
    @Nonnull public Predicate<ProfileRequestContext> getVerificationPredicate() {
        return verificationPredicate;
    }

    /**
     * Set the definition of what a verified request is.
     * 
     * @param predicate predicate to set
     */
    public void setVerificationPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        checkSetterPreconditions();
        verificationPredicate = Constraint.isNotNull(predicate, "Verification predicate cannot be null");
    }
    
    /**
     * Set the global default {@link SecurityConfiguration}.
     * 
     * @param config  global default
     */
    public void setDefaultSecurityConfiguration(@Nullable final SecurityConfiguration config) {
        checkSetterPreconditions();
        defaultSecurityConfiguration = config;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final HashSet<String> configIds = new HashSet<>(rpConfigurations.size());
        for (final RelyingPartyConfiguration config : rpConfigurations) {
            if (configIds.contains(config.getId())) {
                throw new ComponentInitializationException("Multiple relying party configurations with ID "
                        + config.getId() + " detected. Configuration IDs must be unique.");
            }
            configIds.add(config.getId());
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements public Iterable<RelyingPartyConfiguration> resolve(
            @Nullable final ProfileRequestContext context) throws ResolverException {
        checkComponentActive();

        if (context == null) {
            return Collections.emptyList();
        }

        log.debug("Resolving relying party configuration");
        if (!verificationPredicate.test(context)) {
            if (getUnverifiedConfiguration() == null) {
                log.warn("Profile request was unverified, but no such configuration is available");
                return Collections.emptyList();
            }
            log.debug("Profile request is unverified, returning configuration {}",
                    getUnverifiedConfiguration().getId());
            return Collections.singleton(getUnverifiedConfiguration());
        }

        final ArrayList<RelyingPartyConfiguration> matches = new ArrayList<>();

        for (final RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable", configuration.getId());
            if (configuration.test(context)) {
                log.debug("Relying party configuration {} is applicable", configuration.getId());
                matches.add(configuration);
            } else {
                log.debug("Relying party configuration {} is not applicable", configuration.getId());
            }
        }

        if (matches.isEmpty()) {
            log.debug("No matching Relying Party Configuration found, returning the default configuration {}",
                    getDefaultConfiguration().getId());
            return Collections.singleton(getDefaultConfiguration());
        }
        return matches;
    }

    /** {@inheritDoc} */
    @Override @Nullable public RelyingPartyConfiguration resolveSingle(@Nullable final ProfileRequestContext context)
            throws ResolverException {
        checkComponentActive();

        if (context == null) {
            return null;
        }
        
        log.debug("Resolving relying party configuration");
        if (!verificationPredicate.test(context)) {
            if (getUnverifiedConfiguration() == null) {
                log.warn("Profile request was unverified, but no such configuration is available");
                return null;
            }
            log.debug("Profile request is unverified, returning configuration {}",
                    getUnverifiedConfiguration().getId());
            return getUnverifiedConfiguration();
        }

        for (final RelyingPartyConfiguration configuration : rpConfigurations) {
            log.debug("Checking if relying party configuration {} is applicable", configuration.getId());
            if (configuration.test(context)) {
                log.debug("Relying party configuration {} is applicable", configuration.getId());
                return configuration;
            }
            log.debug("Relying party configuration {} is not applicable", configuration.getId());
        }

        log.debug("No relying party configurations are applicable, returning the default configuration {}",
                getDefaultConfiguration().getId());
        return getDefaultConfiguration();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public SecurityConfiguration getDefaultSecurityConfiguration(@Nonnull @NotEmpty final String profileId) {
        return defaultSecurityConfiguration;
    }
    
    /**
     * Get the list of all configured signing credentials.
     * 
     * @return the list of signing credentials
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<Credential> getSigningCredentials() {
        return signingCredentials;
    }
    
    /**
     * Set the list of all configured signing credentials.
     * 
     * @param credentials the list of signing credentials, may be null
     */
    @Autowired
    @Qualifier("signing")
    public void setSigningCredentials(
            @Nullable @NonnullElements final List<RelyingPartyResolverCredentialHolder> credentials) {
        checkSetterPreconditions();
        
        if (credentials != null) {
            signingCredentials = credentials.stream()
                    .flatMap(h -> h.getCredentials().stream())
                    .collect(Collectors.toUnmodifiableList());
        } else {
            signingCredentials = Collections.emptyList();
        }
    }

    /**
     * Get the list of all configured encryption credentials.
     * 
     * @return the list of encryption credentials
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<Credential> getEncryptionCredentials() {
        return encryptionCredentials;
    }
    
    /**
     * Set the list of all configured encryption credentials.
     * 
     * @param credentials the list of encryption credentials, may be null
     */
    @Autowired
    @Qualifier("encryption")
    public void setEncryptionCredentials(
            @Nullable @NonnullElements final List<RelyingPartyResolverCredentialHolder> credentials) {
        checkSetterPreconditions();
        
        if (credentials != null) {
            encryptionCredentials = credentials.stream()
                    .flatMap(h -> h.getCredentials().stream())
                    .collect(Collectors.toUnmodifiableList());
        } else {
            encryptionCredentials = Collections.emptyList();
        }
    }
}
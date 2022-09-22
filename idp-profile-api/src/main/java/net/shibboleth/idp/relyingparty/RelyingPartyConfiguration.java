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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.component.IdentifiedComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** The configuration that applies to a given relying party. */
public class RelyingPartyConfiguration extends AbstractIdentifiableInitializableComponent implements
        IdentifiedComponent, Predicate<ProfileRequestContext> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RelyingPartyConfiguration.class);
    
    /** Lookup function to supply <code>responderId</code> property. */
    @Nonnull private Function<ProfileRequestContext,String> responderIdLookupStrategy;

    /** Controls whether detailed information about errors should be exposed. */
    @Nonnull private Predicate<ProfileRequestContext> detailedErrorsPredicate;

    /** Lookup function to supply <code>profileConfigurations</code> property. */
    @Nonnull
    private Function<ProfileRequestContext,Map<String,ProfileConfiguration>> profileConfigurationsLookupStrategy;

    /** Predicate that must be true for this configuration to be active for a given request. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;

    /** Constructor. */
    public RelyingPartyConfiguration() {
        activationCondition = Predicates.alwaysTrue();
        detailedErrorsPredicate = Predicates.alwaysFalse();
        profileConfigurationsLookupStrategy = FunctionSupport.constant(null);
    }

    /**
     * Get the self-referential ID to use when responding to requests.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return ID to use when responding
     */
    @Nonnull @NotEmpty public String getResponderId(@Nullable final ProfileRequestContext profileRequestContext) {
        return Constraint.isNotEmpty(responderIdLookupStrategy.apply(profileRequestContext),
                "ResponderId cannot be null or empty");
    }

    /**
     * Set the self-referential ID to use when responding to messages.
     * 
     * @param responder ID to use when responding to messages
     */
    public void setResponderId(@Nonnull @NotEmpty final String responder) {
        checkSetterPreconditions();
        final String id =
                Constraint.isNotNull(StringSupport.trimOrNull(responder), "ResponderId cannot be null or empty");
        responderIdLookupStrategy = FunctionSupport.constant(id);
    }

    /**
     * Set a lookup strategy for the <code>responderId</code> property.
     * 
     * @param strategy  lookup strategy
     * 
     * @since 3.4.0
     */
    public void setResponderIdLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        responderIdLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /**
     * Get whether detailed information about errors should be exposed.
     * 
     * @param profileRequestContext current profile request context
     *
     * @return true iff it is acceptable to expose detailed error information
     */
    public boolean isDetailedErrors(@Nullable final ProfileRequestContext profileRequestContext) {
        return detailedErrorsPredicate.test(profileRequestContext);
    }

    /**
     * Set whether detailed information about errors should be exposed.
     * 
     * @param flag  flag to set
     */
    public void setDetailedErrors(final boolean flag) {
        checkSetterPreconditions();
        detailedErrorsPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }
    
    /**
     * Set a condition to determine whether detailed information about errors should be exposed.
     * 
     * @param condition  condition to set
     */
    public void setDetailedErrorsPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        detailedErrorsPredicate = Constraint.isNotNull(condition, "Condition cannot be null");
    }
    
    /**
     * Get the unmodifiable set of profile configurations for this relying party.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return unmodifiable set of profile configurations for this relying party, never null
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Map<String,ProfileConfiguration> getProfileConfigurations(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        final Map<String,ProfileConfiguration> map = profileConfigurationsLookupStrategy.apply(profileRequestContext);
        if (map != null) {
            return Map.copyOf(map);
        }
        return Collections.emptyMap();
    }

    /**
     * Get the profile configuration, for the relying party, for the given profile. This is a convenience method and is
     * equivalent to calling {@link Map#get(Object)} on the return of
     * {@link #getProfileConfigurations(ProfileRequestContext)}. This map contains no null entries, keys, or values.
     * 
     * @param profileRequestContext current profile request context
     * @param profileId the ID of the profile
     * 
     * @return the configuration for the profile or null if the profile ID was null or empty or there is no
     *         configuration for the given profile
     */
    @Nullable public ProfileConfiguration getProfileConfiguration(
            @Nullable final ProfileRequestContext profileRequestContext, @Nullable final String profileId) {
        final String trimmedId = StringSupport.trimOrNull(profileId);
        if (trimmedId == null) {
            return null;
        }

        return getProfileConfigurations(profileRequestContext).get(trimmedId);
    }

    /**
     * Set the profile configurations for this relying party.
     * 
     * @param configs the configurations to set
     */
    public void setProfileConfigurations(@Nullable @NonnullElements final Collection<ProfileConfiguration> configs) {
        checkSetterPreconditions();
        if (configs == null) {
            profileConfigurationsLookupStrategy = FunctionSupport.constant(null);
        } else {
            final HashMap<String,ProfileConfiguration> map = new HashMap<>();
            for (final ProfileConfiguration config : List.copyOf(configs)) {
                final String trimmedId =
                        Constraint.isNotNull(StringSupport.trimOrNull(config.getId()),
                                "ID of profile configuration class " + config.getClass().getName() + " cannot be null");
                map.put(trimmedId, config);
            }
            profileConfigurationsLookupStrategy = FunctionSupport.constant(Map.copyOf(map));
        }
    }

    /**
     * Set a lookup strategy for the <code>profileConfigurations</code> property.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setProfileConfigurationsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Map<String,ProfileConfiguration>> strategy) {
        checkSetterPreconditions();
        profileConfigurationsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Set the condition under which the relying party configuration should be active.
     * 
     * @param condition the activation condition
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        checkSetterPreconditions();
        activationCondition =
                Constraint.isNotNull(condition, "Relying party configuration activation condition cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
    
        if (responderIdLookupStrategy == null) {
            throw new ComponentInitializationException("ResponderID lookup strategy cannot be null");
        }
        
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        checkComponentActive();
        return activationCondition.test(input);
    }

}
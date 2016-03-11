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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletRequest;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.IdentifiedComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;

/** The configuration that applies to a given relying party. */
public class RelyingPartyConfiguration extends AbstractIdentifiableInitializableComponent implements
        IdentifiedComponent, Predicate<ProfileRequestContext> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RelyingPartyConfiguration.class);

    /** Access to servlet request. */
    @Nullable private ServletRequest servletRequest;
    
    /** Lookup function to supply {@link #responderId} property. */
    @Nullable private Function<ProfileRequestContext,String> responderIdLookupStrategy;

    /** Self-referential ID to use when responding to messages. */
    @Nullable @NotEmpty private String responderId;

    /** Lookup function to supply {@link #detailedErrors} property. */
    @Nullable private Function<ProfileRequestContext,Boolean> detailedErrorsLookupStrategy;

    /** Controls whether detailed information about errors should be exposed. */
    private boolean detailedErrors;

    /** Lookup function to supply {@link #profileConfigurations} property. */
    @Nullable
    private Function<ProfileRequestContext,Map<String,ProfileConfiguration>> profileConfigurationsLookupStrategy;
    
    /** Registered and usable communication profile configurations for this relying party. */
    @Nonnull @NonnullElements private Map<String,ProfileConfiguration> profileConfigurations;

    /** Predicate that must be true for this configuration to be active for a given request. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;

    /** Constructor. */
    public RelyingPartyConfiguration() {
        activationCondition = Predicates.alwaysTrue();
        profileConfigurations = Collections.emptyMap();
    }
    
    /**
     * Set the {@link ServletRequest} from which to obtain a reference to the current {@link ProfileRequestContext}.
     * 
     * <p>Generally this would be expected to be a proxy to the actual object.</p>
     * 
     * @param request servlet request
     */
    public void setServletRequest(@Nullable final ServletRequest request) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        servletRequest = request;
    }

    /**
     * Get the self-referential ID to use when responding to requests.
     * 
     * @return ID to use when responding
     */
    @Nonnull @NotEmpty public String getResponderId() {
        return Constraint.isNotNull(getIndirectProperty(responderIdLookupStrategy, responderId),
                "ResponderId cannot be null");
    }

    /**
     * Set the self-referential ID to use when responding to messages.
     * 
     * @param responder ID to use when responding to messages
     */
    public void setResponderId(@Nullable final String responder) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responderId = StringSupport.trimOrNull(responder);
    }

    /**
     * Set a lookup strategy for the {@link #responderId} property.
     * 
     * @param strategy  lookup strategy
     */
    public void setResponderIdLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        responderIdLookupStrategy = strategy;
    }
        
    /**
     * Get whether detailed information about errors should be exposed.
     * 
     * @return true iff it is acceptable to expose detailed error information
     */
    public boolean isDetailedErrors() {
        return getIndirectProperty(detailedErrorsLookupStrategy, detailedErrors);
    }

    /**
     * Set whether detailed information about errors should be exposed.
     * 
     * @param flag flag to set
     */
    public void setDetailedErrors(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        detailedErrors = flag;
    }
    
    /**
     * Set a lookup strategy for the {@link #detailedErrors} property.
     * 
     * @param strategy  lookup strategy
     */
    public void setDetailedErrorsLookupStrategy(@Nullable final Function<ProfileRequestContext,Boolean> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        detailedErrorsLookupStrategy = strategy;
    }
    
    /**
     * Get the unmodifiable set of profile configurations for this relying party.
     * 
     * @return unmodifiable set of profile configurations for this relying party, never null
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive
    public Map<String,ProfileConfiguration> getProfileConfigurations() {
        return ImmutableMap.copyOf(getIndirectProperty(profileConfigurationsLookupStrategy, profileConfigurations));
    }

    /**
     * Get the profile configuration, for the relying party, for the given profile. This is a convenience method and is
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

        return getProfileConfigurations().get(trimmedId);
    }

    /**
     * Set the profile configurations for this relying party.
     * 
     * @param configs the configurations to set
     */
    public void setProfileConfigurations(@Nullable @NonnullElements final Collection<ProfileConfiguration> configs) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (configs == null) {
            profileConfigurations = Collections.emptyMap();
        } else {
            profileConfigurations = new HashMap<>();
            for (final ProfileConfiguration config : Collections2.filter(configs, Predicates.notNull())) {
                final String trimmedId =
                        Constraint.isNotNull(StringSupport.trimOrNull(config.getId()),
                                "ID of profile configuration class " + config.getClass().getName() + " cannot be null");
                profileConfigurations.put(trimmedId, config);
            }
        }
    }

    /**
     * Set a lookup strategy for the {@link #profileConfigurations} property.
     * 
     * @param strategy lookup strategy
     */
    public void setProfileConfigurationsLookupStrategy(
            @Nullable final Function<ProfileRequestContext,Map<String,ProfileConfiguration>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        profileConfigurationsLookupStrategy = strategy;
    }

    /**
     * Set the condition under which the relying party configuration should be active.
     * 
     * @param condition the activation condition
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        activationCondition =
                Constraint.isNotNull(condition, "Relying partying configuration activation condition cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (responderId == null && responderIdLookupStrategy == null) {
            throw new ComponentInitializationException("Responder ID and lookup strategy cannot both be null");
        }
        
    }

    /** {@inheritDoc} */
    @Override public boolean apply(@Nullable final ProfileRequestContext input) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        return activationCondition.apply(input);
    }

    /**
     * Get the current {@link ProfileRequestContext}.
     * 
     * @return current profile request context
     */
    @Nullable private ProfileRequestContext getProfileRequestContext() {
        if (servletRequest != null) {
            final Object object = servletRequest.getAttribute(ProfileRequestContext.BINDING_KEY);
            if (object instanceof ProfileRequestContext) {
                return (ProfileRequestContext) object;
            }
            log.warn("RelyingPartyConfiguration {}: No ProfileRequestContext in request", getId());
        } else {
            log.warn("RelyingPartyConfiguration {}: ServletRequest was null", getId());
        }
        return null;
    }
    
    /**
     * Get a property, possibly through indirection via a lookup function.
     * 
     * @param <T> type of property
     * 
     * @param lookupStrategy lookup strategy function for indirect access
     * @param staticValue static value to return in the absence of a lookup function or if null is returned
     * 
     * @return a dynamic or static result, if any
     */
    @Nullable private <T> T getIndirectProperty(@Nullable final Function<ProfileRequestContext,T> lookupStrategy,
            @Nullable final T staticValue) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        if (lookupStrategy != null) {
            final T prop = lookupStrategy.apply(getProfileRequestContext());
            if (prop != null) {
                return prop;
            }
        }

        return staticValue;
    }

}
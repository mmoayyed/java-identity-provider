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

package net.shibboleth.idp.profile.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletRequest;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for {@link ProfileConfiguration} implementations. */
public abstract class AbstractProfileConfiguration extends AbstractIdentifiableInitializableComponent
        implements ProfileConfiguration {
    
    /** Default value for disallowedFeatures property. */
    @Nonnull public static final Integer DEFAULT_DISALLOWED_FEATURES = 0; 

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractProfileConfiguration.class);

    /** Access to servlet request. */
    @Nullable private ServletRequest servletRequest;

    /** Lookup function to supply {@link #inboundFlows} property. */
    @Nonnull private Function<ProfileRequestContext,List<String>> inboundFlowsLookupStrategy;

    /** Lookup function to supply {@link #outboundFlows} property. */
    @Nonnull private Function<ProfileRequestContext,List<String>> outboundFlowsLookupStrategy;

    /** Lookup function to supply {@link #securityConfiguration} property. */
    @Nonnull private Function<ProfileRequestContext,SecurityConfiguration> securityConfigurationLookupStrategy;

    /** Lookup function to return a bitmask of request features to disallow. */
    @Nonnull private Function<ProfileRequestContext,Integer> disallowedFeaturesLookupStrategy;

    /**
     * Constructor.
     * 
     * @param id ID of the communication profile, never null or empty
     */
    public AbstractProfileConfiguration(@Nonnull @NotEmpty @ParameterName(name="id") final String id) {
        setId(id);
        securityConfigurationLookupStrategy = FunctionSupport.constant(null);
        inboundFlowsLookupStrategy = FunctionSupport.constant(null);
        outboundFlowsLookupStrategy = FunctionSupport.constant(null);
        disallowedFeaturesLookupStrategy = FunctionSupport.constant(DEFAULT_DISALLOWED_FEATURES);
    }

    /**
     * Set the {@link ServletRequest} from which to obtain a reference to the current {@link ProfileRequestContext}.
     *
     * <p>Generally this would be expected to be a proxy to the actual object.</p>
     *
     * @param request servlet request
     * 
     * @since 3.3.0
     */
    public void setServletRequest(@Nullable final ServletRequest request) {
        servletRequest = request;
    }

    /** {@inheritDoc} */
    @Nullable public SecurityConfiguration getSecurityConfiguration(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return securityConfigurationLookupStrategy.apply(profileRequestContext);
    }

    /**
     * Sets the security configuration for this profile.
     * 
     * @param configuration security configuration for this profile
     */
    public void setSecurityConfiguration(@Nullable final SecurityConfiguration configuration) {
        securityConfigurationLookupStrategy = FunctionSupport.constant(configuration);
    }

    /**
     * Set a lookup strategy for the {@link #securityConfiguration} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setSecurityConfigurationLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SecurityConfiguration> strategy) {
        securityConfigurationLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getInboundInterceptorFlows(
        @Nullable final ProfileRequestContext profileRequestContext) {
        return CollectionSupport.buildImmutableList(inboundFlowsLookupStrategy.apply(profileRequestContext));
    }

    /**
     * Set the ordered collection of inbound interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setInboundInterceptorFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            inboundFlowsLookupStrategy =
                    FunctionSupport.constant(new ArrayList<>(StringSupport.normalizeStringCollection(flows)));
        } else {
            inboundFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the {@link #inboundFlows} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setInboundFlowsLookupStrategy(@Nonnull final Function<ProfileRequestContext,List<String>> strategy) {
        inboundFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }


    /** {@inheritDoc} */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getOutboundInterceptorFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        return CollectionSupport.buildImmutableList(outboundFlowsLookupStrategy.apply(profileRequestContext));
    }

    /**
     * Set the ordered collection of outbound interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setOutboundInterceptorFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            outboundFlowsLookupStrategy =
                    FunctionSupport.constant(new ArrayList<>(StringSupport.normalizeStringCollection(flows)));
        } else {
            outboundFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the {@link #outboundFlows} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setOutboundFlowsLookupStrategy(@Nonnull final Function<ProfileRequestContext,List<String>> strategy) {
        outboundFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /**
     * Return true iff the input feature constant is disallowed.
     * 
     * @param profileRequestContext current profile request context
     * @param feature a bit constant
     * 
     * @return true iff the input feature constant is disallowed
     * 
     * @since 3.3.0
     */
    public boolean isFeatureDisallowed(@Nullable final ProfileRequestContext profileRequestContext, final int feature) {
        return (getDisallowedFeatures(profileRequestContext) & feature) == feature;
    }
    
    /**
     * Get a bitmask of disallowed features to block.
     * 
     * <p>Individual profiles define their own feature constants.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return bitmask of features to block
     * 
     * @since 3.3.0
     */
    public int getDisallowedFeatures(@Nullable final ProfileRequestContext profileRequestContext) {
        final Integer mask = disallowedFeaturesLookupStrategy.apply(profileRequestContext); 
        return mask != null ? mask : DEFAULT_DISALLOWED_FEATURES;
    }
    
    /**
     * Set a bitmask of disallowed features to block.
     * 
     * @param mask a bitmask of features to block
     * 
     * @since 3.3.0
     */
    public void setDisallowedFeatures(final int mask) {
        disallowedFeaturesLookupStrategy = FunctionSupport.constant(mask);
    }
    
    /**
     * Set a lookup strategy for the bitmask of disallowed features to block. 
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setDisallowedFeaturesLookupStrategy(@Nonnull final Function<ProfileRequestContext,Integer> strategy) {
        disallowedFeaturesLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof AbstractProfileConfiguration)) {
            return false;
        }

        final AbstractProfileConfiguration other = (AbstractProfileConfiguration) obj;
        return Objects.equals(getId(), other.getId());
    }

    /**
     * Get the current {@link ProfileRequestContext}.
     *
     * @return current profile request context
     * 
     * @since 3.3.0
     */
    @Deprecated
    @Nullable protected ProfileRequestContext getProfileRequestContext() {
        if (servletRequest != null) {
            final Object object = servletRequest.getAttribute(ProfileRequestContext.BINDING_KEY);
            if (object instanceof ProfileRequestContext) {
                return (ProfileRequestContext) object;
            }
            log.warn("ProfileConfiguration {}: No ProfileRequestContext in request", getId());
        } else {
            log.warn("ProfileConfiguration {}: ServletRequest was null", getId());
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
     * 
     * @since 3.3.0
     */
    @Deprecated
    @Nullable protected <T> T getIndirectProperty(@Nullable final Function<ProfileRequestContext,T> lookupStrategy,
            @Nullable final T staticValue) {

        if (lookupStrategy != null) {
            final T prop = lookupStrategy.apply(getProfileRequestContext());
            if (prop != null) {
                return prop;
            }
        }

        return staticValue;
    }
    
}
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletRequest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for {@link ProfileConfiguration} implementations. */
public abstract class AbstractProfileConfiguration implements ProfileConfiguration {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractProfileConfiguration.class);

    /** Access to servlet request. */
    @Nullable private ServletRequest servletRequest;

    /** ID of the profile configured. */
    @Nonnull @NotEmpty private final String profileId;

    /** Lookup function to supply {@link #inboundFlows} property. */
    @Nullable private Function<ProfileRequestContext,List<String>> inboundFlowsLookupStrategy;

    /** Enables inbound interceptor flows. */
    @Nonnull @NonnullElements private List<String> inboundFlows;

    /** Lookup function to supply {@link #outboundFlows} property. */
    @Nullable private Function<ProfileRequestContext,List<String>> outboundFlowsLookupStrategy;

    /** Enables outbound interceptor flows. */
    @Nonnull @NonnullElements private List<String> outboundFlows;

    /** Lookup function to supply {@link #securityConfiguration} property. */
    @Nullable private Function<ProfileRequestContext,SecurityConfiguration> securityConfigurationLookupStrategy;

    /** The security configuration for this profile. */
    @Nullable private SecurityConfiguration securityConfiguration;

    /**
     * Constructor.
     * 
     * @param id ID of the communication profile, never null or empty
     */
    public AbstractProfileConfiguration(@Nonnull @NotEmpty final String id) {
        profileId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Profile identifier cannot be null or empty");
        inboundFlows = Collections.emptyList();
        outboundFlows = Collections.emptyList();
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
    @Override
    @Nonnull @NotEmpty public String getId() {
        return profileId;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public SecurityConfiguration getSecurityConfiguration() {
        return getIndirectProperty(securityConfigurationLookupStrategy, securityConfiguration);
    }

    /**
     * Sets the security configuration for this profile.
     * 
     * @param configuration security configuration for this profile
     */
    public void setSecurityConfiguration(@Nullable final SecurityConfiguration configuration) {
        securityConfiguration = configuration;
    }

    /**
     * Set a lookup strategy for the {@link #securityConfiguration} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setSecurityConfigurationLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SecurityConfiguration> strategy) {
        securityConfigurationLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getInboundInterceptorFlows() {
        return ImmutableList.copyOf(getIndirectProperty(inboundFlowsLookupStrategy, inboundFlows));
    }

    /**
     * Set the ordered collection of inbound interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setInboundInterceptorFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            inboundFlows = new ArrayList<>(StringSupport.normalizeStringCollection(flows));
        } else {
            inboundFlows = Collections.emptyList();
        }
    }

    /**
     * Set a lookup strategy for the {@link #inboundFlows} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setInboundFlowsLookupStrategy(@Nullable final Function<ProfileRequestContext,List<String>> strategy) {
        inboundFlowsLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getOutboundInterceptorFlows() {
        return ImmutableList.copyOf(getIndirectProperty(outboundFlowsLookupStrategy, outboundFlows));
    }

    /**
     * Set the ordered collection of outbound interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setOutboundInterceptorFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            outboundFlows = new ArrayList<>(StringSupport.normalizeStringCollection(flows));
        } else {
            outboundFlows = Collections.emptyList();
        }
    }

    /**
     * Set a lookup strategy for the {@link #outboundFlows} property.
     *
     * @param strategy  lookup strategy
     * 
     * @since 3.3.0
     */
    public void setOutboundFlowsLookupStrategy(@Nullable final Function<ProfileRequestContext,List<String>> strategy) {
        outboundFlowsLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return profileId.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
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
        return Objects.equals(profileId, other.getId());
    }

    /**
     * Get the current {@link ProfileRequestContext}.
     *
     * @return current profile request context
     * 
     * @since 3.3.0
     */
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
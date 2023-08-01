/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.profile.config.AbstractConditionalProfileConfiguration;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Base class for {@link InterceptorAwareProfileConfiguration} implementations.
 * 
 * @since 5.0.0
 */
public abstract class AbstractInterceptorAwareProfileConfiguration extends AbstractConditionalProfileConfiguration
        implements InterceptorAwareProfileConfiguration {
    
    /** Lookup function to supply inboundFlows property. */
    @Nonnull private Function<ProfileRequestContext,List<String>> inboundFlowsLookupStrategy;

    /** Lookup function to supply #outboundFlows property. */
    @Nonnull private Function<ProfileRequestContext,List<String>> outboundFlowsLookupStrategy;

    /**
     * Constructor.
     * 
     * @param id ID of the communication profile, never null or empty
     */
    public AbstractInterceptorAwareProfileConfiguration(@Nonnull @NotEmpty @ParameterName(name="id") final String id) {
        super(id);
        inboundFlowsLookupStrategy = FunctionSupport.constant(null);
        outboundFlowsLookupStrategy = FunctionSupport.constant(null);
    }

    /** {@inheritDoc} */
    @Nonnull @NotLive @Unmodifiable public List<String> getInboundInterceptorFlows(
        @Nullable final ProfileRequestContext profileRequestContext) {
        final List<String> flows = inboundFlowsLookupStrategy.apply(profileRequestContext);
        if (flows != null) {
            return CollectionSupport.copyToList(flows);
        }
        return CollectionSupport.emptyList();
    }

    /**
     * Set the ordered collection of inbound interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setInboundInterceptorFlows(@Nullable final Collection<String> flows) {
        if (flows != null) {
            inboundFlowsLookupStrategy =
                    FunctionSupport.constant(List.copyOf(StringSupport.normalizeStringCollection(flows)));
        } else {
            inboundFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the inbound interceptor flows to enable.
     *
     * @param strategy  lookup strategy
     * 
     * @since 4.2.0
     */
    public void setInboundInterceptorFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,List<String>> strategy) {
        inboundFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NotLive @Unmodifiable public List<String> getOutboundInterceptorFlows(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final List<String> flows = outboundFlowsLookupStrategy.apply(profileRequestContext);
        if (flows != null) {
            return CollectionSupport.copyToList(flows);
        }
        return CollectionSupport.emptyList();
    }

    /**
     * Set the ordered collection of outbound interceptor flows to enable.
     * 
     * @param flows   flow identifiers to enable
     */
    public void setOutboundInterceptorFlows(@Nullable final Collection<String> flows) {
        if (flows != null) {
            outboundFlowsLookupStrategy =
                    FunctionSupport.constant(List.copyOf(StringSupport.normalizeStringCollection(flows)));
        } else {
            outboundFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }
    
    /**
     * Set a lookup strategy for the outbound interceptor flows to enable.
     *
     * @param strategy  lookup strategy
     * 
     * @since 4.2.0
     */
    public void setOutboundInterceptorFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,List<String>> strategy) {
        outboundFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
}
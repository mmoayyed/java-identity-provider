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
package net.shibboleth.idp.saml.metadata.impl;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;
import org.opensaml.saml.metadata.resolver.ChainingMetadataResolver;
import org.opensaml.saml.metadata.resolver.ExtendedBatchMetadataResolver;
import org.opensaml.saml.metadata.resolver.ExtendedRefreshableMetadataResolver;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import net.shibboleth.idp.metrics.ReloadableServiceGaugeSet;
import net.shibboleth.idp.saml.metadata.RelyingPartyMetadataProvider;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Additional gauges for metadata resolvers.
 */
public class MetadataResolverServiceGaugeSet extends ReloadableServiceGaugeSet implements MetricSet, MetricFilter {
    
    /**
     * Constructor.
     * 
     * @param metricName name to include in metric names produced by this set
     */
    // Checkstyle: MethodLength OFF
    public MetadataResolverServiceGaugeSet(
            @Nonnull @NotEmpty @ParameterName(name="metricName") final String metricName) {
        super(metricName);
        
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "update"),
                new Gauge<Map<String,DateTime>>() {
                    public Map<String,DateTime> getValue() {
                        final Builder mapBuilder = ImmutableMap.<String,DateTime>builder();
                        final ServiceableComponent<MetadataResolver> component = getService().getServiceableComponent();
                        if (component != null) {
                            try {                                
                                for (final MetadataResolver resolver : getMetadataResolvers(component.getComponent())) {
                                    if (resolver instanceof RefreshableMetadataResolver 
                                            && ((RefreshableMetadataResolver) resolver).getLastUpdate() != null) {
                                        mapBuilder.put(resolver.getId(),
                                                ((RefreshableMetadataResolver) resolver).getLastUpdate());
                                    }
                                }
                            } finally {
                                component.unpinComponent();
                            }
                        }
                        return mapBuilder.build();
                    }
                });
        
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "refresh"),
                new Gauge<Map<String,DateTime>>() {
                    public Map<String,DateTime> getValue() {
                        final Builder mapBuilder = ImmutableMap.<String,DateTime>builder();
                        final ServiceableComponent<MetadataResolver> component = getService().getServiceableComponent();
                        if (component != null) {
                            try {                                
                                for (final MetadataResolver resolver : getMetadataResolvers(component.getComponent())) {
                                    if (resolver instanceof RefreshableMetadataResolver 
                                            && ((RefreshableMetadataResolver) resolver).getLastRefresh() != null) {
                                        mapBuilder.put(resolver.getId(),
                                                ((RefreshableMetadataResolver) resolver).getLastRefresh());
                                    }
                                }
                            } finally {
                                component.unpinComponent();
                            }
                        }
                        return mapBuilder.build();
                    }
                });
        
        //TODO v4.0.0 - Switch to use RefreshableMetadataResolver when new methods promoted up
        // Checkstyle: AnonInnerLength OFF
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "successfulRefresh"),
                new Gauge<Map<String,DateTime>>() {
                    public Map<String,DateTime> getValue() {
                        final Builder mapBuilder = ImmutableMap.<String,DateTime>builder();
                        final ServiceableComponent<MetadataResolver> component = getService().getServiceableComponent();
                        if (component != null) {
                            try {                                
                                for (final MetadataResolver resolver : getMetadataResolvers(component.getComponent())) {
                                    if (resolver instanceof ExtendedRefreshableMetadataResolver 
                                            && ((ExtendedRefreshableMetadataResolver) resolver)
                                                .getLastSuccessfulRefresh()  != null) {
                                        mapBuilder.put(resolver.getId(),
                                                ((ExtendedRefreshableMetadataResolver) resolver)
                                                    .getLastSuccessfulRefresh());
                                    }
                                }
                            } finally {
                                component.unpinComponent();
                            }
                        }
                        return mapBuilder.build();
                    }
                });
        // Checkstyle: AnonInnerLength ON
        
        //TODO v4.0.0 - Switch to use BatchMetadataResolver when new methods promoted up
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "rootValidUntil"),
                new Gauge<Map<String,DateTime>>() {
                    public Map<String,DateTime> getValue() {
                        final Builder mapBuilder = ImmutableMap.<String,DateTime>builder();
                        final ServiceableComponent<MetadataResolver> component = getService().getServiceableComponent();
                        if (component != null) {
                            try {                                
                                for (final MetadataResolver resolver : getMetadataResolvers(component.getComponent())) {
                                    if (resolver instanceof ExtendedBatchMetadataResolver 
                                            && ((ExtendedBatchMetadataResolver) resolver).getRootValidUntil() != null) {
                                        mapBuilder.put(resolver.getId(),
                                                ((ExtendedBatchMetadataResolver) resolver).getRootValidUntil());
                                    }
                                }
                            } finally {
                                component.unpinComponent();
                            }
                        }
                        return mapBuilder.build();
                    }
                });
    }
    // Checkstyle: MethodLength ON
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final ServiceableComponent component = getService().getServiceableComponent();
        if (component != null) {
            try {
                if (component instanceof MetadataResolver) {
                    return;
                }
            } finally {
                component.unpinComponent();
            }
        }

        throw new ComponentInitializationException("Injected service was null or not a MetadataResolver");
    }

    /**
     * Return the resolvers to report on.
     * 
     * @param rootResolver root component
     * 
     * @return resolvers to report on
     */
    @Nonnull @NonnullElements private Iterable<MetadataResolver> getMetadataResolvers(
            @Nonnull final MetadataResolver rootResolver) {
        
        MetadataResolver root = rootResolver;
        
        // Step down into wrapping component.
        if (root instanceof RelyingPartyMetadataProvider) {
            root = ((RelyingPartyMetadataProvider) root).getEmbeddedResolver();
        }
        
        if (root instanceof ChainingMetadataResolver) {
            return ((ChainingMetadataResolver) root).getResolvers();
        } else {
            return Collections.singletonList(root);
        }
    }

}
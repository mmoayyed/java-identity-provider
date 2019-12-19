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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;

import org.opensaml.saml.metadata.resolver.BatchMetadataResolver;
import org.opensaml.saml.metadata.resolver.ChainingMetadataResolver;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import net.shibboleth.idp.metrics.ReloadableServiceGaugeSet;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Additional gauges for metadata resolvers.
 */
public class MetadataResolverServiceGaugeSet extends ReloadableServiceGaugeSet<MetadataResolver>
    implements MetricSet, MetricFilter {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(MetadataResolverServiceGaugeSet.class);

// Checkstyle: MethodLength OFF
    /**
     * Constructor.
     * 
     * @param metricName name to include in metric names produced by this set
     */
    public MetadataResolverServiceGaugeSet(
            @Nonnull @NotEmpty @ParameterName(name="metricName") final String metricName) {
        super(metricName);

        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "update"),
                new Gauge<Map<String,Instant>>() {
                    public Map<String,Instant> getValue() {
                        return valueGetter(new BiConsumer<Builder<String,Instant>, MetadataResolver>() {
                            public void accept(final Builder<String,Instant> mapBuilder,
                                    final MetadataResolver resolver) {
                                if (resolver instanceof RefreshableMetadataResolver
                                        && ((RefreshableMetadataResolver) resolver).getLastUpdate() != null) {
                                    mapBuilder.put(resolver.getId(),
                                            ((RefreshableMetadataResolver) resolver).getLastUpdate());
                                }
                            };
                        });
                    }
                });
        
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "refresh"),
                new Gauge<Map<String,Instant>>() {
                    public Map<String,Instant> getValue() {
                        return valueGetter(new BiConsumer<Builder<String,Instant>, MetadataResolver>() {
                            public void accept(final Builder<String,Instant> mapBuilder,
                                    final MetadataResolver resolver) {
                                if (resolver instanceof RefreshableMetadataResolver
                                        && ((RefreshableMetadataResolver) resolver).getLastRefresh() != null) {
                                    mapBuilder.put(resolver.getId(),
                                            ((RefreshableMetadataResolver) resolver).getLastRefresh());
                                }
                            };
                        });
                    }
                });
                
        //TODO v4.0.0 - Switch to use RefreshableMetadataResolver when new methods promoted up
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "successfulRefresh"),
                new Gauge<Map<String,Instant>>() {
                    public Map<String,Instant> getValue() {
                        return valueGetter(new BiConsumer<Builder<String,Instant>, MetadataResolver>() {
                            public void accept(final Builder<String,Instant> mapBuilder,
                                    final MetadataResolver resolver) {
                                if (resolver instanceof RefreshableMetadataResolver
                                        && ((RefreshableMetadataResolver) resolver)
                                            .getLastSuccessfulRefresh()  != null) {
                                    mapBuilder.put(resolver.getId(),
                                            ((RefreshableMetadataResolver) resolver).getLastSuccessfulRefresh());
                                }
                            };
                        });
                    }
                });
        
        //TODO v4.0.0 - Switch to use BatchMetadataResolver when new methods promoted up
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "rootValidUntil"),
                new Gauge<Map<String,Instant>>() {
                    public Map<String,Instant> getValue() {
                        return valueGetter(new BiConsumer<Builder<String,Instant>, MetadataResolver>() {
                            public void accept(final Builder<String,Instant> mapBuilder,
                                    final MetadataResolver resolver) {
                                if (resolver instanceof BatchMetadataResolver
                                        && ((BatchMetadataResolver) resolver).getRootValidUntil() != null) {
                                    mapBuilder.put(resolver.getId(),
                                            ((BatchMetadataResolver) resolver).getRootValidUntil());
                                }
                            };
                        });
                    }
                });
    }
// Checkstyle: MethodLength ON

    /**
     * Helper Function for map construction.
     * 
     * <p>
     * This does all the service handling and just calls the specific {@link BiConsumer} to
     * add each appropriate the value to the map.
     * </p>
     * 
     * @param consume the thing which does checking and adding the building
     * @return an appropriate map
     */
    private Map<String,Instant> valueGetter(final BiConsumer<Builder<String,Instant>, MetadataResolver> consume) {
        final Builder<String,Instant> mapBuilder = ImmutableMap.builder();
        final ServiceableComponent<?> component = getService().getServiceableComponent();
        if (component != null) {
            try {
                // Check type - just in case
                if (!(component.getComponent() instanceof MetadataResolver)) {
                    log.warn("{} : Injected Service was not for an Metadata Resolver : ({}) ",
                            getLogPrefix(), component.getComponent().getClass());
                } else {
                    for (final MetadataResolver resolver : getMetadataResolvers(
                            (MetadataResolver) component.getComponent())) {
                        consume.accept(mapBuilder, resolver);
                    }
                }
            } finally {
                component.unpinComponent();
            }
        }
        return mapBuilder.build();
    }

    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final ServiceableComponent<?> component = getService().getServiceableComponent();
        if (component != null) {
            try {
                if (component.getComponent() instanceof MetadataResolver) {
                    return;
                }
                log.error("{} : Injected service was not for a MetadataResolver ({}) ",
                        getLogPrefix(), component.getClass());
                throw new ComponentInitializationException("Injected service was not for a MetadataResolver");
            } finally {
                component.unpinComponent();
            }
        }
        log.debug("{} : Injected service has not initialized sucessfully yet. Skipping type test",
                getLogPrefix());
    }

    /** Get all the resolvers rooted in the provider tree (including the root).
     * @param parent - root of the chaining resolver tree.
     * @return - the list.
     */
    private List<MetadataResolver> getAllChildren(final ChainingMetadataResolver parent) {
        final ArrayList<MetadataResolver> result = new ArrayList<>(1+ parent.getResolvers().size());
        // Add ourselves
        result.add(parent);
        for (final MetadataResolver child: parent.getResolvers()) {
            if (child instanceof ChainingMetadataResolver) {
                result.addAll(getAllChildren((ChainingMetadataResolver) child));
            } else {
                result.add(child);
            }
        }
        return result;
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
        
        if (rootResolver instanceof ChainingMetadataResolver) {
            return getAllChildren((ChainingMetadataResolver) rootResolver);
        }
        return Collections.singletonList(rootResolver);
    }

}
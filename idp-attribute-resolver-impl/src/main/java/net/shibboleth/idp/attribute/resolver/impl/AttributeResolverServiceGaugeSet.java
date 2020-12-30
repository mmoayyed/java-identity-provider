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
package net.shibboleth.idp.attribute.resolver.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.metrics.ReloadableServiceGaugeSet;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Additional gauges for attribute resolver.
 */
public class AttributeResolverServiceGaugeSet extends ReloadableServiceGaugeSet<AttributeResolver>
                        implements MetricSet, MetricFilter {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeResolverServiceGaugeSet.class);

    /**
     * Constructor.
     * 
     * @param metricName name to include in metric names produced by this set
     */
// Checkstyle: AnonInnerLength|MethodLength OFF
    public AttributeResolverServiceGaugeSet(
            @Nonnull @NotEmpty @ParameterName(name="metricName") final String metricName) {
        super(metricName);
        
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "success"),
                new Gauge<Map<String,Instant>>() {
                    public Map<String,Instant> getValue() {
                        final Map<String,Instant> mapBuilder = new HashMap<>();
                        final ServiceableComponent<AttributeResolver> component =
                                getService().getServiceableComponent();
                        if (component != null) {
                            try {                                
                                final Object resolver = component.getComponent();
                                if (resolver instanceof AttributeResolverImpl) {
                                    final Collection<DataConnector> connectors =
                                            ((AttributeResolverImpl) resolver).getDataConnectors().values();
                                    for (final DataConnector connector: connectors) {
                                        if (connector.getLastSuccess() != null) {
                                            mapBuilder.put(connector.getId(), connector.getLastSuccess());
                                        }
                                    }
                                } else if (resolver instanceof AttributeResolver) {
                                   log.debug("{}: Cannot get Data Connector success " +
                                           " information from unsupported class type {}",
                                           getLogPrefix(), resolver.getClass());
                                } else {
                                    log.warn("{}: Injected Service was not for an AttributeResolver ({})",
                                            getLogPrefix(), resolver.getClass());
                                }
                            } finally {
                                component.unpinComponent();
                            }
                        }
                        return Map.copyOf(mapBuilder);
                    }
                });

        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "failure"),
                new Gauge<Map<String,Instant>>() {
                    public Map<String,Instant> getValue() {
                        final Map<String,Instant> mapBuilder = new HashMap<>();
                        final ServiceableComponent<AttributeResolver> component =
                                getService().getServiceableComponent();
                        if (component != null) {
                            try {                                
                                final Object resolver = component.getComponent();
                                if (resolver instanceof AttributeResolverImpl) {
                                    final Collection<DataConnector> connectors =
                                            ((AttributeResolverImpl) resolver).getDataConnectors().values();
                                    for (final DataConnector connector: connectors) {
                                        if (connector.getLastFail() != null) {
                                            mapBuilder.put(connector.getId(), connector.getLastFail());
                                        }
                                    }
                                } else if (resolver instanceof AttributeResolver) {
                                   log.debug("{}: Cannot get Data Connector failure " +
                                           " information from unsupported class type {}",
                                           getLogPrefix(), resolver.getClass());
                                } else {
                                    log.warn("{}: Injected Service was not for an AttributeResolver ({})",
                                            getLogPrefix(), resolver.getClass());
                                }
                            } finally {
                                component.unpinComponent();
                            }
                        }
                        return Map.copyOf(mapBuilder);
                    }
                });
        
    }
// Checkstyle: AnonInnerLength|MethodLength ON

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final ServiceableComponent<?> component = getService().getServiceableComponent();
        if (component != null) {
            try {
                if (component.getComponent() instanceof AttributeResolver) {
                    return;
                }
                log.error("{}: Injected service was not for an AttributeResolver ({})",
                        getLogPrefix(), component.getClass());
                throw new ComponentInitializationException("Injected service was not for an AttributeResolver");
            } finally {
                component.unpinComponent();
            }
        }
    }

}
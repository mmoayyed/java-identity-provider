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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.DataConnectorEx;
import net.shibboleth.idp.metrics.ReloadableServiceGaugeSet;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;

/**
 * Additional gauges for attribute resolver.
 */
public class AttributeResolverServiceGaugeSet extends ReloadableServiceGaugeSet implements MetricSet, MetricFilter {
    
    /**
     * Constructor.
     * 
     * @param metricName name to include in metric names produced by this set
     */
    public AttributeResolverServiceGaugeSet(
            @Nonnull @NotEmpty @ParameterName(name="metricName") final String metricName) {
        super(metricName);
        
// Checkstyle: AnonInnerLength OFF
        getMetricMap().put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "failure"),
                new Gauge<Map<String,DateTime>>() {
                    public Map<String,DateTime> getValue() {
                        final Builder mapBuilder = ImmutableMap.<String,DateTime>builder();
                        final ServiceableComponent<AttributeResolver> component =
                                getService().getServiceableComponent();
                        if (component != null) {
                            try {                                
                                final AttributeResolver resolver = component.getComponent();
                                final Collection<DataConnector> connectors = resolver.getDataConnectors().values();
                                
                                for (final DataConnector connector: connectors) {
                                    if (connector instanceof DataConnectorEx) {
                                        final long lastFail = ((DataConnectorEx) connector).getLastFail();
                                        if (lastFail > 0) {
                                            mapBuilder.put(connector.getId(), new DateTime(lastFail));
                                        }
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
        
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        final ServiceableComponent component = getService().getServiceableComponent();
        if (component != null) {
            try {
                if (component instanceof AttributeResolver) {
                    return;
                }
            } finally {
                component.unpinComponent();
            }
        }

        throw new ComponentInitializationException("Injected service was null or not an AttributeResolver");
    }

}
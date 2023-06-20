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
package net.shibboleth.idp.metrics.impl;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import net.shibboleth.idp.Version;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

/**
 * A set of gauges for core system information.
 */
public class IdPGaugeSet extends ApplicationObjectSupport implements MetricSet, MetricFilter {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp";

    /** The map of gauges. */
    @Nonnull private final Map<String,Metric> gauges;
    
    /** Constructor. */
    public IdPGaugeSet() {
        gauges = new HashMap<>();
        
        gauges.put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, "version"),
                new Gauge<String>() {
                    public String getValue() {
                        return Version.getVersion();
                    }
                });

        gauges.put(
                "org.opensaml.version",
                new Gauge<String>() {
                    public String getValue() {
                        return org.opensaml.core.Version.getVersion();
                    }
                });

        gauges.put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, "starttime"),
                new Gauge<Instant>() {
                    public Instant getValue() {
                        final ApplicationContext context = getApplicationContext();
                        assert context != null;
                        return Instant.ofEpochMilli(context.getStartupDate());
                    }
                });
        
        gauges.put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, "uptime"),
                new Gauge<Duration>() {
                    public Duration getValue() {
                        final ApplicationContext context = getApplicationContext();
                        assert context != null;
                        return Duration.ofMillis(
                                Instant.now().toEpochMilli() - context.getStartupDate());
                    }
                });
    }
    
    /**
     * Set the names of properties to expose as metrics.
     * 
     * @param properties properties to expose
     */
    public void setExposedProperties(@Nullable final Set<String> properties) {
        if (properties != null) {
            final ApplicationContext context = getApplicationContext();
            assert context != null;
            for (final String property : properties) {
                assert property != null;
                gauges.put(
                        MetricRegistry.name(DEFAULT_METRIC_NAME, "properties", property),
                        new Gauge<String>() {
                            public String getValue() {
                                return context.getEnvironment().getProperty(property);
                            }
                        });
            }
        }
    }
    
    /** {@inheritDoc} */
    public Map<String,Metric> getMetrics() {
        return Map.copyOf(gauges);
    }

    /** {@inheritDoc} */
    public boolean matches(final String name, final Metric metric) {
        return gauges.containsKey(name);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isContextRequired() {
        return true;
    }

}
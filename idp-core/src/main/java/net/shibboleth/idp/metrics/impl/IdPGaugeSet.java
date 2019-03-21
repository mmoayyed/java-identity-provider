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
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.context.support.ApplicationObjectSupport;

/**
 * A set of gauges for core system information.
 */
public class IdPGaugeSet extends ApplicationObjectSupport implements MetricSet, MetricFilter {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp";

    /** The map of gauges. */
    @Nonnull @NonnullElements private final Map<String,Metric> gauges;
    
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
                        return Instant.ofEpochMilli(getApplicationContext().getStartupDate());
                    }
                });
        
        gauges.put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, "uptime"),
                new Gauge<Duration>() {
                    public Duration getValue() {
                        return Duration.ofMillis(
                                Instant.now().toEpochMilli() - getApplicationContext().getStartupDate());
                    }
                });
    }
    
    /** {@inheritDoc} */
    public Map<String,Metric> getMetrics() {
        return Collections.unmodifiableMap(gauges);
    }

    /** {@inheritDoc} */
    public boolean matches(final String name, final Metric metric) {
        return gauges.containsKey(name);
    }
    
}
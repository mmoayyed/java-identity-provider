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
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * A set of gauges for core system information.
 */
public class CoreGaugeSet implements MetricSet, MetricFilter {

    /** The map of gauges. */
    @Nonnull @NonnullElements private final Map<String,Metric> gauges;
    
// Checkstyle: MethodLength OFF    
    /** Constructor. */
    public CoreGaugeSet() {
        gauges = new HashMap<>();
        
        gauges.put(
                "os.name",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("os.name");
                    }
                });
        
        gauges.put(
                "os.version",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("os.version");
                    }
                });

        gauges.put(
                "os.arch",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("os.arch");
                    }
                });

        gauges.put(
                "java.class.path",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("java.class.path");
                    }
                });

        gauges.put(
                "java.home",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("java.home");
                    }
                });

        gauges.put(
                "java.vendor",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("java.vendor");
                    }
                });

        gauges.put(
                "java.vendor.url",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("java.vendor.url");
                    }
                });
                
        gauges.put(
                "java.version",
                new Gauge<String>() {
                    public String getValue() {
                        return System.getProperty("java.version");
                    }
                });

        gauges.put(
                "cores.available",
                new Gauge<Integer>() {
                    public Integer getValue() {
                        return Runtime.getRuntime().availableProcessors();
                    }
                });

        gauges.put(
                "memory.free.bytes",
                new Gauge<Long>() {
                    public Long getValue() {
                        return Runtime.getRuntime().freeMemory();
                    }
                });

        gauges.put(
                "memory.free.megs",
                new Gauge<Long>() {
                    public Long getValue() {
                        return Runtime.getRuntime().freeMemory() / (1024 * 1024);
                    }
                });

        gauges.put(
                "memory.used.bytes",
                new Gauge<Long>() {
                    public Long getValue() {
                        final Runtime runtime = Runtime.getRuntime();
                        return runtime.totalMemory() - runtime.freeMemory();
                    }
                });

        gauges.put(
                "memory.used.megs",
                new Gauge<Long>() {
                    public Long getValue() {
                        final Runtime runtime = Runtime.getRuntime();
                        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
                    }
                });

        gauges.put(
                "memory.usage",
                new RatioGauge() {
                    protected Ratio getRatio() {
                        final Runtime runtime = Runtime.getRuntime();
                        return Ratio.of(runtime.totalMemory() - runtime.freeMemory(), runtime.totalMemory());
                    }
                });
    }
// Checkstyle: MethodLength ON
    
    /** {@inheritDoc} */
    public Map<String,Metric> getMetrics() {
        return Collections.unmodifiableMap(gauges);
    }

    /** {@inheritDoc} */
    public boolean matches(final String name, final Metric metric) {
        return gauges.containsKey(name);
    }
    
}
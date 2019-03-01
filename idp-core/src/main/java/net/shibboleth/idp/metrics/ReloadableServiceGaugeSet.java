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
package net.shibboleth.idp.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * A set of gauges for a reloadable service.
 */
public class ReloadableServiceGaugeSet extends AbstractInitializableComponent implements MetricSet, MetricFilter {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty protected static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp";

    /** The map of gauges. */
    @Nonnull @NonnullElements private final Map<String,Metric> gauges;
    
    /** The service to report on. */
    @NonnullAfterInit private ReloadableService service;
    
    /**
     * Constructor.
     * 
     * @param metricName name to include in metric names produced by this set
     */
    public ReloadableServiceGaugeSet(@Nonnull @NotEmpty @ParameterName(name="metricName") final String metricName) {
        Constraint.isNotEmpty(metricName, "Metric name cannot be null or empty");
        
        gauges = new HashMap<>();

        gauges.put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "reload", "success"),
                new Gauge<Instant>() {
                    public Instant getValue() {
                        return service.getLastSuccessfulReloadInstant();
                    }
                });
        
        gauges.put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "reload", "attempt"),
                new Gauge<Instant>() {
                    public Instant getValue() {
                        return service.getLastReloadAttemptInstant();
                    }
                });

        gauges.put(
                MetricRegistry.name(DEFAULT_METRIC_NAME, metricName, "reload", "error"),
                new Gauge<String>() {
                    public String getValue() {
                        return service.getReloadFailureCause() != null
                                ? service.getReloadFailureCause().getMessage() : null;
                    }
                });
    }

    /**
     * Get the service to report on.
     * 
     * @return service to report on
     */
    @NonnullAfterInit public ReloadableService getService() {
        return service;
    }
    
    /**
     * Set the service to report on.
     * 
     * @param svc service instance
     */
    public void setService(@Nonnull final ReloadableService svc) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        service = Constraint.isNotNull(svc, "ReloadableService cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (service == null) {
            throw new ComponentInitializationException("ReloadableService cannot be null");
        }
    }

    /** {@inheritDoc} */
    public Map<String,Metric> getMetrics() {
        return Collections.unmodifiableMap(gauges);
    }

    /** {@inheritDoc} */
    public boolean matches(final String name, final Metric metric) {
        return gauges.containsKey(name);
    }
    
    /**
     * Get the underlying map of metrics.
     * 
     * @return map of metrics
     */
    @Nonnull @NonnullElements @Live protected Map<String,Metric> getMetricMap() {
        return gauges;
    }
}
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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import net.shibboleth.idp.metrics.impl.DisabledCounter;
import net.shibboleth.idp.metrics.impl.DisabledHistogram;
import net.shibboleth.idp.metrics.impl.DisabledMeter;
import net.shibboleth.idp.metrics.impl.DisabledTimer;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * {@link MetricRegistry} that returns a metric or a disabled wrapper for a metric based
 * on a supplied {@link MetricFilter}.
 * 
 * @since 3.3.0
 */
public class FilteredMetricRegistry extends MetricRegistry implements InitializableComponent {

    /** Init flag. */
    private boolean initialized;
    
    /** Filter to apply. */
    @Nonnull private MetricFilter metricFilter;
    
    /** Dummy object. */
    @Nonnull private final DisabledCounter disabledCounter;

    /** Dummy object. */
    @Nonnull private final DisabledHistogram disabledHistogram;

    /** Dummy object. */
    @Nonnull private final DisabledMeter disabledMeter;

    /** Dummy object. */
    @Nonnull private final DisabledTimer disabledTimer;

    /**
     * Constructor.
     */
    public FilteredMetricRegistry() {
        disabledCounter = new DisabledCounter();
        disabledHistogram = new DisabledHistogram();
        disabledMeter = new DisabledMeter();
        disabledTimer = new DisabledTimer();
    }
    
    /**
     * Set the filter to use.
     * 
     * @param filter filter to apply, if any
     */
    public void setMetricFilter(@Nullable final MetricFilter filter) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metricFilter = filter;
    }

    /** {@inheritDoc} */
    @Override public Counter counter(final String name) {
        if (metricFilter.matches(name, null)) {
            return super.counter(name);
        } else {
            return disabledCounter;
        }
    }

    /** {@inheritDoc} */
    @Override public Histogram histogram(final String name) {
        if (metricFilter.matches(name, null)) {
            return super.histogram(name);
        } else {
            return disabledHistogram;
        }
    }

    /** {@inheritDoc} */
    @Override public Meter meter(final String name) {
        if (metricFilter.matches(name, null)) {
            return super.meter(name);
        } else {
            return disabledMeter;
        }
    }

    /** {@inheritDoc} */
    @Override public Timer timer(final String name) {
        if (metricFilter.matches(name, null)) {
            return super.timer(name);
        } else {
            return disabledTimer;
        }
    }

    /**
     * Given multiple metric sets, registers them.
     *
     * @param metricSets any number of metric sets
     * 
     * @throws IllegalArgumentException if any of the names are already registered
     */
    public void registerMultiple(@Nonnull @NonnullElements final Collection<MetricSet> metricSets)
            throws IllegalArgumentException {
        Constraint.isNotNull(metricSets, "Collection cannot be null");
        
        for (final MetricSet set : Collections2.filter(metricSets, Predicates.notNull())) {
            registerAll(set);
        }
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return initialized;
    }

    /** {@inheritDoc} */
    public void initialize() throws ComponentInitializationException {
        if (!initialized) {
            initialized = true;
            if (metricFilter == null) {
                metricFilter = MetricFilter.ALL;
            }
        }
    }
    
}
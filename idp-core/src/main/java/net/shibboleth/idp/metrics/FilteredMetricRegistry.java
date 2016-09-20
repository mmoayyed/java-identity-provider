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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * {@link MetricRegistry} that returns a metric or a disabled wrapper for a metric based
 * on a supplied {@link MetricFilter}.
 * 
 * @since 3.3.0
 */
public class FilteredMetricRegistry extends MetricRegistry {

    /** Filter to apply. */
    @Nonnull private final MetricFilter metricFilter;
    
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
     *
     * @param filter filter to apply, if any
     */
    public FilteredMetricRegistry(@Nullable final MetricFilter filter) {
        metricFilter = filter != null ? filter : MetricFilter.ALL;
        
        disabledCounter = new DisabledCounter();
        disabledHistogram = new DisabledHistogram();
        disabledMeter = new DisabledMeter();
        disabledTimer = new DisabledTimer();
    }

    /** {@inheritDoc} */
    @Override public Counter counter(String name) {
        if (metricFilter.matches(name, null)) {
            return super.counter(name);
        } else {
            return disabledCounter;
        }
    }

    /** {@inheritDoc} */
    @Override public Histogram histogram(String name) {
        if (metricFilter.matches(name, null)) {
            return super.histogram(name);
        } else {
            return disabledHistogram;
        }
    }

    /** {@inheritDoc} */
    @Override public Meter meter(String name) {
        if (metricFilter.matches(name, null)) {
            return super.meter(name);
        } else {
            return disabledMeter;
        }
    }

    /** {@inheritDoc} */
    @Override public Timer timer(String name) {
        if (metricFilter.matches(name, null)) {
            return super.timer(name);
        } else {
            return disabledTimer;
        }
    }

}
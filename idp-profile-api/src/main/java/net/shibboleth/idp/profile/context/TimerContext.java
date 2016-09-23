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

package net.shibboleth.idp.profile.context;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import net.shibboleth.idp.metrics.MetricsSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Child context that supplies instructions to the runtime actions
 * about timers to start and stop to measure performance.
 */
public class TimerContext extends BaseContext {
    
    /** Name of metric registry to access. */
    @Nullable @NotEmpty private String metricRegistryName;
    
    /**
     * Map of objects to timer names to start and objects to stop the timer.
     * 
     * <p>The first member is the timer name, the second the object to associate with the timer.</p>
     */
    @Nonnull @NonnullElements private Multimap<String,Pair<String,String>> timerMap;
    
    /** Map of objects to contexts to perform a stop signal. */
    @Nonnull @NonnullElements private Multimap<String,Timer.Context> timerContextMap;
    
    /** Constructor. */
    public TimerContext() {
        timerMap = ArrayListMultimap.create();
        timerContextMap = ArrayListMultimap.create();
    }
    
    /**
     * Set the name of the metric registry from which to retrieve timers.
     * 
     * @param name registry name
     * 
     * @return this context
     */
    @Nonnull public TimerContext setMetricRegistryName(@Nullable @NotEmpty final String name) {
        metricRegistryName = StringSupport.trimOrNull(name);
        
        return this;
    }
    
    /**
     * Add an object/timer mapping.
     * 
     * @param timerName name of timer
     * @param startId ID of object to start timer with
     * @param stopId ID of object to stop timer
     * 
     * @return this context
     */
    @Nonnull public TimerContext addTimer(@Nonnull @NotEmpty final String timerName,
            @Nonnull @NotEmpty final String startId, @Nonnull @NotEmpty final String stopId) {
        
        final String key = Constraint.isNotNull(StringSupport.trimOrNull(startId),
                "Starting object ID cannot be null or empty");
        final String stop = Constraint.isNotNull(StringSupport.trimOrNull(stopId),
                "Stop object ID cannot be null or empty");
        final String name = Constraint.isNotNull(StringSupport.trimOrNull(timerName),
                "Timer name cannot be null or empty");
        timerMap.put(key, new Pair<>(name, stop));
        
        return this;
    }
    
    /**
     * Get a modifiable collection of timer name / stop object pairs for the supplied
     * start object ID.
     * 
     * @param objectId the object ID input
     * 
     * @return the collection of associated mappings
     */
    @Nonnull @NonnullElements @Live public Collection<Pair<String,String>> getTimerMappings(
            @Nonnull @NotEmpty final String objectId) {
        return timerMap.get(objectId);
    }
    
    /**
     * Conditionally starts one or more timers based on the supplied object identifier.
     * 
     *  <p>The configured state of the context is used to determine whether, and which,
     *  timers to start, further influenced by the runtime state of the system with regard
     *  to enabling of metrics.</p>
     * 
     * @param objectId ID of the object being timed
     */
    public void start(@Nonnull @NotEmpty final String objectId) {
        
        for (final Pair<String,String> timer : timerMap.get(objectId)) {
            if (timer != null) {
                final MetricRegistry reg = metricRegistryName != null
                        ? MetricsSupport.getNamedMetricRegistry(metricRegistryName) :
                            MetricsSupport.getDefaultMetricRegistry();
                timerContextMap.put(timer.getSecond(), reg.timer(timer.getFirst()).time());
            }
        }
    }

    /**
     * Stops any timers associated with the supplied object identifier and removes them
     * from the tracking map.
     * 
     * @param objectId ID of the object being timed
     */
    public void stop(@Nonnull @NotEmpty final String objectId) {
        final Iterator<Timer.Context> iter = timerContextMap.get(objectId).iterator();
        while (iter.hasNext()) {
            final Timer.Context tc = iter.next();
            if (tc != null) {
                tc.stop();
                iter.remove();
            }
        }
    }
    
}
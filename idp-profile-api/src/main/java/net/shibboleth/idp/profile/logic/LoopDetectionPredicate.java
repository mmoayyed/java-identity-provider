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

package net.shibboleth.idp.profile.logic;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.metrics.MetricsSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.SlidingTimeWindowMovingAverages;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

/**
 * A condition that relies on a {@link Meter} to detect looping SPs. 
 *
 * @since 4.1.0
 */
public class LoopDetectionPredicate extends AbstractRelyingPartyPredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LoopDetectionPredicate.class);

    /** Count to trigger warning. */
    private long threshold;
    
    /** Map of RP names to meter names. */
    @Nonnull @NonnullElements private Map<String,String> relyingPartyMap;
    
    /** Lookup strategy to obtain subject name. */
    @Nonnull private Function<ProfileRequestContext,String> usernameLookupStrategy;
    
    /** Constructor. */
    public LoopDetectionPredicate() {
        threshold = 20;
        relyingPartyMap = Collections.emptyMap();
        usernameLookupStrategy = FunctionSupport.constant(null);
    }
    
    /**
     * Set the warning threshold for the 1 minute moving average to exceed.
     * 
     * <p>Defaults to 20.</p>
     * 
     * @param value threshold to use
     */
    public void setThreshold(@Positive final long value) {
        threshold = Constraint.isGreaterThan(0, value, "Threshold must be positive");
    }
    
    /**
     * Set the map of relying party names to meter names to track counts.
     * 
     * @param map map of RP/meter mappings
     */
    public void setRelyingPartyMap(@Nullable @NonnullElements final Map<String,String> map) {
        if (map != null) {
            relyingPartyMap = Map.copyOf(map);
        } else {
            relyingPartyMap = Collections.emptyMap();
        }
    }
    
    /**
     * Set lookup strategy to obtain username.
     * 
     * @param strategy lookup strategy
     */
    public void setUsernameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        usernameLookupStrategy = Constraint.isNotNull(strategy, "Username lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        final String username = usernameLookupStrategy.apply(input);
        final RelyingPartyContext rpCtx = getRelyingPartyContextLookupStrategy().apply(input);
        
        if (username != null && rpCtx != null && rpCtx.getRelyingPartyId() != null) {
            String meterName = relyingPartyMap.get(rpCtx.getRelyingPartyId());
            if (meterName != null) {
                meterName = MetricRegistry.name("net.shibboleth.idp.loopDetection", meterName,
                        username.replace(".",""));
                final Meter meter = MetricsSupport.getMetricRegistry().meter(meterName,
                        new MetricSupplier<Meter>() {
                            public Meter newMetric() {
                                return new Meter(new SlidingTimeWindowMovingAverages());
                            }
                        });
                meter.mark();
                final double rate = meter.getOneMinuteRate();
                if (rate > threshold) {
                    log.warn("Meter {} rate of {} exceeded threshold of {}", meterName, rate, threshold);
                    return true;
                }
            }
        }
        
        return false;
    }

}
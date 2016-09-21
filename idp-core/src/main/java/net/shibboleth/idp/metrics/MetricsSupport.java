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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Support code for use of metrics.
 * 
 * @since 3.3.0
 */
public final class MetricsSupport {

    /** Default metric registry name for the IdP. */
    @Nonnull @NotEmpty public static final String DEFAULT_METRIC_REGISTRY = "net.shibboleth.idp";
    
    /**
     * Get the default metric registry to use.
     * 
     * @return default registry
     */
    @Nonnull public static MetricRegistry getDefaultMetricRegistry() {
        return SharedMetricRegistries.getOrCreate(DEFAULT_METRIC_REGISTRY);
    }
    
    /**
     * Get a named metric registry to use.
     * 
     * @param name registry name
     * 
     * @return named registry
     */
    @Nonnull public static MetricRegistry getNamedMetricRegistry(@Nonnull @NotEmpty final String name) {
        return SharedMetricRegistries.getOrCreate(name);
    }
    
    /**
     * Private constructor.
     */
    private MetricsSupport() {
        
    }

}
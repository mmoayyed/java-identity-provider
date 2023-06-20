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

package net.shibboleth.idp.module.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.Nonnull;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.profile.module.ModuleContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Guage set to report the Modules' statuses.
 */
public class ModuleGuageSet extends ApplicationObjectSupport implements MetricSet, MetricFilter {
    
    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.modules";

    /** The map of gauges. */
    @Nonnull private final Map<String,Metric> gauges = new HashMap<>();
    
    /** Constructor. */
    public ModuleGuageSet() {
        gauges.put(MetricRegistry.name(DEFAULT_METRIC_NAME, "list"),
                new Gauge<Map<String, Boolean>>() {
                    public Map<String, Boolean> getValue() {
                        return getModules();
                    }
                });
    }

    /**
     * Return the module Ids and whether rhey are enabled or not.
     * 
     * @return the modules
     */
    @Nonnull @Unmodifiable @NotLive private Map<String, Boolean> getModules() {
        final Map<String, Boolean> result = new HashMap<>();
        final Iterator<IdPModule> modules = ServiceLoader.load(IdPModule.class).iterator();
        final ModuleContext mc = new ModuleContext(getIdpHome());
        while (modules.hasNext()) {
            final IdPModule module = modules.next();
            result.put(module.getId(), module.isEnabled(mc));
        }
        return CollectionSupport.copyToMap(result);
    }

    /**
     * Get the idp home location (from the properties in the context).
     * 
     * @return idp home
     */
    @Nonnull private String getIdpHome() {
        final ApplicationContext context = getApplicationContext();
        assert context != null;
        return Constraint.isNotNull(StringSupport.trimOrNull(
                context.getEnvironment().getProperty("idp.home")), "idp.home is not available");
    }

    /** {@inheritDoc} */
    public Map<String, Metric> getMetrics() {
        return gauges;
    }
    
    /** {@inheritDoc} */
    protected boolean isContextRequired() {
        return true;
    }

    /** {@inheritDoc} */
    public boolean matches(final String name, final Metric metric) {
        return gauges.containsKey(name);
    }
    
}
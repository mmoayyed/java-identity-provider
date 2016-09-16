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

package net.shibboleth.idp.profile.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;

/**
 * Action that outputs one or more {@link Metric} objects.
 * 
 * <p>On success, a 200 HTTP status is returned. On failure, a non-successful HTTP status is returned.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 */
public class OutputMetrics extends AbstractProfileAction {

    /** Constant representing all metrics. */
    @Nonnull @NotEmpty public static final String ALL_METRICS = "all";

    /** Flow variable indicating ID of metric or group of metrics to output. */
    @Nonnull @NotEmpty public static final String METRIC_ID = "metricId";
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(OutputMetrics.class);
        
    /** The metric registry. */
    @NonnullAfterInit private MetricRegistry metricRegistry;

    /** Value for Access-Control-Allow-Origin header, if any. */
    @Nullable private String allowedOrigin;
    
    /** Name of JSONP callback function, if any. */
    @Nullable private String jsonpCallbackName;

    /** Map of custom metric groups to filters. */
    @Nonnull @NonnullElements private Map<String,MetricFilter> metricFilterMap;
    
    /** Metric ID to operate on. */
    @Nullable private String metricId;
    
    /** Constructor. */
    public OutputMetrics() {
        metricFilterMap = Collections.emptyMap();
    }

    /**
     * Set the registry of metrics.
     * 
     * @param registry metric registry
     */
    public void setMetricRegistry(@Nonnull final MetricRegistry registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metricRegistry = Constraint.isNotNull(registry, "MetricRegistry cannot be null");
    }
    
    /**
     * Set the value of the Access-Control-Allow-Origin CORS header, if any.
     * 
     * @param origin header value
     */
    public void setAllowedOrigin(@Nullable final String origin) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        allowedOrigin = StringSupport.trimOrNull(origin);
    }
    
    /**
     * Set a JSONP callback function to wrap the result in, if any.
     * 
     * @param callbackName callback function name.
     */
    public void setJSONPCallbackName(@Nullable final String callbackName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        jsonpCallbackName = StringSupport.trimOrNull(callbackName);
    }
    
    /**
     * Set the map of custom group names to metric filters.
     * 
     * @param map group to filter map
     */
    public void setMetricFilterMap(@Nonnull @NonnullElements final Map<String,MetricFilter> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        Constraint.isNotNull(map, "MetricFilter map cannot be null");
        metricFilterMap = new HashMap<>(map.size());
        for (Map.Entry<String,MetricFilter> entry : map.entrySet()) {
            final String trimmed = StringSupport.trimOrNull(entry.getKey());
            if (trimmed != null && entry.getValue() != null) {
                metricFilterMap.put(trimmed, entry.getValue());
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (metricRegistry == null) {
            throw new ComponentInitializationException("MetricRegistry cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        } else if (getHttpServletResponse() == null) {
            log.debug("{} No HttpServletResponse available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        final SpringRequestContext springRequestContext =
                profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestContext == null) {
            log.warn("{} Spring request context not found in profile request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        final RequestContext requestContext = springRequestContext.getRequestContext();
        if (requestContext == null) {
            log.warn("{} Web Flow request context not found in Spring request context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        metricId = (String) requestContext.getFlowScope().get(METRIC_ID);
        if (metricId == null) {
            log.warn("{} No {} flow variable found in request", getLogPrefix(), METRIC_ID);
            try {
                getHttpServletResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (final IOException e) {
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
            return false;
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(ProfileRequestContext profileRequestContext) {
        
        MetricFilter filter = ALL_METRICS.equals(metricId) ? MetricFilter.ALL : metricFilterMap.get(metricId);
        if (filter == null) {
            // Use a filter matching one metric. 
            filter = new MetricFilter() {
                public boolean matches(String name, Metric metric) {
                    return name.equals(metricId);
                }
            };
        }
        
        try {
            final HttpServletResponse response = getHttpServletResponse();
            
            response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
            response.setStatus(HttpServletResponse.SC_OK);
            if (allowedOrigin != null) {
                response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
            }
            
            final ObjectMapper mapper = new ObjectMapper().registerModule(
                    new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, true, filter));
            if (jsonpCallbackName != null) {
                response.setContentType("application/javascript");
                mapper.writer().writeValue(response.getOutputStream(),
                        new JSONPObject(jsonpCallbackName, metricRegistry));
            } else {
                response.setContentType("application/json");
                mapper.writer().writeValue(response.getOutputStream(), metricRegistry);
            }
        } catch (final IOException e) {
            log.error("{} I/O error responding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }

}
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * {@link MetricFilter} that evaluates a metric by prefixing the name and then evaluating
 * whether the corresponding logging category is enabled at a level corresponding to a map value
 * or at INFO level.
 * 
 * @since 3.3.0
 */
public class LoggerDrivenMetricFilter implements MetricFilter {

    /** Filtering level, corresponding to available logging levels. */
    public enum Level {
        /** Trace level. */
        TRACE,
        
        /** Debug level. */
        DEBUG,
        
        /** Info level. */
        INFO,
        
        /** Warn level. */
        WARN,
        
        /** Error level. */
        ERROR,
    };
    
    /** Prefix to prepend to metric name. */
    @Nonnull @NotEmpty private final String loggerPrefix;
    
    /** Map of metrics to logging levels. */
    @Nonnull @NonnullElements private final Map<String,Level> levelMap;
    
    /**
     * Constructor.
     *
     * @param prefix prefix to attach to metric name before evaluating
     */
    public LoggerDrivenMetricFilter(@Nonnull @NotEmpty final String prefix) {
        this(prefix, Collections.<String,Level>emptyMap());
    }
    
    /**
     * Constructor.
     *
     * @param prefix prefix to attach to metric name before evaluating
     * @param map map of metric names to logging levels
     */
    public LoggerDrivenMetricFilter(@Nonnull @NotEmpty final String prefix,
            @Nullable @NonnullElements final Map<String,Level> map) {
        loggerPrefix = Constraint.isNotNull(StringSupport.trimOrNull(prefix), "Prefix cannot be null or empty.");
        
        if (map == null || map.isEmpty()) {
            levelMap = Collections.emptyMap();
        } else {
            levelMap = new HashMap(map.size());
            for (final Map.Entry<String,Level> entry : map.entrySet()) {
                final String trimmed = StringSupport.trimOrNull(entry.getKey());
                if (trimmed != null && entry.getValue() != null) {
                    levelMap.put(trimmed, entry.getValue());
                }
            }
        }
    }
    
    /** {@inheritDoc} */
    public boolean matches(final String name, final Metric metric) {
        final Logger logger = LoggerFactory.getLogger(loggerPrefix + name);
        final Level level = levelMap.get(logger.getName());
        if (level == null) {
            return logger.isInfoEnabled();
        }
        
        switch (level) {
            case TRACE:
                return logger.isTraceEnabled();
                
            case DEBUG:
                return logger.isDebugEnabled();
                
            case INFO:
                return logger.isInfoEnabled();
                
            case WARN:
                return logger.isWarnEnabled();
            
            case ERROR:
                return logger.isErrorEnabled();
                
            default:
                return logger.isInfoEnabled();
        }
        
    }

}
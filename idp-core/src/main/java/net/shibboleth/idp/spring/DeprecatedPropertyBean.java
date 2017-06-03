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

package net.shibboleth.idp.spring;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;


/**
 * A simple bean that may be used with Spring to initialize the OpenSAML library
 * with injected instances of some critical objects.
 */
public class DeprecatedPropertyBean extends AbstractInitializableComponent implements ApplicationContextAware {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DeprecationSupport.LOG_CATEGORY);
    
    /** Spring context. */
    @Nonnull private ApplicationContext applicationContext;
    
    /** Deprecated properties. */
    @Nonnull private Map<String,String> deprecatedProperties;

    /** Dead properties. */
    @Nonnull @NonnullElements private Collection<String> deadProperties;

    /** Constructor. */
    public DeprecatedPropertyBean() {
        deprecatedProperties = Collections.emptyMap();
        deadProperties = Collections.emptyList();
    }
    
    /**
     * Set the property names to deprecate, along with an optional replacement.
     * 
     * @param map deprecated property names and replacements
     */
    public void setDeprecatedProperties(@Nonnull final Map<String,String> map) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(map, "Property map cannot be null");
        
        deprecatedProperties = new HashMap<>(map.size());
        for (final Map.Entry<String,String> entry : map.entrySet()) {
            deprecatedProperties.put(StringSupport.trimOrNull(entry.getKey()),
                    StringSupport.trimOrNull(entry.getValue()));
        }
    }
    
    /**
     * Set the property names to treat as defunct.
     * 
     * @param properties defunct property names
     */
    public void setDeadProperties(@Nonnull @NonnullElements final Collection<String> properties) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(properties, "Property collection cannot be null");
        
        deadProperties = StringSupport.normalizeStringCollection(properties);
    }

    /** {@inheritDoc} */
    public void setApplicationContext(final ApplicationContext context) {
        applicationContext = Constraint.isNotNull(context, "ApplicationContext cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        
        for (final Map.Entry<String,String> entry : deprecatedProperties.entrySet()) {
            if (applicationContext.getEnvironment().containsProperty(entry.getKey())) {
                DeprecationSupport.warn(ObjectType.PROPERTY, entry.getKey(), null, entry.getValue());
            }
        }

        for (final String name : deadProperties) {
            if (applicationContext.getEnvironment().containsProperty(name)) {
                log.warn("property '{}' is no longer supported", name);
            }
        }
    }
    
}
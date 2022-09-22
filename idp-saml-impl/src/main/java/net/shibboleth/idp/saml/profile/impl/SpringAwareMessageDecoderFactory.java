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

package net.shibboleth.idp.saml.profile.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.decoder.MessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;


/**
 * A function that returns the correct {@link MessageDecoder} to use based on a simple map of
 * strings to bean IDs.
 */
@ThreadSafeAfterInit
public class SpringAwareMessageDecoderFactory extends AbstractInitializableComponent
        implements Function<String,MessageDecoder>, ApplicationContextAware {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringAwareMessageDecoderFactory.class);
    
    /** Application context injected by surroundings. */
    @Nullable private ApplicationContext applicationContext;
    
    /** Map of strings to bean IDs. */
    @Nonnull private Map<String,String> beanMappings;
    
    /** Constructor. */
    public SpringAwareMessageDecoderFactory() {
        beanMappings = Collections.emptyMap();
    }
    
    /**
     * Set mappings of strings to names of {@link MessageDecoder} beans.
     * 
     * @param mappings string to bean ID mappings
     */
    public void setBeanMappings(@Nonnull final Map<String,String> mappings) {
        checkSetterPreconditions();
        Constraint.isNotNull(mappings, "Mappings cannot be null");
        
        beanMappings = new HashMap<>(mappings.size());
        
        for (final Map.Entry<String,String> entry : mappings.entrySet()) {
            final String key = StringSupport.trimOrNull(entry.getKey());
            final String value = StringSupport.trimOrNull(entry.getValue());
            if (key != null && value != null) {
                beanMappings.put(key, value);
            }
        }
    }
    
    /** {@inheritDoc} */
    public void setApplicationContext(@Nullable final ApplicationContext context) {
        applicationContext = context;
    }
    
    /** {@inheritDoc} */
    @Nullable public MessageDecoder apply(@Nullable final String input) {
        checkComponentActive();
        
        final String beanID = beanMappings.get(StringSupport.trimOrNull(input));
        
        if (applicationContext == null) {
            log.warn("No Spring ApplicationContext set");
            return null;
        } else if (beanID == null) {
            log.warn("No bean ID associated with input value {}", input);
            return null;
        }
        
        log.debug("Looking up message decoder with bean ID: {}", beanID);
        
        try {
            return applicationContext.getBean(beanID, MessageDecoder.class);
        } catch (final BeansException e) {
            log.warn("Error instantiating message decoder from bean ID {}", beanID, e);
        }
        
        return null;
    }
    
}
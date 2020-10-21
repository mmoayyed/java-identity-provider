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

package net.shibboleth.idp.attribute.resolver.dc.storage.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.storage.StorageServiceSearch;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ExecutableSearchBuilder} that generates the {@link StorageService} context and key
 * using Velocity templates.
 * 
 * @since 4.1.0
 */
public class TemplatedSearchBuilder extends AbstractInitializableComponent
        implements ExecutableSearchBuilder<StorageServiceSearch> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TemplatedSearchBuilder.class);

    /** Context template to be evaluated. */
    @NonnullAfterInit private Template contextTemplate;

    /** Key template to be evaluated. */
    @NonnullAfterInit private Template keyTemplate;
    
    /** Text of context template to be evaluated. */
    @NonnullAfterInit private String contextTemplateText;

    /** Text of key template to be evaluated. */
    @NonnullAfterInit private String keyTemplateText;

    /** VelocityEngine. */
    @NonnullAfterInit private VelocityEngine engine;
    
    /** A custom object to inject into the template. */
    @Nullable private Object customObject;
    
    /**
     * Get the context template to be evaluated.
     * 
     * @return template
     */
    @NonnullAfterInit public Template getContextTemplate() {
        return contextTemplate;
    }

    /**
     * Get the key template to be evaluated.
     * 
     * @return template
     */
    @NonnullAfterInit public Template getKeyTemplate() {
        return keyTemplate;
    }

    /**
     * Get the context template text to be evaluated.
     * 
     * @return template text
     */
    @NonnullAfterInit public String getContextTemplateText() {
        return contextTemplateText;
    }

    /**
     * Set the context template to be evaluated.
     * 
     * @param text template to be evaluated
     */
    public void setContextTemplateText(@Nullable final String text) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        contextTemplateText = StringSupport.trimOrNull(text);
    }

    /**
     * Get the key template text to be evaluated.
     * 
     * @return template text
     */
    @NonnullAfterInit public String getKeyTemplateText() {
        return keyTemplateText;
    }

    /**
     * Set the key template to be evaluated.
     * 
     * @param text template to be evaluated
     */
    public void setKeyTemplateText(@Nullable final String text) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        keyTemplateText = StringSupport.trimOrNull(text);
    }

    /**
     * Get the {@link VelocityEngine} to be used.
     * 
     * @return template engine
     */
    @NonnullAfterInit public VelocityEngine getVelocityEngine() {
        return engine;
    }

    /**
     * Set the {@link VelocityEngine} to be used.
     * 
     * @param velocityEngine engine to be used
     */
    public void setVelocityEngine(@Nonnull final VelocityEngine velocityEngine) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        engine = Constraint.isNotNull(velocityEngine, "Velocity engine cannot be null");
    }
    
    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    public void setCustomObject(@Nullable final Object object) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        customObject = object;
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {

        if (null == engine) {
            throw new ComponentInitializationException("Velocity engine cannot be null");
        }

        if (null == contextTemplateText) {
            throw new ComponentInitializationException("Context template text cannot be null");
        } else if (null == keyTemplateText) {
            throw new ComponentInitializationException("Key template text cannot be null");
        }

        contextTemplate = Template.fromTemplate(engine, contextTemplateText);
        keyTemplate = Template.fromTemplate(engine, keyTemplateText);
    }

    /** {@inheritDoc} */
    @Override public StorageServiceSearch build(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) throws ResolutionException {
        
        final Pair<String,String> searchParams = getContextAndKey(resolutionContext, dependencyAttributes);
        
        return new StorageServiceSearch() {
            
            /** {@inheritDoc} */
            @Nullable public String getResultCacheKey() {
                if (searchParams.getFirst() != null && searchParams.getSecond() != null) {
                    return searchParams.getFirst() + "!" + searchParams.getSecond();
                }
                return null;
            }

            /** {@inheritDoc} */
            public StorageRecord<?> execute(@Nonnull final StorageService storageService) throws IOException {
                return storageService.read(searchParams.getFirst(), searchParams.getSecond());
            }
        };
    }
    
    @Nonnull private Pair<String,String> getContextAndKey(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) throws ResolutionException {

        final VelocityContext context = new VelocityContext();
        log.trace("Creating search criteria using attribute resolution context {}", resolutionContext);
        context.put("resolutionContext", resolutionContext);
        context.put("custom", customObject);

        // inject dependencies
        if (dependencyAttributes != null && !dependencyAttributes.isEmpty()) {
            for (final Map.Entry<String, List<IdPAttributeValue>> entry : dependencyAttributes.entrySet()) {
                final List<Object> values = new ArrayList<>(entry.getValue().size());
                for (final IdPAttributeValue value : entry.getValue()) {
                    values.add(value.getNativeValue());
                }
                log.trace("Adding dependency {} to context with {} value(s)", entry.getKey(), values.size());
                context.put(entry.getKey(), values);
            }
        }

        try {
            final String ctx = contextTemplate.merge(context);
            final String key = keyTemplate.merge(context);
            log.debug("Produced search context '{}', key '{}'", ctx, key);
            return new Pair<>(ctx, key);
        } catch (final VelocityException e) {
            log.error("Error running template engine: {}", e.getMessage());
            throw new ResolutionException("Error running template engine", e);
        }
    }

}
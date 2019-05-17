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

package net.shibboleth.idp.attribute.resolver.dc.http.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

/**
 * An {@link net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder} that generates the URL to
 * request by evaluating a {@link Template} against the currently resolved attributes within an
 * {@link AttributeResolutionContext}.
 */
public class TemplatedURLBuilder extends AbstractHTTPSearchBuilder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TemplatedURLBuilder.class);

    /** Template to be evaluated. */
    @NonnullAfterInit private Template template;

    /** Text of template to be evaluated. */
    @NonnullAfterInit private String templateText;

    /** VelocityEngine. */
    @NonnullAfterInit private VelocityEngine engine;
    
    /** Escaper for form parameters. */
    @Nonnull private final Escaper paramEscaper;

    /** Escaper for fragments. */
    @Nonnull private final Escaper fragmentEscaper;

    /** Escaper for path segments. */
    @Nonnull private final Escaper pathEscaper;
    
    /** A custom object to inject into the template. */
    @Nullable private Object customObject;
    
    /** Constructor. */
    public TemplatedURLBuilder() {
        paramEscaper = UrlEscapers.urlFormParameterEscaper();
        fragmentEscaper = UrlEscapers.urlFragmentEscaper();
        pathEscaper = UrlEscapers.urlPathSegmentEscaper();
    }
    
    /**
     * Get the template to be evaluated.
     * 
     * @return template
     */
    @NonnullAfterInit public Template getTemplate() {
        return template;
    }

    /**
     * Get the template text to be evaluated.
     * 
     * @return template text
     */
    @NonnullAfterInit public String getTemplateText() {
        return templateText;
    }

    /**
     * Set the template to be evaluated.
     * 
     * @param text template to be evaluated
     */
    public void setTemplateText(@Nullable final String text) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        templateText = StringSupport.trimOrNull(text);
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
        super.doInitialize();

        if (null == engine) {
            throw new ComponentInitializationException("Velocity engine cannot be null");
        }

        if (null == templateText) {
            throw new ComponentInitializationException("Template text cannot be null");
        }

        template = Template.fromTemplate(engine, templateText);
    }

    /**
     * Invokes {@link Template#merge(org.apache.velocity.context.Context)} on the supplied context.
     * 
     * @param context to merge
     * 
     * @return result of the merge operation
     */
    @Nonnull @NotEmpty protected String merge(@Nonnull final VelocityContext context) {
        final String result = template.merge(context);
        log.debug("Template text {} yields {}", templateText, result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected String getURL(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) throws ResolutionException {

        final VelocityContext context = new VelocityContext();
        log.trace("Creating request URL using attribute resolution context {}", resolutionContext);
        context.put("resolutionContext", resolutionContext);

        context.put("httpClientSecurityParameters", getHttpClientSecurityParameters());
        context.put("paramEscaper", paramEscaper);
        context.put("fragmentEscaper", fragmentEscaper);
        context.put("pathEscaper", pathEscaper);
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
            return merge(context);
        } catch (final VelocityException e) {
            log.error("Error running template engine", e);
            throw new ResolutionException("Error running template engine", e);
        }
    }
    
}

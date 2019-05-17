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

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.xml.XmlEscapers;

/**
 * An {@link net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder} that generates a
 * request by evaluating {@link Template}s against the currently resolved attributes within an
 * {@link AttributeResolutionContext} to produce a URL and body, via GET or POST, and a configurable
 * cache key.
 */
public class TemplatedBodyBuilder extends AbstractHTTPSearchBuilder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TemplatedBodyBuilder.class);

    /** URL template to be evaluated. */
    @NonnullAfterInit private Template urlTemplate;

    /** Body template to be evaluated. */
    @NonnullAfterInit private Template bodyTemplate;

    /** Cache key template to be evaluated. */
    @NonnullAfterInit private Template cacheKeyTemplate;
    
    /** Text of URL template to be evaluated. */
    @NonnullAfterInit private String urlTemplateText;

    /** Text of body template to be evaluated. */
    @NonnullAfterInit private String bodyTemplateText;

    /** Text of cache key template to be evaluated. */
    @NonnullAfterInit private String cacheKeyTemplateText;

    /** VelocityEngine. */
    @NonnullAfterInit private VelocityEngine engine;
    
    /** HTTP method. */
    @Nonnull @NotEmpty private String method;

    /** MIME type. */
    @Nonnull @NotEmpty private String mimeType;

    /** Character set. */
    @Nullable private String charset;
    
    /** Content type. */
    @NonnullAfterInit private ContentType contentType;
    
    /** Escaper for form parameters. */
    @Nonnull private final Escaper paramEscaper;

    /** Escaper for fragments. */
    @Nonnull private final Escaper fragmentEscaper;

    /** Escaper for path segments. */
    @Nonnull private final Escaper pathEscaper;

    /** Escaper for XML Attributes. */
    @Nonnull private final Escaper xmlAttributeEscaper;

    /** Escaper for XML content. */
    @Nonnull private final Escaper xmlContentEscaper;
    
    /** A custom object to inject into the template. */
    @Nullable private Object customObject;

    /** Constructor. */
    public TemplatedBodyBuilder() {
        method = "POST";
        mimeType ="text/plain";
        
        paramEscaper = UrlEscapers.urlFormParameterEscaper();
        fragmentEscaper = UrlEscapers.urlFragmentEscaper();
        pathEscaper = UrlEscapers.urlPathSegmentEscaper();
        xmlAttributeEscaper = XmlEscapers.xmlAttributeEscaper();
        xmlContentEscaper = XmlEscapers.xmlContentEscaper();
    }
    
    /**
     * Get the URL template to be evaluated.
     * 
     * @return template
     */
    @NonnullAfterInit public Template getURLTemplate() {
        return urlTemplate;
    }

    /**
     * Get the body template to be evaluated.
     * 
     * @return template
     */
    @NonnullAfterInit public Template getBodyTemplate() {
        return bodyTemplate;
    }

    /**
     * Get the cache key template to be evaluated.
     * 
     * @return template
     */
    @Nullable public Template getCacheKeyTemplate() {
        return cacheKeyTemplate;
    }

    /**
     * Get the URL template text to be evaluated.
     * 
     * @return template text
     */
    @NonnullAfterInit public String getURLTemplateText() {
        return urlTemplateText;
    }

    /**
     * Set the URL template to be evaluated.
     * 
     * @param text template to be evaluated
     */
    public void setURLTemplateText(@Nullable final String text) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        urlTemplateText = StringSupport.trimOrNull(text);
    }

    /**
     * Get the body template text to be evaluated.
     * 
     * @return template text
     */
    @NonnullAfterInit public String getBodyTemplateText() {
        return bodyTemplateText;
    }

    /**
     * Set the body template to be evaluated.
     * 
     * @param text template to be evaluated
     */
    public void setBodyTemplateText(@Nullable final String text) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        bodyTemplateText = StringSupport.trimOrNull(text);
    }

    /**
     * Get the cache key template text to be evaluated.
     * 
     * @return template text
     */
    @Nullable public String getCacheKeyTemplateText() {
        return cacheKeyTemplateText;
    }

    /**
     * Set the cache key template to be evaluated.
     * 
     * @param text template to be evaluated
     */
    public void setCacheKeyTemplateText(@Nullable final String text) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        cacheKeyTemplateText = StringSupport.trimOrNull(text);
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
     * Set the HTTP method.
     * 
     * <p>Defaults to "POST".</p>
     * 
     * @param m method
     */
    public void setMethod(@Nonnull @NotEmpty final String m) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        method = Constraint.isNotNull(StringSupport.trimOrNull(m), "HTTP method cannot be null or empty");
        Constraint.isTrue(HttpPost.METHOD_NAME.equals(method) || HttpPut.METHOD_NAME.equals(method),
                "HTTP method must be POST or PUT");
    }

    /**
     * Set the MIME type.
     * 
     * <p>Defaults to "text/plain".</p>
     * 
     * @param type MIME type
     */
    public void setMIMEType(@Nonnull @NotEmpty final String type) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        mimeType = Constraint.isNotNull(StringSupport.trimOrNull(type), "MIME type cannot be null or empty");
    }

    /**
     * Set the character set.
     * 
     * @param c character set
     */
    public void setCharacterSet(@Nullable final String c) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        charset = StringSupport.trimOrNull(c);
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

        if (null == urlTemplateText) {
            throw new ComponentInitializationException("URL template text cannot be null");
        } else if (null == bodyTemplateText) {
            throw new ComponentInitializationException("Body template text cannot be null");
        }

        urlTemplate = Template.fromTemplate(engine, urlTemplateText);
        bodyTemplate = Template.fromTemplate(engine, bodyTemplateText);
        
        if (null != cacheKeyTemplateText) {
            cacheKeyTemplate = Template.fromTemplate(engine, cacheKeyTemplateText);
        }
        
        contentType = ContentType.create(mimeType, charset);
    }

    /**
     * Invokes {@link Template#merge(org.apache.velocity.context.Context)} on the supplied template and context.
     * 
     * @param template template to merge
     * @param context to merge
     * 
     * @return result of the merge operation
     */
    @Nonnull @NotEmpty protected String merge(@Nonnull final Template template,
            @Nonnull final VelocityContext context) {
        return template.merge(context);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected HttpUriRequest getHttpRequest(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) throws ResolutionException {

        final VelocityContext context = new VelocityContext();
        log.trace("Creating request using attribute resolution context {}", resolutionContext);
        context.put("resolutionContext", resolutionContext);

        context.put("httpClientSecurityParameters", getHttpClientSecurityParameters());
        context.put("paramEscaper", paramEscaper);
        context.put("fragmentEscaper", fragmentEscaper);
        context.put("pathEscaper", pathEscaper);
        context.put("xmlAttributeEscaper", xmlAttributeEscaper);
        context.put("xmlContentEscaper", xmlContentEscaper);
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

        final String url;
        final String body;
        final HttpEntityEnclosingRequestBase request;
        
        try {
            url = merge(urlTemplate, context);
            body = merge(bodyTemplate, context);
        } catch (final VelocityException e) {
            log.error("Error running template engine", e);
            throw new ResolutionException("Error running template engine", e);
        }
        
        try {
            if (HttpPost.METHOD_NAME.equals(method)) {
                request = new HttpPost(url);
            } else if (HttpPut.METHOD_NAME.equals(method)) {
                request = new HttpPut(url);
            } else {
                throw new ResolutionException("Unsupported HTTP method");
            }
            
            request.setEntity(new StringEntity(body, contentType));
        } catch (final IllegalArgumentException e) {
            throw new ResolutionException(e);
        }
        
        return request;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getResultCacheKey(@Nonnull final HttpUriRequest request,
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue>> dependencyAttributes) {
        
        if (cacheKeyTemplate == null) {
            return null;
        }
        
        final VelocityContext context = new VelocityContext();
        log.trace("Creating cache key using attribute resolution context {}", resolutionContext);
        context.put("resolutionContext", resolutionContext);

        context.put("httpClientSecurityParameters", getHttpClientSecurityParameters());
        
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

        return merge(cacheKeyTemplate, context);
    }

}

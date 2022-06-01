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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.http.HTTPResponseMappingStrategy;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.scripting.AbstractScriptEvaluator;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

/**
 * {@link HTTPResponseMappingStrategy} that relies on a script to map the response to the
 * attribute set.
 * 
 * <p>Well-suited to JSON output formats that can be parsed by the scripting engine.</p>
 */
public final class ScriptedResponseMappingStrategy extends AbstractScriptEvaluator
        implements HTTPResponseMappingStrategy {
    
    /** The id of the object where the results go. */
    @Nonnull public static final String RESULTS_STRING = "connectorResults";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedResponseMappingStrategy.class);

    /** "Successful" statuses. */
    @Nonnull private Set<Integer> acceptStatuses;
    
    /** Acceptable Content-Types. */
    @Nonnull private Set<String> acceptTypes;
    
    /** Limit on content-length. */
    private long maxLength;
    
    /**
     * Constructor.
     *
     * @param theScript the script to run
     */
    private ScriptedResponseMappingStrategy(@Nonnull final EvaluableScript theScript) {
        super(theScript);
        
        acceptStatuses = Collections.singleton(HttpStatus.SC_OK);
        acceptTypes = Collections.emptySet();
    }
    
    /**
     * Set the HTTP status codes to treat as successful.
     * 
     * @param statuses successful codes
     */
    public void setAcceptStatuses(@Nonnull @NonnullElements final Collection<Integer> statuses) {
        acceptStatuses = Set.copyOf(Constraint.isNotNull(statuses, "Statuses cannot be null"));
    }

    /**
     * Set the content-types to allow.
     * 
     * @param types types to allow
     */
    public void setAcceptTypes(@Nonnull @NonnullElements final Collection<String> types) {
        acceptTypes = Set.copyOf(StringSupport.normalizeStringCollection(
                Constraint.isNotNull(types, "Types cannot be null")));
    }
    
    /**
     * Set a limit on content-length.
     * 
     * <p>Defaults to 0, allowing any. Setting a limit implies that an unknown length will be rejected.</p>
     * 
     * @param len limit on size
     */
    public void setMaxLength(final long len) {
        maxLength = len;
    }
    
    /** {@inheritDoc} */
    @Nonnull public Map<String,IdPAttribute> map(@Nonnull final Map<String,IdPAttribute> results)
            throws ResolutionException {
        return results;
    }
    
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Map<String,IdPAttribute> handleResponse(final HttpResponse response) throws IOException {
        
        log.debug("{} Handling HTTP response", getLogPrefix());
        
        if (response == null) {
            log.debug("{} HTTP response was missing", getLogPrefix());
            throw new IOException(getLogPrefix() + " HTTP response was missing");
        }
        
        checkStatus(response);
        
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            log.debug("{} Response body was missing", getLogPrefix());
            throw new IOException(getLogPrefix() + " Response body was missing");
        }
        
        checkContentType(entity);
        checkContentLength(entity);
        
        try {
            return (Map<String,IdPAttribute>) evaluate(response);
        } catch (final RuntimeException e) {
            throw new IOException(getLogPrefix() + " Script did not run successfully", e);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {
        log.debug("{} Adding to-be-populated attribute set '{}' to script context", getLogPrefix(), RESULTS_STRING);
        scriptContext.setAttribute(RESULTS_STRING, new HashSet<>(), ScriptContext.ENGINE_SCOPE);
        
        scriptContext.setAttribute("response", input[0], ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("log", log, ScriptContext.ENGINE_SCOPE);
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected Object finalizeContext(@Nonnull final ScriptContext scriptContext,
            @Nullable final Object scriptResult) throws ScriptException {
        
        // The real result in our case is a variable in the context.
        final Object res = scriptContext.getAttribute(RESULTS_STRING);

        if (null == res) {
            log.error("{} Could not locate output variable '{}' from script", getLogPrefix(), RESULTS_STRING);
            throw new ScriptException("Could not locate output from script");
        }
        if (!(res instanceof Collection)) {
            log.error("{} Output '{}' was of type '{}', expected '{}'", getLogPrefix(), res.getClass().getName(),
                    Collection.class.getName());
            throw new ScriptException("Output was of the wrong type");
        }

        final Collection<?> outputCollection = (Collection<?>) res;
        final Map<String, IdPAttribute> outputMap = new HashMap<>(outputCollection.size());
        for (final Object o : outputCollection) {
            if (o instanceof IdPAttribute) {
                final IdPAttribute attribute = (IdPAttribute) o;
                if (null == attribute.getId()) {
                    log.warn("{} Anonymous Attribute encountered, ignored", getLogPrefix());
                } else {
                    checkValues(attribute);
                    outputMap.put(attribute.getId(), attribute);
                }
            } else {
                log.warn("{} Output collection contained an object of type '{}', ignored", getLogPrefix(),
                        o.getClass().getName());
            }
        }

        return outputMap;
    }
    
    /**
     * Enforce any status code requirements.
     * 
     * @param response HTTP response
     * 
     * @throws IOException if the status is unacceptable
     */
    private void checkStatus(@Nonnull final HttpResponse response) throws IOException {
        if (!acceptStatuses.isEmpty()) {
            if (response.getStatusLine() == null
                    || !acceptStatuses.contains(response.getStatusLine().getStatusCode())) {
                log.debug("{} Unacceptable HTTP status: {}", getLogPrefix(),
                        response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : "unknown");
                throw new IOException(getLogPrefix() + " HTTP status unknown or unacceptable");
            }
        }
    }

    /**
     * Enforce Content-Type requirements.
     * 
     * @param entity the entity body
     * 
     * @throws IOException if the type is unacceptable
     */
    private void checkContentType(@Nonnull final HttpEntity entity) throws IOException {
        if (!acceptTypes.isEmpty()) {
            
            final ContentType contentType = ContentType.get(entity);
            if (contentType == null || !acceptTypes.contains(contentType.getMimeType())) {
                log.debug("{} Unacceptable Content-Type: {}", getLogPrefix(),
                        contentType != null ? contentType.getMimeType() : "unknown");
                throw new IOException(getLogPrefix() + " Content-Type unknown or unacceptable");
            }
        }
    }

    /**
     * Check the content length.
     * 
     * @param entity the entity body
     * 
     * @throws IOException if the length is unacceptable
     */
    private void checkContentLength(@Nonnull final HttpEntity entity) throws IOException {
        if (maxLength > 0) {
            if (entity.getContentLength() < 0 || entity.getContentLength() > maxLength) {
                log.debug("{} Unacceptable Content-Length: {}", getLogPrefix(), entity.getContentLength());
                throw new IOException(getLogPrefix() + " Content-Length exceeded acceptable limits or was unset");
            }
        }
    }

    /**
     * Ensure that all the values in the attribute are of the correct type.
     * 
     * @param attribute the attribute to look at
     */
    private void checkValues(final IdPAttribute attribute) {

        if (null == attribute.getValues()) {
            log.info("{} Attribute '{}' has no values provided.", getLogPrefix(), attribute.getId());
            attribute.setValues(Collections.<IdPAttributeValue> emptyList());
            return;
        }
        log.debug("{} Attribute '{}' has {} value(s).", getLogPrefix(), attribute.getId(),
                attribute.getValues().size());
        final List<IdPAttributeValue> inputValues = attribute.getValues();
        final List<IdPAttributeValue> outputValues = new ArrayList<>(inputValues.size());

        for (final Object o : inputValues) {
            if (o instanceof IdPAttributeValue) {
                outputValues.add((IdPAttributeValue) o);
            } else {
                log.error("{} Attribute '{} has attribute value of type {}.  This will be ignored", getLogPrefix(),
                        attribute.getId(), o.getClass().getName());
            }
        }
        attribute.setValues(outputValues);
    }
    
    /**
     * Factory to create {@link ScriptedResponseMappingStrategy} from a {@link Resource}.
     * 
     * @param engineName the language
     * @param resource the resource to look at
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull static ScriptedResponseMappingStrategy resourceScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull final Resource resource) throws ScriptException, IOException {
        try (final InputStream is = resource.getInputStream()) {
            final EvaluableScript script = new EvaluableScript();
            script.setEngineName(engineName);
            script.setScript(is);
            try {
                script.initialize();
            } catch (final ComponentInitializationException e) {
                final Throwable cause = e.getCause();

                if (cause != null) {
                    if (cause instanceof ScriptException) {
                        throw (ScriptException) cause;
                    }
                    else if (cause instanceof IOException) {
                        throw (IOException) cause;
                    }
                }
                throw new ScriptException(e);
            }
            return new ScriptedResponseMappingStrategy(script);
        }
    }

    /**
     * Factory to create {@link ScriptedResponseMappingStrategy} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull static ScriptedResponseMappingStrategy resourceScript(@Nonnull final Resource resource)
            throws ScriptException, IOException {
        return resourceScript(DEFAULT_ENGINE, resource);
    }

    /**
     * Factory to create {@link ScriptedResponseMappingStrategy} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull static ScriptedResponseMappingStrategy inlineScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        try {
            script.initialize();
        } catch (final ComponentInitializationException e) {
            final Throwable cause = e.getCause();

            if (cause != null && cause instanceof ScriptException) {
                throw (ScriptException) cause;
            }
            throw new ScriptException(e);
        }
        return new ScriptedResponseMappingStrategy(script);
    }

    /**
     * Factory to create {@link ScriptedResponseMappingStrategy} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull static ScriptedResponseMappingStrategy inlineScript(@Nonnull @NotEmpty final String scriptSource)
            throws ScriptException {
        return inlineScript(DEFAULT_ENGINE, scriptSource);
    }

}

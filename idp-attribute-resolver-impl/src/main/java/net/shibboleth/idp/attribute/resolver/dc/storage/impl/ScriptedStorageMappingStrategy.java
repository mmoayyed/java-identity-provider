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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.opensaml.storage.StorageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.storage.StorageMappingStrategy;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.scripting.AbstractScriptEvaluator;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

/**
 * {@link StorageMappingStrategy} that relies on a script to map the record to the
 * attribute set.
 * 
 * <p>Well-suited to JSON output formats that can be parsed by the scripting engine.</p>
 * 
 * @since 4.1.0
 */
public final class ScriptedStorageMappingStrategy extends AbstractScriptEvaluator
        implements StorageMappingStrategy {
    
    /** The id of the object where the results go. */
    @Nonnull public static final String RESULTS_STRING = "connectorResults";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedStorageMappingStrategy.class);
    
    /**
     * Constructor.
     *
     * @param theScript the script to run
     */
    private ScriptedStorageMappingStrategy(@Nonnull final EvaluableScript theScript) {
        super(theScript);
    }
    
    /** {@inheritDoc} */
    @Nonnull public Map<String,IdPAttribute> map(@Nonnull final StorageRecord<?> results)
            throws ResolutionException {
        log.debug("{} Handling StorageRecord", getLogPrefix());
        
        if (results == null) {
            log.debug("{} StorageRecord was missing", getLogPrefix());
            throw new ResolutionException(getLogPrefix() + " StorageRecord was missing");
        }
        
        try {
            return (Map<String,IdPAttribute>) evaluate(results);
        } catch (final RuntimeException e) {
            throw new ResolutionException(getLogPrefix() + " Script did not run successfully", e);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {
        log.debug("{} Adding to-be-populated attribute set '{}' to script context", getLogPrefix(), RESULTS_STRING);
        scriptContext.setAttribute(RESULTS_STRING, new HashSet<>(), ScriptContext.ENGINE_SCOPE);
        
        scriptContext.setAttribute("record", input[0], ScriptContext.ENGINE_SCOPE);
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
     * Factory to create {@link ScriptedStorageMappingStrategy} from a {@link Resource}.
     * 
     * @param engineName the language
     * @param resource the resource to look at
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull static ScriptedStorageMappingStrategy resourceScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull final Resource resource) throws ScriptException, IOException {
        try (final InputStream is = resource.getInputStream()) {
            final EvaluableScript script = new EvaluableScript();
            script.setEngineName(engineName);
            script.setScript(is);
            script.initializeWithScriptException();
            return new ScriptedStorageMappingStrategy(script);
        }
    }

    /**
     * Factory to create {@link ScriptedStorageMappingStrategy} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull static ScriptedStorageMappingStrategy resourceScript(@Nonnull final Resource resource)
            throws ScriptException, IOException {
        return resourceScript(DEFAULT_ENGINE, resource);
    }

    /**
     * Factory to create {@link ScriptedStorageMappingStrategy} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull static ScriptedStorageMappingStrategy inlineScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        script.initializeWithScriptException();
        return new ScriptedStorageMappingStrategy(script);
    }

    /**
     * Factory to create {@link ScriptedStorageMappingStrategy} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull static ScriptedStorageMappingStrategy inlineScript(@Nonnull @NotEmpty final String scriptSource)
            throws ScriptException {
        return inlineScript(DEFAULT_ENGINE, scriptSource);
    }

}

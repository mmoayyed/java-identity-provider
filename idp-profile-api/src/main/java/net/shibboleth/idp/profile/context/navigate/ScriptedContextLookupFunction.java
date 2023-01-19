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

package net.shibboleth.idp.profile.context.navigate;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.scripting.AbstractScriptEvaluator;
import net.shibboleth.shared.scripting.EvaluableScript;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.core.io.Resource;

/**
 * A {@link java.util.function.Function} over a {@link BaseContext}
 * which calls out to a supplied script.
 * 
 * @param <T> the specific type of context
 */
@SuppressWarnings("removal")
public class ScriptedContextLookupFunction<T extends BaseContext> extends AbstractScriptEvaluator
        implements ContextDataLookupFunction<T,Object> {

    /** What class we want the input to test against. */
    @Nonnull private final Class<T> inputClass;

    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param theScript the script we will evaluate.
     * @param extraInfo debugging information.
     */
    protected ScriptedContextLookupFunction(@Nonnull final Class<T> inClass, @Nonnull final EvaluableScript theScript,
            @Nullable final String extraInfo) {
        super(theScript);
        inputClass = Constraint.isNotNull(inClass, "Supplied inputClass cannot be null");
        setLogPrefix("Scripted Function from " + extraInfo + ":");
    }

    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param theScript the script we will evaluate.
     */
    protected ScriptedContextLookupFunction(@Nonnull final Class<T> inClass, @Nonnull final EvaluableScript theScript) {
        super(theScript);
        inputClass = Constraint.isNotNull(inClass, "Supplied inputClass cannot be null");
        setLogPrefix("Anonymous Scripted Function:");
    }

    /**
     * Constructor.
     * 
     * @param inClass the class we accept as input.
     * @param theScript the script we will evaluate.
     * @param extraInfo debugging information.
     * @param outputType the type to test against.
     */
    protected ScriptedContextLookupFunction(@Nonnull final Class<T> inClass, @Nonnull final EvaluableScript theScript,
            @Nullable final String extraInfo, @Nullable final Class<?> outputType) {
        this(inClass, theScript, extraInfo);
        setOutputType(outputType);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public Object getCustomObject() {
        return super.getCustomObject();
    }

    /** {@inheritDoc} */
    public Object apply(@Nullable final T context) {

        if (null != context && !inputClass.isInstance(context)) {
            throw new ClassCastException(getLogPrefix() + " Input was type " + context.getClass()
                    + " which is not an instance of " + inputClass);
        }
        
        return evaluate(context);
    }

    /** {@inheritDoc} */
    @Override
    protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {
        // We don't actually know that the context is a PRC, but we'll keep this for compatibility.
        // We can't use the variable name "context" because Rhino appears to reserve that name.
        scriptContext.setAttribute("profileContext", input != null ? input[0] : null, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("input", input != null ? input[0] : null, ScriptContext.ENGINE_SCOPE);
    }
    
    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link ProfileRequestContext}s from a
     * {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> resourceScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull final Resource resource) throws ScriptException,
            IOException {
        return resourceScript(engineName, resource, null);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link ProfileRequestContext}s from a
     * {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> resourceScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull final Resource resource,
            @Nullable final Class<?> outputType) throws ScriptException, IOException {
        try (InputStream is = resource.getInputStream()) {
            final EvaluableScript script = new EvaluableScript();
            script.setEngineName(engineName);
            script.setScript(is);
            script.initializeWithScriptException();
            return new ScriptedContextLookupFunction<>(ProfileRequestContext.class, script, resource.getDescription(),
                outputType);
        }
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> resourceScript(final Resource resource)
            throws ScriptException, IOException {
        return resourceScript(DEFAULT_ENGINE, resource, null);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link ProfileRequestContext}s from a
     * {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> resourceScript(final Resource resource,
            @Nullable final Class<?> outputType) throws ScriptException, IOException {
        return resourceScript(DEFAULT_ENGINE, resource, outputType);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link ProfileRequestContext}s from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> inlineScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull @NotEmpty final String scriptSource)
            throws ScriptException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        script.initializeWithScriptException();
        return new ScriptedContextLookupFunction<>(ProfileRequestContext.class, script, "Inline");
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link ProfileRequestContext}s from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> inlineScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull @NotEmpty final String scriptSource,
            @Nullable final Class<?> outputType) throws ScriptException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        script.initializeWithScriptException();
        return new ScriptedContextLookupFunction<>(ProfileRequestContext.class, script, "Inline", outputType);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link ProfileRequestContext}s from inline data.
     * 
     * @param scriptSource the script, as a string
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> inlineScript(
            @Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        return inlineScript(DEFAULT_ENGINE, scriptSource);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link ProfileRequestContext}s from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<ProfileRequestContext> inlineScript(
            @Nonnull @NotEmpty final String scriptSource, @Nullable final Class<?> outputType) throws ScriptException {
        return inlineScript(DEFAULT_ENGINE, scriptSource, outputType);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> resourceMessageContextScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull final Resource resource) throws ScriptException,
            IOException {
        return resourceMessageContextScript(engineName, resource, null);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> resourceMessageContextScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull final Resource resource,
            @Nullable final Class<?> outputType) throws ScriptException, IOException {
        try (InputStream is = resource.getInputStream()) {
            final EvaluableScript script = new EvaluableScript();
            script.setEngineName(engineName);
            script.setScript(is);
            script.initializeWithScriptException();
            return new ScriptedContextLookupFunction<>(MessageContext.class, script, resource.getDescription(),
                    outputType);
        }
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> resourceMessageContextScript(final Resource resource)
            throws ScriptException, IOException {
        return resourceMessageContextScript(DEFAULT_ENGINE, resource, null);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> resourceMessageContextScript(final Resource resource,
            @Nullable final Class<?> outputType) throws ScriptException, IOException {
        return resourceMessageContextScript(DEFAULT_ENGINE, resource, outputType);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> inlineMessageContextScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull @NotEmpty final String scriptSource)
            throws ScriptException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        script.initializeWithScriptException();
        return new ScriptedContextLookupFunction<>(MessageContext.class, script, "Inline");
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> inlineMessageContextScript(
            @Nonnull @NotEmpty final String engineName, @Nonnull @NotEmpty final String scriptSource,
            @Nullable final Class<?> outputType) throws ScriptException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        script.initializeWithScriptException();
        return new ScriptedContextLookupFunction<>(MessageContext.class, script, "Inline", outputType);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from inline data.
     * 
     * @param scriptSource the script, aMessageContexts a string
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> inlineMessageContextScript(
            @Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        return inlineMessageContextScript(DEFAULT_ENGINE, scriptSource);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} for {@link MessageContext}s from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param outputType the type to test against.
     * @return the function
     * @throws ScriptException if the compile fails
     */
    @Nonnull
    public static ScriptedContextLookupFunction<MessageContext> inlineMessageContextScript(
            @Nonnull @NotEmpty final String scriptSource, @Nullable final Class<?> outputType) throws ScriptException {
        return inlineMessageContextScript(DEFAULT_ENGINE, scriptSource, outputType);
    }

}
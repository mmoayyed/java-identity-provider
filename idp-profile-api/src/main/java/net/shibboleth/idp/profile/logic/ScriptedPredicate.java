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

package net.shibboleth.idp.profile.logic;


import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.scripting.EvaluableScript;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * A {@link java.util.function.Predicate} which calls out to a supplied script.
 */
@SuppressWarnings("removal")
public class ScriptedPredicate
        extends net.shibboleth.shared.logic.ScriptedPredicate<ProfileRequestContext> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedPredicate.class);
    
    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate.
     * @param extraInfo debugging information.
     */
    public ScriptedPredicate(@Nonnull @NotEmpty @ParameterName(name="theScript") final EvaluableScript theScript,
            @Nullable @NotEmpty @ParameterName(name="extraInfo") final String extraInfo) {
        super(theScript, extraInfo);
        setInputType(ProfileRequestContext.class);
    }

    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate.
     */
    public ScriptedPredicate(@Nonnull @NotEmpty @ParameterName(name="theScript") final EvaluableScript theScript) {
        super(theScript);
        setInputType(ProfileRequestContext.class);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {
        super.prepareContext(scriptContext, input);
        scriptContext.setAttribute("profileContext", input != null ? input[0] : null, ScriptContext.ENGINE_SCOPE);
    }
    
    /**
     * Factory to create {@link ScriptedPredicate} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    public static ScriptedPredicate resourceScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull final Resource resource) throws ScriptException, IOException {
        try (final InputStream is = resource.getInputStream()) {
            final EvaluableScript script = new EvaluableScript();
            script.setEngineName(engineName);
            script.setScript(is);
            script.initializeWithScriptException();
            return new ScriptedPredicate(script, resource.getDescription());
        }
    }

    /**
     * Factory to create {@link ScriptedPredicate} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    public static ScriptedPredicate resourceScript(@Nonnull final Resource resource)
            throws ScriptException, IOException {
        return resourceScript(DEFAULT_ENGINE, resource);
    }

    /**
     * Factory to create {@link ScriptedPredicate} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     */
    public static ScriptedPredicate inlineScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        script.initializeWithScriptException();
        return new ScriptedPredicate(script, "Inline");
    }

    /**
     * Factory to create {@link ScriptedPredicate} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @return the predicate
     * @throws ScriptException if the compile fails
     */
    public static ScriptedPredicate inlineScript(@Nonnull @NotEmpty final String scriptSource) throws ScriptException {
        return inlineScript(DEFAULT_ENGINE, scriptSource);
    }

}
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.google.common.base.Function;

/**
 * A {@link Function} over a {@link ProfileRequestContext} which calls out to a supplied script.
 */
public class ScriptedContextLookupFunction implements Function<ProfileRequestContext,Object> {

    /** The default language is Javascript. */
    @Nonnull @NotEmpty public static final String DEFAULT_ENGINE = "JavaScript";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedContextLookupFunction.class);

    /** The script we care about. */
    @Nonnull private final EvaluableScript script;

    /** Debugging info. */
    @Nullable private final String logPrefix;

    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate.
     * @param extraInfo debugging information.
     */
    public ScriptedContextLookupFunction(@Nonnull EvaluableScript theScript, @Nullable String extraInfo) {
        script = Constraint.isNotNull(theScript, "Supplied script cannot be null");
        logPrefix = "Scripted Predicate from " + extraInfo + " :";
    }

    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate.
     */
    public ScriptedContextLookupFunction(@Nonnull EvaluableScript theScript) {
        script = Constraint.isNotNull(theScript, "Supplied script should not be null");
        logPrefix = "Anonymous Scripted Predicate :";
    }

    /** {@inheritDoc} */
    @Override public Object apply(@Nullable ProfileRequestContext profileContext) {
        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("profileContext", profileContext, ScriptContext.ENGINE_SCOPE);

        try {
            return script.eval(scriptContext);
        } catch (final ScriptException e) {
            log.error("{} Error while executing Function script", logPrefix, e);
            return null;
        }
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    static ScriptedContextLookupFunction resourceScript(@Nonnull @NotEmpty String engineName,
            @Nonnull Resource resource) throws ScriptException, IOException {
        final EvaluableScript script = new EvaluableScript(engineName, resource.getFile());
        return new ScriptedContextLookupFunction(script, resource.getDescription());
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the function
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     */
    static ScriptedContextLookupFunction resourceScript(Resource resource) throws ScriptException, IOException {
        return resourceScript(DEFAULT_ENGINE, resource);
    }

    /**
     * Factory to create {@link ScriptedContextLookupFunction} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the function
     * @throws ScriptException if the compile fails
     */
    static ScriptedContextLookupFunction inlineScript(@Nonnull @NotEmpty String engineName,
            @Nonnull @NotEmpty String scriptSource) throws ScriptException {
        final EvaluableScript script = new EvaluableScript(engineName, scriptSource);
        return new ScriptedContextLookupFunction(script, "Inline");
    }
    
    /**
     * Factory to create {@link ScriptedContextLookupFunction} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @return the function
     * @throws ScriptException if the compile fails
     */
    static ScriptedContextLookupFunction inlineScript(@Nonnull @NotEmpty String scriptSource) throws ScriptException {
        final EvaluableScript script = new EvaluableScript(DEFAULT_ENGINE, scriptSource);
        return new ScriptedContextLookupFunction(script, "Inline");
    }

}
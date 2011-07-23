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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import java.util.Collection;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Script permit or deny matcher.
 * 
 * This is just a scripting shim around {@link AttributeValueMatcher#getMatchingValues}.
 */
@ThreadSafe
public class ScriptedMatcher implements AttributeValueMatcher {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ScriptedMatcher.class);

    /** The scripting language. */
    private final String scriptLanguage;

    /** The script to execute. */
    private final String script;

    /** The script engine to execute the script. */
    private final ScriptEngine scriptEngine;

    /** The compiled form of the script, if the script engine supports compiling. */
    private final CompiledScript compiledScript;

    /**
     * Constructor.
     * 
     * @param theLanguage the scripting language
     * @param theScript the script to execute
     */
    public ScriptedMatcher(final String theLanguage, final String theScript) {
        scriptLanguage = theLanguage;

        final String trimmedScript = StringSupport.trimOrNull(theScript);
        Assert.isNotNull(trimmedScript, "Script for ScriptedMatcher must be non-null and non empty");
        script = trimmedScript;

        final ScriptEngineManager sem = new ScriptEngineManager();
        scriptEngine = sem.getEngineByName(scriptLanguage);
        compiledScript = compileScript();

        // Validate
        Assert.isNotNull(scriptEngine, "ScriptedMatcher: unable to create scripting engine for the language: "
                + scriptLanguage);
    }

    /** private Constructor to maintain the invariants about the script and language being non null. */
    @SuppressWarnings("unused")
    private ScriptedMatcher() {
        scriptLanguage = null;
        compiledScript = null;
        scriptEngine = null;
        script = null;
        Assert.isFalse(true, "No default constructor");
    }

    /**
     * Gets the scripting language used.
     * 
     * @return scripting language used. This is always a valid language.
     */
    public String getScriptLanguage() {
        return scriptLanguage;
    }

    /**
     * Gets the script that will be executed.
     * 
     * @return script that will be executed. This is never null or empty.
     */
    public String getScript() {
        return script;
    }

    /**
     * Get this resolver's script engine.
     * 
     * @return the engine. Never null.
     */
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    /**
     * Get the compiled script for this engine. Note that this will be null if the language does not support
     * compilation.
     * 
     * @return the compiled script.
     */
    public CompiledScript getCompiledScript() {
        return compiledScript;
    }

    /**
     * Compiles the script if the scripting engine supports it.
     * 
     * @return the compiled script.
     */
    protected CompiledScript compileScript() {
        try {
            if (scriptEngine != null && scriptEngine instanceof Compilable) {
                return ((Compilable) scriptEngine).compile(script);
            }
        } catch (ScriptException e) {
            log.warn("ScriptedMatcher cannot compile {} even though the scripting engine"
                    + " supports this functionality: {}", script, e.toString());
            log.debug("Fails", e);
        }
        return null;
    }

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(Attribute<?> attribute, AttributeFilterContext filterContext)
            throws AttributeFilteringException {

        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("filterContext", filterContext, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("attribute", attribute, ScriptContext.ENGINE_SCOPE);
        final Object result;
        try {
            if (compiledScript != null) {
                result = compiledScript.eval(scriptContext);
            } else {
                result = scriptEngine.eval(script, scriptContext);
            }

            if (result instanceof Collection) {
                return (Collection) result;
            } else {
                throw new AttributeFilteringException("Matcher script did not return a Collection");
            }

        } catch (ScriptException e) {
            throw new AttributeFilteringException("Unable to execute match functor script", e);
        }
    }

}

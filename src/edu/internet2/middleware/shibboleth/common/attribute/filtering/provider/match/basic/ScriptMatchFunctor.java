/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;

/**
 * Match function based on a JSR-268 script.
 */
public class ScriptMatchFunctor implements MatchFunctor {

    /** Class logger. */
    private final Logger log = Logger.getLogger(ScriptMatchFunctor.class);

    /** The scripting language. */
    private String scriptLanguage;

    /** The script to execute. */
    private String script;

    /** The script engine to execute the script. */
    private ScriptEngine scriptEngine;

    /** The compiled form of the script, if the script engine supports compiling. */
    private CompiledScript compiledScript;

    /**
     * Constructor.
     * 
     * @param language the scripting language
     * @param newScript the script to execute
     */
    public ScriptMatchFunctor(String language, String newScript) {
        scriptLanguage = language;
        script = newScript;

        ScriptEngineManager sem = new ScriptEngineManager();
        scriptEngine = sem.getEngineByName(scriptLanguage);
        compileScript();
    }

    /** {@inheritDoc} */
    public boolean evaluatePermitValue(ShibbolethFilteringContext filterContext, String attributeId,
            Object attributeValue) throws FilterProcessingException {
        ScriptContext context = getScriptContext(filterContext, attributeId, attributeValue);
        return executeScript(context);
    }

    /** {@inheritDoc} */
    public boolean evaluatePolicyRequirement(ShibbolethFilteringContext filterContext) throws FilterProcessingException {
        ScriptContext context = getScriptContext(filterContext, null, null);
        return executeScript(context);
    }

    /** Compiles the script if the scripting engine supports it. */
    protected void compileScript() {
        if (DatatypeHelper.isEmpty(script)) {
            return;
        }

        try {
            if (scriptEngine != null && scriptEngine instanceof Compilable) {
                compiledScript = ((Compilable) scriptEngine).compile(script);
            }
        } catch (ScriptException e) {
            compiledScript = null;
            log.warn("Unable to pre-compile JSR-268 script: " + script, e);
        }
    }

    /**
     * Creates the script execution context from the resolution context.
     * 
     * @param filterContext current resolution context
     * @param attributeId ID of the attribute currently being evaluted
     * @param attributeValue attribute currently being validated
     * 
     * @return constructed script context
     */
    protected ScriptContext getScriptContext(ShibbolethFilteringContext filterContext, String attributeId,
            Object attributeValue) {
        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("filterContext", filterContext, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("attributeId", attributeId, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("attributeValue", attributeValue, ScriptContext.ENGINE_SCOPE);
        return scriptContext;
    }

    /**
     * Executes the functor's script.
     * 
     * @param scriptContext the script execution context
     * 
     * @return the result of the script
     * 
     * @throws FilterProcessingException thrown if there is a problem evaluating the script
     */
    protected Boolean executeScript(ScriptContext scriptContext) throws FilterProcessingException {
        Boolean result;
        try {
            if (compiledScript != null) {
                result = (Boolean) compiledScript.eval(scriptContext);
            } else {
                result = (Boolean) scriptEngine.eval(script, scriptContext);
            }

            if (result != null) {
                return result.booleanValue();
            } else {
                return Boolean.FALSE;
            }
        } catch (ScriptException e) {
            throw new FilterProcessingException("Unable to execute match functor script", e);
        }
    }
}
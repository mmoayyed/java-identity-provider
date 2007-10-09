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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;

/**
 * An attribute definition the computes the attribute definition by executing a script written in some JSR-223
 * supporting language.
 */
public class ScriptedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ScriptedAttributeDefinition.class);

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
     * @param language the scripting language that will be used
     */
    public ScriptedAttributeDefinition(String language) {
        scriptLanguage = language;
    }

    /**
     * Gets the scripting language used.
     * 
     * @return scripting language used
     */
    public String getScriptLanguage() {
        return scriptLanguage;
    }

    /**
     * Gets the script that will be executed.
     * 
     * @return script that will be executed
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the script that will be executed.
     * 
     * @param newScript script that will be executed
     */
    public void setScript(String newScript) {
        script = newScript;
        compileScript();
    }

    /** Initializes this attribute definition. */
    public void initialize() {
        ScriptEngineManager sem = new ScriptEngineManager();
        scriptEngine = sem.getEngineByName(scriptLanguage);
        compileScript();
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        if (scriptEngine == null) {
            log.error("ScriptletAttributeDefinition " + getId()
                    + " unable to create scripting engine for the language: " + scriptLanguage);
            throw new AttributeResolutionException("ScriptletAttributeDefinition " + getId()
                    + " unable to create scripting engine for the language: " + scriptLanguage);
        }
    }

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        ScriptContext context = getScriptContext(resolutionContext);

        try {
            if (compiledScript != null) {
                compiledScript.eval(context);
            } else {
                scriptEngine.eval(script, context);
            }

            return (BaseAttribute) context.getAttribute(getId());
        } catch (ScriptException e) {
            log.error("ScriptletAttributeDefinition " + getId() + " unable to execute script", e);
            throw new AttributeResolutionException("ScriptletAttributeDefinition " + getId()
                    + " unable to execute script", e);
        }
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
            log.warn("ScriptletAttributeDefinition {} unable to compile even though the scripting engine supports this functionality.", getId());
        }
    }

    /**
     * Creates the script execution context from the resolution context.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return constructed script context
     * 
     * @throws AttributeResolutionException thrown if dependent data connectors or attribute definitions can not be
     *             resolved
     */
    protected ScriptContext getScriptContext(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute(getId(), null, ScriptContext.ENGINE_SCOPE);

        scriptContext.setAttribute("requestContext", resolutionContext.getAttributeRequestContext(),
                ScriptContext.ENGINE_SCOPE);

        ResolutionPlugIn plugin;
        Map<String, BaseAttribute> attributes;
        BaseAttribute attribute;

        if (!getDependencyIds().isEmpty()) {
            for (String dependency : getDependencyIds()) {
                plugin = resolutionContext.getResolvedPlugins().get(dependency);
                if (plugin instanceof DataConnector) {
                    attributes = ((DataConnector)plugin).resolve(resolutionContext);
                    if (attributes != null) {
                        for (BaseAttribute attr : attributes.values()) {
                            scriptContext.setAttribute(attr.getId(), attr, ScriptContext.ENGINE_SCOPE);
                        }
                    }
                } else if (plugin instanceof AttributeDefinition) {
                    attribute = ((AttributeDefinition)plugin).resolve(resolutionContext);
                    if (attribute != null) {
                        scriptContext.setAttribute(attribute.getId(), attribute, ScriptContext.ENGINE_SCOPE);
                    }
                }
            }
        }

        return scriptContext;
    }
}
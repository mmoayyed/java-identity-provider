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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Set;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.shibboleth.idp.ComponentValidationException;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition the computes the attribute definition by executing a script written in some JSR-223
 * supporting language.
 */
public class ScriptedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ScriptedAttributeDefinition.class);

    /** The scripting language. */
    private final String scriptLanguage;

    /** The script to execute. */
    private final String script;

    /** The script engine to execute the script. */
    private final ScriptEngine scriptEngine;

    /** The compiled form of the script, if the script engine supports compiling. */
    private CompiledScript compiledScript;

    /**
     * Constructor.
     * 
     * @param id the identifier of the resolver definition
     * @param theLanguage which language the script is in
     * @param theScript the text of the script
     */
    public ScriptedAttributeDefinition(String id, String theLanguage, String theScript) {
        super(id);
        scriptLanguage = theLanguage;
        script = theScript;
        ScriptEngineManager sem = new ScriptEngineManager();
        scriptEngine = sem.getEngineByName(scriptLanguage);
        compileScript();

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

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        if (scriptEngine == null) {
            log.error("ScriptletAttributeDefinition " + getId()
                    + " unable to create scripting engine for the language: " + scriptLanguage);
            throw new ComponentValidationException("ScriptletAttributeDefinition " + getId()
                    + " unable to create scripting engine for the language: " + scriptLanguage);
        }
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<ResolverPluginDependency> depends = getDependencies();
        ScriptContext context = getScriptContext(resolutionContext);

        if (null == depends) {
            log.info("Scripted definition " + getId() + " had no dependencies");
            return null;
        }
        try {
            if (compiledScript != null) {
                compiledScript.eval(context);
            } else {
                scriptEngine.eval(script, context);
            }

        } catch (ScriptException e) {
            log.error("ScriptletAttributeDefinition " + getId() + " unable to execute script", e);
            throw new AttributeResolutionException("ScriptletAttributeDefinition " + getId()
                    + " unable to execute script", e);
        }

        return (Attribute<?>) context.getAttribute(getId());
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
            log.warn("{} unable to compile even though the scripting engine supports this functionality: {}", getId(),
                    e.toString());
            log.debug("Fails", e);
        }
    }

    /**
     * Creates the script execution context from the resolution context.
     * 
     * Inserts all the dependent (shibboleth) attributes as (script) attributes.
     * It also adds a (script) attribute called 'requestContext' with our context.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return constructed script context
     * 
     * @throws AttributeResolutionException thrown if dependent data connectors or attribute definitions can not be
     *             resolved
     */
    protected ScriptContext getScriptContext(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute(getId(), null, ScriptContext.ENGINE_SCOPE);
        Set<ResolverPluginDependency> depends = getDependencies();

        scriptContext.setAttribute("requestContext", resolutionContext, ScriptContext.ENGINE_SCOPE);

        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                scriptContext.setAttribute(dependentAttribute.getId(), dependentAttribute, ScriptContext.ENGINE_SCOPE);
            }
        }
        return scriptContext;
    }
}
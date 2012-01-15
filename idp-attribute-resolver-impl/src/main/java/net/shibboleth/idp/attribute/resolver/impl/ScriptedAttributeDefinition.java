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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that computes the attribute definition by executing a script written in some JSR-223
 * supporting language.
 */
@ThreadSafe
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
     * The the language.
     * 
     * @param theLanguage which language the script is in
     */
    public synchronized void setScriptLanguage(final String theLanguage) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Scrpited Attribute definition " + getId()
                    + " has already been initialized, language value can not be changed.");
        }
        scriptLanguage = StringSupport.trimOrNull(theLanguage);
    }

    /**
     * Gets the scripting language used.
     * 
     * @return scripting language used. After initialization, this is always a valid language
     */
    public String getScriptLanguage() {
        return scriptLanguage;
    }

    /**
     * Set the actual script.
     * 
     * @param theScript the text of the script
     */
    public synchronized void setScript(final String theScript) {
        script = StringSupport.trimOrNull(theScript);
    }

    /**
     * Gets the script that will be executed.
     * 
     * @return script that will be executed. After initialization, this is never null or empty.
     */
    public String getScript() {
        return script;
    }

    /**
     * Get this resolver's script engine.
     * 
     * @return the engine. After initialization, this is never null.
     */
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    /**
     * Get the compiled script for this engine. Note that this will be null if the language does not support
     * compilation. Only valid after initialization.
     * 
     * @return the compiled script.
     */
    public CompiledScript getCompiledScript() {
        return compiledScript;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == scriptLanguage) {
            throw new ComponentInitializationException("Mapped Attribute definition " + getId()
                    + " is being initialized, without a language being set");
        }

        final ScriptEngineManager sem = new ScriptEngineManager();
        scriptEngine = sem.getEngineByName(scriptLanguage);
        if (null == scriptEngine) {
            throw new ComponentInitializationException("Mapped Attribute definition " + getId()
                    + ". Unable to create scripting engine for the language:  " + getScriptLanguage());
        }
        
        if (null == script) {
            throw new ComponentInitializationException("Mapped Attribute definition " + getId()
                    + " is being initialized, without a script being set");
        }
        compiledScript = compileScript();
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            log.info("Scripted definition " + getId() + " had no dependencies");
            return null;
        }

        final ScriptContext context = getScriptContext(resolutionContext);
        try {
            if (compiledScript != null) {
                compiledScript.eval(context);
            } else {
                scriptEngine.eval(script, context);
            }

        } catch (ScriptException e) {
            final String message = "ScriptletAttributeDefinition " + getId() + " unable to execute script";
            log.error(message, e);
            throw new AttributeResolutionException(message, e);
        }

        return (Attribute<?>) context.getAttribute(getId());
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
            log.warn("{} unable to compile even though the scripting engine supports this functionality: {}", getId(),
                    e.toString());
            log.debug("Fails", e);
        }
        return null;
    }

    /**
     * Creates the script execution context from the resolution context.
     * 
     * Inserts all the dependent (shibboleth) attributes as (script) attributes. It also adds a (script) attribute
     * called 'requestContext' with our context.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return constructed script context
     * 
     * @throws AttributeResolutionException thrown if dependent data connectors or attribute definitions can not be
     *             resolved
     */
    protected ScriptContext getScriptContext(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute(getId(), null, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("requestContext", resolutionContext, ScriptContext.ENGINE_SCOPE);

        for (ResolverPluginDependency dep : getDependencies()) {
            final Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                scriptContext.setAttribute(dependentAttribute.getId(), dependentAttribute, ScriptContext.ENGINE_SCOPE);
            }
        }
        return scriptContext;
    }
}
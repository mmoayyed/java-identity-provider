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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * An attribute definition that computes the attribute definition by executing a script written in some JSR-223
 * supporting language.
 */
@ThreadSafe
public class ScriptedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ScriptedAttributeDefinition.class);

    /** Script to be evaluated. */
    private EvaluableScript script;

    /**
     * Gets the script to be evaluated.
     * 
     * @return the script to be evaluated
     */
    public EvaluableScript getScript() {
        return script;
    }

    /**
     * Sets the script to be evaluated.
     * 
     * @param matcherScript the script to be evaluated
     */
    public synchronized void setScript(EvaluableScript matcherScript) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        script = matcherScript;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            throw new ComponentInitializationException("Attribute definition " + getId()
                    + " no script has been provided");
        }
    }

    /** {@inheritDoc} */
    protected Optional<Attribute> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            log.info("Scripted definition " + getId() + " had no dependencies");
            return null;
        }

        final ScriptContext context = getScriptContext(resolutionContext);
        try {
            script.eval(context);
        } catch (ScriptException e) {
            final String message = "ScriptletAttributeDefinition " + getId() + " unable to execute script";
            log.error(message, e);
            throw new AttributeResolutionException(message, e);
        }

        return Optional.fromNullable((Attribute) context.getAttribute(getId()));
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

        Map<String, Set<AttributeValue>> dependencyAttributes =
                PluginDependencySupport.getAllAttributeValues(resolutionContext, getDependencies());
        for (Entry<String, Set<AttributeValue>> dependencyAttribute : dependencyAttributes.entrySet()) {
            scriptContext.setAttribute(dependencyAttribute.getKey(), dependencyAttribute.getValue(),
                    ScriptContext.ENGINE_SCOPE);
        }

        return scriptContext;
    }
}
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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.provider.V2SAMLProfileRequestContext;

/**
 * An {@link BaseAttributeDefinition} that executes a script in order to populate the values of the generated attribute.
 * 
 * <p>
 * The evaluated script has access to the following information:
 * <ul>
 * <li>A script attribute whose name is the ID of this attribute definition and whose value is a newly constructed
 * {@link IdPAttribute}.</li>
 * <li>A script attribute whose name is <code>context</code> and whose value is the current
 * {@link AttributeResolutionContext}</li>
 * <li>A script attribute for every attribute produced by the dependencies of this attribute definition. The name of the
 * script attribute is the ID of the {@link IdPAttribute} and its value is the {@link Set} of {@link AttributeValue} for
 * the attribute.</li>
 * </ul>
 * </p>
 * <p>
 * The evaluated script should populated the values of the newly constructed {@link IdPAttribute} mentioned above. No
 * other information from the script will be taken in to account.
 * </p>
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
    @Nullable @NonnullAfterInit public EvaluableScript getScript() {
        return script;
    }

    /**
     * Sets the script to be evaluated.
     * 
     * @param definitionScript the script to be evaluated
     */
    public synchronized void setScript(@Nonnull EvaluableScript definitionScript) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        script = Constraint.isNotNull(definitionScript, "Attribute definition script can not be null");
    }

    /** {@inheritDoc} */
    @Nullable protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        final ScriptContext context = getScriptContext(resolutionContext);

        try {
            script.eval(context);
        } catch (ScriptException e) {
            throw new ResolutionException(getLogPrefix() + " unable to execute script", e);
        }
        Object result = context.getAttribute(getId());

        if (null == result) {
            log.info("{} no value returned", getLogPrefix());
            return null;
        }

        if (result instanceof ScriptedIdPAttribute) {

            ScriptedIdPAttribute scriptedAttribute = (ScriptedIdPAttribute) result;
            return scriptedAttribute.getResultingAttribute();

        } else {

            throw new ResolutionException(getLogPrefix() + " returned variable was of wrong type ("
                    + result.getClass().toString() + ")");
        }

    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            throw new ComponentInitializationException(getLogPrefix() + " no script was configured");
        }
    }

    /**
     * Constructs the {@link ScriptContext} used when evaluating the script.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return constructed script context
     * 
     * @throws ResolutionException thrown if dependent data connectors or attribute definitions can not be resolved
     */
    @Nonnull private ScriptContext getScriptContext(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        final Map<String, Set<AttributeValue<?>>> dependencyAttributes =
                PluginDependencySupport.getAllAttributeValues(resolutionContext, getDependencies());

        if (dependencyAttributes.containsKey(getId())) {
            log.debug("{} to-be-populated attribute is a dependency.  Not created", getLogPrefix());
        } else {
            log.debug("{} adding to-be-populated attribute to script context", getLogPrefix());
            final IdPAttribute newAttribute = new IdPAttribute(getId());
            scriptContext.setAttribute(getId(), new ScriptedIdPAttribute(newAttribute, getLogPrefix()),
                    ScriptContext.ENGINE_SCOPE);
        }

        log.debug("{} adding current attribute resolution context to script context", getLogPrefix());
        scriptContext.setAttribute("resolutionContext", resolutionContext, ScriptContext.ENGINE_SCOPE);

        log.debug("{} adding emulated V2 request context context to script context", getLogPrefix());
        scriptContext.setAttribute("requestContext", new V2SAMLProfileRequestContext(resolutionContext, getId()),
                ScriptContext.ENGINE_SCOPE);

        for (Entry<String, Set<AttributeValue<?>>> dependencyAttribute : dependencyAttributes.entrySet()) {
            log.debug("{} adding dependant attribute '{}' with the following values to the script context: {}",
                    new Object[] {getLogPrefix(), dependencyAttribute.getKey(), dependencyAttribute.getValue(),});
            final IdPAttribute pseudoAttribute = new IdPAttribute(dependencyAttribute.getKey());
            pseudoAttribute.setValues(dependencyAttribute.getValue());

            scriptContext.setAttribute(dependencyAttribute.getKey(), new ScriptedIdPAttribute(pseudoAttribute,
                    getLogPrefix()), ScriptContext.ENGINE_SCOPE);
        }

        return scriptContext;
    }
}
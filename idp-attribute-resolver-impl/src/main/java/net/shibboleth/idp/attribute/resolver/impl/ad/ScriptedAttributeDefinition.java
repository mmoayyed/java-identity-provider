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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * An {@link BaseAttributeDefinition} that executes a script in order to populate the values of the generated attribute.
 * 
 * <p>
 * The evaluated script has access to the following information:
 * <ul>
 * <li>A script attribute whose name is the ID of this attribute definition and whose value is a newly constructed
 * {@link Attribute}.</li>
 * <li>A script attribute whose name is <code>context</code> and whose value is the current
 * {@link AttributeResolutionContext}</li>
 * <li>A script attribute for every attribute produced by the dependencies of this attribute definition. The name of the
 * script attribute is the ID of the {@link Attribute} and its value is the {@link Set} of {@link AttributeValue} for
 * the attribute.</li>
 * </ul>
 * </p>
 * <p>
 * The evaluated script should populated the values of the newly constructed {@link Attribute} mentioned above. No other
 * information from the script will be taken in to account.
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
    @Nullable public EvaluableScript getScript() {
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
    @Nonnull protected Optional<Attribute> doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        final ScriptContext context = getScriptContext(resolutionContext);

        try {
            script.eval(context);
        } catch (ScriptException e) {
            throw new AttributeResolutionException("ScriptletAttributeDefinition " + getId()
                    + " unable to execute script", e);
        }

        return Optional.fromNullable((Attribute) context.getAttribute(getId()));
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no script was configured");
        }
    }

    /**
     * Constructs the {@link ScriptContext} used when evaluating the script.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return constructed script context
     * 
     * @throws AttributeResolutionException thrown if dependent data connectors or attribute definitions can not be
     *             resolved
     */
    @Nonnull private ScriptContext getScriptContext(@Nonnull final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Constraint.isNotNull(resolutionContext, "Attribute resolution context can not be null");

        final SimpleScriptContext scriptContext = new SimpleScriptContext();

        log.debug("Attribute definition '{}': adding to-be-populated attribute to script context", getId());
        scriptContext.setAttribute(getId(), new Attribute(getId()), ScriptContext.ENGINE_SCOPE);

        log.debug("Attribute definition '{}': adding current attribute resolution context to script context", getId());
        scriptContext.setAttribute("requestContext", resolutionContext, ScriptContext.ENGINE_SCOPE);

        final Map<String, Set<AttributeValue>> dependencyAttributes =
                PluginDependencySupport.getAllAttributeValues(resolutionContext, getDependencies());
        for (Entry<String, Set<AttributeValue>> dependencyAttribute : dependencyAttributes.entrySet()) {
            log.debug("Attribute definition '{}': adding dependant attribute '{}' "
                    + " with the following values to the script context: {}", new Object[] {getId(),
                    dependencyAttribute.getKey(), dependencyAttribute.getValue(),});
            scriptContext.setAttribute(dependencyAttribute.getKey(), dependencyAttribute.getValue(),
                    ScriptContext.ENGINE_SCOPE);
        }

        return scriptContext;
    }
}
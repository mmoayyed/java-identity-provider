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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.AbstractScriptEvaluator;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.provider.V2SAMLProfileRequestContext;

/**
 * An {@link net.shibboleth.idp.attribute.resolver.AttributeDefinition} that executes a script in order to populate the
 * values of the generated attribute.
 * 
 * <p>
 * The evaluated script has access to the following information:
 * <ul>
 * <li>A script attribute whose name is the ID of this attribute definition and whose value is a newly constructed
 * {@link IdPAttribute}.</li>
 * <li>A script attribute whose name is <code>context</code> and whose value is the current
 * {@link AttributeResolutionContext}</li>
 * <li>A script attribute for every attribute produced by the dependencies of this attribute definition. The name of the
 * script attribute is the ID of the {@link IdPAttribute} and its value is the {@link List} of {@link IdPAttributeValue}
 * for the attribute.</li>
 * </ul>
 * </p>
 * <p>
 * The evaluated script should populate the values of the newly constructed {@link IdPAttribute} mentioned above. No
 * other information from the script will be taken in to account.
 * </p>
 */
@ThreadSafe
public class ScriptedAttributeDefinition extends AbstractAttributeDefinition {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedAttributeDefinition.class);

    /** Script to be evaluated. */
    @NonnullAfterInit private EvaluableScript script;
    
    /** Evaluator. */
    @NonnullAfterInit private AttributeDefinitionScriptEvaluator scriptEvaluator;

    /** Strategy used to locate the {@link ProfileRequestContext} to use. */
    @Nonnull private Function<AttributeResolutionContext,ProfileRequestContext> prcLookupStrategy;

    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;

    /** The custom object we inject into all scripts. */
    @Nullable private Object customObject;

    /** Constructor. */
    public ScriptedAttributeDefinition() {
        // Defaults to ProfileRequestContext -> AttributeContext.
        prcLookupStrategy = new ParentContextLookup<>();
        scLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }

    /**
     * Return the custom (externally provided) object.
     * 
     * @return the custom object
     */
    @Nullable public Object getCustomObject() {
        return customObject;
    }

    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    @Nullable public void setCustomObject(final Object object) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        customObject = object;
    }

    /**
     * Gets the script to be evaluated.
     * 
     * @return the script to be evaluated
     */
    @NonnullAfterInit public EvaluableScript getScript() {
        return script;
    }

    /**
     * Sets the script to be evaluated.
     * 
     * @param definitionScript the script to be evaluated
     */
    public void setScript(@Nonnull final EvaluableScript definitionScript) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        script = Constraint.isNotNull(definitionScript, "Attribute definition script cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link ProfileRequestContext} associated with a given
     * {@link AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate the {@link ProfileRequestContext} associated with a given
     *            {@link AttributeResolutionContext}
     */
    public void setProfileRequestContextLookupStrategy(
            @Nonnull final Function<AttributeResolutionContext,ProfileRequestContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        prcLookupStrategy = Constraint.isNotNull(strategy, "ProfileRequestContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link SubjectContext} associated with a given
     * {@link AttributeResolutionContext}.
     * 
     * @param strategy strategy used to locate the {@link SubjectContext} associated with a given
     *            {@link AttributeResolutionContext}
     */
    public void
            setSubjectContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        scLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            throw new ComponentInitializationException(getLogPrefix() + " no script was configured");
        }
        
        scriptEvaluator = new AttributeDefinitionScriptEvaluator(script);
        scriptEvaluator.setCustomObject(customObject);
        scriptEvaluator.setLogPrefix(getLogPrefix());
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttribute doAttributeDefinitionResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
        Constraint.isNotNull(resolutionContext, "AttributeResolutionContext cannot be null");
        Constraint.isNotNull(workContext, "AttributeResolverWorkContext cannot be null");

        return scriptEvaluator.execute(resolutionContext, workContext);
    }

    /**
     * Evaluator bound to the AttributeDefinition semantic.
     */
    private class AttributeDefinitionScriptEvaluator extends AbstractScriptEvaluator {

        /**
         * Constructor.
         * 
         * @param theScript the script we will evaluate.
         */
        public AttributeDefinitionScriptEvaluator(@Nonnull final EvaluableScript theScript) {
            super(theScript);
        }

        /**
         * Execution hook.
         * 
         * @param resolutionContext resolution context
         * @param workContext work context
         * 
         * @return script result
         * @throws ResolutionException if the script fails
         */
        @Nullable protected IdPAttribute execute(@Nonnull final AttributeResolutionContext resolutionContext,
                @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
            try {
                return (IdPAttribute) evaluate(resolutionContext, workContext);
            } catch (final RuntimeException e) {
                throw new ResolutionException(getLogPrefix() + "Script did not run successfully", e);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {

            final Map<String, List<IdPAttributeValue<?>>> dependencyAttributes =
                    PluginDependencySupport.getAllAttributeValues(
                            (AttributeResolverWorkContext) input[1], 
                            getAttributeDependencies(), 
                            getDataConnectorDependencies());

            if (dependencyAttributes.containsKey(getId())) {
                log.debug("{} The attribute ID to be populated is a dependency, not created", getLogPrefix());
            } else {
                log.debug("{} Adding to-be-populated attribute to script context", getLogPrefix());
                final IdPAttribute newAttribute = new IdPAttribute(getId());
                scriptContext.setAttribute(getId(), new ScriptedIdPAttributeImpl(newAttribute, getLogPrefix()),
                        ScriptContext.ENGINE_SCOPE);
            }

            log.debug("{} Adding contexts to script context", getLogPrefix());
            scriptContext.setAttribute("resolutionContext", input[0], ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("workContext",
                    new DelegatedWorkContext((AttributeResolverWorkContext) input[1], getLogPrefix()),
                    ScriptContext.ENGINE_SCOPE);
            
            final ProfileRequestContext prc = prcLookupStrategy.apply((AttributeResolutionContext) input[0]);
            if (null == prc) {
                log.error("{} ProfileRequestContext could not be located", getLogPrefix());
            }
            scriptContext.setAttribute("profileContext", prc, ScriptContext.ENGINE_SCOPE);

            final SubjectContext sc = scLookupStrategy.apply(prc);
            if (null == sc) {
                log.debug("{} Could not locate SubjectContext", getLogPrefix());
            } else {
                final List<Subject> subjects = sc.getSubjects();
                if (null == subjects) {
                    log.debug("{} Could not locate Subjects", getLogPrefix());
                } else {
                    scriptContext.setAttribute("subjects", subjects.toArray(new Subject[subjects.size()]),
                            ScriptContext.ENGINE_SCOPE);
                }
            }

            log.debug("{} Adding emulated V2 request context to script context", getLogPrefix());
            scriptContext.setAttribute("requestContext",
                    new V2SAMLProfileRequestContext((AttributeResolutionContext) input[0], getId()),
                    ScriptContext.ENGINE_SCOPE);

            for (final Entry<String,List<IdPAttributeValue<?>>> dependencyAttribute : dependencyAttributes.entrySet()) {
                log.trace("{} Adding dependent attribute '{}' with the following values to the script context: {}",
                        new Object[] {getLogPrefix(), dependencyAttribute.getKey(), dependencyAttribute.getValue(),});
                final IdPAttribute pseudoAttribute = new IdPAttribute(dependencyAttribute.getKey());
                pseudoAttribute.setValues(dependencyAttribute.getValue());

                scriptContext.setAttribute(dependencyAttribute.getKey(),
                        new ScriptedIdPAttributeImpl(pseudoAttribute, getLogPrefix()), ScriptContext.ENGINE_SCOPE);
            }
        }

        /** {@inheritDoc} */
        @Override
        @Nullable protected Object finalizeContext(@Nonnull final ScriptContext scriptContext,
                @Nullable final Object scriptResult) throws ScriptException {
            
            final Object result = scriptContext.getAttribute(getId());
            if (null == result) {
                log.info("{} No value returned", getLogPrefix());
                return null;
            }

            if (result instanceof ScriptedIdPAttributeImpl) {
                final ScriptedIdPAttributeImpl scriptedAttribute = (ScriptedIdPAttributeImpl) result;
                try {
                    return scriptedAttribute.getResultingAttribute();
                } catch (final ResolutionException e) {
                    throw new ScriptException(e);
                }
            } else {
                throw new ScriptException(getLogPrefix() + " returned variable was of wrong type ("
                        + result.getClass().toString() + ")");
            }
        }
    }

}
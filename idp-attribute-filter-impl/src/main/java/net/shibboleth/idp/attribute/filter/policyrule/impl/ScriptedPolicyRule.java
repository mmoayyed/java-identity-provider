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

package net.shibboleth.idp.attribute.filter.policyrule.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.script.ScriptContext;
import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.AbstractScriptEvaluator;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * A {@link net.shibboleth.idp.attribute.filter.PolicyRequirementRule} that delegates to a JSR-223 script for its actual
 * processing.
 * 
 */
@ThreadSafe
public class ScriptedPolicyRule extends AbstractIdentifiableInitializableComponent implements PolicyRequirementRule,
        UnmodifiableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedPolicyRule.class);

    /** Script to be evaluated. */
    @NonnullAfterInit private EvaluableScript script;

    /** Evaluator. */
    @NonnullAfterInit private PolicyRuleScriptEvaluator scriptEvaluator;

    /** Strategy used to locate the {@link ProfileRequestContext} to use. */
    @Nonnull private Function<AttributeFilterContext,ProfileRequestContext> prcLookupStrategy;

    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;

    /** The custom object we inject into all scripts. */
    @Nullable private Object customObject;

    /** Constructor. */
    public ScriptedPolicyRule() {
        // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
        prcLookupStrategy =
                Functions.compose(new ParentContextLookup<RelyingPartyContext, ProfileRequestContext>(),
                        new ParentContextLookup<AttributeFilterContext, RelyingPartyContext>());
        scLookupStrategy = new ChildContextLookup<ProfileRequestContext, SubjectContext>(SubjectContext.class);
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
    public void setCustomObject(@Nullable final Object object) {
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
     * @param matcherScript the script to be evaluated
     */
    public void setScript(@Nonnull final EvaluableScript matcherScript) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        script = Constraint.isNotNull(matcherScript, "Attribute value matching script can not be null");
    }

    /**
     * Set the strategy used to locate the {@link ProfileRequestContext} associated with a given
     * {@link AttributeFilterContext}.
     * 
     * @param strategy strategy used to locate the {@link ProfileRequestContext} associated with a given
     *            {@link AttributeFilterContext}
     */
    public void setProfileRequestContextLookupStrategy(
            @Nonnull final Function<AttributeFilterContext, ProfileRequestContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        prcLookupStrategy = Constraint.isNotNull(strategy, "ProfileRequestContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link SubjectContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link SubjectContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void
            setSubjectContextLookupStrategy(@Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        scLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Calculate the PolicyRule.
     * <p>
     * When the script is evaluated, the following property will be available via the {@link ScriptContext}:
     * <ul>
     * <li><code>filterContext</code> - the current instance of {@link AttributeFilterContext}</li>
     * </ul>
     * The script <strong>MUST</strong> return a {@link java.lang.Boolean}
     * </p>
     * {@inheritDoc}
     */
    @Override
    @Nonnull public Tristate matches(@Nonnull final AttributeFilterContext filterContext) {
        Constraint.isNotNull(filterContext, "Attribute filter context cannot be null");

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        return scriptEvaluator.execute(filterContext);
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            // never met so long as we have the assert in the constructor
            throw new ComponentInitializationException("No script has been provided");
        }

        scriptEvaluator = new PolicyRuleScriptEvaluator(script);
        scriptEvaluator.setCustomObject(customObject);
        
        final StringBuilder builder = new StringBuilder("Scripted Attribute Filter '").append(getId()).append("':");
        scriptEvaluator.setLogPrefix(builder.toString());
    }

    /** {@inheritDoc} */
    @Override public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ScriptedPolicyRule)) {
            return false;
        }

        final ScriptedPolicyRule other = (ScriptedPolicyRule) obj;

        return script.equals(other.getScript());
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return Objects.hashCode(script, getId());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("Script", getScript()).toString();
    }

    /**
     * Evaluator bound to the Matcher semantic.
     */
    private class PolicyRuleScriptEvaluator extends AbstractScriptEvaluator {

        /**
         * Constructor.
         * 
         * @param theScript the script we will evaluate.
         */
        public PolicyRuleScriptEvaluator(@Nonnull final EvaluableScript theScript) {
            super(theScript);
            // Guarantee result is a Boolean.
            setOutputType(Boolean.class);
            // Return a FAIL result on errors.
            setReturnOnError(Tristate.FAIL);
            // Turn ScriptException into default error result (FAIL).
            setHideExceptions(true);
        }
        
        /**
         * Execution hook.
         * 
         * @param filterContext filter context
         * 
         * @return script result
         */
        @Nonnull public Tristate execute(@Nonnull final AttributeFilterContext filterContext) {

            final Object result = evaluate(filterContext);
            if (null == result) {
                log.error("{} Matcher script did not return a result", getLogPrefix());
                return Tristate.FAIL;
            }

            // Non-null result can only be a Boolean or Tristate.
            if (result instanceof Boolean) {
                return ((Boolean) result).booleanValue() ? Tristate.TRUE : Tristate.FALSE;
            } else {
                return (Tristate) result;
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {

            scriptContext.setAttribute("filterContext", input[0], ScriptContext.ENGINE_SCOPE);
            
            final ProfileRequestContext prc = prcLookupStrategy.apply((AttributeFilterContext) input[0]);
            scriptContext.setAttribute("profileContext", prc, ScriptContext.ENGINE_SCOPE);

            final SubjectContext sc;
            if (null == prc) {
                log.error("{} Could not locate ProfileRequestContext", getLogPrefix());
                sc = null;
            } else {
                sc = scLookupStrategy.apply(prc);
            }

            if (null == sc) {
                log.warn("{} Could not locate SubjectContext", getLogPrefix());
            } else {
                final List<Subject> subjects = sc.getSubjects();
                if (null == subjects) {
                    log.warn("{} Could not locate Subjects", getLogPrefix());
                } else {
                    scriptContext.setAttribute("subjects", subjects.toArray(new Subject[subjects.size()]),
                            ScriptContext.ENGINE_SCOPE);
                }
            }
        }
    }

}
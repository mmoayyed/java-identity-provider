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

package net.shibboleth.idp.attribute.filter.matcher.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.script.ScriptContext;
import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
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

import com.google.common.base.MoreObjects;

/**
 * A {@link net.shibboleth.idp.attribute.filter.Matcher} that delegates to a JSR-223 script for its actual processing.
 * 
 */
@ThreadSafe
public class ScriptedMatcher extends AbstractIdentifiableInitializableComponent implements Matcher,
        UnmodifiableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedMatcher.class);

    /** The script to evaluate. */
    @NonnullAfterInit private EvaluableScript script;

    /** Evaluator. */
    @NonnullAfterInit private MatcherScriptEvaluator scriptEvaluator;

    /** Custom object for script. */
    @Nullable private Object customObject;
    
    /** Strategy used to locate the {@link ProfileRequestContext} to use. */
    @Nonnull private Function<AttributeFilterContext,ProfileRequestContext> prcLookupStrategy;

    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> scLookupStrategy;

    /** Constructor. */
    public ScriptedMatcher() {
        // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
        prcLookupStrategy =
                new ParentContextLookup<>(ProfileRequestContext.class).compose(
                        new ParentContextLookup<>(RelyingPartyContext.class));
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

        script = Constraint.isNotNull(matcherScript, "Attribute value matching script cannot be null");
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
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        scLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (null == script) {
            throw new ComponentInitializationException("No script has been provided");
        }
        
        scriptEvaluator = new MatcherScriptEvaluator(script);
        scriptEvaluator.setCustomObject(customObject);
        
        final StringBuilder builder = new StringBuilder("Scripted Attribute Filter '").append(getId()).append("':");
        scriptEvaluator.setLogPrefix(builder.toString());
    }
    
    /**
     * Perform the AttributeValueMatching.
     * <p>
     * When the script is evaluated, the following properties will be available via the {@link ScriptContext}:
     * <ul>
     * <li><code>filterContext</code> - the current instance of {@link AttributeFilterContext}</li>
     * <li><code>attribute</code> - the attribute whose values are to be evaluated
     * </ul>
     * The script <strong>MUST</strong> return a {@link Set} containing the {@link IdPAttributeValue} objects that were
     * matched.
     * </p>
     * {@inheritDoc}
     */
    @Override @Nullable @NonnullElements @Unmodifiable public Set<IdPAttributeValue> getMatchingValues(
            @Nonnull final IdPAttribute attribute, @Nonnull final AttributeFilterContext filterContext) {
        Constraint.isNotNull(attribute, "Attribute to be filtered cannot be null");
        Constraint.isNotNull(filterContext, "AttributeFilterContext cannot be null");
        
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        return scriptEvaluator.execute(attribute, filterContext);
    }

    /** {@inheritDoc} */
    @Override public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ScriptedMatcher)) {
            return false;
        }

        final ScriptedMatcher other = (ScriptedMatcher) obj;

        return script.equals(other.getScript());
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return Objects.hash(script, getId());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("Script", getScript()).toString();
    }

    /**
     * Evaluator bound to the Matcher semantic.
     */
    private class MatcherScriptEvaluator extends AbstractScriptEvaluator {

        /**
         * Constructor.
         * 
         * @param theScript the script we will evaluate.
         */
        public MatcherScriptEvaluator(@Nonnull final EvaluableScript theScript) {
            super(theScript);
            // Guarantee result is a Set or null.
            setOutputType(Set.class);
            // Turn ScriptException result into default "error" result, which is left at null.
            setHideExceptions(true);
        }

        /**
         * Execution hook.
         * 
         * @param attribute attribute to be filtered
         * @param filterContext filter context
         * 
         * @return script result
         */
        @Nullable @NonnullElements @Unmodifiable public Set<IdPAttributeValue> execute(
                @Nonnull final IdPAttribute attribute, @Nonnull final AttributeFilterContext filterContext) {
            final Object result = evaluate(attribute, filterContext);
            if (null == result) {
                log.error("{} Matcher script did not return a result", getLogPrefix());
                return null;
            }

            final Set<IdPAttributeValue> returnValues = new LinkedHashSet<>(attribute.getValues());
            returnValues.retainAll((Set<?>) result);
            return Collections.unmodifiableSet(returnValues);
        }

        /** {@inheritDoc} */
        @Override
        protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {

            scriptContext.setAttribute("attribute", input[0], ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("filterContext", input[1], ScriptContext.ENGINE_SCOPE);
            
            final ProfileRequestContext prc = prcLookupStrategy.apply((AttributeFilterContext) input[1]);
            scriptContext.setAttribute("profileContext", prc, ScriptContext.ENGINE_SCOPE);
            
            final SubjectContext sc;
            if (null == prc) {
                log.error("{} Could not locate ProfileRequestContext", getLogPrefix());
                sc = null;
            }  else {
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

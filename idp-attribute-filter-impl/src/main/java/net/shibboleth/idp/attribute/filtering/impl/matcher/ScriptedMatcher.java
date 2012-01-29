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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractDestructableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import com.google.common.base.Optional;

/**
 * A {@link AttributeValueMatcher} that delegates to a JSR-223 script for its actual processing.
 * 
 * <p>
 * When the script is evaluated, the following properties will be available via the {@link ScriptContext}:
 * <ul>
 * <li><code>filterContext</code> - the current instance of {@link AttributeFilterContext}</li>
 * <li><code>attribute</code> - the attribute whose values are to be evaluated
 * </ul>
 * The script <strong>MUST</strong> return a {@link Set} containing the {@link AttributeValue} objects that were
 * matched.
 * </p>
 */
@ThreadSafe
public class ScriptedMatcher extends AbstractDestructableInitializableComponent implements AttributeValueMatcher,
        UnmodifiableComponent {

    /** Script to be evaluated. */
    private EvaluableScript script;

    /**
     * Constructor.
     *
     * @param matchingScript script used to determine matching attibute values
     */
    public ScriptedMatcher(@Nonnull EvaluableScript matchingScript){
        setScript(matchingScript);
    }
    
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
    protected void setScript(@Nonnull EvaluableScript matcherScript) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException();
        }

        script = Assert.isNotNull(matcherScript, "Attribute value matching script can not be null");
    }

    /** {@inheritDoc} */
    @Nonnull @NonnullElements @Unmodifiable public Set<AttributeValue> getMatchingValues(@Nonnull Attribute attribute,
            @Nonnull AttributeFilterContext filterContext) throws AttributeFilteringException {
        assert attribute != null : "Attribute to be filtered can not be null";
        assert filterContext != null : "Attribute filter contet can not be null";

        final EvaluableScript currentScript = script;
        ifNotInitializedThrowUninitializedComponentException();
        ifDestroyedThrowDestroyedComponentException();

        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("filterContext", filterContext, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("attribute", attribute, ScriptContext.ENGINE_SCOPE);

        try {
            final Optional<Object> optionalResult = currentScript.eval(scriptContext);
            if (!optionalResult.isPresent()) {
                throw new AttributeFilteringException("Matcher script did not return a result");
            }

            final Object result = optionalResult.get();
            if (result instanceof Set) {
                HashSet<AttributeValue> returnValues = new HashSet<AttributeValue>(attribute.getValues());
                returnValues.retainAll((Set) result);
                return Collections.unmodifiableSet(returnValues);
            } else {
                throw new AttributeFilteringException("Matcher script did not return a Collection");
            }
        } catch (ScriptException e) {
            throw new AttributeFilteringException("Error while executing value matching script", e);
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        script = null;
        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            throw new ComponentInitializationException("No script has been provided");
        }
    }
}
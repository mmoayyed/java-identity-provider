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

package net.shibboleth.idp.attribute.filtering.impl.policy;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.StringSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.UninitializedComponentException;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.opensaml.util.criteria.AbstractBiasedEvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;

/**
 * The basis of all Targetted Regex-based Filter criteria.
 * 
 * Just as for {@link BaseStringCompare}, Principal, AttributeValue, AttributeScope regex criteria all extend this
 * class. This class's job is to just provide the match functor that they call and to police the constructor.
 * 
 */
@ThreadSafe
public abstract class BaseRegexCompare extends AbstractBiasedEvaluableCriterion<AttributeFilterContext> implements
        InitializableComponent, UnmodifiableComponent {

    /** Regular expression to match. */
    private Pattern regex;

    /** The source of the regexp. */
    private String patternText;

    /** Initialization state. */
    private boolean initialized;

    /**
     * Has initialize been called on this object. {@inheritDoc}.
     */
    public final boolean isInitialized() {
        return initialized;
    }

    /** Mark the object as initialized having checked parameters. {@inheritDoc}. */
    public final synchronized void initialize() throws ComponentInitializationException {
        if (isInitialized()) {
            return;
        }

        doInitialize();

        initialized = true;
    }

    /**
     * Set the text of the regexp. Cannot be called after initialization.
     * 
     * @param pattern the pattern we will use.
     */
    public synchronized void setRegularExpression(String pattern) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attempting to set the regexp patter after class initialization");
        }
        patternText = StringSupport.trimOrNull(pattern);
    }

    /**
     * Gets the regular expression to match.
     * 
     * @return regular expression to match
     */
    public String getRegularExpression() {
        return patternText;
    }

    /**
     * Matches the given value against the provided regular expression. {@link Object#toString()} is used to produce the
     * string value to evaluate.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not.
     * @throws EvaluationException if we have not been initialized.
     */
    protected boolean isMatch(final Object value) throws EvaluationException {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Class not initialized");
        }

        if (regex.matcher(value.toString()).matches()) {
            return true;
        }

        return false;
    }

    /**
     * Make any instantiation checks. Inherited classes should re-implement this and call the eponymous method in the
     * super class. Called with the object locked.
     * 
     * @throws ComponentInitializationException if no pattern has been set.
     */
    protected void doInitialize() throws ComponentInitializationException {
        if (null == patternText) {
            throw new ComponentInitializationException(
                    "Regexp comparison criterion being initialized without a valid match pattern being set");
        }

        try {
            regex = Pattern.compile(patternText);
        } catch (PatternSyntaxException e) {
            throw new ComponentInitializationException(e);
        }
    }
}
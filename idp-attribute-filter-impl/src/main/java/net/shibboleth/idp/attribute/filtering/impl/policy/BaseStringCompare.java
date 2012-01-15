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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.util.criteria.AbstractBiasedEvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;

/**
 * The basis of all String-based Filter criteria.
 * 
 * Principal, AttributeValue, AttributeScope criteria all extend this. This class's job is to just provide the match
 * functor that they call.
 */
@ThreadSafe
public abstract class BaseStringCompare extends AbstractBiasedEvaluableCriterion<AttributeFilterContext> implements
        InitializableComponent, UnmodifiableComponent {

    /** String to match for a positive evaluation. */
    private String matchString;

    /** Whether the match evaluation is case sensitive. */
    private boolean caseSensitive;

    /** Whether the match evaluation has been set (no sensible default). */
    private boolean caseSensitiveSet;

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
     * Make any instantiation checks. Inherited classes should re-implement this and call the eponymous method in the
     * super class. Called with the object locked.
     * 
     * @throws ComponentInitializationException if something is not set up correctly.
     */
    protected void doInitialize() throws ComponentInitializationException {
        if (!caseSensitiveSet) {
            throw new ComponentInitializationException(
                    "String comparison criterion being initialized without case sensitivity being set");
        }

        if (null == matchString) {
            throw new ComponentInitializationException(
                    "String comparison criterion being initialized without a valid match string being set");
        }
    }

    /**
     * Set the match string. Cannot be called after initialization.
     * 
     * @param match the string we are matching against.
     */
    public synchronized void setMatchString(final String match) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attempting to set match string after class initialization");
        }
        matchString = StringSupport.trimOrNull(match);
    }

    /**
     * Gets the string to match.
     * 
     * @return string to match, never null or empty after initialization.
     */
    public String getMatchString() {
        return matchString;
    }

    /**
     * Sets the case sensitivity. Cannot be called after initialization.
     * 
     * @param isCaseSensitive whether to do a case sensitive comparison.
     */
    public synchronized void setCaseSensitive(final boolean isCaseSensitive) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attempting to set case sensitivity after class initialization");
        }
        caseSensitive = isCaseSensitive;
        caseSensitiveSet = true;
    }

    /**
     * Gets whether the match evaluation is case sensitive.
     * 
     * @return whether the match evaluation is case sensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Does this provided object match the constructed string. {@link Object#toString()} is used to produce the string
     * value to evaluate.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     * @throws EvaluationException if we have not been initialized.
     */
    public Boolean isMatch(final Object value) throws EvaluationException {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Class not initialized");
        }

        if (caseSensitive) {
            return matchString.equals(value.toString());
        } else {
            return matchString.equalsIgnoreCase(value.toString());
        }
    }

}

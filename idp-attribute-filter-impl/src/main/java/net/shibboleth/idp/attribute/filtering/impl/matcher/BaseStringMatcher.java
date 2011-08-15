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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;

import org.opensaml.util.StringSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;

/**
 * The basis of all String-based Filter matchers.
 * 
 * Principal, AttributeValue, AttributeScope matchers all extend this. This class's job is to just provide the match
 * functor that they call. By making this an initializable and unmodifiable component we can be sure that the
 * caseSensitivity and matchSwtring will not move either relative to each other or during the iteration over an
 * attribute's values.
 */
@ThreadSafe
public abstract class BaseStringMatcher implements InitializableComponent, UnmodifiableComponent {

    /** String to match for a positive evaluation. */
    private String matchString;

    /** Whether the match evaluation is case sensitive. */
    private boolean caseSensitive;

    /** Has case sensitivity been set? */
    private boolean caseSensitiveSet;

    /** Initialized state. */
    private boolean initialized;

    /**
     * Has initialize been called on this object. {@inheritDoc}.
     * */
    public boolean isInitialized() {
        return initialized;
    }

    /** Mark the object as initialized. {@inheritDoc}. */
    public synchronized void initialize() throws ComponentInitializationException {
        if (initialized) {
            throw new ComponentInitializationException("String Matcher being initialized multiple times");
        }
        if (null == matchString) {
            throw new ComponentInitializationException("String Matcher has not had a valid string set");
        }
        if (!caseSensitiveSet) {
            throw new ComponentInitializationException("String Matcher has not had case sentivity set");
        }
        initialized = true;
    }

    /**
     * Set the match string.
     * 
     * @param match the string we are matching against.
     */
    public synchronized void setMatchString(final String match) {
        if (initialized) {
            throw new UnmodifiableComponentException("String expression matcher has already been initialized");
        }
        matchString = StringSupport.trimOrNull(match);
    }

    /**
     * Gets the string to match for a positive evaluation.
     * 
     * @return string to match for a positive evaluation, never null or empty.
     */
    public String getMatchString() {
        return matchString;
    }

    /**
     * Set the case sensitivity of this matcher.
     * 
     * @param isCaseSensitive whether to do a case sensitive comparison.
     */
    public synchronized void setCaseSentitive(final boolean isCaseSensitive) {
        if (initialized) {
            throw new UnmodifiableComponentException("String expression matcher has already been initialized");
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
     * @throws AttributeFilteringException if the component has not be initialized
     * @return true if the value matches the given match string, false if not
     */
    protected Boolean isMatch(final Object value) throws AttributeFilteringException {
        if (!initialized) {
            throw new AttributeFilteringException("String comparison performed on uninitialzed Matcher");
        }
        if (caseSensitive) {
            return matchString.equals(value.toString());
        } else {
            return matchString.equalsIgnoreCase(value.toString());
        }
    }
}

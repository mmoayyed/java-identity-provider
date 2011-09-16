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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;

import org.opensaml.util.StringSupport;
import org.opensaml.util.component.AbstractInitializableComponent;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UninitializedComponentException;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;

/**
 * The basis of all Regex-based Filter matchers.
 * 
 * 
 * Just as for {@link net.shibboleth.idp.attribute.filtering.impl.policy.BaseStringCompare} Principal, AttributeValue,
 * AttributeScope regex matchers all extend this class. This class's job is to just provide the match functor that they
 * call and to police the constructor. <br />
 * 
 * We make this an initializable and unmodifiable functor which allows us to know that the regex will no move under our
 * feet while we are doing the comparison over all of an attribute's values and finesses the issue of getting a
 * PatternSyntaxException during non intializing operation.
 * 
 * 
 */
@ThreadSafe
public abstract class BaseRegexMatcher extends AbstractInitializableComponent implements UnmodifiableComponent {

    /** Regular expression to match. */
    private Pattern regex;

    /** The text of the expression. */
    private String expressionAsText;

    /**
     * Set the expression.
     * 
     * @param expression the regexp under consideration. Must not be null or empty
     */
    public synchronized void setRegularExpression(final String expression) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Regular expression matcher has already been initialized");
        }

        expressionAsText = StringSupport.trimOrNull(expression);
    }

    /**
     * Gets the regular expression to match.
     * 
     * @return regular expression to match. Might return null if no expression provided.
     */
    public String getRegularExpression() {
        return regex.toString();
    }

    /**
     * Matches the given value against the provided regular expression. {@link Object#toString()} is used to produce the
     * string value to evaluate. We do not use our local
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     * @throws AttributeFilteringException if we haven't been initialized
     */
    protected boolean isMatch(final Object value) throws AttributeFilteringException {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Regexp Matcher has not been initialized");
        }

        if (regex.matcher(value.toString()).matches()) {
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == expressionAsText) {
            throw new ComponentInitializationException("No valid pattern provided to Regexp Matcher");
        }

        try {
            regex = Pattern.compile(expressionAsText);
        } catch (PatternSyntaxException e) {
            throw new ComponentInitializationException("Regexp Matcher: Could not compile provided pattern "
                    + expressionAsText, e);
        }
    }
}

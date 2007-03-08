/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider;

import java.util.regex.Pattern;

/**
 * Base class for {@link MatchFunctor} that match a string value against a given regular expression.
 */
public abstract class AbstractRegexMatchFunctor extends AbstractMatchFunctor {

    /** Regular expression to match. */
    private Pattern regex;

    /**
     * Gets the regular expression to match.
     * 
     * @return regular expression to match
     */
    public String getRegularExpression() {
        return regex.pattern();
    }

    /**
     * Sets the regular expression to match.
     * 
     * @param expression regular expression to match
     */
    public void setRegularExpression(String expression) {
        regex = Pattern.compile(expression);
    }

    /**
     * Matches the given value against the provided regular expression. {@link Object#toString()} is used to produce the
     * string value to evaluate.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     */
    protected boolean isMatch(Object value) {
        if (regex == null || value == null) {
            return false;
        }

        if (regex.matcher(value.toString()).matches()) {
            return true;
        }

        return false;
    }
}
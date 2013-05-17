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

/**
 * General {@link net.shibboleth.idp.attribute.filtering.MatchFunctor} for regexp comparison of strings in Attribute
 * Filters.
 */
public abstract class AbstractRegexpStringMatchFunctor extends AbstractValueMatcherFunctor {

    /** Regular expression to match. */
    private Pattern regex;

    /**
     * Gets the regular expression to match.
     * 
     * @return rsegular expression to match
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
     * Matches the given value against the provided regular expression.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     */
    public boolean regexpCompare(String value) {
        if (regex == null || value == null) {
            return false;
        }

        if (regex.matcher(value).matches()) {
            return true;
        }

        return false;
    }

}

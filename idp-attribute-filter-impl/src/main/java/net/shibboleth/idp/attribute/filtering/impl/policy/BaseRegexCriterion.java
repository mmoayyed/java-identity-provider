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

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/**
 * The basis of all String-based Filter criteria.
 * 
 * just as for {@link BaseStringCriterion} Principal, AttributeValue, AttributeScope regex criteria all extend this.
 * This class's job is to just provide the match functor that they call.
 * 
 */
public abstract class BaseRegexCriterion {
    
    /** Regular expression to match. */
    private final Pattern regex;
    
    /**
     * Constructor.  
     *
     * @param expression the regexp under consideration.  Must not be null or empty.
     */
    protected BaseRegexCriterion(final String expression) {
        String exp = StringSupport.trimOrNull(expression);
        Assert.isNotNull(exp, "Null or empy string passed to a Regexp attribute filter");
        regex = Pattern.compile(expression);        
    }

    /** Private Constructor. Here uniquely to guarantee that we always have non null members. */
    @SuppressWarnings("unused")
    private BaseRegexCriterion() {
        Assert.isTrue(false, "Private constructor should not be called");
        regex = null;
    }

    /**
     * Gets the regular expression to match.
     * 
     * @return regular expression to match
     */
    public String getRegularExpression() {
        return regex.pattern();
    }

    /**
     * Matches the given value against the provided regular expression. {@link Object#toString()} is used to produce the
     * string value to evaluate.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     */
    protected boolean isMatch(final Object value) {
        if (regex.matcher(value.toString()).matches()) {
            return true;
        }

        return false;
    }
}

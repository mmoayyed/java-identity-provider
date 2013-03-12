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

package net.shibboleth.idp.attribute.filtering.impl.predicate;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * General Matching Predicate for regexp comparison if strings in Attribute Filters.   
 */
public abstract class BaseRegexpPredicate implements Predicate {

    /** Logger. */
    private Logger log = LoggerFactory.getLogger(BaseRegexpPredicate.class);

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
    public boolean apply(Object value) {
        if (regex == null || value == null) {
            return false;
        }

        if (!(value instanceof String)) {
            log.error("FilterPredicate : Object supplied to String comparison was of class {}, not String", value
                    .getClass().getName());
            return false;
        }

        final String valueAsString = (String) value;

        if (regex.matcher(valueAsString).matches()) {
            return true;
        }

        return false;
    }
    
}

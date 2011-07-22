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

package net.shibboleth.idp.attribute.filtering.impl;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/**
 * The basis of all String-based Filter criteria.
 * 
 * Principal, AttributeValue, AttributeScope criteria all extend this. This class's job is to just provide the match
 * functor that they call.
 */
@ThreadSafe
public abstract class BaseStringCompare {

    /** String to match for a positive evaluation. */
    private final String matchString;

    /** Whether the match evaluation is case sensitive. */
    private final boolean caseSensitive;

    /**
     * Constructor.
     * 
     * @param match the string we are matching against.
     * @param isCaseSensitive whether to do a case sensitive comparison.
     */
    protected BaseStringCompare(final String match, final boolean isCaseSensitive) {
        matchString = StringSupport.trimOrNull(match);
        Assert.isNotNull(matchString,
                "String comparison base Policy Requirement rule must have non null matching string");
        caseSensitive = isCaseSensitive;
    }

    /** Private Constructor. Here uniquely to guarantee that we always have non null members. */
    @SuppressWarnings("unused")
    private BaseStringCompare() {
        Assert.isTrue(false, "Private constructor should not be called");
        matchString = null;
        caseSensitive = true;
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
     * Gets whether the match evaluation is case sensitive.
     * 
     * @return whether the match evaluation is case sensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /** 
     * Does this provided object match the constructed string.  {@link Object#toString()} is used to produce the
     * string value to evaluate.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     */
    public Boolean isMatch(final Object value) {
        if (caseSensitive) {
            return matchString.equals(value.toString());
        } else {
            return matchString.equalsIgnoreCase(value.toString());
        }
    }

}

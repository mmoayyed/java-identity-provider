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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;

/**
 * A match functor that checks if the given attribute has more than the minimum number of values but less than the
 * maximum.
 */
public class NumOfAttributeValuesMatchFunctor implements MatchFunctor {

    /** ID of the attribute that will be checked. */
    private String attributeId;

    /** Minimum allowed number of attribute values. */
    private int minimumValues;

    /** Maximum allowed number of attribute values. */
    private int maximumValues;

    /**
     * Constructor.
     * 
     * @param id ID of the attribute to be checked
     * @param min minimum number of values allowed
     * @param max maximum number of values allowed
     */
    public NumOfAttributeValuesMatchFunctor(String id, int min, int max) {
        attributeId = id;
        minimumValues = min;
        maximumValues = max;
    }

    /** {@inheritDoc} */
    public boolean evaluatePolicyRequirement(ShibbolethFilteringContext filterContext) throws FilterProcessingException {
        return isWithinRange(filterContext.getUnfilteredAttributes().get(attributeId));
    }

    /** {@inheritDoc} */
    public boolean evaluatePermitValue(ShibbolethFilteringContext filterContext, String id, Object value)
            throws FilterProcessingException {
        return isWithinRange(filterContext.getUnfilteredAttributes().get(attributeId));
    }

    /**
     * Checks that the number of values for the given attribute is within the given range.
     * 
     * @param attribute attribute to check
     * 
     * @return true if the attribute has more than the minimum number of values and less than the maximum.
     */
    protected boolean isWithinRange(BaseAttribute attribute) {
        if (attribute == null) {
            return false;
        }

        int numOfValues = attribute.getValues().size();

        return numOfValues >= minimumValues && numOfValues <= maximumValues;
    }
}
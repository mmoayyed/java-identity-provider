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

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;

/**
 * A match function that evaluates an attribute's value against the given match string.
 */
public class AttributeValueStringMatchFunctor extends AbstractAttributeTargetedStringMatchFunctor {

    /**
     * Evaluates to true if any value for the specified attribute matches the given match string.
     * 
     * {@inheritDoc}
     */
    protected boolean doEvaluatePolicyRequirement(ShibbolethFilteringContext filterContext)
            throws FilterProcessingException {
        Attribute attribute = filterContext.getUnfilteredAttributes().get(getAttributeId());
        if (attribute != null && attribute.getValues() != null && !attribute.getValues().isEmpty()) {
            for (Object value : attribute.getValues()) {
                if (isMatch(value)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Evaluates to true if the given attribute value matches the given match string.
     * 
     * {@inheritDoc}
     */
    protected boolean doEvaluatePermitValue(ShibbolethFilteringContext filterContext, String id, Object attributeValue)
            throws FilterProcessingException {

        return isMatch(attributeValue);
    }
}
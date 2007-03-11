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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match;

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.FilterContext;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;

/**
 * A {@link MatchFunctor} that evaluates an attribute's value against the provided regular expression.
 */
public class AttributeValueRegexMatchFunctor extends AbstractRegexMatchFunctor {

    /** ID of the attribute whose values will be evaluated. */
    private String attributeId;

    /**
     * Gets the ID of the attribute whose values will be evaluated.
     * 
     * @return ID of the attribute whose values will be evaluated
     */
    public String getAttributeId() {
        return attributeId;
    }

    /**
     * Sets the ID of the attribute whose values will be evaluated.
     * 
     * @param id ID of the attribute whose values will be evaluated
     */
    public void setAttributeId(String id) {
        attributeId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /**
     * Evaluates to true if any value for the specified attribute matches the provided regular expression.
     * 
     * {@inheritDoc}
     */
    protected boolean doEvaluate(FilterContext filterContext) throws FilterProcessingException {
        if (attributeId == null) {
            throw new FilterProcessingException("No attribute ID specified");
        }

        return matchAttributeValues(filterContext);
    }

    /**
     * Evaluates to true if the given attribute value matches the provided regular expression.
     * 
     * {@inheritDoc}
     */
    protected boolean doEvaluate(FilterContext filterContext, String id, Object attributeValue)
            throws FilterProcessingException {

        if (attributeId == null) {
            return isMatch(attributeValue);
        }

        return matchAttributeValues(filterContext);
    }

    /**
     * Returns true if any value of the given attribute matches the provided regular expression.
     * 
     * @param filterContext current filter context
     * 
     * @return true if a value of the given attribute matches the provided regular expression
     */
    protected boolean matchAttributeValues(FilterContext filterContext) {
        Attribute attribute = filterContext.getAttributes().get(attributeId);
        if (attribute == null || attribute.getValues() == null) {
            return false;
        }

        for (Object value : attribute.getValues()) {
            if (isMatch(value)) {
                return true;
            }
        }

        return false;
    }
}
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
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttribute;

/**
 * Match functor that checks if a {@link ScopedAttribute} scope is equal to a given regular expression.
 */
public class AttributeScopeRegexMatchFunctor extends AbstractRegexMatchFunctor {

    /** ID of the attribute whose scope will be evaluated. */
    private String attributeId;

    /**
     * Gets the ID of the attribute whose scope will be evaluated.
     * 
     * @return ID of the attribute whose scope will be evaluated
     */
    public String getAttributeId() {
        return attributeId;
    }

    /**
     * Sets the ID of the attribute whose scope will be evaluated.
     * 
     * @param id ID of the attribute whose scope will be evaluated
     */
    public void setAttributeId(String id) {
        attributeId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    protected boolean doEvaluatePermitValue(ShibbolethFilteringContext filterContext, String attributeId,
            Object attributeValue) throws FilterProcessingException {
        String id = attributeId;
        if (DatatypeHelper.isEmpty(id)) {
            id = this.attributeId;
        }

        Attribute attribute = filterContext.getUnfilteredAttributes().get(id);
        if (attribute != null && attribute instanceof ScopedAttribute) {
            return isMatch(((ScopedAttribute) attribute).getScope());
        }

        return false;
    }

    /** {@inheritDoc} */
    protected boolean doEvaluatePolicyRequirement(ShibbolethFilteringContext filterContext)
            throws FilterProcessingException {
        Attribute attribute = filterContext.getUnfilteredAttributes().get(attributeId);
        if (attribute != null && attribute instanceof ScopedAttribute) {
            return isMatch(((ScopedAttribute) attribute).getScope());
        }

        return false;
    }
}
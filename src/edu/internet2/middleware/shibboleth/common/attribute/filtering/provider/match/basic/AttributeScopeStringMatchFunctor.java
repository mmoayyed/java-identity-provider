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

import java.util.SortedSet;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttributeValue;

/**
 * Match functor that checks if an attribute's scoped values are equal to a given string.
 * 
 * Attribute values evaluated by this functor <strong>must</strong> be of type {@link ScopedAttributeValue}.
 */
public class AttributeScopeStringMatchFunctor extends AbstractAttributeTargetedStringMatchFunctor {

    /**
     * Checks if the given attribute value's scope matchs the given string.
     * 
     * {@inheritDoc}
     */
    protected boolean doEvaluatePermitValue(ShibbolethFilteringContext filterContext, String attributeId,
            Object attributeValue) throws FilterProcessingException {
        return isMatch(((ScopedAttributeValue) attributeValue).getScope());
    }

    /**
     * Checks if any of the scopes for the values of the given attribute match the given string.
     * 
     * {@inheritDoc}
     */
    protected boolean doEvaluatePolicyRequirement(ShibbolethFilteringContext filterContext)
            throws FilterProcessingException {
        Attribute attribute = filterContext.getUnfilteredAttributes().get(getAttributeId());
        if (attribute == null) {
            return false;
        }
        
        SortedSet values = attribute.getValues();
        ScopedAttributeValue scoped;
  
        //
        // Let's make some sense of this.  If there are values, then we look at every member.
        // If the member is a scopedAttribute we will look at the scope and see if it fits.
        // Otherwise keep on going - we may find something which fits.  If we get to the end
        // and nothing has fit, say false.
        //

        if (values != null) {
            for (Object value : values) {
                if (value instanceof ScopedAttributeValue) {
                    scoped = (ScopedAttributeValue) value;
                    if (isMatch(scoped.getScope())) {
                        return true;
                    }                  
                } 
            }
        }
        
        return false;
    }
}
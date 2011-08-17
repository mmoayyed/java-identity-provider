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

import java.util.Collection;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the AttributeScopeString policy activation criterion.
 * 
 * If the supplied attribute name has any value with a scope which matches the provided string (according to the
 * provided case sensitivity criterion) then we return true.
 */
@ThreadSafe
public class AttributeScopeStringCriterion extends BaseStringCompare implements
        EvaluableCriterion<AttributeFilterContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeScopeStringCriterion.class);

    /**
     * {@inheritDoc}
     * 
     * @throws EvaluationException if the attribute name could not be found.
     */
    public Boolean doEvaluate(final AttributeFilterContext target) throws EvaluationException {
        Attribute<?> attribute = target.getPrefilteredAttributes().get(getAttributeName());

        if (null == attribute) {
            String msg = "PolicyRequirementRule: Could not locate atribute named " + getAttributeName();
            log.warn(msg);
            throw new EvaluationException(msg);
        }

        //
        // Let's make some sense of this. If there are values, then we look at every one.
        // If a value is a ScopedAttributeValue we will look at the scope and see if it fits.
        // Otherwise keep on going - we may find something which fits. If we get to the end
        // and nothing has fit, say false.
        //

        Collection<?> values = attribute.getValues();
        for (Object value : values) {
            if (value instanceof ScopedAttributeValue) {
                ScopedAttributeValue scopedValue = (ScopedAttributeValue) value;
                if (isMatch(scopedValue.getScope())) {
                    return true;
                }
            }
        }
        return false;
    }

}

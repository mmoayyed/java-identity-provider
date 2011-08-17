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
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the AttributeValueRegex policy activation criterion.
 * 
 * If the supplied attribute name has any value which matches the provided regex then we return true.
 */
@ThreadSafe
public class AttributeValueRegexCriterion extends BaseRegexCompare implements
        EvaluableCriterion<AttributeFilterContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeValueRegexCriterion.class);

    /** {@inheritDoc} 
     * @throws EvaluationException if the attribute doesn't match.
     */
    public Boolean doEvaluate(final AttributeFilterContext target) throws EvaluationException {
        Attribute<?> attribute = target.getPrefilteredAttributes().get(getAttributeName());

        if (null == attribute) {
            String msg = "PolicyRequirementRule: Could not locate atribute named " + getAttributeName();
            log.warn(msg);
            throw new EvaluationException(msg);
        }

        Collection<?> values = attribute.getValues();
        for (Object value : values) {
            if (isMatch(value)) {
                return true;
            }
        }
        return false;
    }

}

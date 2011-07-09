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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.impl.BaseStringCompare;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.xml.security.EvaluableCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the AttributeValueString policy activation criterion.
 * 
 * If the supplied attribute name has any value which matches the provided string (according to the provided case
 * sensitivity criterion) then we return true.
 */
public class AttributeValueStringCriterion extends BaseStringCompare implements
        EvaluableCriteria<AttributeFilterContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeValueStringCriterion.class);

    /** The name of the target attribute. */
    private final String attributeName;

    /**
     * Constructor.
     * 
     * @param match the string we are matching against.
     * @param isCaseSensitive whether to do a case sensitive comparison.
     * @param attribute the name of the attribute
     */
    protected AttributeValueStringCriterion(final String match, final boolean isCaseSensitive, final String attribute) {
        super(match, isCaseSensitive);
        attributeName = StringSupport.trimOrNull(attribute);
        Assert.isNotNull(attributeName, "AttributeValue Policy Requirement rule must have non null attribute name");
    }

    /** Private Constructor. Here uniquely to guarantee that we always have non null members. */
    private AttributeValueStringCriterion() {
        super(null, true);
        Assert.isTrue(false, "Private constructor should not be called");
        attributeName = null;
    }

    /**
     * Gets the name of the attribute under consideration.
     * 
     * @return the name of the attribute under consideration, never null or empty.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /** {@inheritDoc} */
    public Boolean evaluate(final AttributeFilterContext target) {
        Attribute<?> attribute = target.getPrefilteredAttributes().get(attributeName);

        if (null == attribute) {
            log.warn("PolicyRequirementRule: Could not locate atribute named " + attributeName);
            return false;
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

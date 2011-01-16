/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.filtering;

import java.util.List;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Represents a value filtering rule for a particular attribute. */
@ThreadSafe
public class AttributeValueFilterPolicy {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeValueFilterPolicy.class);

    /** Unique ID of the attribute this rule applies to. */
    private final String attributeId;

    /**
     * Whether this attribute rule will treat values that its {@link AttributeValueMatcher} as values that are permitted
     * or denied.
     */
    private final boolean matchingPermittedValues;

    /** Filter that permits the release of attribute values. */
    private final AttributeValueMatcher valueMatchingRule;

    /**
     * Constructor.
     * 
     * @param id unique ID of this rule
     * @param matcher matcher used to matching attribute values filtered by this rule
     */
    public AttributeValueFilterPolicy(final String id, final AttributeValueMatcher matcher) {
        attributeId = StringSupport.trimOrNull(id);
        Assert.isNotNull(id, "Attribute rule ID may not be null or empty");

        matchingPermittedValues = true;

        Assert.isNotNull(matcher, "Attribute value matching rule may not be null");
        valueMatchingRule = matcher;
    }

    /**
     * Gets the ID of the attribute to which this rule applies.
     * 
     * @return ID of the attribute to which this rule applies
     */
    public String getAttributeId() {
        return attributeId;
    }

    /**
     * Gets whether this attribute rule will treat values that its {@link AttributeValueMatcher} as values that are
     * permitted or denied.
     * 
     * @return true if matching attribute rules are permitted values, false if they are not
     */
    public boolean isMatchingPermittedValues() {
        return matchingPermittedValues;
    }

    /**
     * Gets the matcher used to matching attribute values filtered by this rule.
     * 
     * @return matcher used to matching attribute values filtered by this rule
     */
    public AttributeValueMatcher getValueMatcher() {
        return valueMatchingRule;
    }

    /**
     * Applies this rule to the respective attribute in the filter context.
     * 
     * @param attribute attribute whose values will be filtered by this policy
     * @param filterContext current filter context
     * 
     * @throws AttributeFilteringException thrown if there is a problem applying this rule to the current filter context
     */
    public void apply(final Attribute<?> attribute, final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        log.debug("Filtering values for attribute '{}' which currently contains {} values", attribute.getId(),
                attribute.getValues().size());
        List<?> matchingValues = valueMatchingRule.getMatchingValues(attribute, filterContext);

        if (matchingPermittedValues) {
            attribute.getValues().retainAll(matchingValues);
        } else {
            attribute.getValues().removeAll(matchingValues);
        }
        log.debug("Attribute '{}' contains {} values after filtering", attribute.getId(), attribute.getValues().size());
    }
}
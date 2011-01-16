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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//TODO performance metrics

/**
 * A policy describing if a set of attribute value filters is applicable.
 * 
 * Note, this filter policy operates on the {@link AttributeFilterContext#getFilteredAttributes()} attribute set. The
 * idea being that as policies run they will retain or remove attributes and values for this collection. After all
 * policies run this collection will contain the final result.
 */
@ThreadSafe
public class AttributeFilterPolicy {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterPolicy.class);

    /** Unique identifier for this policy. */
    private final String policyId;

    /** Requirement that must be met for this policy to apply. */
    private final AttributeFilterPolicyRequirementRule requirementRule;

    /** Filters to be used on attribute values. */
    private final List<AttributeValueFilterPolicy> valuePolicies;

    /**
     * Constructor.
     * 
     * @param id unique ID for the policy, never null
     * @param policyRequirementRule rule that indicates when this policy is active, never null
     * @param attributeValuePolicies set of attribute rules enforced when this rule is active
     */
    public AttributeFilterPolicy(final String id, final AttributeFilterPolicyRequirementRule policyRequirementRule,
            final List<AttributeValueFilterPolicy> attributeValuePolicies) {
        policyId = StringSupport.trimOrNull(id);
        Assert.isNotNull(policyId, "Attribute filter policy ID may not be null or empty");

        Assert.isNotNull(policyRequirementRule, "Attribute filter policy requirement rule may not be null");
        requirementRule = policyRequirementRule;

        if (attributeValuePolicies == null) {
            valuePolicies = Collections.emptyList();
        } else {
            valuePolicies = Collections.unmodifiableList(new ArrayList<AttributeValueFilterPolicy>(
                    attributeValuePolicies));
        }
    }

    /**
     * Gets the unique ID for this policy.
     * 
     * @return unique ID for this policy
     */
    public String getPolicyId() {
        return policyId;
    }

    /**
     * Gets the requirement for this policy.
     * 
     * @return requirement for this policy
     */
    public AttributeFilterPolicyRequirementRule getRequirementRule() {
        return requirementRule;
    }

    /**
     * Gets the attribute rules that are in effect if this policy is in effect.
     * 
     * @return attribute rules that are in effect if this policy is in effect, never null
     */
    public List<AttributeValueFilterPolicy> getAttributeValuePolicies() {
        return valuePolicies;
    }

    /**
     * Checks if the given filter context meets the requirements for this attribute filter policy as given by the
     * {@link AttributeFilterPolicyRequirementRule}.
     * 
     * @param filterContext current filter context
     * 
     * @return true if this policy should be active for the given request, false otherwise
     * 
     * @throws AttributeFilteringException thrown if there is a problem evaluating this filter's requirement rule
     */
    public boolean isActive(final AttributeFilterContext filterContext) throws AttributeFilteringException {
        log.debug("Checking if attribute filter policy '{}' is active", getPolicyId());

        boolean isActive = requirementRule.isSatisfied(filterContext);
        if (isActive) {
            log.debug("Attribute filter policy '{}' is active");
        } else {
            log.debug("Attribute filter policy '{}' is not active");
        }

        return isActive;

    }

    /**
     * Applies this filter policy to the given filter context. Note, this method does not check whether this policy is
     * applicable, it is up to the caller to ensure that is true (e.g. via {@link #isActive(AttributeFilterContext)}).
     * 
     * @param filterContext current filter context
     * 
     * @throws AttributeFilteringException thrown if there is a problem filtering out the attributes and values for this
     *             request
     */
    public void apply(AttributeFilterContext filterContext) throws AttributeFilteringException {
        Map<String, Attribute<?>> attributes = filterContext.getFilteredAttributes();
        log.debug("Applying attribute filter policy '{}' to current set of attributes: {}", getPolicyId(), attributes
                .keySet());

        Attribute<?> attribute;
        for (AttributeValueFilterPolicy valuePolicy : valuePolicies) {
            attribute = attributes.get(valuePolicy.getAttributeId());
            if (attribute != null) {
                if (!attribute.getValues().isEmpty()) {
                    valuePolicy.apply(attribute, filterContext);
                }

                if (attribute.getValues().isEmpty()) {
                    log.debug("Removing attribute '{}' from attribute collection, it no longer contains any values",
                            attribute.getId());
                    attributes.remove(attribute.getId());
                }
            }
        }
    }
}
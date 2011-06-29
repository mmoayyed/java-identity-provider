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

package net.shibboleth.idp.attribute.filtering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.AbstractComponent;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.xml.security.EvaluableCriteria;
import org.opensaml.xml.security.StaticResponseEvaluableCritieria;
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
public class AttributeFilterPolicy extends AbstractComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterPolicy.class);

    /** Criteria that must be met for this policy to be active for a given request. */
    private final EvaluableCriteria<AttributeFilterContext> activationCriteria;

    /** Filters to be used on attribute values. */
    private final List<AttributeValueFilterPolicy> valuePolicies;

    /**
     * Constructor.
     * 
     * @param id unique ID for the policy, never null
     * @param policyRequirementRule rule that indicates when this policy is active, if null then
     *            {@link StaticResponseEvaluableCritieria#FALSE_RESPONSE} is used
     * @param attributeValuePolicies set of attribute rules enforced when this rule is active
     */
    public AttributeFilterPolicy(final String id,
            final EvaluableCriteria<AttributeFilterContext> policyRequirementRule,
            final List<AttributeValueFilterPolicy> attributeValuePolicies) {
        super(id);

        if (policyRequirementRule == null) {
            activationCriteria = StaticResponseEvaluableCritieria.FALSE_RESPONSE;
        } else {
            activationCriteria = policyRequirementRule;
        }

        if (attributeValuePolicies == null) {
            valuePolicies = Collections.emptyList();
        } else {
            valuePolicies =
                    Collections.unmodifiableList(new ArrayList<AttributeValueFilterPolicy>(attributeValuePolicies));
        }
    }

    /**
     * Gets the criteria that must be met for this policy to be active for a given request.
     * 
     * @return criteria that must be met for this policy to be active for a given request, never null
     */
    public EvaluableCriteria<AttributeFilterContext> getActivationCriteria() {
        return activationCriteria;
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
    public boolean isApplicable(final AttributeFilterContext filterContext) throws AttributeFilteringException {
        log.debug("Checking if attribute filter policy '{}' is active", getId());

        Boolean isActive = activationCriteria.evaluate(filterContext);
        if (isActive) {
            log.debug("Attribute filter policy '{}' is active", getId());
        } else {
            log.debug("Attribute filter policy '{}' is not active", getId());
        }

        return isActive;
    }

    /**
     * Applies this filter policy to the given filter context. Note, this method does not check whether this policy is
     * applicable, it is up to the caller to ensure that is true (e.g. via {@link #isApplicable(AttributeFilterContext)}
     * ).
     * 
     * @param filterContext current filter context
     * 
     * @throws AttributeFilteringException thrown if there is a problem filtering out the attributes and values for this
     *             request
     */
    public void apply(final AttributeFilterContext filterContext) throws AttributeFilteringException {
        final Map<String, Attribute<?>> attributes = filterContext.getPrefilteredAttributes();
        log.debug("Applying attribute filter policy '{}' to current set of attributes: {}", getId(),
                attributes.keySet());

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
                    filterContext.removeFilteredAttribute(attribute.getId());
                }
            }
        }
    }
}
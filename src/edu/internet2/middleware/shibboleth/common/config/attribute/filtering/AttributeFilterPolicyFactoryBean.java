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

package edu.internet2.middleware.shibboleth.common.config.attribute.filtering;

import java.util.List;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeFilterPolicy;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeRule;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;

/**
 * Spring factory for {@link AttributeFilterPolicy}s.
 */
public class AttributeFilterPolicyFactoryBean extends AbstractFactoryBean {

    /** Unique identifier for this policy. */
    private String policyId;

    /** Requirement that must be met for this policy to apply. */
    private MatchFunctor policyRequirement;

    /** Filters to be used on attribute values. */
    private List<AttributeRule> attributeRules;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return AttributeFilterPolicy.class;
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
     * Sets the unique ID for this policy.
     * 
     * @param id unique ID for this policy
     */
    public void setPolicyId(String id) {
        policyId = id;
    }

    /**
     * Gets the requirement for this policy.
     * 
     * @return requirement for this policy
     */
    public MatchFunctor getPolicyRequirement() {
        return policyRequirement;
    }

    /**
     * Sets the requirement for this policy.
     * 
     * @param requirement requirement for this policy
     */
    public void setPolicyRequirement(MatchFunctor requirement) {
        policyRequirement = requirement;
    }

    /**
     * Gets the attribute rules that are in effect if this policy is in effect.
     * 
     * @return attribute rules that are in effect if this policy is in effect, never null
     */
    public List<AttributeRule> getAttributeRules() {
        return attributeRules;
    }

    /**
     * Sets the attribute rules that are in effect if this policy is in effect.
     * 
     * @param rules attribute rules that are in effect if this policy is in effect
     */
    public void setAttributeRules(List<AttributeRule> rules) {
        attributeRules = rules;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        AttributeFilterPolicy policy = new AttributeFilterPolicy(policyId);
        policy.setPolicyRequirementRule(policyRequirement);
        policy.getAttributeRules().addAll(attributeRules);

        return policy;
    }
}
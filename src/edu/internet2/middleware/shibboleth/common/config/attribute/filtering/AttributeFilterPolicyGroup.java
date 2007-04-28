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

import java.util.Collection;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeRule;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.AttributeFilterPolicy;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;

/**
 * A collection of attribute filter policies and globally declared filter components.
 */
public class AttributeFilterPolicyGroup {

    /** Unique ID for this set of policies. */
    private String afpId;
    
    /** Declared filter policies. */
    private Collection<AttributeFilterPolicy> attributeFilterPolicies;
    
    /** Globally declared policy requirements. */
    private Collection<MatchFunctor> policyRequirementRules;
    
    /** Globally declared attribute rules. */
    private Collection<AttributeRule> attributeRules;
    
    /** Globally declared permit value rules. */
    private Collection<MatchFunctor> permitValueRules;
    
    /**
     * Constructor.
     *
     * @param id unique ID for this set of policies
     */
    public AttributeFilterPolicyGroup(String id){
        afpId = id;
    }

    /**
     * Gets the globally declared attribute rules.
     * 
     * @return globally declared attribute rules
     */
    public Collection<AttributeRule> getAttributeRules() {
        return attributeRules;
    }

    /**
     * Sets the globally declared attribute rules.
     * 
     * @param rules globally declared attribute rules
     */
    public void setAttributeRules(Collection<AttributeRule> rules) {
        attributeRules = rules;
    }

    /**
     * Gets the declared filter policies.
     * 
     * @return declared filter policies
     */
    public Collection<AttributeFilterPolicy> getAttributeFilterPolicies() {
        return attributeFilterPolicies;
    }

    /**
     * Sets the declared filter policies.
     * 
     * @param policies declared filter policies
     */
    public void setAttributeFilterPolicies(Collection<AttributeFilterPolicy> policies) {
        attributeFilterPolicies = policies;
    }

    /**
     * Gets the globally declared policy requirements.
     * 
     * @return globally declared policy requirements
     */
    public Collection<MatchFunctor> getPolicyRequirementRules() {
        return policyRequirementRules;
    }

    /**
     * Sets the globally declared policy requirements.
     * 
     * @param requirements globally declared policy requirements
     */
    public void setPolicyRequirementRules(Collection<MatchFunctor> requirements) {
        policyRequirementRules = requirements;
    }

    /**
     * Gets the globally declared value filters.
     * 
     * @return globally declared value filters
     */
    public Collection<MatchFunctor> getPermitValueRules() {
        return permitValueRules;
    }

    /**
     * Sets the globally declared value filters.
     * 
     * @param filters globally declared value filters
     */
    public void setPermitValueRules(Collection<MatchFunctor> filters) {
        permitValueRules = filters;
    }

    /**
     * Gets the ID for this policy set.
     * 
     * @return ID for this policy set
     */
    public String getAfpId() {
        return afpId;
    }
}
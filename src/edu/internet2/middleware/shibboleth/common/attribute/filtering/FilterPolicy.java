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

package edu.internet2.middleware.shibboleth.common.attribute.filtering;

import java.util.ArrayList;
import java.util.List;

/**
 * A policy describing if a set of attribute value filters is applicable.
 */
public class FilterPolicy {

    /** Requirement that must be met for this policy to apply. */
    private List<MatchFunctor> policyRequirements;

    /** Filters to be used on attribute values. */
    private List<AttributeRule> attribtueRules;

    /** Constructor. */
    public FilterPolicy() {
        policyRequirements = new ArrayList<MatchFunctor>();
        attribtueRules = new ArrayList<AttributeRule>();
    }

    /**
     * Gets the requirements for this policy.
     * 
     * @return requirements for this policy
     */
    public List<MatchFunctor> getPolicyRequirements() {
        return policyRequirements;
    }

    /**
     * Sets the requirements for this policy.
     * 
     * @param requirements requirements for this policy
     */
    public void setPolicyRequirements(List<MatchFunctor> requirements) {
        policyRequirements = requirements;
    }
    
    /**
     * Gets the attribute rules that are in effect if this policy is in effect.
     * 
     * @return attribute rules that are in effect if this policy is in effect
     */
    public List<AttributeRule> getAttributeRules(){
        return attribtueRules;
    }
    
    /**
     * Sets the attribute rules that are in effect if this policy is in effect.
     * 
     * @param rules attribute rules that are in effect if this policy is in effect
     */
    public void setAttributeRules(List<AttributeRule> rules){
        attribtueRules = rules;
    }
}
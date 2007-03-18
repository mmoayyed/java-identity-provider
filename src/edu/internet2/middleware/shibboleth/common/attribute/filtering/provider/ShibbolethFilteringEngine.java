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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.AndMatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;

/**
 * Implementation of {@link AttributeFilteringEngine}.
 */
public class ShibbolethFilteringEngine implements AttributeFilteringEngine<ShibbolethAttributeRequestContext> {

    /** Class logger. */
    private static Logger log = Logger.getLogger(ShibbolethFilteringEngine.class);

    /** Active attribute rules. */
    private List<AttributeFilterPolicy> filterPolicies;

    /** Constructor. */
    public ShibbolethFilteringEngine() {

    }

    /**
     * Gets the filter policies active for this engine.
     * 
     * @return filter policies active for this engine
     */
    public List<AttributeFilterPolicy> getFilterPolicies() {
        return filterPolicies;
    }

    /**
     * Sets the filter policies active for this engine.
     * 
     * @param policies filter policies active for this engine
     */
    public void setFilterPolicies(List<AttributeFilterPolicy> policies) {
        filterPolicies = policies;
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> filterAttributes(Map<String, Attribute> attributes,
            ShibbolethAttributeRequestContext context) throws AttributeFilteringException {

        if (getFilterPolicies() == null) {
            return new HashMap<String, Attribute>();
        }

        ShibbolethFilteringContext filterContext = new ShibbolethFilteringContext(attributes, context);
        Collection<AttributeRule> rules = generateEffectiveAttributeRules(filterContext);
        for (AttributeRule rule : rules) {
            filterAttributeValues(rule, filterContext);
        }

        return filterContext.getAttributes();
    }

    /**
     * Evaluates all the filter policies and returns the list of effective attribute rules. An effective attribute rule
     * is created by logically ORing all the value filters for each IDed attribute in all polcies whose requirements are
     * met.
     * 
     * @param filterContext the current filtering context
     * 
     * @return the effective attribute rules for the given context
     * 
     * @throws FilterProcessingException thrown the requirements for a filter policy can not be evaluated
     */
    protected Collection<AttributeRule> generateEffectiveAttributeRules(ShibbolethFilteringContext filterContext)
            throws FilterProcessingException {
        if (log.isDebugEnabled()) {
            log.debug("Determing effective filter policies for principal "
                    + filterContext.getAttribtueRequestContext().getPrincipalName());
        }
        ArrayList<AttributeFilterPolicy> effectivePolicies = new ArrayList<AttributeFilterPolicy>();
        for (AttributeFilterPolicy policy : getFilterPolicies()) {
            if (policy.getPolicyRequirement().evaluate(filterContext)) {
                if (log.isDebugEnabled()) {
                    log.debug("Filter policy " + policy.getPolicyId() + " is in effect for principal "
                            + filterContext.getAttribtueRequestContext().getPrincipalName());
                }
                effectivePolicies.add(policy);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Filter policy " + policy.getPolicyId()
                            + " policy requirements not met, policy is not in effect for principal "
                            + filterContext.getAttribtueRequestContext().getPrincipalName());
                }
            }
        }

        return mergeAttributeRules(effectivePolicies);
    }

    /**
     * Merges all the attribute rules for the given policies.
     * 
     * @param policies policies whose attribute rules should be merged
     * 
     * @return list of merged rules
     */
    protected Collection<AttributeRule> mergeAttributeRules(List<AttributeFilterPolicy> policies) {
        if (policies.size() == 0) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Determine effective attribute rules");
        }

        Map<String, AttributeRule> effectiveRules = new HashMap<String, AttributeRule>();
        AttributeRule effectiveRule;
        AndMatchFunctor effectiveFilter;
        for (AttributeFilterPolicy policy : policies) {
            for (AttributeRule rule : policy.getAttributeRules()) {
                effectiveRule = effectiveRules.get(rule.getAttributeId());
                if (effectiveRule == null) {
                    effectiveRule = new AttributeRule(rule.getAttributeId());
                    effectiveRule.setPermitValue(new AndMatchFunctor());
                    effectiveRules.put(rule.getAttributeId(), effectiveRule);
                }

                effectiveFilter = (AndMatchFunctor) effectiveRule.getPermitValue();
                effectiveFilter.getFunctors().add(rule.getPermitValue());
            }
        }

        return effectiveRules.values();
    }

    /**
     * Filters the values of the attribute the rule applies to.
     * 
     * @param rule the attribute rule being evaluated
     * @param filterContext the current filtering context
     * 
     * @throws FilterProcessingException thrown if the value filter criteria can not be evaluated
     */
    protected void filterAttributeValues(AttributeRule rule, ShibbolethFilteringContext filterContext)
            throws FilterProcessingException {
        Attribute attribute = filterContext.getAttributes().get(rule.getAttributeId());
        if (attribute.getValues() == null) {
            if (log.isDebugEnabled()) {
                log.debug("No values present for attribute " + attribute.getId()
                        + ", removing it from list of attributes");
            }
            filterContext.getAttributes().remove(attribute.getId());
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Filtering values for attribute " + rule.getAttributeId());
        }
        MatchFunctor valueFilter = rule.getPermitValue();
        for (Object value : attribute.getValues()) {
            if (!valueFilter.evaluate(filterContext, attribute.getId(), value)) {
                if (log.isDebugEnabled()) {
                    log.debug("The following attribute value was filtered out: " + value.toString());
                }
                attribute.getValues().remove(value);
            }
        }

        if (attribute.getValues().size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No values left for attribute " + attribute.getId()
                                + ", removing it from list of attributes");
            }
            filterContext.getAttributes().remove(attribute.getId());
        }
    }
}
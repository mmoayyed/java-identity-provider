/*
 * Copyright 2007 University Corporation for Advanced Internet Development, Inc.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringException;
import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;

/**
 * Implementation of {@link AttributeFilteringEngine}.
 */
public class ShibbolethAttributeFilteringEngine extends BaseReloadableService implements
        AttributeFilteringEngine<SAMLProfileRequestContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ShibbolethAttributeFilteringEngine.class);

    /** List of unmodifiable loaded filter policies. */
    private List<AttributeFilterPolicy> filterPolicies;

    /** Constructor. */
    public ShibbolethAttributeFilteringEngine() {
        super();
        filterPolicies = new ArrayList<AttributeFilterPolicy>();
    }

    /**
     * Gets the filter policies active for this engine.
     * 
     * @return filter policies active for this engine
     */
    public List<AttributeFilterPolicy> getFilterPolicies() {
        return filterPolicies;
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> filterAttributes(Map<String, BaseAttribute> attributes,
            SAMLProfileRequestContext context) throws AttributeFilteringException {

        log.debug(getId() + " filtering {} attributes for principal {}", attributes.size(), context.getPrincipalName());

        if (attributes.size() == 0) {
            return new HashMap<String, BaseAttribute>();
        }

        if (getFilterPolicies() == null) {
            log.debug("No filter policies were loaded in {}, filtering out all attributes for {}", getId(), context
                    .getPrincipalName());
            return new HashMap<String, BaseAttribute>();
        }

        ShibbolethFilteringContext filterContext = new ShibbolethFilteringContext(attributes, context);
        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        for (AttributeFilterPolicy filterPolicy : filterPolicies) {
            filterAttributes(filterContext, filterPolicy);
            runDenyRules(filterContext);
        }
        readLock.unlock();

        Iterator<Entry<String, BaseAttribute>> attributeEntryItr = attributes.entrySet().iterator();
        Entry<String, BaseAttribute> attributeEntry;
        BaseAttribute attribute;
        Collection retainedValues;
        while (attributeEntryItr.hasNext()) {
            attributeEntry = attributeEntryItr.next();
            attribute = attributeEntry.getValue();
            retainedValues = filterContext.getRetainedValues(attribute.getId(), false);
            attribute.getValues().retainAll(retainedValues);
            if (attribute.getValues().size() == 0) {
                log.debug("Removing attribute from return set, no more values: {}", attribute.getId());
                attributeEntryItr.remove();
            }
        }

        log.debug("Filtered attributes for principal {}.  The following attributes remain: {}", context
                .getPrincipalName(), attributes.keySet());
        return attributes;
    }

    /**
     * Evaluates the given policy's requirement and, if the requirement is met, filters the attributes according to the
     * policy.
     * 
     * @param filterContext current filtering context
     * @param filterPolicy current filter policy
     * 
     * @throws FilterProcessingException thrown if the given policy can be evaluated
     */
    protected void filterAttributes(ShibbolethFilteringContext filterContext, AttributeFilterPolicy filterPolicy)
            throws FilterProcessingException {
        log.debug("Evaluating if filter policy {} is active for principal {}", filterPolicy.getPolicyId(),
                filterContext.getAttributeRequestContext().getPrincipalName());
        MatchFunctor policyRequirement = filterPolicy.getPolicyRequirementRule();
        if (policyRequirement == null || !policyRequirement.evaluatePolicyRequirement(filterContext)) {
            log.debug("Filter policy {} is not active for principal {}", filterPolicy.getPolicyId(), filterContext
                    .getAttributeRequestContext().getPrincipalName());
            return;
        }

        log.debug("Filter policy {} is active for principal {}", filterPolicy.getPolicyId(), filterContext
                .getAttributeRequestContext().getPrincipalName());
        for (AttributeRule attributeRule : filterPolicy.getAttributeRules()) {
            filterAttributes(filterContext, attributeRule);
        }
    }

    /**
     * Evaluates the given attribute rule. If the attribute rule contains a permit value rule then that rule is
     * evaluated against the unfiltered attributes and those values that meet the rule are moved into the filter
     * contexts retained value set. If the attribute rule contains a deny value rule that rule is registered with the
     * filter context so that it may be evaluated after all the permit value rules have run.
     * 
     * @param filterContext current filtering context
     * @param attributeRule current attribute rule
     * 
     * @throws FilterProcessingException thrown if the given attribute rule can be evaluated
     */
    protected void filterAttributes(ShibbolethFilteringContext filterContext, AttributeRule attributeRule)
            throws FilterProcessingException {
        String attributeId = attributeRule.getAttributeId();
        Collection attributeValues = filterContext.getRetainedValues(attributeId, false);

        MatchFunctor permitRule = attributeRule.getPermitValueRule();
        if (permitRule != null) {
            log.debug("Processing permit value rule for attribute {} for principal {}", attributeId, filterContext
                    .getAttributeRequestContext().getPrincipalName());
            Collection unfilteredValues = filterContext.getUnfilteredAttributes().get(attributeId).getValues();
            for (Object attributeValue : unfilteredValues) {
                if (permitRule.evaluatePermitValue(filterContext, attributeId, attributeValue)) {
                    attributeValues.add(attributeValue);
                } else {
                    log.trace("The following value for attribute {} does not meet permit value rule: {}", attributeId,
                            attributeValue.toString());
                }
            }
        }

        MatchFunctor denyRule = attributeRule.getDenyValueRule();
        if (denyRule != null) {
            log.debug("Registering deny value rule for attribute {} for principal {}", attributeId, filterContext
                    .getAttributeRequestContext().getPrincipalName());
            List<MatchFunctor> denyRules = filterContext.getDenyValueRules().get(attributeId);

            if (denyRules == null) {
                denyRules = new ArrayList<MatchFunctor>();
                filterContext.getDenyValueRules().put(attributeId, denyRules);
            }

            denyRules.add(denyRule);
        }
    }

    /**
     * Runs the deny rules registered with the filter context upon the retained value set.
     * 
     * @param filterContext current filtering context
     * 
     * @throws FilterProcessingException thrown if there is a problem evaluating a deny value rule
     */
    protected void runDenyRules(ShibbolethFilteringContext filterContext) throws FilterProcessingException {
        Map<String, List<MatchFunctor>> denyRuleEntries = filterContext.getDenyValueRules();
        if (denyRuleEntries.isEmpty()) {
            return;
        }

        List<MatchFunctor> denyRules;
        Collection attributeValues;
        Object attributeValue;
        for (Entry<String, List<MatchFunctor>> denyRuleEntry : denyRuleEntries.entrySet()) {
            denyRules = denyRuleEntry.getValue();
            attributeValues = filterContext.getRetainedValues(denyRuleEntry.getKey(), false);
            if (denyRules.isEmpty() || attributeValues.isEmpty()) {
                continue;
            }

            Iterator<?> attributeValueItr = attributeValues.iterator();
            for (MatchFunctor denyRule : denyRules) {
                while (attributeValueItr.hasNext()) {
                    attributeValue = attributeValueItr.next();
                    if (denyRule.evaluateDenyRule(filterContext, denyRuleEntry.getKey(), attributeValue)) {
                        log.trace("Removing the following value of attribute {} per deny rule: {}", denyRuleEntry
                                .getKey(), attributeValue);
                        attributeValueItr.remove();
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    protected void onNewContextCreated(ApplicationContext newServiceContext) throws ServiceException {
        List<AttributeFilterPolicy> oldFilterPolicies = filterPolicies;

        try {
            List<AttributeFilterPolicy> newFilterPolicies = new ArrayList<AttributeFilterPolicy>();
            String[] beanNames = newServiceContext.getBeanNamesForType(AttributeFilterPolicy.class);
            for (String beanName : beanNames) {
                newFilterPolicies.add((AttributeFilterPolicy) newServiceContext.getBean(beanName));
            }
            filterPolicies = newFilterPolicies;
        } catch (Exception e) {
            filterPolicies = oldFilterPolicies;
            throw new ServiceException(getId() + " configuration is not valid, retaining old configuration", e);
        }
    }
}
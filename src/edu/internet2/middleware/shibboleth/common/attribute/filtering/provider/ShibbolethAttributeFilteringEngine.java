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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringException;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;
import edu.internet2.middleware.shibboleth.common.storage.Resource;
import edu.internet2.middleware.shibboleth.common.storage.ResourceException;

/**
 * Implementation of {@link AttributeFilteringEngine}.
 */
public class ShibbolethAttributeFilteringEngine extends BaseReloadableService implements
        AttributeFilteringEngine<ShibbolethAttributeRequestContext> {

    /** Class logger. */
    private static Logger log = Logger.getLogger(ShibbolethAttributeFilteringEngine.class);

    /** List of unmodifiable loaded filter policies. */
    private List<AttributeFilterPolicy> filterPolicies;

    /**
     * Constructor.
     * 
     * @param resources list of policy resources
     */
    public ShibbolethAttributeFilteringEngine(List<Resource> resources) {
        super(resources);
        filterPolicies = new ArrayList<AttributeFilterPolicy>();
    }

    /**
     * Constructor.
     * 
     * @param resources list of policy resources
     * @param pollingFrequency the frequency, in milliseconds, to poll the policy resources for changes, must be greater
     *            than zero
     * @param pollingRetryAttempts maximum number of poll attempts before a policy resource is considered inaccessible,
     *            must be greater than zero
     */
    public ShibbolethAttributeFilteringEngine(List<Resource> resources, long pollingFrequency, int pollingRetryAttempts) {
        super(resources, pollingFrequency, pollingRetryAttempts);
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
    public Map<String, Attribute> filterAttributes(Map<String, Attribute> attributes,
            ShibbolethAttributeRequestContext context) throws AttributeFilteringException {

        if (log.isDebugEnabled()) {
            log.debug("Filtering " + attributes.size() + " attributes for principal " + context.getPrincipalName());
        }

        if (getFilterPolicies() == null) {
            return new HashMap<String, Attribute>();
        }

        ShibbolethFilteringContext filterContext = new ShibbolethFilteringContext(attributes, context);
        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        for (AttributeFilterPolicy filterPolicy : filterPolicies) {
            filterAttributes(filterContext, filterPolicy);
        }
        readLock.unlock();

        Attribute attribute;
        for (Entry<String, Attribute> attributeEntry : attributes.entrySet()) {
            attribute = attributeEntry.getValue();
            attribute.getValues().retainAll(filterContext.getRetainedValues(attribute.getId()));
        }

        return filterContext.getUnfilteredAttributes();
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
        if (log.isDebugEnabled()) {
            log.debug("Evaluating if filter policy " + filterPolicy.getPolicyId() + " is active for principal "
                    + filterContext.getAttribtueRequestContext().getPrincipalName());
        }
        MatchFunctor policyRequirement = filterPolicy.getPolicyRequirement();
        if (policyRequirement == null || !policyRequirement.evaluatePolicyRequirement(filterContext)) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Filter policy " + filterPolicy.getPolicyId() + " is active for principal "
                    + filterContext.getAttribtueRequestContext().getPrincipalName());
        }
        for (AttributeRule attributeRule : filterPolicy.getAttributeRules()) {
            filterAttributes(filterContext, attributeRule);
        }
    }

    /**
     * Evaluates the given attribute rules, filtering out attribute values based on the rule's permit value clause.
     * 
     * @param filterContext current filtering context
     * @param attributeRule current attribute rule
     * 
     * @throws FilterProcessingException thrown if the given attribute rule can be evaluated
     */
    protected void filterAttributes(ShibbolethFilteringContext filterContext, AttributeRule attributeRule)
            throws FilterProcessingException {
        SortedSet attributeValues = filterContext.getRetainedValues(attributeRule.getAttributeId());
        MatchFunctor permitValue = attributeRule.getPermitValue();

        if (log.isDebugEnabled()) {
            log.debug("Filtering values of attribute " + attributeRule.getAttributeId() + " for principal "
                    + filterContext.getAttribtueRequestContext().getPrincipalName());
        }

        for (Object attributeValue : attributeValues) {
            if (!permitValue.evaluatePermitValue(filterContext, attributeRule.getAttributeId(), attributeValue)) {
                attributeValues.remove(attributeValue);
            }
        }
    }

    /** {@inheritDoc} */
    protected void newContextCreated(ApplicationContext newServiceContext) throws ResourceException {
        filterPolicies.clear();
        String[] beanNames = newServiceContext.getBeanNamesForType(AttributeFilterPolicy.class);
        for (String beanName : beanNames) {
            filterPolicies.add((AttributeFilterPolicy) newServiceContext.getBean(beanName));
        }
    }
}
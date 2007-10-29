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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import org.opensaml.util.resource.Resource;
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
     * @param timer timer resource polling tasks are scheduled with
     * @param resources list of policy resources
     * @param pollingFrequency the frequency, in milliseconds, to poll the policy resources for changes, must be greater
     *            than zero
     */
    public ShibbolethAttributeFilteringEngine(List<Resource> resources, Timer timer, long pollingFrequency) {
        super(timer, resources, pollingFrequency);
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

        log.debug("Filtering {} attributes for principal {}", attributes.size(), context.getPrincipalName());

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
                log.debug("Removing attribute from return set, no more values: {}" + attribute.getId());
                attributeEntryItr.remove();
            }
        }

        log.debug("Filtered attributes for principal {}.  {} attributes remain.", context.getPrincipalName(),
                attributes.size());
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
            return;
        }

        log.debug("Filter policy {} is active for principal {}", filterPolicy.getPolicyId(), filterContext
                .getAttributeRequestContext().getPrincipalName());
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
        Collection attributeValues = filterContext.getRetainedValues(attributeRule.getAttributeId(), true);
        MatchFunctor permitValue = attributeRule.getPermitValueRule();

        log.debug("Filtering values of attribute {} for principal {}" + attributeRule.getAttributeId(), filterContext
                .getAttributeRequestContext().getPrincipalName());

        Iterator<Object> attributeValueItr = attributeValues.iterator();
        Object attributeValue;
        while (attributeValueItr.hasNext()) {
            attributeValue = attributeValueItr.next();
            if (!permitValue.evaluatePermitValue(filterContext, attributeRule.getAttributeId(), attributeValue)) {
                attributeValueItr.remove();
            }
        }
    }

    /** {@inheritDoc} */
    protected void newContextCreated(ApplicationContext newServiceContext) throws ServiceException {
        filterPolicies.clear();
        String[] beanNames = newServiceContext.getBeanNamesForType(AttributeFilterPolicy.class);
        for (String beanName : beanNames) {
            filterPolicies.add((AttributeFilterPolicy) newServiceContext.getBean(beanName));
        }
    }
}
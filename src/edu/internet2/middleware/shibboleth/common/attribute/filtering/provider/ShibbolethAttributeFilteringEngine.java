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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.xml.sax.InputSource;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringException;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.storage.Resource;
import edu.internet2.middleware.shibboleth.common.storage.ResourceChangeWatcher;
import edu.internet2.middleware.shibboleth.common.storage.ResourceException;
import edu.internet2.middleware.shibboleth.common.storage.ResourceListener;

/**
 * Implementation of {@link AttributeFilteringEngine}.
 * 
 * This filter engine loads policy files from given resources. If, at construction time, polling frequency and retry
 * attempt are given then the policy resources will be watched for changes. If a change is detected then the current
 * configuration will be dropped and a new one created from all resource files. If there is a problem loading a resource
 * file during this process the existing configuration is kept and an error is logged. If this occurs when this engine
 * is initialized, via {@link #initialize()} then a configuration that blocks all attributes is loaded by default.
 */
public class ShibbolethAttributeFilteringEngine implements AttributeFilteringEngine<ShibbolethAttributeRequestContext>,
        ApplicationContextAware {

    /** Class logger. */
    private static Logger log = Logger.getLogger(ShibbolethAttributeFilteringEngine.class);

    /** Application context owning this engine. */
    private ApplicationContext owningContext;

    /** Application context containing loaded with AFP content. */
    private ApplicationContext afpContext;

    /** Read/Write lock for the AFP context. */
    private ReentrantReadWriteLock afpContextLock;

    /** List of resources representing filter policy groups. */
    private List<Resource> policyResources;

    /** Frequency policy resources are polled for updates. */
    private long policyResourcePollingFrequency;

    /** Number of policy resource polling retry attempts. */
    private int policyResourcePollingRetryAttempts;

    /** List of unmodifiable loaded filter policies. */
    private List<AttributeFilterPolicy> filterPolicies;

    /**
     * Constructor.
     * 
     * @param resources list of policy resources
     */
    public ShibbolethAttributeFilteringEngine(List<Resource> resources) {
        policyResourcePollingFrequency = 0;
        policyResourcePollingRetryAttempts = 0;
        afpContextLock = new ReentrantReadWriteLock(true);
        policyResources = new ArrayList<Resource>(resources);
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
        if (pollingFrequency <= 0 || pollingRetryAttempts <= 0) {
            throw new IllegalArgumentException("Polling frequency and retry attempts must be greater than zero.");
        }
        policyResourcePollingFrequency = pollingFrequency;
        policyResourcePollingRetryAttempts = pollingRetryAttempts;

        afpContextLock = new ReentrantReadWriteLock(true);
        policyResources = new ArrayList<Resource>(resources);
        filterPolicies = new ArrayList<AttributeFilterPolicy>();
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext applicationContext) {
        owningContext = applicationContext;
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
     * Initializes this filtering engine by reading all policy filters, setting change watchers on them, and loading
     * them into the engine.
     * 
     * @throws ResourceException thrown if there is a problem reading the given resource
     */
    public void initialize() throws ResourceException {
        if (policyResourcePollingFrequency > 0) {
            ResourceChangeWatcher changeWatcher;
            PolicyResourceListener changeListener = new PolicyResourceListener();
            for (Resource policyResource : policyResources) {
                changeWatcher = new ResourceChangeWatcher(policyResource, policyResourcePollingFrequency,
                        policyResourcePollingRetryAttempts);
                changeWatcher.getResourceListeners().add(changeListener);
            }
        }

        reloadAfpContext();
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
        ReadLock readLock = afpContextLock.readLock();
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

    /**
     * Reloads the AFP application context.
     */
    protected void reloadAfpContext() {
        GenericApplicationContext newAfpContext = new GenericApplicationContext(owningContext);
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(newAfpContext);

        Resource policyResource = null;
        try {
            for (Resource resource : policyResources) {
                policyResource = resource;
                configReader.loadBeanDefinitions(new InputSource(policyResource.getInputStream()));
            }

            WriteLock writeLock = afpContextLock.writeLock();
            writeLock.lock();
            afpContext = newAfpContext;

            filterPolicies.clear();
            String[] beanNames = afpContext.getBeanNamesForType(AttributeFilterPolicy.class);
            for (String beanName : beanNames) {
                filterPolicies.add((AttributeFilterPolicy) afpContext.getBean(beanName));
            }
            writeLock.unlock();
        } catch (ResourceException e) {
            log.error("New filter policy configuration was not loaded, unable to load resource: " + policyResource, e);
        } catch (BeanDefinitionStoreException e) {
            log.error("New filter policy configuration was not loaded, error parsing policy resource: "
                    + policyResource, e);
        }
    }

    /** A listener for policy resource changes that triggers a reloading of the AFP context. */
    protected class PolicyResourceListener implements ResourceListener {

        /** {@inheritDoc} */
        public void onResourceCreate(Resource resource) {
            reloadAfpContext();
        }

        /** {@inheritDoc} */
        public void onResourceDelete(Resource resource) {
            reloadAfpContext();
        }

        /** {@inheritDoc} */
        public void onResourceUpdate(Resource resource) {
            reloadAfpContext();
        }
    }
}
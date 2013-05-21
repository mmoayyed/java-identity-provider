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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

//TODO(lajoie) performance metrics

/**
 * A policy describing if a set of attribute value filters is applicable.
 * 
 * Note, this filter policy operates on the {@link AttributeFilterContext#getFilteredAttributes()} attribute set. The
 * idea being that as policies run they will retain or remove attributes and values for this collection. After all
 * policies run this collection will contain the final result.
 */
@ThreadSafe
public class AttributeFilterPolicy extends AbstractDestructableIdentifiableInitializableComponent implements
        ValidatableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterPolicy.class);

    /** Criterion that must be met for this policy to be active for a given request. */
    private final MatchFunctor policyRequirementRule;

    /** Filters to be used on attribute values. */
    private final List<AttributeRule> valuePolicies;

    /**
     * Constructor.
     * 
     * @param policyId unique ID of this policy
     * @param requirementRule criterion used to determine if this policy is active for a given request
     * @param policies value filtering policies employed if this policy is active
     */
    public AttributeFilterPolicy(@Nonnull @NotEmpty String policyId, @Nonnull MatchFunctor requirementRule,
            @Nullable Collection<AttributeRule> policies) {
        setId(policyId);

        policyRequirementRule =
                Constraint.isNotNull(requirementRule, "Attribute filter policy activiation criterion can not be null");

        ArrayList<AttributeRule> checkedPolicies = new ArrayList<AttributeRule>();
        CollectionSupport.addIf(checkedPolicies, policies, Predicates.notNull());
        if (null != policies) {
            valuePolicies = ImmutableList.copyOf(Iterables.filter(policies, Predicates.notNull()));
        } else {
            valuePolicies = Collections.EMPTY_LIST;
        }
    }

    /**
     * Gets the criteria that must be met for this policy to be active for a given request.
     * 
     * @return criteria that must be met for this policy to be active for a given request
     */
    @Nonnull public MatchFunctor getActivationCriteria() {
        return policyRequirementRule;
    }

    /**
     * Gets the unmodifiable attribute rules that are in effect if this policy is in effect.
     * 
     * @return attribute rules that are in effect if this policy is in effect
     */
    @Nonnull @NonnullElements @Unmodifiable public List<AttributeRule> getAttributeValuePolicies() {
        return valuePolicies;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.validate(policyRequirementRule);

        for (AttributeRule valuePolicy : valuePolicies) {
            valuePolicy.validate();
        }
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
    public boolean isApplicable(@Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        log.debug("Checking if attribute filter policy '{}' is active", getId());

        final boolean isActive = policyRequirementRule.evaluatePolicyRule(filterContext);
        if (isActive) {
            log.debug("Attribute filter policy '{}' is active for this request", getId());
        } else {
            log.debug("Attribute filter policy '{}' is not active for this request", getId());
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
    public void apply(@Nonnull final AttributeFilterContext filterContext) throws AttributeFilteringException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        final Map<String, Attribute> attributes = filterContext.getPrefilteredAttributes();
        log.debug("Applying attribute filter policy '{}' to current set of attributes: {}", getId(),
                attributes.keySet());

        Attribute attribute;
        for (AttributeRule valuePolicy : valuePolicies) {
            attribute = attributes.get(valuePolicy.getAttributeId());
            if (attribute != null) {
                if (!attribute.getValues().isEmpty()) {
                    valuePolicy.apply(attribute, filterContext);
                }

                if (attribute.getValues().isEmpty()) {
                    log.debug("Removing attribute '{}' from attribute collection, it no longer contains any values",
                            attribute.getId());
                    filterContext.getFilteredAttributes().remove(attribute.getId());
                }
            }
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        ComponentSupport.initialize(policyRequirementRule);

        for (AttributeRule valuePolicy : valuePolicies) {
            valuePolicy.initialize();
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        ComponentSupport.destroy(policyRequirementRule);

        for (AttributeRule valuePolicy : valuePolicies) {
            valuePolicy.destroy();
        }

        super.doDestroy();
    }
}
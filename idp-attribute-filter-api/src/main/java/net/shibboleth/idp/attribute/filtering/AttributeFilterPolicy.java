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
import java.util.Map;
import java.util.SortedSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.component.AbstractDestrucableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSortedSet;

//TODO(lajoie) performance metrics

/**
 * A policy describing if a set of attribute value filters is applicable.
 * 
 * Note, this filter policy operates on the {@link AttributeFilterContext#getFilteredAttributes()} attribute set. The
 * idea being that as policies run they will retain or remove attributes and values for this collection. After all
 * policies run this collection will contain the final result.
 */
@ThreadSafe
public class AttributeFilterPolicy extends AbstractDestrucableIdentifiableInitializableComponent implements
        ValidatableComponent, UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterPolicy.class);

    /** Criterion that must be met for this policy to be active for a given request. */
    private final Predicate<AttributeFilterContext> activationCriteria;

    /** Filters to be used on attribute values. */
    private final SortedSet<AttributeValueFilterPolicy> valuePolicies;

    /**
     * Constructor.
     * 
     * @param policyId unique ID of this policy
     * @param criterion criterion used to determine if this policy is active for a given request
     * @param policies value filtering policies employed if this policy is active
     */
    public AttributeFilterPolicy(@Nonnull @NotEmpty String policyId, @Nullable Predicate criterion,
            @Nullable Collection<AttributeValueFilterPolicy> policies) {
        setId(policyId);

        activationCriteria =
                Assert.isNotNull(criterion, "Attribute filter policy activiation criterion can not be null");

        ArrayList<AttributeValueFilterPolicy> checkedPolicies = new ArrayList<AttributeValueFilterPolicy>();
        CollectionSupport.addIf(checkedPolicies, policies, Predicates.notNull());
        valuePolicies = ImmutableSortedSet.copyOf(checkedPolicies);
    }

    /**
     * Gets the criteria that must be met for this policy to be active for a given request.
     * 
     * @return criteria that must be met for this policy to be active for a given request
     */
    @Nonnull public Predicate<AttributeFilterContext> getActivationCriteria() {
        return activationCriteria;
    }

    /**
     * Gets the unmodifiable attribute rules that are in effect if this policy is in effect.
     * 
     * @return attribute rules that are in effect if this policy is in effect
     */
    @Nonnull @NonnullElements @Unmodifiable public SortedSet<AttributeValueFilterPolicy> getAttributeValuePolicies() {
        return valuePolicies;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        ComponentSupport.validate(activationCriteria);

        for (AttributeValueFilterPolicy valuePolicy : valuePolicies) {
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
    public boolean isApplicable(@Nonnull final AttributeFilterContext filterContext) throws AttributeFilteringException {
        assert filterContext != null : "Attribute filter context can not be null";

        ifNotInitializedThrowUninitializedComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        log.debug("Checking if attribute filter policy '{}' is active", getId());

        boolean isActive = activationCriteria.apply(filterContext);
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
        assert filterContext != null : "Attribute filter context can not be null";

        ifNotInitializedThrowUninitializedComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        final Map<String, Attribute> attributes = filterContext.getPrefilteredAttributes();
        log.debug("Applying attribute filter policy '{}' to current set of attributes: {}", getId(),
                attributes.keySet());

        Attribute attribute;
        for (AttributeValueFilterPolicy valuePolicy : valuePolicies) {
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

        ComponentSupport.initialize(activationCriteria);

        for (AttributeValueFilterPolicy valuePolicy : valuePolicies) {
            valuePolicy.initialize();
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        ComponentSupport.destroy(activationCriteria);

        for (AttributeValueFilterPolicy valuePolicy : valuePolicies) {
            valuePolicy.destroy();
        }

        super.doDestroy();
    }
}
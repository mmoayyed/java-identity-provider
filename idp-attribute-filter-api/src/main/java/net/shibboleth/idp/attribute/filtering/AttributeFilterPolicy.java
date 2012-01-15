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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.TransformedInputCollectionBuilder;
import net.shibboleth.utilities.java.support.component.AbstractDestrucableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

//TODO performance metrics

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

    /**
     * Criterion that must be met for this policy to be active for a given request. Default value:
     * {@link Predicates#alwaysFalse()}
     */
    private Predicate<AttributeFilterContext> activationCriteria;

    /** Filters to be used on attribute values. Default value: {@link Collections#emptyList()} */
    private List<AttributeValueFilterPolicy> valuePolicies;

    /** Constructor. */
    public AttributeFilterPolicy() {
        activationCriteria = Predicates.alwaysFalse();
        valuePolicies = Collections.emptyList();
    }

    /** {@inheritDoc} */
    public synchronized void setId(@Nonnull @NotEmpty final String componentId) {
        super.setId(componentId);
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
     * Sets the criteria that must be met for this policy to be active for a given request.
     * 
     * @param criteria criteria that must be met for this policy to be active for a given request
     */
    public synchronized void setActivationCriteria(@Nonnull final Predicate<AttributeFilterContext> criteria) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute filter policy " + getId()
                    + " has already been initialized, its activiation criteria can not be changed.");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

        activationCriteria = Assert.isNull(criteria, "Activitation criteria can not be null");
    }

    /**
     * Gets the unmodifiable attribute rules that are in effect if this policy is in effect.
     * 
     * @return attribute rules that are in effect if this policy is in effect
     */
    @Nonnull @NonnullElements @Unmodifiable public List<AttributeValueFilterPolicy> getAttributeValuePolicies() {
        return valuePolicies;
    }

    /**
     * Sets the attribute rules that are in effect if this policy is in effect.
     * 
     * @param policies attribute rules that are in effect if this policy is in effect
     */
    public synchronized void setAttributeValuePolicies(
            @Nullable @NullableElements final List<AttributeValueFilterPolicy> policies) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute filter policy " + getId()
                    + " has already been initialized, its attribute value filter policies can not be changed.");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

        valuePolicies = new TransformedInputCollectionBuilder().addAll(policies).buildImmutableList();
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

        if (!isInitialized()) {
            throw new UnmodifiableComponentException("Attribute filter policy " + getId()
                    + " has not been initialized and can not be used yet");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

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

        if (!isInitialized()) {
            throw new AttributeFilteringException("Attribute filtering policy " + getId()
                    + " has not be initialized and can not yet be used");
        }

        if (isDestroyed()) {
            throw new DestroyedComponentException(this);
        }

        final Map<String, Attribute<?>> attributes = filterContext.getPrefilteredAttributes();
        log.debug("Applying attribute filter policy '{}' to current set of attributes: {}", getId(),
                attributes.keySet());

        Attribute<?> attribute;
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

        if (activationCriteria == null) {
            activationCriteria = Predicates.alwaysFalse();
        } else {
            ComponentSupport.initialize(activationCriteria);
        }

        for (AttributeValueFilterPolicy valuePolicy : valuePolicies) {
            valuePolicy.initialize();
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        ComponentSupport.destroy(activationCriteria);
        activationCriteria = Predicates.alwaysFalse();

        for (AttributeValueFilterPolicy valuePolicy : valuePolicies) {
            valuePolicy.destroy();
        }

        super.doDestroy();
    }
}
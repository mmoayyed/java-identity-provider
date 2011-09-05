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

package net.shibboleth.idp.attribute.filtering.impl.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.ComponentSupport;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.DestructableComponent;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.ValidatableComponent;
import org.opensaml.util.criteria.AbstractBiasedEvaluableCriterion;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the OR policy activation criterion.
 * 
 * To evaluate this work through the provided in order, following the usual semantics that the first true result will
 * cause us to stop processing. NULL criteria are silently ignored. An entirely empty (or null) list will cause a
 * warning to be emitted, but processing will continue, returning FALSE.
 * 
 */
@ThreadSafe
public class OrCriterion extends AbstractBiasedEvaluableCriterion<AttributeFilterContext> implements
        InitializableComponent, DestructableComponent, ValidatableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OrCriterion.class);

    /**
     * The supplied criteria to be ORed together.
     * 
     * This list in unmodifiable.
     */
    private List<EvaluableCriterion<AttributeFilterContext>> criteria = Collections.EMPTY_LIST;

    /** Initialization state. */
    private boolean initialized;

    /** Destructor state. */
    private boolean destroyed;

    /**
     * Has initialize been called on this object. {@inheritDoc}.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /** Mark the object as initialized having initialized any children. {@inheritDoc}. */
    public final synchronized void initialize() throws ComponentInitializationException {
        if (initialized) {
            throw new ComponentInitializationException("Or Criterion being initialized multiple times");
        }

        for (EvaluableCriterion<AttributeFilterContext> criterion : criteria) {
            ComponentSupport.initialize(criterion);
        }
        initialized = true;
    }

    /** tear down any destructable children. {@inheritDoc} */
    public void destroy() {
        destroyed = true;
        for (EvaluableCriterion<AttributeFilterContext> criterion : criteria) {
            ComponentSupport.destroy(criterion);
        }
        // Clear after the setting of the flag top avoid race with doEvaluate
        criteria = null;
    }

    /**
     * Validate any validatable children. {@inheritDoc}
     * 
     * @throws ComponentValidationException if any of the child validates failed.
     */
    public void validate() throws ComponentValidationException {
        if (!initialized) {
            throw new ComponentValidationException("Object not initialized");
        }
        for (EvaluableCriterion<AttributeFilterContext> criterion : criteria) {
            ComponentSupport.validate(criterion);
        }
    }

    /**
     * Set the sub criteria we will use. Note that this can be called after initialization.
     * 
     * @param theCriteria a list of sub criteria.
     */
    public void setSubCriteria(final List<EvaluableCriterion<AttributeFilterContext>> theCriteria) {

        List<EvaluableCriterion<AttributeFilterContext>> workingCriteriaList =
                CollectionSupport.addNonNull(theCriteria, new ArrayList<EvaluableCriterion<AttributeFilterContext>>());

        if (workingCriteriaList.isEmpty()) {
            log.warn("No sub-criteria provided to OR PolicyRequirementRule, this always returns FALSE");
            criteria = Collections.EMPTY_LIST;
        } else {
            criteria = Collections.unmodifiableList(workingCriteriaList);
        }
    }

    /**
     * Get the sub-criteria for this filter.
     * 
     * @return the criteria
     */
    public List<EvaluableCriterion<AttributeFilterContext>> getSubCriteria() {
        // criteria is created as unmodifiable.
        return criteria;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws EvaluationException s
     */
    public Boolean doEvaluate(final AttributeFilterContext target) throws EvaluationException {

        if (!initialized) {
            throw new EvaluationException("Or Criterion has not been initialized");
        }
        // NOTE capture the criteria to avoid race with setSubCriterion.
        // Do this before the test on destruction to avoid race with destroy code.

        List<EvaluableCriterion<AttributeFilterContext>> theCriteria = criteria;

        if (destroyed) {
            throw new EvaluationException("And Criterion has been destroyed");
        }

        for (EvaluableCriterion<AttributeFilterContext> criterion : theCriteria) {
            if (criterion.evaluate(target)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}

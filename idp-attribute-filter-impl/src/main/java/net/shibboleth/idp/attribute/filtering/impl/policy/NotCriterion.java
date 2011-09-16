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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.ComponentSupport;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.DestructableComponent;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.UninitializedComponentException;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.opensaml.util.component.ValidatableComponent;
import org.opensaml.util.criteria.AbstractBiasedEvaluableCriterion;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;

/**
 * Implement the NOT policy activation criterion.
 * 
 * If the supplied subcontext is true then this returns false and vice versa
 */
@ThreadSafe
public class NotCriterion extends AbstractBiasedEvaluableCriterion<AttributeFilterContext> implements
        InitializableComponent, DestructableComponent, ValidatableComponent, UnmodifiableComponent {

    /** The criterion we are NOT ing. */
    private EvaluableCriterion<AttributeFilterContext> criterion;

    /** Initialization state. */
    private boolean initialized;

    /** Destructor state. */
    private boolean destroyed;

    /**
     * Has initialize been called on this object. {@inheritDoc}.
     */
    public final boolean isInitialized() {
        return initialized;
    }

    /** Mark the object as initialized having the child. {@inheritDoc}. */
    public final synchronized void initialize() throws ComponentInitializationException {
        if (initialized) {
            return;
        }

        if (null == criterion) {
            throw new ComponentInitializationException("Not Criterion being initialized with no sub criterion");
        }

        ComponentSupport.initialize(criterion);

        initialized = true;
    }

    /** tear down the child criterion (if destructable). {@inheritDoc} */
    public void destroy() {
        destroyed = true;
        ComponentSupport.destroy(criterion);
        // Clear after the setting of the flag top avoid race with doEvaluate
        criterion = null;
    }

    /**
     * Validate the child criterion (if validatable). {@inheritDoc}
     * 
     * @throws ComponentValidationException if any of the child validates failed.
     */
    public void validate() throws ComponentValidationException {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Not criterion not initialized");
        }
        ComponentSupport.validate(criterion);
    }

    /**
     * Setter for the sub criterion.
     * 
     * @param theCriterion we are 'not'ing.
     */
    public synchronized void setSubCriterion(final EvaluableCriterion<AttributeFilterContext> theCriterion) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Cannot modify a Not critrion");
        }
        criterion = theCriterion;
    }

    /**
     * Return the criterion we are 'not'ing.
     * 
     * @return the criterion
     */
    public EvaluableCriterion<AttributeFilterContext> getSubCriterion() {
        return criterion;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws EvaluationException if a child throws.
     */
    public Boolean doEvaluate(final AttributeFilterContext target) throws EvaluationException {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Not Criterion not initialized");
        }
        EvaluableCriterion<AttributeFilterContext> theCriterion = criterion;
        if (destroyed) {
            throw new EvaluationException("Not Criterion has been destroyed");
        }
        return !theCriterion.evaluate(target);
    }

}

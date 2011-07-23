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

import org.opensaml.util.Assert;
import org.opensaml.util.criteria.AbstractBiasedEvaluableCriterion;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;


/**
 * Implement the NOT policy activation criterion.
 * 
 * If the supplied subcontext is true then this returns false and vice versa
 */
@ThreadSafe
public class NotCriterion extends AbstractBiasedEvaluableCriterion<AttributeFilterContext> {

    /** The criterion we are NOT ing. */
    private final EvaluableCriterion<AttributeFilterContext> criterion;

    /**
     * Constructor.
     * 
     * @param theCriterion we are 'not'ing.
     */
    public NotCriterion(final EvaluableCriterion<AttributeFilterContext> theCriterion) {
        Assert.isNotNull(theCriterion, "Null criterion added to NOT functot");
        criterion = theCriterion;
    }

    /**
     * private Constructor. By making the default constructor private we can be sure that the criterion is always
     * non-null.
     */
    @SuppressWarnings("unused")
    private NotCriterion() {
        criterion = null;
    }

    /**
     * Return the criterion we are 'not'ing.
     * @return the criterion
     */
    public EvaluableCriterion<AttributeFilterContext> getSubCriterion() {
        return criterion;
    }

    /** {@inheritDoc} 
     * @throws EvaluationException if a child throws. */
    public Boolean doEvaluate(final AttributeFilterContext target) throws EvaluationException {
        return !criterion.evaluate(target);
    }

}

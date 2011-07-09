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

import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.Assert;
import org.opensaml.xml.security.EvaluableCriteria;


/**
 * Implement the NOT policy activation criterion.
 * 
 * If the supplied subcontext is true then this returns false and vice versa
 */
public class NotCriterion implements EvaluableCriteria<AttributeFilterContext> {

    /** The criterion we are NOT ing. */
    private final EvaluableCriteria<AttributeFilterContext> criterion;

    /**
     * Constructor.
     * 
     * @param theCriterion we are 'not'ing.
     */
    public NotCriterion(final EvaluableCriteria<AttributeFilterContext> theCriterion) {
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
    public EvaluableCriteria<AttributeFilterContext> getSubCriterion() {
        return criterion;
    }

    /** {@inheritDoc} */
    public Boolean evaluate(final AttributeFilterContext target) {
        return !criterion.evaluate(target);
    }

}

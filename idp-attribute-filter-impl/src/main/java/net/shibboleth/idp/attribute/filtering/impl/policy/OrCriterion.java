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
import org.opensaml.xml.security.EvaluableCriteria;
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
public class OrCriterion implements EvaluableCriteria<AttributeFilterContext> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OrCriterion.class);

    /**
     * The supplied criteria to be ORed together.
     * 
     * This list in unmodifiable.
     */
    private final List<EvaluableCriteria<AttributeFilterContext>> criteria;

    /**
     * Constructor.
     * 
     * @param theCriteria a list of sub criteria.
     */
    public OrCriterion(final List<EvaluableCriteria<AttributeFilterContext>> theCriteria) {

        List<EvaluableCriteria<AttributeFilterContext>> workingCriteriaList =
                new ArrayList<EvaluableCriteria<AttributeFilterContext>>();

        CollectionSupport.addNonNull(theCriteria, workingCriteriaList);
        if (workingCriteriaList.isEmpty()) {
            log.warn("No sub-criteria provided to OR PolicyRequirementRule, this always returns FALSE");
        }
        criteria = Collections.unmodifiableList(workingCriteriaList);
    }

    /**
     * Get the sub-criteria for this filter.
     * 
     * @return the criteria
     */
    public List<EvaluableCriteria<AttributeFilterContext>> getSubCriteria() {
        // criteria is created as unmodifiable.
        return criteria;
    }

    /** {@inheritDoc} */
    public Boolean evaluate(final AttributeFilterContext target) {

        for (EvaluableCriteria<AttributeFilterContext> criterion : criteria) {
            if (criterion.evaluate(target)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}

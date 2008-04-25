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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;

/**
 * A match functor that performs a logical NOT on the result of another functor.
 */
public class NotMatchFunctor extends AbstractMatchFunctor {

    /** Match functor to negate. */
    private MatchFunctor targetRule;
    
    /**
     * Constructor.
     *
     * @param rule rule to NOT
     */
    public NotMatchFunctor(MatchFunctor rule){
        targetRule = rule;
    }

    /**
     * Gets the match functor that will be the target of the logical NOT.
     * 
     * @return match functor that will be the target of the logical NOT
     */
    public MatchFunctor getTargetRule() {
        return targetRule;
    }

    /**
     * Sets the match functor that will be the target of the logical NOT.
     * 
     * @param target match functor that will be the target of the logical NOT
     */
    public void setTargetRule(MatchFunctor target) {
        targetRule = target;
    }

    /** {@inheritDoc} */
    protected boolean doEvaluateValue(ShibbolethFilteringContext filterContext, String attributeId,
            Object attributeValue) throws FilterProcessingException {
        return !targetRule.evaluatePermitValue(filterContext, attributeId, attributeValue);
    }

    /** {@inheritDoc} */
    protected boolean doEvaluatePolicyRequirement(ShibbolethFilteringContext filterContext)
            throws FilterProcessingException {
        return !targetRule.evaluatePolicyRequirement(filterContext);
    }
}
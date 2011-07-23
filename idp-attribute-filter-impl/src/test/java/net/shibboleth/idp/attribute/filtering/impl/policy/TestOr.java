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
import java.util.List;

import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** tests for the OR criterion. */
public class TestOr {

    /** Test various combinations of Or. 
     * @throws EvaluationException if a child throws. */
    @Test
    public void orCriterionTest() throws EvaluationException {
        OrCriterion or = new OrCriterion(null);
        
        Assert.assertEquals(or.getSubCriteria().size(), 0, "null list");
        Assert.assertFalse(or.evaluate(null), "or(NULL)");

        EvaluableCriterion<AttributeFilterContext> t = new AnyCriterion();
        EvaluableCriterion<AttributeFilterContext> f = new NotCriterion(new AnyCriterion());
        
        List<EvaluableCriterion<AttributeFilterContext>> list =
            new ArrayList<EvaluableCriterion<AttributeFilterContext>>(3);
        list.add(f);
        list.add(null);
        or = new OrCriterion(list);
        Assert.assertFalse(or.evaluate(null), "and(FALSE, NULL)");

        list.set(1, f);
        or = new OrCriterion(list);
        Assert.assertFalse(or.evaluate(null), "and(FALSE, FALSE)");
        
        list.set(0, t);
        Assert.assertFalse(or.evaluate(null), "test immutability of parameter");
        or = new OrCriterion(list);
        Assert.assertTrue(or.evaluate(null), "and(FALSE, TRUE)");
        
    }
}

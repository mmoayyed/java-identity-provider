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

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** tests for the AND criterion. */
public class TestAnd {

    /** Test whether not denies a null parameter. 
     * @throws EvaluationException if a child throws*/
    @Test
    public void andCriterionWithNullTest() throws EvaluationException {
        AndCriterion and = new AndCriterion(null);

        Assert.assertEquals(and.getSubCriteria().size(), 0, "null list");
        Assert.assertFalse(and.evaluate(null), "and(NULL)");
        //
        // We cannot add null to a list via CollectionSupport.toList.
        //
        List<EvaluableCriterion<AttributeFilterContext>> list =
                new ArrayList<EvaluableCriterion<AttributeFilterContext>>(3);
        list.add(null);
        list.add(null);
        list.add(null);

        and = new AndCriterion(list);
        Assert.assertEquals(and.getSubCriteria().size(), 0, "null list");

        list = new ArrayList<EvaluableCriterion<AttributeFilterContext>>(2);
        list.add(null);
        list.add(new AnyCriterion());
        and = new AndCriterion(list);
        Assert.assertEquals(and.getSubCriteria().size(), 1, "list size");
        Assert.assertTrue(and.evaluate(null), "and(NULL, TRUE)");

    }

    /** Test various combinations of And. 
     * @throws EvaluationException if a child throws*/
    @Test
    public void andCriterionTest() throws EvaluationException {
        EvaluableCriterion<AttributeFilterContext> t = new AnyCriterion();
        EvaluableCriterion<AttributeFilterContext> f = new NotCriterion(new AnyCriterion());

        List<EvaluableCriterion<AttributeFilterContext>> list = CollectionSupport.toList(t, t, t);
        AndCriterion and = new AndCriterion(list);
        Assert.assertTrue(and.evaluate(null), "and(TRUE, TRUE, TRUE)");

        list.set(0, f);
        Assert.assertTrue(and.evaluate(null), "list is unmodifiable");

        and = new AndCriterion(list);
        Assert.assertFalse(and.evaluate(null), "and(FALSE, TRUE, TRUE");

    }
}

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
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** tests for the AND criterion. */
public class TestAnd {

    /**
     * Test whether not denies a null parameter.
     * 
     * @throws EvaluationException if a child throws
     * @throws ComponentInitializationException never
     */
    @Test public void andCriterionWithNullTest() throws EvaluationException, ComponentInitializationException {
        AndCriterion and = new AndCriterion();

        Assert.assertEquals(and.getSubCriteria().size(), 0, "null list");
        and.initialize();
        Assert.assertFalse(and.evaluate(null), "and(NULL)");
        //
        // We cannot add null to a list via CollectionSupport.toList.
        //
        List<EvaluableCriterion<AttributeFilterContext>> list =
                new ArrayList<EvaluableCriterion<AttributeFilterContext>>(3);
        list.add(null);
        list.add(null);
        list.add(null);

        and.setSubCriteria(list);
        Assert.assertEquals(and.getSubCriteria().size(), 0, "null list");

        list = new ArrayList<EvaluableCriterion<AttributeFilterContext>>(2);
        list.add(null);
        list.add(new AnyCriterion());
        and.setSubCriteria(list);
        Assert.assertEquals(and.getSubCriteria().size(), 1, "list size");
        Assert.assertTrue(and.evaluate(null), "and(NULL, TRUE)");

        boolean thrown = false;
        try {
            and.initialize();
            Assert.assertTrue(false, "unreachable code (double initialize)");
        } catch (ComponentInitializationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "double initialize should throw");
    }

    /**
     * Test various combinations of And.
     * 
     * @throws EvaluationException if a child throws
     * @throws ComponentInitializationException never
     * @throws ComponentValidationException never
     */
    @Test public void andCriterionTest() throws EvaluationException, ComponentInitializationException,
            ComponentValidationException {
        EvaluableCriterion<AttributeFilterContext> t = new AnyCriterion();
        DestroyableValidatableAnyCriterion d = new DestroyableValidatableAnyCriterion();
        NotCriterion f = new NotCriterion();
        f.setSubCriterion(d);

        List<EvaluableCriterion<AttributeFilterContext>> list = Lists.newArrayList(t, t, t);
        AndCriterion and = new AndCriterion();
        and.initialize();
        and.setSubCriteria(list);
        Assert.assertTrue(and.evaluate(null), "and(TRUE, TRUE, TRUE)");

        list.set(0, f);
        Assert.assertTrue(and.evaluate(null), "list is unmodifiable");

        and = new AndCriterion();

        Assert.assertFalse(d.isInitialized(), "initialization of subcriteria should not have happened yet");
        and.setSubCriteria(list);
        and.initialize();
        Assert.assertTrue(d.isInitialized(), "initialization of subcriteria should have happened");

        Assert.assertFalse(d.isValidated(), "validation of subcriteria should not have happened yet");
        and.validate();
        Assert.assertTrue(d.isValidated(), "validation of subcriteria should have happened");

        Assert.assertFalse(and.evaluate(null), "and(FALSE, TRUE, TRUE");

        Assert.assertFalse(d.isDestroyed(), "destruction of subcriteria should not have happened yet");
        and.destroy();
        Assert.assertTrue(d.isDestroyed(), "destruction of subcretia should have happened");

        boolean thrown = false;
        try {
            and.evaluate(null);
            Assert.assertTrue(false, "unreachable code (evaluate after destroy)");
        } catch (EvaluationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "evaluate after destroy should throw");

    }
}

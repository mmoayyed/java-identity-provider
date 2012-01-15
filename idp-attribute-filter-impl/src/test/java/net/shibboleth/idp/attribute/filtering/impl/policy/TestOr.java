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

/** tests for the OR criterion. */
public class TestOr {

    /**
     * Test various combinations of Or.
     * 
     * @throws EvaluationException if a child throws.
     * @throws ComponentValidationException never
     * @throws ComponentInitializationException never
     */
    @Test
    public void orCriterionTest() throws EvaluationException, ComponentValidationException, ComponentInitializationException {
        OrCriterion or = new OrCriterion();

        Assert.assertEquals(or.getSubCriteria().size(), 0, "null list");
        or.initialize();
        Assert.assertFalse(or.evaluate(null), "or(NULL)");
        or.setSubCriteria(null);
        Assert.assertFalse(or.evaluate(null), "or(NULL)");


        EvaluableCriterion<AttributeFilterContext> t = new AnyCriterion();
        DestroyableValidatableAnyCriterion d = new DestroyableValidatableAnyCriterion();
        NotCriterion f = new NotCriterion();
        f.setSubCriterion(d);

        List<EvaluableCriterion<AttributeFilterContext>> list =
                new ArrayList<EvaluableCriterion<AttributeFilterContext>>(3);
        list.add(f);
        list.add(null);
        or = new OrCriterion();
        or.setSubCriteria(list);

        
        Assert.assertFalse(d.isInitialized(), "initialize trickle down (pre)");
        or.initialize();
        Assert.assertTrue(d.isInitialized(), "initialize trickle down (post)");
        Assert.assertFalse(or.evaluate(null), "or(FALSE, NULL)");

        
        Assert.assertFalse(d.isValidated(), "Validate trickle down (pre)");
        or.validate();
        Assert.assertTrue(d.isValidated(), "Validated trickle down (post)");
        
        
        list.set(1, f);
        or.setSubCriteria(list);
        Assert.assertFalse(or.evaluate(null), "or(FALSE, FALSE)");

        list.set(0, t);
        Assert.assertFalse(or.evaluate(null), "test immutability of parameter");
        or.setSubCriteria(list);
        Assert.assertTrue(or.evaluate(null), "or(FALSE, TRUE)");


        Assert.assertFalse(d.isDestroyed(), "Destroyed trickle down (pre)");
        or.destroy();
        Assert.assertTrue(d.isDestroyed(), "Destroyed trickle down (post)");

        boolean thrown = false;
        try {
            or.evaluate(null);
            Assert.assertTrue(false, "unreachable code (evaluate after destroy)");
        } catch (EvaluationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "evaluate after destroy should throw");
        
    }
}

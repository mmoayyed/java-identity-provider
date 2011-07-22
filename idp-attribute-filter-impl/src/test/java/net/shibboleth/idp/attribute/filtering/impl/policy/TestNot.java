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

import org.opensaml.util.criteria.EvaluableCriterion;
import org.testng.Assert;
import org.testng.annotations.Test;

/** tests for the NOT criterion. */
public class TestNot {

    /** Test whether not denies a null parameter. */
    @Test
    public void notCriterionWithNullTest() {
        try {
            new NotCriterion(null);
            Assert.assertTrue(false, "NOT should not accept a null parameter");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "Expected code path");
        }
    }
    
    /** Test various combinations of not. */
    @Test
    public void notCriterionTest() {
        EvaluableCriterion<AttributeFilterContext> base = new AnyCriterion();
        NotCriterion not = new NotCriterion(base);
        EvaluableCriterion<AttributeFilterContext> notNot = new NotCriterion(not);
        
        Assert.assertEquals(not.getSubCriterion(), base, "test getSubcriterion");
        
        Assert.assertFalse(not.evaluate(null), "not(true)");
        Assert.assertTrue(notNot.evaluate(null), "not(not(true))");
    }
}

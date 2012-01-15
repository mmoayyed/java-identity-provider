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
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** tests for the NOT criterion. */
public class TestNot {

    /**
     * Test various combinations of not.
     * 
     * @throws EvaluationException if a child throws.
     * @throws ComponentInitializationException never
     */
    @Test
    public void notCriterionTest() throws EvaluationException, ComponentInitializationException {
        boolean thrown = false;
        try {
            new NotCriterion().initialize();
           
            Assert.assertTrue(false, "NOT should not accept a null parameter");
        } catch (ComponentInitializationException e) {
            thrown = true;
        } 
        Assert.assertTrue(thrown, "Expected code path");

        EvaluableCriterion<AttributeFilterContext> base = new AnyCriterion();
        NotCriterion not = new NotCriterion();
        not.setSubCriterion(base);
        
        NotCriterion notNot = new NotCriterion();
        notNot.setSubCriterion(not);
        
        Assert.assertEquals(not.getSubCriterion(), base, "test getSubcriterion");
        
        thrown = false;
        try {
            not.evaluate(null);
            Assert.assertTrue(false, "NOT should evaluate without being initialized");
        } catch (Exception e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Expected code path");

        notNot.initialize();

        thrown = false;
        try {
            not.initialize();
            Assert.assertTrue(false, "NOT should initialize twice");
        } catch (Exception e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Expected code path");
       
        Assert.assertFalse(not.evaluate(null), "not(true)");
        Assert.assertTrue(notNot.evaluate(null), "not(not(true))");

        // validate and destroy propoagation tested with AND criteria
        not.destroy();
        thrown = false;
        try {
            not.initialize();
            Assert.assertTrue(false, "NOT should fail after destruction");
        } catch (Exception e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Expected code path");
    }
}

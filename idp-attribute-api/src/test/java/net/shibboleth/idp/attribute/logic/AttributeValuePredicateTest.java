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

package net.shibboleth.idp.attribute.logic;

import java.util.Arrays;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/** tests for {@link AttributeValuePredicate}. */
public class AttributeValuePredicateTest {

    private static final String VALUE_ONE = "one";

    private static final String VALUE_TWO = "two";

    @Test public void testAttributeValuePredicate() throws CloneNotSupportedException {
        final Attribute oneVal = new Attribute("oneVal");
        oneVal.setValues(Arrays.asList((AttributeValue) new StringAttributeValue(VALUE_ONE)));

        final Attribute twoVals = new Attribute("twoVals");
        twoVals.setValues(Arrays.asList((AttributeValue) new StringAttributeValue(VALUE_ONE), new StringAttributeValue(
                VALUE_TWO)));

        final Attribute threeVals = twoVals.clone();
        threeVals.getValues().add(new StringAttributeValue("three"));

        final Predicate matchOne =
                AttributeLogicSupport.stringAttributeValueMatches(Predicates.equalTo((CharSequence) VALUE_ONE));
        final Predicate matchTwo =
                AttributeLogicSupport.stringAttributeValueMatches(Predicates.equalTo((CharSequence) VALUE_TWO));
        final Predicate matchBoth = Predicates.or(matchOne, matchTwo);

        try {
            new AttributeValuePredicate(null, true);
            Assert.fail();
        } catch (AssertionError e) {
            // OK
        }

        final Predicate matchAtLeastOne = AttributeLogicSupport.atLeastOneAttributeValueMatches(matchTwo);
        
        try {
            matchAtLeastOne.apply(null);
            Assert.fail();
        } catch (AssertionError e) {
            // OK
        }

        Assert.assertFalse(matchAtLeastOne.apply(oneVal));
        Assert.assertTrue(matchAtLeastOne.apply(twoVals));
        Assert.assertTrue(matchAtLeastOne.apply(threeVals));
        
        final Predicate matchAll = AttributeLogicSupport.allAttributeValuesMatches(matchBoth);
        
        try {
            matchAll.apply(null);
            Assert.fail();
        } catch (AssertionError e) {
            // OK
        }

        Assert.assertTrue(matchAll.apply(oneVal));
        Assert.assertTrue(matchAll.apply(twoVals));
        Assert.assertFalse(matchAll.apply(threeVals));

    }
}

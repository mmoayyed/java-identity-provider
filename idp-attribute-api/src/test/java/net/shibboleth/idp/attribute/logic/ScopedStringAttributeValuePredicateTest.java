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

import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/** Test for {@link ScopedStringAttributeValuePredicate}. */

public class ScopedStringAttributeValuePredicateTest {

    private static final String VALUE_ONE = "one";

    private static final String VALUE_TWO = "two";

    @Test public void stringAttributeValueMatches() {
        try {
            new ScopedStringAttributeValueScopePredicate(null);
            Assert.fail();
        } catch (AssertionError e) {
            // OK
        }

        Predicate pred =
                AttributeLogicSupport.scopedStringAttributeValueScopeMatches(Predicates
                        .equalTo((CharSequence) VALUE_ONE));

        try {
            pred.apply(new StringAttributeValue(VALUE_ONE));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            pred.apply(new Integer(2));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK
        }

        Assert.assertFalse(pred.apply(new ScopedStringAttributeValue(VALUE_ONE, VALUE_TWO)));
        Assert.assertTrue(pred.apply(new ScopedStringAttributeValue(VALUE_TWO, VALUE_ONE)));
    }
}

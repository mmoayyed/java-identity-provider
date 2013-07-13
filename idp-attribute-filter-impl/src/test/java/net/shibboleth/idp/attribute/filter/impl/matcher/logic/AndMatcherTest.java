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

package net.shibboleth.idp.attribute.filter.impl.matcher.logic;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.or;

import java.util.Collections;
import java.util.Set;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.impl.matcher.AbstractMatcherPolicyRuleTest;
import net.shibboleth.idp.attribute.filter.impl.matcher.MockValuePredicateMatcher;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** {@link AndMatcher} unit test. */
public class AndMatcherTest extends AbstractMatcherPolicyRuleTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        Matcher valuePredicate = Matcher.MATCHES_ALL;
        AndMatcher matcher = new AndMatcher(Lists.<Matcher> newArrayList(valuePredicate));
        matcher.setId("test");
        matcher.initialize();

        try {
            matcher.getMatchingValues(null, filterContext);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            matcher.getMatchingValues(attribute, null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            matcher.getMatchingValues(null, null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testGetMatchingValues() throws Exception {
        AndMatcher matcher =
                new AndMatcher(Lists.<Matcher> newArrayList(
                        new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2))),
                        new MockValuePredicateMatcher(or(equalTo(value2), equalTo(value3)))));

        try {
            matcher.getMatchingValues(attribute, filterContext);
            Assert.fail();
        } catch (UninitializedComponentException e) {
            // expect this
        }

        matcher.setId("test");
        matcher.initialize();

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.contains(value2));

        matcher.destroy();
        try {
            matcher.getMatchingValues(attribute, filterContext);
            Assert.fail();
        } catch (DestroyedComponentException e) {
            // expect this
        }
    }

    @Test public void testFails() throws Exception {
        AndMatcher matcher = new AndMatcher(Lists.<Matcher> newArrayList(Matcher.MATCHES_ALL, Matcher.MATCHER_FAILS));
        matcher.setId("test");
        matcher.initialize();

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNull(result);

        matcher = new AndMatcher(Lists.<Matcher> newArrayList(Matcher.MATCHER_FAILS, Matcher.MATCHES_ALL));
        matcher.setId("test");
        matcher.initialize();

        result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNull(result);
    }

    @Test(expectedExceptions = {ComponentInitializationException.class}) public void emptyInput()
            throws ComponentInitializationException {
        AndMatcher matcher = new AndMatcher(Collections.EMPTY_LIST);
        matcher.setId("test");
        matcher.initialize();
    }

    @Test public void emptyResults() throws ComponentInitializationException, AttributeFilterException {
        AndMatcher matcher =
                new AndMatcher(Lists.<Matcher> newArrayList(
                        new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2))),
                        new MockValuePredicateMatcher(equalTo(value3))));

        matcher.setId("Test");
        matcher.initialize();
        Assert.assertTrue(matcher.getMatchingValues(attribute, filterContext).isEmpty());
    }
}
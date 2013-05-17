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

package net.shibboleth.idp.attribute.filtering.impl.matcher.logic;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.or;

import java.util.Collections;
import java.util.Set;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.MatchFunctor;
import net.shibboleth.idp.attribute.filtering.impl.matcher.AbstractMatcherTest;
import net.shibboleth.idp.attribute.filtering.impl.matcher.AbstractValueMatcherFunctor;
import net.shibboleth.idp.attribute.filtering.impl.matcher.MockValuePredicateMatcher;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** {@link AndMatcher} unit test. */
public class AndMatcherTest extends AbstractMatcherTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        AbstractValueMatcherFunctor valuePredicate = new MockValuePredicateMatcher(alwaysTrue());
        AndMatcher matcher = new AndMatcher(Lists.<MatchFunctor> newArrayList(valuePredicate));
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
                new AndMatcher(Lists.<MatchFunctor> newArrayList(
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

    @Test public void emptyResults() throws ComponentInitializationException, AttributeFilteringException {
        AndMatcher matcher = new AndMatcher(Collections.EMPTY_LIST);
        matcher.setId("test");
        matcher.initialize();
        Assert.assertTrue(matcher.getMatchingValues(attribute, filterContext).isEmpty());

        matcher =
                new AndMatcher(Lists.<MatchFunctor> newArrayList(
                        new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2))),
                        new MockValuePredicateMatcher(equalTo(value3))));

        matcher.setId("Test");
        matcher.initialize();
        Assert.assertTrue(matcher.getMatchingValues(attribute, filterContext).isEmpty());
    }

    // TODO
    // @Test 
    public void testEqualsHashToString() throws ComponentInitializationException {
        AndMatcher matcher =
                new AndMatcher(Lists.<MatchFunctor> newArrayList(new MockValuePredicateMatcher(equalTo(value2)),
                        new MockValuePredicateMatcher(equalTo(value3))));

        matcher.toString();

        Assert.assertFalse(matcher.equals(null));
        Assert.assertTrue(matcher.equals(matcher));
        Assert.assertFalse(matcher.equals(this));

        AndMatcher other =
                new AndMatcher(Lists.<MatchFunctor> newArrayList(new MockValuePredicateMatcher(equalTo(value2)),
                        new MockValuePredicateMatcher(equalTo(value3))));

        Assert.assertTrue(matcher.equals(other));
        Assert.assertEquals(matcher.hashCode(), other.hashCode());

        other =
                new AndMatcher(Lists.<MatchFunctor> newArrayList(new MockValuePredicateMatcher(equalTo(value3)),
                        new MockValuePredicateMatcher(equalTo(value2))));

        Assert.assertFalse(matcher.equals(other));
        Assert.assertNotSame(matcher.hashCode(), other.hashCode());

    }

    @Test public void testPredicate() throws ComponentInitializationException, AttributeFilteringException {
        AndMatcher matcher = new AndMatcher(null);
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertFalse(matcher.evaluatePolicyRule(null));

        matcher = new AndMatcher(Collections.EMPTY_SET);
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertFalse(matcher.evaluatePolicyRule(null));

        matcher =
                new AndMatcher(Lists.<MatchFunctor> newArrayList(new MockValuePredicateMatcher(false),
                        new MockValuePredicateMatcher(false)));
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertFalse(matcher.evaluatePolicyRule(null));

        matcher =
                new AndMatcher(Lists.<MatchFunctor> newArrayList(new MockValuePredicateMatcher(true), null,
                        new MockValuePredicateMatcher(false)));
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertFalse(matcher.evaluatePolicyRule(null));

        matcher =
                new AndMatcher(Lists.<MatchFunctor> newArrayList(new MockValuePredicateMatcher(true), null,
                        new MockValuePredicateMatcher(true)));
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertTrue(matcher.evaluatePolicyRule(null));

    }
}
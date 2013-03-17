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

import java.util.Set;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.impl.matcher.AbstractMatcherTest;
import net.shibboleth.idp.attribute.filtering.impl.matcher.BaseValuePredicateMatcher;
import net.shibboleth.idp.attribute.filtering.impl.matcher.MatchFunctor;
import net.shibboleth.idp.attribute.filtering.impl.matcher.TestValuePredicateMatcher;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** Test the {@link NotMatcher} matcher. */
public class NotMatcherTest extends AbstractMatcherTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        try {
            new TestValuePredicateMatcher(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        BaseValuePredicateMatcher valuePredicate = new TestValuePredicateMatcher(alwaysTrue());
        NotMatcher matcher = new NotMatcher(valuePredicate);
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

        try {
            new NotMatcher(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testInitValidateDestroy() throws AttributeFilteringException, ComponentInitializationException,
            ComponentValidationException {
        AbstractComposedMatcherTest.TestMatcher inMatcher = new AbstractComposedMatcherTest.TestMatcher();
        NotMatcher matcher = new NotMatcher(inMatcher);

        try {
            matcher.getMatchingValues(attribute, filterContext);
            Assert.fail();
        } catch (UninitializedComponentException e) {
            // expect this
        }
        Assert.assertFalse(inMatcher.isInitialized());
        Assert.assertFalse(inMatcher.getValidateCount() > 0);
        Assert.assertFalse(inMatcher.isDestroyed());

        try {
            matcher.validate();
            Assert.fail();
        } catch (UninitializedComponentException e) {
            // expect this
        }

        matcher.initialize();
        Assert.assertTrue(inMatcher.isInitialized());
        Assert.assertFalse(inMatcher.getValidateCount() > 0);

        matcher.validate();
        Assert.assertTrue(inMatcher.isInitialized());
        Assert.assertTrue(inMatcher.getValidateCount() > 0);
        Assert.assertFalse(inMatcher.isDestroyed());

        inMatcher.setFailValidate(true);
        try {
            matcher.validate();
        } catch (ComponentValidationException e) {
            // OK
        }

        matcher.destroy();
        Assert.assertTrue(inMatcher.isDestroyed());
        Assert.assertTrue(inMatcher.isInitialized());
        Assert.assertTrue(inMatcher.getValidateCount() > 0);

        try {
            matcher.initialize();
        } catch (DestroyedComponentException e) {
            // OK
        }

        try {
            matcher.validate();
        } catch (DestroyedComponentException e) {
            // OK
        }

    }

    @Test public void testGetMatchingValues() throws Exception {
        NotMatcher matcher = new NotMatcher(new TestValuePredicateMatcher(or(equalTo(value1), equalTo(value2))));

        matcher.initialize();

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.contains(value3));
        matcher.destroy();
        try {
            matcher.getMatchingValues(attribute, filterContext);
            Assert.fail();
        } catch (DestroyedComponentException e) {
            // expect this
        }

        matcher =
                new NotMatcher(new OrMatcher(Lists.<MatchFunctor> newArrayList(new TestValuePredicateMatcher(
                        equalTo(value1)), new TestValuePredicateMatcher(equalTo(value2)),
                        new TestValuePredicateMatcher(equalTo(value3)))));

        matcher.initialize();

        result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 0);
    }

    @Test public void testEqualsHashToString() {
        NotMatcher matcher = new NotMatcher(new TestValuePredicateMatcher(equalTo(value2)));

        matcher.toString();

        Assert.assertFalse(matcher.equals(null));
        Assert.assertTrue(matcher.equals(matcher));
        Assert.assertFalse(matcher.equals(this));

        NotMatcher other = new NotMatcher(new TestValuePredicateMatcher(equalTo(value2)));

        Assert.assertTrue(matcher.equals(other));
        Assert.assertEquals(matcher.hashCode(), other.hashCode());

        other = new NotMatcher(new TestValuePredicateMatcher(equalTo(value3)));

        Assert.assertFalse(matcher.equals(other));
        Assert.assertNotSame(matcher.hashCode(), other.hashCode());

    }

    @Test public void testPredicate() throws ComponentInitializationException {
        NotMatcher matcher = new NotMatcher(new TestValuePredicateMatcher(true));
        matcher.initialize();
        Assert.assertFalse(matcher.apply(null));

        matcher = new NotMatcher(new TestValuePredicateMatcher(false));
        matcher.initialize();
        Assert.assertTrue(matcher.apply(null));

    }

}

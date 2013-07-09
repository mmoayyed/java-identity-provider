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

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.or;

import java.util.Collections;
import java.util.Set;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.impl.matcher.AbstractComparisonMatcher;
import net.shibboleth.idp.attribute.filter.impl.matcher.AbstractMatcherTest;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.idp.attribute.filter.impl.matcher.MockValuePredicateMatcher;
import net.shibboleth.idp.attribute.filter.impl.matcher.logic.OrMatcher;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/** {@link OrMatcher} unit test. */
public class OrMatcherTest extends AbstractMatcherTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        AbstractComparisonMatcher valuePredicate = new MockValuePredicateMatcher(alwaysTrue());
        OrMatcher matcher = new OrMatcher(Lists.<Matcher> newArrayList(valuePredicate));
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
        OrMatcher matcher =
                new OrMatcher(Lists.<Matcher> newArrayList(
                        new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2))),
                        new MockValuePredicateMatcher(equalTo(value2))));

        try {
            matcher.getMatchingValues(attribute, filterContext);
            Assert.fail();
        } catch (UninitializedComponentException e) {
            // expect this
        }

        matcher.setId("Test");
        matcher.initialize();

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(value2) && result.contains(value1));

        matcher.destroy();
        try {
            matcher.getMatchingValues(attribute, filterContext);
            Assert.fail();
        } catch (DestroyedComponentException e) {
            // expect this
        }

        matcher = new OrMatcher(Collections.EMPTY_LIST);
        matcher.setId("test");
        try {
            matcher.initialize();
            Assert.fail();
        } catch (ComponentInitializationException ex) {
            // OK
        }
    }

    @Test public void testRegressionGetValues() throws ComponentInitializationException, AttributeFilterException {
        OrMatcher matcher =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(Predicates.alwaysFalse()),
                        new MockValuePredicateMatcher(Predicates.alwaysFalse()), new MockValuePredicateMatcher(
                                equalTo(value1)), new MockValuePredicateMatcher(equalTo(value2))));
        matcher.setId("Test");
        matcher.initialize();

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(value2) && result.contains(value1));

        matcher.destroy();

    }

    @Test public void testNoMatchingValues() throws Exception {
        OrMatcher matcher =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(equalTo("Nothing")),
                        new MockValuePredicateMatcher(equalTo("Zippo"))));

        matcher.setId("Test");
        matcher.initialize();

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());

    }

    // TODO
    // @Test
    public void testEqualsHashToString() throws ComponentInitializationException {
        OrMatcher matcher =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(equalTo(value2)),
                        new MockValuePredicateMatcher(equalTo(value3))));

        matcher.toString();

        Assert.assertFalse(matcher.equals(null));
        Assert.assertTrue(matcher.equals(matcher));
        Assert.assertFalse(matcher.equals(this));

        OrMatcher other =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(equalTo(value2)),
                        new MockValuePredicateMatcher(equalTo(value3))));

        Assert.assertTrue(matcher.equals(other));
        Assert.assertEquals(matcher.hashCode(), other.hashCode());

        other =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(equalTo(value3)),
                        new MockValuePredicateMatcher(equalTo(value2))));

        Assert.assertFalse(matcher.equals(other));
        Assert.assertNotSame(matcher.hashCode(), other.hashCode());
    }

    @Test(expectedExceptions = {ComponentInitializationException.class}) public void emptyInput()
            throws ComponentInitializationException {
        OrMatcher matcher = new OrMatcher(null);
        matcher.setId("test");
        matcher.initialize();
    }
/*
    @Test public void testPredicate() throws ComponentInitializationException, AttributeFilterException {
        OrMatcher matcher =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(false),
                        new MockValuePredicateMatcher(false)));
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertFalse(matcher.matches(DataSources.unPopulatedFilterContext()));

        matcher =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(false), null,
                        new MockValuePredicateMatcher(true)));
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertTrue(matcher.matches(DataSources.unPopulatedFilterContext()));

        matcher =
                new OrMatcher(Lists.<Matcher> newArrayList(new MockValuePredicateMatcher(true), null,
                        new MockValuePredicateMatcher(true)));
        matcher.setId("Test");
        matcher.initialize();
        Assert.assertTrue(matcher.matches(DataSources.unPopulatedFilterContext()));

    }
*/
}
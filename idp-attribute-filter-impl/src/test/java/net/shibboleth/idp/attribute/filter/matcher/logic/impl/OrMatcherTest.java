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

package net.shibboleth.idp.attribute.filter.matcher.logic.impl;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.or;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.matcher.impl.AbstractMatcherPolicyRuleTest;
import net.shibboleth.idp.attribute.filter.matcher.impl.MockValuePredicateMatcher;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/** {@link OrMatcher} unit test. */
public class OrMatcherTest extends AbstractMatcherPolicyRuleTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        final Matcher valuePredicate = Matcher.MATCHES_ALL;
        final OrMatcher matcher = newOrMatcher(Collections.singletonList(valuePredicate));
        matcher.setId("test");
        matcher.initialize();

        try {
            matcher.getMatchingValues(null, filterContext);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }

        try {
            matcher.getMatchingValues(attribute, null);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }

        try {
            matcher.getMatchingValues(null, null);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }
    }
    
    @Test public void testSingleton() throws Exception {
        final OrMatcher matcher =
                newOrMatcher(Collections.singletonList((Matcher) new MockValuePredicateMatcher(or(equalTo(value1),
                        equalTo(value2)))));

        matcher.setId("test");
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertEquals(result.size(), 2);
        assertTrue(result.contains(value2));
        assertTrue(result.contains(value1));

    }


    @Test public void testGetMatchingValues() throws Exception {
        OrMatcher matcher =
                newOrMatcher(Arrays.<Matcher>asList(
                        new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2))),
                        new MockValuePredicateMatcher(equalTo(value2))));

        try {
            matcher.getMatchingValues(attribute, filterContext);
            fail();
        } catch (final UninitializedComponentException e) {
            // expect this
        }

        matcher.setId("Test");
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertTrue(result.contains(value2) && result.contains(value1));

        matcher.destroy();
        try {
            matcher.getMatchingValues(attribute, filterContext);
            fail();
        } catch (final DestroyedComponentException e) {
            // expect this
        }

        matcher = newOrMatcher(Collections.EMPTY_LIST);
        matcher.setId("test");
        try {
            matcher.initialize();
            fail();
        } catch (final ComponentInitializationException ex) {
            // OK
        }
    }

    @Test public void testRegressionGetValues() throws ComponentInitializationException {
        final OrMatcher matcher =
                newOrMatcher(Arrays.<Matcher>asList(new MockValuePredicateMatcher(Predicates.alwaysFalse()),
                        new MockValuePredicateMatcher(Predicates.alwaysFalse()), new MockValuePredicateMatcher(
                                equalTo(value1)), new MockValuePredicateMatcher(equalTo(value2))));
        matcher.setId("Test");
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertTrue(result.contains(value2) && result.contains(value1));

        matcher.destroy();

    }

    @Test public void testNoMatchingValues() throws Exception {
        final OrMatcher matcher =
                newOrMatcher(Arrays.<Matcher>asList(new MockValuePredicateMatcher(equalTo("Nothing")),
                        new MockValuePredicateMatcher(equalTo("Zippo"))));

        matcher.setId("Test");
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test public void testFails() throws Exception {
        final OrMatcher matcher = newOrMatcher(Arrays.<Matcher>asList(Matcher.MATCHES_ALL, Matcher.MATCHER_FAILS));
        matcher.setId("test");
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertNull(result);
    }
    
    @Test(expectedExceptions = {ComponentInitializationException.class}) public void emptyInput()
            throws ComponentInitializationException {
        final OrMatcher matcher = newOrMatcher(null);
        matcher.setId("test");
        matcher.initialize();
    }
    
    static public OrMatcher newOrMatcher(final Collection<Matcher> what) {
        final OrMatcher matcher = new OrMatcher();
        matcher.setSubsidiaries(what);
        return matcher;
    }

}

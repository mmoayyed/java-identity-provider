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

package net.shibboleth.idp.attribute.filter.impl.matcher;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.or;

import java.util.Set;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;

/** Unit test for {@link AbstractComparisonMatcher}. */
public class AbstractComparisonMatcherTest extends AbstractMatcherPolicyRuleTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        AbstractComparisonMatcher matcher = new MockValuePredicateMatcher(alwaysTrue());

        boolean thrown = false;
        try {
            matcher.getMatchingValues(null, filterContext);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        thrown = false;
        try {
            matcher.getMatchingValues(attribute, null);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        thrown = false;
        try {
            matcher.getMatchingValues(null, null);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        thrown = false;
        try {
            new MockValuePredicateMatcher(null);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
    }

    @Test public void testGetMatchingValues() throws AttributeFilterException, ComponentInitializationException {
        AbstractComparisonMatcher matcher =
                new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2)));

        Set<AttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(value1) && result.contains(value2));
        
        matcher = new MockValuePredicateMatcher(false);
        Assert.assertTrue(matcher.getMatchingValues(attribute, filterContext).isEmpty());

        matcher = new MockValuePredicateMatcher(true);
        result = matcher.getMatchingValues(attribute, filterContext);
        Assert.assertEquals(result.size(), 3);
    }
    
    @Test(expectedExceptions={ComponentInitializationException.class})
    public void testInitializeNoPredicate() throws ComponentInitializationException {
        AbstractComparisonMatcher matcher = new AbstractComparisonMatcher() {};
        matcher.setId("none");
        matcher.initialize();
    }

    @Test(expectedExceptions={ComponentInitializationException.class})
    public void testInitializeBothPredicates() throws ComponentInitializationException {
        AbstractComparisonMatcher matcher = new AbstractComparisonMatcher() {};
        matcher.setId("both");
        matcher.setPolicyPredicate(new Predicate<AttributeFilterContext>() {
            
            public boolean apply(@Nullable AttributeFilterContext input) {
                return false;
            }
        });
        matcher.setValuePredicate(new Predicate<AttributeValue>() {

            public boolean apply(@Nullable AttributeValue input) {
                return false;
            }});
        matcher.initialize();
    }

    // TODO 
    // @Test
    public void testEqualsHashToString() throws ComponentInitializationException {
        AbstractComparisonMatcher matcher =
                new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2)));

        matcher.toString();

        Assert.assertFalse(matcher.equals(null));
        Assert.assertTrue(matcher.equals(matcher));
        Assert.assertFalse(matcher.equals(this));

        AbstractComparisonMatcher other = new MockValuePredicateMatcher(or(equalTo(value1), equalTo(value2)));

        Assert.assertTrue(matcher.equals(other));
        Assert.assertEquals(matcher.hashCode(), other.hashCode());

        other = new MockValuePredicateMatcher(or(equalTo(value2), equalTo(value1)));

        Assert.assertFalse(matcher.equals(other));
        Assert.assertNotSame(matcher.hashCode(), other.hashCode());

    }
}
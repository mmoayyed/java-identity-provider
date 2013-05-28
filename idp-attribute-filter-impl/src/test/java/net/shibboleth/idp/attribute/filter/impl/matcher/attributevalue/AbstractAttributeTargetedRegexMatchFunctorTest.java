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

package net.shibboleth.idp.attribute.filter.impl.matcher.attributevalue;

import java.util.Collections;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.idp.attribute.filter.impl.matcher.attributevalue.AbstractAttributeTargetedRegexMatchFunctor;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AbstractAttributeTargetedRegexMatchFunctor}.
 */
public class AbstractAttributeTargetedRegexMatchFunctorTest {

    @Test public void setterGetterTest() throws ComponentInitializationException {

        final String NAME = "foo";
        final AbstractAttributeTargetedRegexMatchFunctor functor = new AbstractAttributeTargetedRegexMatchFunctor() {
            public boolean compareAttributeValue(@Nullable AttributeValue value) {
                return false;
            }
        };
        Assert.assertNull(functor.getAttributeId());

        functor.setAttributeId(NAME);
        Assert.assertEquals(functor.getAttributeId(), NAME);
        Assert.assertNotEquals(functor.getAttributeId(), NAME.toUpperCase());

        functor.setId("Test");

        functor.initialize();
        try {
            functor.setAttributeId(NAME);
            Assert.fail();
        } catch (UnmodifiableComponentException e) {
            // OK
        }
    }
    @Test public void testTargetedPolicy() throws ComponentInitializationException, AttributeFilterException {
        final String NAME = "foo";
        AbstractAttributeTargetedRegexMatchFunctor functor = new AbstractAttributeTargetedRegexMatchFunctor() {
            public boolean compareAttributeValue(@Nullable AttributeValue value) {
                return false;
            }
        };
        Assert.assertNull(functor.getAttributeId());

        functor.setAttributeId(NAME);
        functor.setId("Test");
        functor.initialize();
        
        Assert.assertFalse(functor.evaluatePolicyRule(DataSources.unPopulatedFilterContext()));
        final AttributeFilterContext context = DataSources.unPopulatedFilterContext();
        final Attribute attribute = new Attribute(NAME);
        attribute.setValues(Collections.singleton((AttributeValue)new StringAttributeValue("value")));
        context.setPrefilteredAttributes(Collections.singleton(attribute));
        Assert.assertFalse(functor.evaluatePolicyRule(context));
        functor =  new AbstractAttributeTargetedRegexMatchFunctor() {
            public boolean compareAttributeValue(@Nullable AttributeValue value) {
                return true;
            }
        };
        functor.setAttributeId(NAME);
        functor.setId("Test");
        functor.initialize();
        Assert.assertTrue(functor.evaluatePolicyRule(context));
    }
    @Test public void testUnargetedValue() throws ComponentInitializationException, AttributeFilterException {
        final String NAME = "foo";
        final AbstractAttributeTargetedRegexMatchFunctor functor = new AbstractAttributeTargetedRegexMatchFunctor() {
            public boolean compareAttributeValue(@Nullable AttributeValue value) {
                return true;
            }
        };
        Assert.assertNull(functor.getAttributeId());

        functor.setId("Test");
        functor.initialize();
        final Attribute attribute = new Attribute(NAME);
        attribute.setValues(Collections.singleton((AttributeValue)new StringAttributeValue("value")));
        Assert.assertEquals(functor.getMatchingValues(attribute, DataSources.unPopulatedFilterContext()).size(), 1);
    }}

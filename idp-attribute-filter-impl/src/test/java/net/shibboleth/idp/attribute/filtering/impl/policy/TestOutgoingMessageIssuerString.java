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

import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** tests for the {@link OutgoingMessageIssuerStringCriterion}. */
public class TestOutgoingMessageIssuerString {

    /**
     * Test outgoing message issuer matching. Parameterization is tested in other tests.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test
    public void outgoingMessageIssuerStringCriterionTest() throws EvaluationException, ComponentInitializationException {

        AttributeFilterContext filterContext = new AttributeFilterContext(new TestContextContainer());
        OutgoingMessageIssuerStringCriterion filter = new OutgoingMessageIssuerStringCriterion();
        String matcher = TestContextContainer.IDP_ENTITY_ID;
        filter.setMatchString(matcher);
        filter.setCaseSensitive(false);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "match against \"" + matcher + "\"");

        filterContext = new AttributeFilterContext(new TestContextContainer());
        filter = new OutgoingMessageIssuerStringCriterion();
        matcher = TestContextContainer.SP_ENTITY_ID.toLowerCase();
        filter.setMatchString(matcher);
        filter.setCaseSensitive(false);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "case insentitive match against " + matcher);

        filterContext = new AttributeFilterContext(new TestContextContainer());
        filter = new OutgoingMessageIssuerStringCriterion();
        filter.setMatchString(matcher);
        filter.setCaseSensitive(true);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "case sentitive match against " + matcher);

        filterContext = new AttributeFilterContext(new TestContextContainer());
        filter = new OutgoingMessageIssuerStringCriterion();
        matcher = TestContextContainer.SP_ENTITY_ID;
        filter.setMatchString(matcher);
        filter.setCaseSensitive(true);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "case sentitive match against " + matcher);

    }

}

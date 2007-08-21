/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;

/**
 * test the @link(AnyMatchFunctor}.
 */
public class AttributeValueRegexMatchFunctorTest extends BaseTestCase {

    /** {@inheritDoc} */
    public void setUp() throws Exception {
        super.setUp();
        AttributeValueRegexMatchFunctor functor = new AttributeValueRegexMatchFunctor();
        matchFunctor = functor;
        functor.setAttributeId(sAttribute.getId());
        functor.setRegularExpression("o.e");
    }
    
    /**
     * Test that values to permit value test. 
     */
    public void testPermitValue() {
        try {
            assertTrue("evaluatePermitValue", matchFunctor.evaluatePermitValue(filterContext, null, "one"));
            assertFalse("evaluatePermitValue", matchFunctor.evaluatePermitValue(filterContext, null, "two"));
        } catch (FilterProcessingException e) {
           fail(e.getLocalizedMessage());
        }
    }

    /**
     * Test the policy.
     */
    public void testPolicyRequirement() {
        try {
            AttributeValueRegexMatchFunctor functor = (AttributeValueRegexMatchFunctor) matchFunctor;
            assertTrue("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            functor.setRegularExpression("[tT].*e");
            assertFalse("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            sAttribute.getValues().add("two");
            assertFalse("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            sAttribute.getValues().add("three");
            assertTrue("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            functor.setAttributeId("ScopedValue");
            assertFalse("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            functor.setAttributeId("Scope");
            assertFalse("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            
            
        } catch (FilterProcessingException e) {
           fail(e.getLocalizedMessage());
        }
    }
}

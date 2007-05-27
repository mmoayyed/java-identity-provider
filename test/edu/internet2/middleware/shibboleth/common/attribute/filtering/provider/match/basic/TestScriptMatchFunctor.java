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
public class TestScriptMatchFunctor extends BaseTestCase {
    
    /**
     * Simple test to check whether parameters are passed OK.
     */
    private static final String SCRIPT = 
         "function Result(filterContext, attributeId, attributeValue) {" +
         "  if (attributeId == null) return true;" +
         "  if (attributeValue == null) return true;" +
         "  return filterContext.getUnfilteredAttributes().get(attributeId).getValues().first().toString().equals(attributeValue);" +
         " }" +
         "Result(filterContext, attributeId, attributeValue); ";

    /** {@inheritDoc} */
    public void setUp() throws Exception {
        super.setUp();
        matchFunctor = new ScriptMatchFunctor("JavaScript", "true;");
    }
    
    /**
     * Test two one line tests.
     */
    public void testSimpleScript() {
        try {
            assertTrue("Simple Value", matchFunctor.evaluatePermitValue(filterContext, null, null));
            assertTrue("Simple Requirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            matchFunctor = new ScriptMatchFunctor("JavaScript", "false;");
            assertFalse("Simple Value", matchFunctor.evaluatePermitValue(null, null, null));
            assertFalse("Simple Requirement", matchFunctor.evaluatePolicyRequirement(filterContext));
        } catch (FilterProcessingException e) {
           fail(e.getLocalizedMessage());
        }
    }

    /**
     * Test the complex script above (which does a very selected and unprotected equality test).  
     */
    public void testComplexScript() {
        try {
            matchFunctor = new ScriptMatchFunctor("JavaScript", SCRIPT);
            assertTrue("Complex Requirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            assertTrue("Complex Value", matchFunctor.evaluatePermitValue(filterContext, sAttribute.getId(), "one"));
            assertFalse("Complex Value", matchFunctor.evaluatePermitValue(filterContext, sAttribute.getId(), "two"));
            assertFalse("Complex Value", matchFunctor.evaluatePermitValue(filterContext, iAttribute.getId(), "two"));
            assertTrue("Complex Value", matchFunctor.evaluatePermitValue(filterContext, scope.getId(), "ScopedValue"));
            assertFalse("Complex Value", matchFunctor.evaluatePermitValue(filterContext, scope.getId(), "ScopedScope"));
        } catch (FilterProcessingException e) {
           fail(e.getLocalizedMessage());
        }
    }
}

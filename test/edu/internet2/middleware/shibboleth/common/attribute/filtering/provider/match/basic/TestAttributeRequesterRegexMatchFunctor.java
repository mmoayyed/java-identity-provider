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
public class TestAttributeRequesterRegexMatchFunctor extends BaseTestCaseMetadata {

    /** {@inheritDoc} */

    public void setUp() throws Exception {
        super.setUp();
        
        AttributeRequesterRegexMatchFunctor functor = new AttributeRequesterRegexMatchFunctor();
        //
        // requester is setup as being called "Rely"
        //
        functor.setRegularExpression("[rR].*[lL].");
        matchFunctor = functor;
      
    }
    
    /**
     * test against the issuer name ("Rely") in the metadata. 
     */
    public void testIssuerRegexp() {
        AttributeRequesterRegexMatchFunctor functor = (AttributeRequesterRegexMatchFunctor) matchFunctor;
        try {
            assertTrue(matchFunctor.evaluatePermitValue(filterContext, null, null));
            assertTrue(matchFunctor.evaluatePolicyRequirement(filterContext));
            functor.setRegularExpression(".*r");
            assertFalse(matchFunctor.evaluatePolicyRequirement(filterContext));
            } catch (FilterProcessingException e) {
            fail(e.getLocalizedMessage());
        }
    }
}

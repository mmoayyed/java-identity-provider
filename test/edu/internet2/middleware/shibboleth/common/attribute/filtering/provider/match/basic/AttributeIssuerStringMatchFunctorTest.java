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
public class AttributeIssuerStringMatchFunctorTest extends BaseTestCaseMetadata {

    /** {@inheritDoc} */

    public void setUp() throws Exception {
        super.setUp();
        
        AttributeIssuerStringMatchFunctor functor = new AttributeIssuerStringMatchFunctor();
        functor.setMatchString("provide");
        functor.setCaseSensitive(true);
        matchFunctor = functor;
      
    }
    
    /**
     * test against the issuer name ("Provide") in the metadata. 
     */
    public void testIssuerCaseSensitive() {
        AttributeIssuerStringMatchFunctor functor = (AttributeIssuerStringMatchFunctor) matchFunctor;
        try {
            assertFalse(matchFunctor.evaluatePermitValue(filterContext, null, null));
            assertFalse(matchFunctor.evaluatePolicyRequirement(filterContext));
            functor.setMatchString("Provide");
            assertTrue(matchFunctor.evaluatePolicyRequirement(filterContext));
            } catch (FilterProcessingException e) {
            fail(e.getLocalizedMessage());
        }
    }
    
    /**
     * test against almost the issuer name in the metadata. 
     */
    public void testIssuerCaseInsensitive() {
        AttributeIssuerStringMatchFunctor functor = (AttributeIssuerStringMatchFunctor) matchFunctor;
        functor.setCaseSensitive(false);
        try {
            assertTrue(matchFunctor.evaluatePermitValue(filterContext, null, null));
            assertTrue(matchFunctor.evaluatePolicyRequirement(filterContext));
        } catch (FilterProcessingException e) {
            fail(e.getLocalizedMessage());
        }
    }

    /**
     * test against nothing like the issuer name in the metadata. 
     */
    public void testIssuerMismatch() {
        AttributeIssuerStringMatchFunctor functor = (AttributeIssuerStringMatchFunctor) matchFunctor;
        functor.setMatchString("Rely");
        try {
            assertFalse(matchFunctor.evaluatePermitValue(filterContext, null, null));
            assertFalse(matchFunctor.evaluatePolicyRequirement(filterContext));
        } catch (FilterProcessingException e) {
            fail(e.getLocalizedMessage());
        }
    }
    
}

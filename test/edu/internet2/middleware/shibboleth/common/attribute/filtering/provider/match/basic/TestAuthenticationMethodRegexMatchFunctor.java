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
public class TestAuthenticationMethodRegexMatchFunctor extends BaseTestCase {

    /** {@inheritDoc} */
    public void setUp() throws Exception {
        super.setUp();
        AuthenticationMethodRegexMatchFunctor functor = new AuthenticationMethodRegexMatchFunctor();
        matchFunctor = functor;
        functor.setRegularExpression("B.*h");
        requestContext.setPrincipalAuthenticationMethod("Blind Faith");
    }
    
    public void testPermitValue() {
        try {
            assertTrue("evaluatePermitValue", 
                        matchFunctor.evaluatePermitValue(filterContext, null, null));
            requestContext.setPrincipalAuthenticationMethod(" Retinal Scan ");
            assertFalse("evaluatePermitValue", 
                        matchFunctor.evaluatePermitValue(filterContext, null, null));
        } catch (FilterProcessingException e) {
           fail(e.getLocalizedMessage());
        }
    }

    public void testPolicyRequirement() {
        try {
            assertTrue("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            requestContext.setPrincipalAuthenticationMethod(" Retinal Scan ");
            assertFalse("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            AuthenticationMethodRegexMatchFunctor functor = (AuthenticationMethodRegexMatchFunctor) matchFunctor;
            functor.setRegularExpression("[rR]etinal [sS]can");
            assertTrue("evaluatePolicyRequirement", matchFunctor.evaluatePolicyRequirement(filterContext));
            
        } catch (FilterProcessingException e) {
           fail(e.getLocalizedMessage());
        }
    }
}

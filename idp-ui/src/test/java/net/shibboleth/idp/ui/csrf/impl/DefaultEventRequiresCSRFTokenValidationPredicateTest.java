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

package net.shibboleth.idp.ui.csrf.impl;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockFlowExecutionContext;
import org.springframework.webflow.test.MockFlowSession;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.ui.csrf.BaseCSRFTest;
import net.shibboleth.idp.ui.csrf.BaseCSRFTokenPredicate;
import net.shibboleth.idp.ui.csrf.BaseCSRFTokenPredicateTest;


/**
 * Tests the {@link DefaultEventRequiresCSRFTokenValidationPredicate}. See also {@link BaseCSRFTokenPredicateTest}
 * for more detailed testing of the included and excluded view states.
 */
public class DefaultEventRequiresCSRFTokenValidationPredicateTest extends BaseCSRFTest{

    /** Test event and view requires CSRF verification.*/
    @Test public void testEventRequiresCSRFTokenValidation() {

        DefaultEventRequiresCSRFTokenValidationPredicate predicate = new DefaultEventRequiresCSRFTokenValidationPredicate();

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);

        Assert.assertTrue(predicate.test(src,new Event(this,"proceed")));
    }
    
    /** Test a random eventId, as all eventIds should be included.*/
    @Test public void testARandomEventIdIsEvaluated() {

        DefaultEventRequiresCSRFTokenValidationPredicate predicate = new DefaultEventRequiresCSRFTokenValidationPredicate();

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");        
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);

        Assert.assertTrue(predicate.test(src,new Event(this,"random-event-id")));
    }
    
    /** Test that token verification is not required if state is excluded.*/
    @Test public void testEventDoesNotRequiresCSRFTokenValidationExcludedState() {

        DefaultEventRequiresCSRFTokenValidationPredicate predicate = new DefaultEventRequiresCSRFTokenValidationPredicate();        
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        currentState.getAttributes().put(BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, true);
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);

        Assert.assertFalse(predicate.test(src,new Event(this,"proceed")));
    }

    

}

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

import java.util.Arrays;
import java.util.List;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockFlowExecutionContext;
import org.springframework.webflow.test.MockFlowSession;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.ui.csrf.BaseCSRFTest;
import net.shibboleth.idp.ui.csrf.BaseCSRFTokenPredicateTest;


/**
 * Tests the {@link DefaultEventRequiresCSRFTokenValidationPredicate}. See also {@link BaseCSRFTokenPredicateTest}
 * for more detailed testing of the included and excluded view states.
 */
public class DefaultEventRequiresCSRFTokenValidationPredicateTest extends BaseCSRFTest{

    @Test public void testEventRequiresCSRFTokenValidation() {

        DefaultEventRequiresCSRFTokenValidationPredicate predicate = new DefaultEventRequiresCSRFTokenValidationPredicate();

        List<String> includedViewStates = Arrays.asList(new String[] {"*"});
        predicate.setIncludedViewStateIds(includedViewStates);

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

        List<String> includedViewStates = Arrays.asList(new String[] {"*"});
        predicate.setIncludedViewStateIds(includedViewStates);

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);

        Assert.assertTrue(predicate.test(src,new Event(this,"random-event-id")));
    }
    
    @Test public void testEventDoesNotRequiresCSRFTokenValidationExcludedState() {

        DefaultEventRequiresCSRFTokenValidationPredicate predicate = new DefaultEventRequiresCSRFTokenValidationPredicate();

        List<String> includedViewStates = Arrays.asList(new String[] {"*"});
        List<String> excludedViewStateIds = Arrays.asList(new String[] {"a-view-state"});
        predicate.setIncludedViewStateIds(includedViewStates);
        predicate.setExcludedViewStateIds(excludedViewStateIds);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);

        Assert.assertFalse(predicate.test(src,new Event(this,"proceed")));
    }

    

}

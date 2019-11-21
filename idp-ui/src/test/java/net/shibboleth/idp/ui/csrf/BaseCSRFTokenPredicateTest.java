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

package net.shibboleth.idp.ui.csrf;

import java.util.Arrays;
import java.util.List;
import java.util.Set;


import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.webflow.test.MockFlowExecutionContext;
import org.springframework.webflow.test.MockFlowSession;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class BaseCSRFTokenPredicateTest extends BaseCSRFTest{
    

    /** Test setting the included view states.*/
    @Test public void testSetNullIncludedViewStates() {

        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        predicate.setIncludedViewStateIds(null);

        Object excludedListObject = ReflectionTestUtils.getField(predicate, INCLUDED_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(excludedListObject);
        Assert.assertTrue(excludedListObject instanceof Set);
        Assert.assertEquals(((Set) excludedListObject).size(), 0);

    }
   
    /** Test setting the excluded view states.*/
    @Test public void testSetExcludedViewStates() {

        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
       
        predicate.setExcludedViewStateIds(null);
        // should be set as empty list
        Object excludedListObject = ReflectionTestUtils.getField(predicate, EXCLUDED_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(excludedListObject);
        Assert.assertTrue(excludedListObject instanceof Set);
        Assert.assertEquals(((Set) excludedListObject).size(), 0);

        // test setting an actual excluded view state
        List<String> excludedViewStates = Arrays.asList(new String[] {"excludedViewId"});
        predicate.setExcludedViewStateIds(excludedViewStates);
        excludedListObject = ReflectionTestUtils.getField(predicate, EXCLUDED_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(excludedListObject);
        Assert.assertTrue(excludedListObject instanceof Set);
        Assert.assertEquals(((Set) excludedListObject).size(), 1);
        Assert.assertTrue(((Set) excludedListObject).contains("excludedViewId"));

    }
    
    /** Test setting one included view states.*/
    @Test public void testSetOneIncludedViewStates() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        
     // test setting an actual include view state, but no wildcard
        List<String> includedViewStates = Arrays.asList(new String[] {"includedViewId"});
        predicate.setIncludedViewStateIds(includedViewStates);
        Object includedListObject = ReflectionTestUtils.getField(predicate, INCLUDED_VIEWSTATES_FIELDNAME);
        Object includeAllViews = ReflectionTestUtils.getField(predicate, INCLUDE_ALL_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(includedListObject);
        Assert.assertTrue(includedListObject instanceof Set);
        Assert.assertEquals(((Set) includedListObject).size(), 1);
        Assert.assertTrue(((Set) includedListObject).contains("includedViewId"));
        Assert.assertNotNull(includeAllViews);
        Assert.assertTrue(includeAllViews instanceof Boolean);
        Assert.assertFalse((Boolean)includeAllViews);
    }
    
    /** Test setting two included view states.*/
    @Test public void testSetTwoIncludedViewStates() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        
     // test setting two actual include view state, but no wildcard
        List<String> includedViewStates = Arrays.asList(new String[] {"includedViewId","includedViewId2"});
        predicate.setIncludedViewStateIds(includedViewStates);
        Object includedListObject = ReflectionTestUtils.getField(predicate, INCLUDED_VIEWSTATES_FIELDNAME);
        Object includeAllViews = ReflectionTestUtils.getField(predicate, INCLUDE_ALL_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(includedListObject);
        Assert.assertTrue(includedListObject instanceof Set);
        Assert.assertEquals(((Set) includedListObject).size(), 2);
        Assert.assertTrue(((Set) includedListObject).contains("includedViewId"));
        Assert.assertTrue(((Set) includedListObject).contains("includedViewId2"));
        Assert.assertNotNull(includeAllViews);
        Assert.assertTrue(includeAllViews instanceof Boolean);
        Assert.assertFalse((Boolean)includeAllViews);
    }
    
    /** Test setting a wildcard include list element.*/
    @Test public void testSetWildcardIncludedViewStates() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        
      //test setting a wildcard include list element
        List<String> includedViewStates = Arrays.asList(new String[] {BaseCSRFTokenPredicate.INCLUDE_ALL_WILDCARD});
        predicate.setIncludedViewStateIds(includedViewStates);
        Object includedListObject = ReflectionTestUtils.getField(predicate, INCLUDED_VIEWSTATES_FIELDNAME);
        Object includeAllViews = ReflectionTestUtils.getField(predicate, INCLUDE_ALL_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(includedListObject);
        Assert.assertTrue(includedListObject instanceof Set);
        Assert.assertEquals(((Set) includedListObject).size(), 0);
        Assert.assertNotNull(includeAllViews);
        Assert.assertTrue(includeAllViews instanceof Boolean);
        Assert.assertTrue((Boolean)includeAllViews);
    }
    
    /** Test setting a wildcard include list element alongside other viewstates.*/
    @Test public void testSetWildcardAndOtherIncludedViewStates() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        
      //test setting a wildcard include list element alongside other includes.
        //should result in 0 includedViewstateIds being set, but the includeAllViews=true
        List<String> includedViewStates = Arrays.asList(new String[] {"anotherViewState","*","moreViewStates"});
        predicate.setIncludedViewStateIds(includedViewStates);
        Object includedListObject = ReflectionTestUtils.getField(predicate, INCLUDED_VIEWSTATES_FIELDNAME);
        Object includeAllViews = ReflectionTestUtils.getField(predicate, INCLUDE_ALL_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(includedListObject);
        Assert.assertTrue(includedListObject instanceof Set);
        Assert.assertEquals(((Set) includedListObject).size(), 0);
        Assert.assertNotNull(includeAllViews);
        Assert.assertTrue(includeAllViews instanceof Boolean);
        Assert.assertTrue((Boolean)includeAllViews);
    }
    
    /** Test setting  empty and null viewstate ids.*/
    @Test public void testSetEmptyAndNullIncludedViewStates() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        
        //test setting one empty and one null element. Should leave 0 included.
        List<String>  includedViewStates = Arrays.asList(new String[] {"",null});        
        predicate.setIncludedViewStateIds(includedViewStates);
        Object includedListObject = ReflectionTestUtils.getField(predicate, INCLUDED_VIEWSTATES_FIELDNAME);
        Object includeAllViews = ReflectionTestUtils.getField(predicate, INCLUDE_ALL_VIEWSTATES_FIELDNAME);
        Assert.assertNotNull(includedListObject);
        Assert.assertTrue(includedListObject instanceof Set);
        Assert.assertEquals(((Set) includedListObject).size(), 0);
        Assert.assertNotNull(includeAllViews);
        Assert.assertTrue(includeAllViews instanceof Boolean);
        Assert.assertFalse((Boolean)includeAllViews);
    }
    
    /** Test all states included, nothing excluded, predicate true.*/
    @Test public void testAllStatesAreIncluded() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        List<String> includedViewStates = Arrays.asList(new String[] {"*"});
        predicate.setIncludedViewStateIds(includedViewStates);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        Assert.assertTrue(predicate.isStateIncluded(src));
        
    }
    
    /** Test one states included, nothing excluded, predicate true.*/
    @Test public void testViewStatesIsIncluded() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        List<String> includedViewStates = Arrays.asList(new String[] {"a-view-state"});
        predicate.setIncludedViewStateIds(includedViewStates);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        Assert.assertTrue(predicate.isStateIncluded(src));
        
    }
    
    /** Test one state included, same state excluded, predicate false.*/
    @Test public void testViewStatesIsIncludedAndExcluded() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        List<String> includedViewStates = Arrays.asList(new String[] {"a-view-state"});
        List<String> excludeddViewStates = Arrays.asList(new String[] {"a-view-state"});
        predicate.setIncludedViewStateIds(includedViewStates);
        predicate.setExcludedViewStateIds(excludeddViewStates);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        Assert.assertFalse(predicate.isStateIncluded(src));
        
    }
    
    /** Test one state included, other states are excluded, predicate true.*/
    @Test public void testViewStatesIsIncludedAndOthersExcluded() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        List<String> includedViewStates = Arrays.asList(new String[] {"a-view-state"});
        List<String> excludeddViewStates = Arrays.asList(new String[] {"a-view-state-not-same-as-included"});
        predicate.setIncludedViewStateIds(includedViewStates);
        predicate.setExcludedViewStateIds(excludeddViewStates);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        Assert.assertTrue(predicate.isStateIncluded(src));
        
    }
    
    /** Test all states included, the current state excluded excluded, predicate false.*/
    @Test public void testAllViewStatesIncludedOneExcluded() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        List<String> includedViewStates = Arrays.asList(new String[] {"*"});
        List<String> excludeddViewStates = Arrays.asList(new String[] {"a-view-state"});
        predicate.setIncludedViewStateIds(includedViewStates);
        predicate.setExcludedViewStateIds(excludeddViewStates);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        Assert.assertFalse(predicate.isStateIncluded(src));
        
    }
    
    /** Test nothing included, one excluded, so predicate always false.*/
    @Test public void testExcludedNoneIncluded() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        List<String> excludeddViewStates = Arrays.asList(new String[] {"a-view-state"});
        predicate.setExcludedViewStateIds(excludeddViewStates);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        Assert.assertFalse(predicate.isStateIncluded(src));
        
    }
    
    
    /**
     * Mock concrete implementation of the {@link BaseCSRFTokenPredicate}.
     */
    private class MockBaseCSRFTokenPredicateImplementaiton extends BaseCSRFTokenPredicate{
        
    }

}

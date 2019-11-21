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
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.MockFlowExecutionContext;
import org.springframework.webflow.test.MockFlowSession;
import org.springframework.webflow.test.MockParameterMap;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.ui.csrf.BaseCSRFTest;
import net.shibboleth.idp.ui.csrf.CSRFToken;
import net.shibboleth.idp.ui.csrf.CSRFTokenManager;
import net.shibboleth.idp.ui.csrf.InvalidCSRFTokenException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * Test the {@link CSRFTokenFlowExecutionListener}.
 */
public class CSRFTokenFlowExecutionListenerTest extends BaseCSRFTest{
    
    /** the HTTP request parameter name that holds the CSRF token.*/
    private final String CSRF_PARAM_NAME = "csrf_token";

    /** The listener instance to test.*/
    private CSRFTokenFlowExecutionListener listener;

    /**
     * Add a {@link DefaultEventRequiresCSRFTokenValidationPredicate} and  {@link DefaultViewRequiresCSRFTokenPredicate} to
     * the input {@link CSRFTokenFlowExecutionListener} in-place. Configure their included and excluded views if not null.
     * 
     * @param list the listener to add the prediates to.
     * @param includeViews the list of included views.
     * @param excludeViews the list of excluded views.
     */
    private void addCSRFViewAndEventPredicates(@Nonnull final CSRFTokenFlowExecutionListener list, 
            @Nullable final List<String> includeViews, @Nullable final List<String> excludeViews) {
        
        DefaultEventRequiresCSRFTokenValidationPredicate eventPredicate = new DefaultEventRequiresCSRFTokenValidationPredicate();
        DefaultViewRequiresCSRFTokenPredicate viewPredicate  = new DefaultViewRequiresCSRFTokenPredicate();    
        if (includeViews!=null) {
            eventPredicate.setIncludedViewStateIds(includeViews);
            viewPredicate.setIncludedViewStateIds(includeViews);
        }
        if (excludeViews!=null) {            
            eventPredicate.setExcludedViewStateIds(excludeViews);
            viewPredicate.setExcludedViewStateIds(excludeViews);
        }       
        list.setEventRequiresCSRFTokenValidationPredicate(eventPredicate);
        list.setViewRequiresCSRFTokenPredicate(viewPredicate);
        
    }
    
    @BeforeMethod public void setup() {
        
        CSRFTokenManager manager = new CSRFTokenManager();
        manager.setCsrfParameterName(CSRF_PARAM_NAME);
        listener = new CSRFTokenFlowExecutionListener();
        listener.setCsrfTokenManager(manager);
        listener.setEnabled(true);
    }

    /** Test the listener adds the CSRF token to the viewScope on view rendering.*/
    @Test public void testAddingCsrfTokenToViewScopeOnRendering() {

      
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"a-view-state"}), null);
        
        listener.viewRendering(src, new MockView("login",src), currentState);
        Object csrfTokenValueObject = src.getViewScope().get(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME);
        Assert.assertNotNull(csrfTokenValueObject);
        Assert.assertTrue(csrfTokenValueObject instanceof CSRFToken);
        Assert.assertFalse(((CSRFToken) csrfTokenValueObject).getToken().isEmpty());

    }
    
    /**
     * Test the listener throws an {@link InvalidCSRFTokenException} if the viewScope and request token do not match.
     * All views are included, none are excluded.
     */
    @Test(expectedExceptions=InvalidCSRFTokenException.class) public void testAllIncludedNoneExcluded() {
        
        addCSRFViewAndEventPredicates(listener,Arrays.asList(new String[] {"*"}),null);

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "will-fail");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    

    /**
     * Test the listener does not thrown an {@link InvalidCSRFTokenException}. The tokens do not match,
     * ALL views are included, but the specific view is excluded.
     */
    @Test public void testAllIncludedViewExcluded() {
        
      
        addCSRFViewAndEventPredicates(listener,Arrays.asList(new String[] {"*"}),Arrays.asList(new String[] {"a-view-state"}));

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "would-fail-but-is-excluded");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }

    /**
     * Test the listener throws an {@link InvalidCSRFTokenException} if the viewScope and request token do not match.
     * Note, you can not tell from the exception thrown if the issue was specifically a token mismatch - although
     * the construction of the test should enforce that it is.
     */
    @Test(expectedExceptions=InvalidCSRFTokenException.class) public void testCsrfTokenMismatch() {

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "will-fail");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);
       
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"a-view-state"}), null);
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    

    
    /**
     * Test the listener does not throw an {@link InvalidCSRFTokenException} even though the tokens do not match, because
     * the listener is disabled.
     */
    @Test public void testDisabled() {
        
        listener.setEnabled(false);
        
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"*"}), null);

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "should-fail-but-disabled");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"proceed")); 
    }

    /** 
     * Test the listener gracefully (no exception) handles a non view-state. Note this
     * should really not happen in production, as SWF should not call the viewRendering method
     * unless in a view-state.
     * */
    @Test public void testDoesNotAddTokenToNonViewState() {

        MockFlowSession flowSession = new MockFlowSession();
        ActionState currentState = new ActionState(new Flow("testFlow"), "action-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        addCSRFViewAndEventPredicates(listener, null,null);
       
        listener.viewRendering(src, new MockView("login",src), currentState);
    }    

    /** 
     * Test the listener does not test the CSRF token if called from in a non view-state. 
     * No exception should be thrown. Note, if it did test the tokens, as none are present, an exception
     * would be thrown - which is undesirable.
     * */
    @Test public void testDoesNotTestTokenInNonViewState() {
        
        MockFlowSession flowSession = new MockFlowSession();
        ActionState currentState = new ActionState(new Flow("testFlow"), "action-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        addCSRFViewAndEventPredicates(listener,null,null);
       
        listener.eventSignaled(src, new Event(this,"proceed"));     
    }
    
   
    
    /** Test the listener throws an {@link InvalidCSRFTokenException} if no CSRF token is present in the Http request.*/
    @Test(expectedExceptions=InvalidCSRFTokenException.class) public void testHttpRequestTokenException() {

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), "csrf_token");
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
       
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"a-view-state"}), null);
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    
    /**
     * Test the listener does throw an {@link InvalidCSRFTokenException} as the tokens do not match.
     * A new {@link EventRequiresCSRFTokenValidationPredicate} is created to match the new eventId.
     */
    @Test(expectedExceptions=InvalidCSRFTokenException.class) public void testInvalidTokenUsingNewEventRequiresCSRFTokenValidationPredicate() {
        
        addCSRFViewAndEventPredicates(listener,Arrays.asList(new String[] {"*"}),null);
        
        listener.setEventRequiresCSRFTokenValidationPredicate(new BiPredicate<RequestContext, Event>() {
            
            public boolean test(RequestContext context, Event event) {
               if (event.getId().equals("new-event-id")) {
                   return true;
               }
               return false;
            }
        });

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "should-fail");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"new-event-id"));  
        
    }
    
    /** Test the listener throws an {@link InvalidCSRFTokenException} if no CSRF token is present in the viewscope.*/
    @Test(expectedExceptions=InvalidCSRFTokenException.class) public void testNoViewScopeTokenException() {

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
       
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"a-view-state"}),null);
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    
    /**
     * Test the listener does not throw an {@link InvalidCSRFTokenException} even though the tokens do not match, because
     * the view has not been included (or excluded, but that should not matter).
     */
    @Test public void testNoViewsIncludedOrExcluded() {
        
        addCSRFViewAndEventPredicates(listener,null, null);
       
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "should-fail-but-view-not-included");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    
    
    /** Test setting a CSRF token manager.*/
    @Test public void testSetCsrfTokenManager() {      
        listener.setCsrfTokenManager(new CSRFTokenManager());
    }
    
    /** Test setting the listener's enabled property.*/
    @Test public void testSetEnabled() {

        //create a new instance to test default enabled = false.
        CSRFTokenFlowExecutionListener listener = new CSRFTokenFlowExecutionListener();
        // test default is false.
        Object enabledObject = ReflectionTestUtils.getField(listener, "enabled");
        Assert.assertNotNull(enabledObject);
        Assert.assertTrue(enabledObject instanceof Boolean);
        Assert.assertFalse(((Boolean) enabledObject));

        // test enabling
        listener.setEnabled(true);
        enabledObject = ReflectionTestUtils.getField(listener, "enabled");
        Assert.assertNotNull(enabledObject);
        Assert.assertTrue(enabledObject instanceof Boolean);
        Assert.assertTrue(((Boolean) enabledObject));
    }
    
    /** Test the listener does not add CSRF token to the viewScope on view rendering, as listener disabled.*/
    @Test public void testTokenNotAddedToViewScopeOnRenderingWhenDisabled() {

        //create a new instance to test default enabled = false.
        CSRFTokenFlowExecutionListener listener = new CSRFTokenFlowExecutionListener();
        // test enabling
        listener.setEnabled(false);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockRequestContext src = new MockRequestContext(context);
        
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"a-view-state"}), null);
        
        listener.viewRendering(src, new MockView("login",src), currentState);
        Object csrfTokenValueObject = src.getViewScope().get(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME);
        Assert.assertNull(csrfTokenValueObject);
      

    }
    

    
    /** Test setting a null CSRF token manager triggers a {@link ConstraintViolationException}.*/
    @Test(expectedExceptions = ConstraintViolationException.class) public void testSetNullCsrfTokenManager() {       
        listener.setCsrfTokenManager(null);
    }
    
    /** Test setting a null event requires csrf validation predicate triggers a {@link ConstraintViolationException}.*/
    @Test(expectedExceptions = ConstraintViolationException.class) public void testSetNullEventRequiresCSRFValidationPredicate() {       
        listener.setEventRequiresCSRFTokenValidationPredicate(null);
    }
    
    /** Test setting a null view requires csrf token predicate triggers a {@link ConstraintViolationException}.*/
    @Test(expectedExceptions = ConstraintViolationException.class) public void testSetNullViewRequiresCSRFTokenPredicate() {       
        listener.setViewRequiresCSRFTokenPredicate(null);
    }
    
    /**
     * Tests the {@link InvalidCSRFTokenException} is thrown when the view scoped token is the wrong type -
     * String rather than {@link CSRFToken}.
     */
    @Test(expectedExceptions=InvalidCSRFTokenException.class) public void testTokenWrongType() {
        
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"*"}), null);
        
        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
      
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, "string-token");
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "string-token");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"proceed")); 
    }
    
    /** Test an unset csrf token manager triggers an initialisation exception.*/
    @Test(expectedExceptions=ComponentInitializationException.class) void testUnsetCsrfTokenManager() 
            throws ComponentInitializationException{
        
        CSRFTokenFlowExecutionListener listener = new CSRFTokenFlowExecutionListener();
        listener.setViewRequiresCSRFTokenPredicate(new DefaultViewRequiresCSRFTokenPredicate());
        listener.setEventRequiresCSRFTokenValidationPredicate(new DefaultEventRequiresCSRFTokenValidationPredicate());
        listener.initialize();
    }
    
    /** Test an unset ViewRequiresCSRFToken predicate triggers an initialisation exception.*/
    @Test(expectedExceptions=ComponentInitializationException.class) void testUnsetEventRequiresCSRFValidationPredicate() 
            throws ComponentInitializationException{
        
        CSRFTokenFlowExecutionListener listener = new CSRFTokenFlowExecutionListener();
        listener.setCsrfTokenManager(new CSRFTokenManager());
        listener.setViewRequiresCSRFTokenPredicate(new DefaultViewRequiresCSRFTokenPredicate());
        listener.initialize();
    }
    
    /** Test an unset EventRequiresCSRFTokenValidation predicate triggers an initialisation exception.*/
    @Test(expectedExceptions=ComponentInitializationException.class) void testUnsetViewRequiresCSRFTokenPredicate() 
            throws ComponentInitializationException{
        
        CSRFTokenFlowExecutionListener listener = new CSRFTokenFlowExecutionListener();
        listener.setCsrfTokenManager(new CSRFTokenManager());
        listener.setEventRequiresCSRFTokenValidationPredicate(new DefaultEventRequiresCSRFTokenValidationPredicate());
        listener.initialize();
    }
    
    
    /** 
     * Test the listener is able to verify the token present in the viewScope matches that supplied in the 
     * HTTP request. No exception signifies success.
     */
    @Test public void testValidCsrfToken() {

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, viewScopeToken.getToken());
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        addCSRFViewAndEventPredicates(listener, Arrays.asList(new String[] {"a-view-state"}), null);
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    
    /**
     * Test the listener does not throw an {@link InvalidCSRFTokenException} even though the tokens do not match, because
     * the view has not been included, and has been excluded (although if not included, exclude should not matter).
     */
    @Test public void testViewExcludedNoneIncluded() {
        

        addCSRFViewAndEventPredicates(listener,null, Arrays.asList(new String[] {"a-view-state"}));

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "should-fail-but-view-excluded");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    
   
    /**
     * Test the listener does not throw an {@link InvalidCSRFTokenException} even though the tokens do not match, because
     * the view has not been excluded even though it has been included.
     */ 
    @Test public void testViewIncludedAndExcluded() {
        
        addCSRFViewAndEventPredicates(listener,Arrays.asList(new String[] {"a-view-state"}), Arrays.asList(new String[] {"a-view-state"}));

        MockFlowSession flowSession = new MockFlowSession();
        MockViewState currentState = new MockViewState("testFlow", "a-view-state");
        flowSession.setState(currentState);
        
        CSRFToken viewScopeToken = new SimpleCSRFToken(UUID.randomUUID().toString(), CSRF_PARAM_NAME);
        flowSession.getViewScope().put(CSRFTokenFlowExecutionListener.CSRF_TOKEN_VIEWSCOPE_NAME, viewScopeToken);
       
        
        MockFlowExecutionContext context = new MockFlowExecutionContext(flowSession);
        MockParameterMap map = new MockParameterMap();
        map.put(CSRF_PARAM_NAME, "should-fail-but-view-excluded");
        MockExternalContext extContext = new MockExternalContext(map);        
        MockRequestContext src = new MockRequestContext(context);
        src.setExternalContext(extContext);       
        
        listener.eventSignaled(src, new Event(this,"proceed"));        

    }
    

}

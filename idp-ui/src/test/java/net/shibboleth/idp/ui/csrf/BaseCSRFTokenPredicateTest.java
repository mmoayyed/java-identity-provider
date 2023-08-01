/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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


import org.springframework.webflow.definition.StateDefinition;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Base CSRF token predicate tests.
 */
public class BaseCSRFTokenPredicateTest extends BaseCSRFTest{
    

    @Test public void testSafeGetBooleanAttributeWithNullStateDefinition(){
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        Assert.assertFalse(predicate.safeGetBooleanStateAttribute(null, "test", false));
        Assert.assertTrue(predicate.safeGetBooleanStateAttribute(null, "test", true));
        
    }
    
    @Test public void testSafeGetBooleanAttributeWithNullAttrName(){
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        StateDefinition state = new MockViewState("test", "test-view");
        Assert.assertFalse(predicate.safeGetBooleanStateAttribute(state, null, false));
        Assert.assertTrue(predicate.safeGetBooleanStateAttribute(state, null, true));
        
    }
    
    /** Test normal operation of the safeGetBoolean method with a true csrf exclusion attribute.*/
    @Test public void testSafeGetBooleanTrue() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        StateDefinition state = new MockViewState("test", "test-view");
        state.getAttributes().put(BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, true);
        Assert.assertTrue(predicate.safeGetBooleanStateAttribute(state, 
                BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, false));

    }
    
    /** Test normal operation of the safeGetBoolean method with a false csrf exclusion attribute.*/
    @Test public void testSafeGetBooleanFalse() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        StateDefinition state = new MockViewState("test", "test-view");
        state.getAttributes().put(BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, false);
        Assert.assertFalse(predicate.safeGetBooleanStateAttribute(state, 
                BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, false));

    }
    
    /** Test normal operation of the safeGetBoolean method with a null csrf exclusion attribute.*/
    @Test public void testSafeGetBooleanNullAttributeFalse() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        StateDefinition state = new MockViewState("test", "test-view");
        Assert.assertFalse(predicate.safeGetBooleanStateAttribute(state, 
                BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, false));

    }
    
    /** Test normal operation of the safeGetBoolean method with a wrongly typed csrf exclusion attribute.*/
    @Test public void testSafeGetBooleanWrongTypeAttributeFalse() {
        
        BaseCSRFTokenPredicate predicate = new MockBaseCSRFTokenPredicateImplementaiton();
        StateDefinition state = new MockViewState("test", "test-view");
        state.getAttributes().put(BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, "true");
        Assert.assertFalse(predicate.safeGetBooleanStateAttribute(state, 
                BaseCSRFTokenPredicate.CSRF_EXCLUDED_ATTRIBUTE_NAME, false));

    }
    
    
    /**
     * Mock concrete implementation of the {@link BaseCSRFTokenPredicate}.
     */
    private class MockBaseCSRFTokenPredicateImplementaiton extends BaseCSRFTokenPredicate{
        
    }

}

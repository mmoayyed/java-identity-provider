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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import junit.framework.TestCase;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttributeValue;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethSAMLAttributeRequestContext;

/**
 * Base class for JUnit test cases.
 */
public class BaseTestCase extends TestCase {

    /** Base path for data files. */
    public static final String DATA_PATH = 
         "/test/data/edu/internet2/middleware/shibboleth/common/attribute/filtering/provider/match/basic";
    

    /**
     * Simple filtering context for use by tests.
     */
    protected ShibbolethFilteringContext filterContext;
    
    /**
     * A simple attribute included in filterContext. 
     */
    protected BaseAttribute<Integer> iAttribute;
    /**
     * A simple attribute included in filterContext. 
     */
    protected BaseAttribute<String> sAttribute;
    
    /**
     * A Scoped attributed included in filter context.
     */
    protected BasicAttribute<ScopedAttributeValue> scope;
    
    /**
     * Request Context included in filter context.
     */
    protected ShibbolethSAMLAttributeRequestContext requestContext; 

    /**
     * The Functor under test. 
     */
    protected MatchFunctor matchFunctor; 
    
    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        //
        // Set up the two simple Attributes and then put them into our
        // filtering context
        //
        
        BasicAttribute<Integer> ia = new BasicAttribute<Integer>("IntegerAttr");
        TreeSet<Integer> iTree = new TreeSet<Integer>();
        iTree.add(new Integer(1));
        iTree.add(new Integer(2));
        iTree.add(new Integer(3));
        ia.setValues(iTree);
        iAttribute = ia;
        
        BasicAttribute<String> sa = new BasicAttribute<String>("StringAttr");
        sAttribute = sa;
        TreeSet<String> sTree = new TreeSet<String>();
        sTree.add("one");
        sa.setValues(sTree);

        scope = new BasicAttribute<ScopedAttributeValue>("Scope");
        TreeSet<ScopedAttributeValue> tree = new TreeSet<ScopedAttributeValue>();
        tree.add(new ScopedAttributeValue("ScopedValue","ScopedScope"));
        scope.setValues(tree);

        
        Map<String,BaseAttribute> map = new HashMap<String, BaseAttribute>(5);
        map.put(sAttribute.getId(), sAttribute);
        map.put(iAttribute.getId(), iAttribute);
        map.put(scope.getId(), scope);
        
        requestContext = new ShibbolethSAMLAttributeRequestContext();
        
        filterContext = new ShibbolethFilteringContext(map, requestContext);   
}
    
     
    /**
     * Test for the expected result with the given function, (both PermitValue and PolicyRequirement).
     * Thuis method is particularly useful for the boolean cases.
     * 
     * @param testName the error message to extrude
     * @param functor what to test
     * @param expectedResult whether we expect the test to succeed for fail
     */
    protected void testBoth(String testName, MatchFunctor functor, boolean expectedResult) {
        try {
            if (expectedResult) {
                assertTrue(testName + " (permitValue)", 
                           functor.evaluatePermitValue(filterContext, 
                           iAttribute.getId(), null)); 
                assertTrue(testName + " (policyRequirement)", 
                           functor.evaluatePolicyRequirement(filterContext)); 
            } else {
                assertFalse(testName + " (permitValue)", 
                            functor.evaluatePermitValue(filterContext, 
                            iAttribute.getId(), null)); 
                assertFalse(testName + " (policyRequirement)", functor.evaluatePolicyRequirement(filterContext));
            }
        } catch (FilterProcessingException e) {
           fail(testName + " threw " + e.getLocalizedMessage());
        }
    }
    
    /**
     * 
     * Test for the expected result with base clase functor, (both PermitValue and PolicyRequirement).
     * @param testName error string to exit
     * @param expectedResult whether we expect to pass or fail.
     */
    protected void testBoth(String testName, boolean expectedResult) {
        testBoth(testName, matchFunctor, expectedResult);
    }
    
    /**
     * placeholder to allow us to test an entire folder. 
     */
    public void testBase() {
    }
}
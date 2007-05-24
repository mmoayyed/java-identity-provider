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

import org.bouncycastle.jce.provider.JDKDSASigner.stdDSA;
import org.opensaml.DefaultBootstrap;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;

/**
 * Base class for JUnit test cases.
 */
public class BaseTestCase extends TestCase {

    /** Base path for data files. */
    public static final String DATA_PATH = "/data/edu/internet2/middleware/shibboleth/common/attribute/filtering/provider/match/basic";
    

    /**
     * Simple filtering context for use by tests.
     */
    protected ShibbolethFilteringContext filterContext;
    
    /**
     * A simple attribute included in filterContext. 
     */
    protected Attribute<Integer> iAttribute;
    /**
     * A simple attribute included in filterContext. 
     */
    protected Attribute<String> sAttribute;

    /**
     * The Functor under test. 
     */
    MatchFunctor matchFunctor; 
    
  
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
        
        Map<String,Attribute> map = new HashMap<String, Attribute>(5);
        map.put(sAttribute.getId(), sAttribute);
        map.put(iAttribute.getId(), iAttribute);
        
        filterContext = new ShibbolethFilteringContext(map, null);   
    }
    
    /**
     * Test for the expected result with the given function, (both PermitValue and PolicyRequirement)
     * 
     * @param testName the error message to extrude
     * @param functor what to test
     * @param expectedResult whether we expect the test to succeed for fail
     */
    protected void performTest(String testName, MatchFunctor functor, boolean expectedResult) {
        try {
            if (expectedResult) {
                assertTrue(testName + " (permitValue)", functor.evaluatePermitValue(filterContext, iAttribute.getId(), null)); 
                assertTrue(testName + " (policyRequirement)", functor.evaluatePolicyRequirement(filterContext)); 
            } else {
                assertFalse(testName + " (permitValue)", functor.evaluatePermitValue(filterContext, iAttribute.getId(), null)); 
                assertFalse(testName + " (policyRequirement)", functor.evaluatePolicyRequirement(filterContext));
            }
        } catch (FilterProcessingException e) {
           fail(testName + " threw " + e.getLocalizedMessage());
        }
    }
    
    /**
     * 
     * Test for the expected result with base clase functor, (both PermitValue and PolicyRequirement)
     */
    protected void performTest(String testName, boolean expectedResult) {
        performTest(testName, matchFunctor, expectedResult);
    }
    
    public void testBase()
    {
        //
        // placeholder to allow us to test an entire folder
        //
    }
}
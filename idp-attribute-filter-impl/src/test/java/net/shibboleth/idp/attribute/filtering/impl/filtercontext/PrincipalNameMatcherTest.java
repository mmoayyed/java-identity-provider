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

package net.shibboleth.idp.attribute.filtering.impl.filtercontext;

import net.shibboleth.idp.attribute.filtering.impl.matcher.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link PrincipalNameMatcher}.
 */
public class PrincipalNameMatcherTest {
    
    @Test public void testNull() throws ComponentInitializationException {

        PrincipalNameMatcher matcher = new PrincipalNameMatcher();
        
        try {
            matcher.apply(null);
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    
        matcher.setMatchString("principal");
        matcher.setCaseSensitive(true);
        matcher.setId("Test");
        matcher.initialize();
        // TODO
        // Assert.assertFalse(matcher.apply(null));
        
        try {
            matcher.apply(DataSources.unPopulatedFilterContext());
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK
        }
        
        Assert.assertFalse(matcher.apply(DataSources.populatedFilterContext(null, null, null)));
    }
    
    @Test public void testCaseSensitive() throws ComponentInitializationException {

        PrincipalNameMatcher matcher = new PrincipalNameMatcher();
        matcher.setId("Test");
        matcher.setMatchString("principal");
        matcher.setCaseSensitive(true);
        matcher.initialize();
        
        Assert.assertFalse(matcher.apply(DataSources.populatedFilterContext("wibble", null, null)));
        Assert.assertFalse(matcher.apply(DataSources.populatedFilterContext("PRINCIPAL", null, null)));
        Assert.assertTrue(matcher.apply(DataSources.populatedFilterContext("principal", null, null)));        
    }

    
    @Test public void testCaseinSensitive() throws ComponentInitializationException {

        PrincipalNameMatcher matcher = new PrincipalNameMatcher();
        matcher.setMatchString("principal");
        matcher.setCaseSensitive(false);
        matcher.setId("test");
        matcher.initialize();
        
        Assert.assertFalse(matcher.apply(DataSources.populatedFilterContext("wibble", null, null)));
        Assert.assertTrue(matcher.apply(DataSources.populatedFilterContext("PRINCIPAL", null, null)));
        Assert.assertTrue(matcher.apply(DataSources.populatedFilterContext("principal", null, null)));        
    }

}

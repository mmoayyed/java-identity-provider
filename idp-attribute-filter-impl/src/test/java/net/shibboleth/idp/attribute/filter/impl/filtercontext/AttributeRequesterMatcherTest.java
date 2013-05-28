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

package net.shibboleth.idp.attribute.filter.impl.filtercontext;

import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.impl.filtercontext.AttributeIssuerMatcher;
import net.shibboleth.idp.attribute.filter.impl.filtercontext.AttributeRequesterMatcher;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AttributeIssuerMatcher}.
 */
public class AttributeRequesterMatcherTest {
    
    @Test public void testNull() throws ComponentInitializationException {

        AttributeRequesterMatcher matcher = new AttributeRequesterMatcher();
        
        try {
            matcher.doCompare(null);
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    
        matcher.setMatchString("requester");
        matcher.setCaseSensitive(true);
        matcher.setId("Test");
        matcher.initialize();
        // TODO
        // Assert.assertFalse(matcher.doCompare(null));
        
        try {
            matcher.doCompare(DataSources.unPopulatedFilterContext());
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK
        }
        
        Assert.assertFalse(matcher.doCompare(DataSources.populatedFilterContext(null, null, null)));
    }
    
    @Test public void testCaseSensitive() throws ComponentInitializationException {

        AttributeRequesterMatcher matcher = new AttributeRequesterMatcher();
        matcher.setId("Test");
        matcher.setMatchString("requester");
        matcher.setCaseSensitive(true);
        matcher.initialize();
        
        Assert.assertFalse(matcher.doCompare(DataSources.populatedFilterContext(null, null, null)));
        Assert.assertFalse(matcher.doCompare(DataSources.populatedFilterContext(null, null, "wibble")));
        Assert.assertFalse(matcher.doCompare(DataSources.populatedFilterContext(null, null, "REQUESTER")));
        Assert.assertTrue(matcher.doCompare(DataSources.populatedFilterContext(null, null, "requester")));        
    }

    
    @Test public void testCaseInsensitive() throws ComponentInitializationException, AttributeFilterException {

        AttributeRequesterMatcher matcher = new AttributeRequesterMatcher();
        matcher.setMatchString("requester");
        matcher.setCaseSensitive(false);
        matcher.setId("test");
        matcher.initialize();
        
        Assert.assertFalse(matcher.matches(DataSources.populatedFilterContext(null, null, "wibble")));
        Assert.assertTrue(matcher.doCompare(DataSources.populatedFilterContext(null, null, "REQUESTER")));
        Assert.assertTrue(matcher.doCompare(DataSources.populatedFilterContext(null, null, "requester")));        
    }
}
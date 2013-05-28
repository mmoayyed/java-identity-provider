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
import net.shibboleth.idp.attribute.filter.impl.filtercontext.AttributeRequesterRegexpMatcher;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AttributeRequesterRegexpMatcher}.
 */
public class AttributeRequesterRegexpMatcherTest {
    
    @Test public void testAll() throws ComponentInitializationException, AttributeFilterException {

        AttributeRequesterRegexpMatcher matcher = new AttributeRequesterRegexpMatcher();
        
        try {
            matcher.doCompare(null);
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    
        matcher.setRegularExpression("^requ.*");
        matcher.setId("Test");
        matcher.initialize();
        
        try {
            matcher.doCompare(DataSources.unPopulatedFilterContext());
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK
        }
        // TODO
        // Assert.assertFalse(matcher.apply(null));
        
        Assert.assertFalse(matcher.evaluatePolicyRule(DataSources.populatedFilterContext(null, null, null)));
        Assert.assertFalse(matcher.evaluatePolicyRule(DataSources.populatedFilterContext(null, null, "wibble")));
        Assert.assertFalse(matcher.evaluatePolicyRule(DataSources.populatedFilterContext(null, null, "REQUESTER")));
        Assert.assertTrue(matcher.evaluatePolicyRule(DataSources.populatedFilterContext(null, null, "requester")));        
    }


}

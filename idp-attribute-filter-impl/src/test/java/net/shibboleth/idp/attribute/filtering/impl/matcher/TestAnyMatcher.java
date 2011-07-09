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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import java.util.Collection;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;

import org.opensaml.util.collections.CollectionSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Test for the ANY match functor. */
public class TestAnyMatcher {
    
    /** test the ANY matcher. */
    @Test
    public void testAnyMatcher(){
        Attribute<String> attribute = new Attribute<String>("attribute");
        Collection<String> values = CollectionSupport.toList("val1", "val2", "val3"); 
        attribute.setValues(values);
        
        AnyMatcher filter = new AnyMatcher();
        
        Collection result = null;
        try {
            result = filter.getMatchingValues(attribute, null);
        } catch (AttributeFilteringException e) {
            Assert.assertTrue(false, "unreachable code");
        }
        
        Assert.assertEquals(values, result);
    }

}

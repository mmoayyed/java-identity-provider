/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AttributeTest {

    @Test
    public void testAttributeCreation(){
        Attribute<String> attrib = new Attribute<String>("foo");
        
        Assert.assertEquals(attrib.getId(), "foo");
        
        Assert.assertNotNull(attrib.getDisplayDescriptions());
        Assert.assertTrue(attrib.getDisplayDescriptions().isEmpty());
        
        Assert.assertNotNull(attrib.getDisplayNames());
        Assert.assertTrue(attrib.getDisplayNames().isEmpty());
        
        Assert.assertNotNull(attrib.getEncoders());
        Assert.assertTrue(attrib.getEncoders().isEmpty());
        
        Assert.assertNotNull(attrib.getValues());
        Assert.assertTrue(attrib.getValues().isEmpty());
        
        Assert.assertNotNull(attrib.hashCode());
        
        Assert.assertTrue(attrib.equals(new Attribute<String>("foo")));
    }
    
    @Test
    public void testNullEmptyId(){
        try{
            new Attribute(null);
            throw new AssertionError("able to create attribute with null ID");
        }catch(IllegalArgumentException e){
            // expected this
        }
        
        try{
            new Attribute("");
            throw new AssertionError("able to create attribute with empty ID");
        }catch(IllegalArgumentException e){
            // expected this
        }
        
        try{
            new Attribute(" ");
            throw new AssertionError("able to create attribute with empty ID");
        }catch(IllegalArgumentException e){
            // expected this
        }
    }
}
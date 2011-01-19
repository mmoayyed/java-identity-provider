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

/** Unit test for {@link ScopedAttributeValue}. */
public class ScopedAttributeValueTest {

    /** Test proper instantiation of the object. */
    @Test
    public void testInstantiation(){
        ScopedAttributeValue value = new ScopedAttributeValue(" foo ", " bar ");
        
        Assert.assertEquals(value.getValue(), "foo");
        Assert.assertEquals(value.toString(), "foo");
        
        Assert.assertEquals(value.getScope(), "bar");
        
        try{
            new ScopedAttributeValue(null, "bar");
            throw new AssertionError("able to set null attribute value");
        }catch(IllegalArgumentException e){
            // expected this
        }
        
        try{
            new ScopedAttributeValue("", "bar");
            throw new AssertionError("able to set empty attribute value");
        }catch(IllegalArgumentException e){
            // expected this
        }
        
        try{
            new ScopedAttributeValue("foo", null);
            throw new AssertionError("able to set null attribute scope");
        }catch(IllegalArgumentException e){
            // expected this
        }
        
        try{
            new ScopedAttributeValue("foo", "");
            throw new AssertionError("able to set empty attribute scope");
        }catch(IllegalArgumentException e){
            // expected this
        }
    }
    
    /** Test equality of two objects. */
    @Test
    public void testEquality(){
        ScopedAttributeValue value1 = new ScopedAttributeValue(" foo ", " bar ");
        ScopedAttributeValue value2 = new ScopedAttributeValue("foo", "bar");
        ScopedAttributeValue value3 = new ScopedAttributeValue(" foo ", "baz ");
        
        Assert.assertTrue(value1.equals(value2));
        Assert.assertTrue(value2.equals(value1));
        Assert.assertEquals(value1.hashCode(), value2.hashCode());
        
        Assert.assertFalse(value1.equals(value3));
        Assert.assertFalse(value3.equals(value1));
        
        Assert.assertFalse(value2.equals(value3));
        Assert.assertFalse(value3.equals(value2));
    }
}
/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

package net.shibboleth.idp.attribute.filtering;

import java.util.ArrayList;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazyList;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AttributeFilterContext}. */
public class AttributeFilterContextTest {

    /** Test that post-construction state is what is expected. */
    @Test
    public void testPostConstructionState() {
        AttributeFilterContext context = new AttributeFilterContext(null);
        Assert.assertNotNull(context.getFilteredAttributes());
        Assert.assertTrue(context.getFilteredAttributes().isEmpty());
        Assert.assertNull(context.getOwner());
        Assert.assertNotNull(context.getPrefilteredAttributes());
        Assert.assertTrue(context.getPrefilteredAttributes().isEmpty());
        Assert.assertNotNull(context.getPermittedAttributeValues());
        Assert.assertTrue(context.getPermittedAttributeValues().isEmpty());
        Assert.assertNotNull(context.getDeniedAttributeValues());
        Assert.assertTrue(context.getDeniedAttributeValues().isEmpty());
    }

    /** Test methods related to prefiltered attributes. */
    @Test
    public void testPrefilteredAttributes() {
        AttributeFilterContext context = new AttributeFilterContext(null);

        Attribute<?> attribute1 = new Attribute<String>("attribute1");
        context.addPrefilteredAttribute(attribute1);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);

        Attribute<?> attribute2 = new Attribute<String>("attribute2");
        Attribute<?> attribute3 = new Attribute<String>("attribute3");
        LazyList<Attribute<?>> attributes = CollectionSupport.toList(attribute2, attribute3);
        context.setPrefilteredAttributes(attributes);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertFalse(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.addPrefilteredAttribute(attribute1);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 3);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.removePrefilteredAttribute("attribute2");
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertFalse(context.getPrefilteredAttributes().containsKey("attribute2"));
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.addPrefilteredAttribute(null);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.removePrefilteredAttribute(null);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.setPrefilteredAttributes(null);
        Assert.assertNotNull(context.getPrefilteredAttributes());
        Assert.assertTrue(context.getPrefilteredAttributes().isEmpty());

        try {
            context.getPrefilteredAttributes().put(attribute2.getId(), attribute2);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Test methods related to filtered attributes. */
    @Test
    public void testFilteredAttributes() {
        AttributeFilterContext context = new AttributeFilterContext(null);

        Attribute<?> attribute1 = new Attribute<String>("attribute1");
        context.addFilteredAttribute(attribute1);
        Assert.assertEquals(context.getFilteredAttributes().size(), 1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);

        Attribute<?> attribute2 = new Attribute<String>("attribute2");
        Attribute<?> attribute3 = new Attribute<String>("attribute3");
        LazyList<Attribute<?>> attributes = CollectionSupport.toList(attribute2, attribute3);
        context.setFilteredAttributes(attributes);
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertFalse(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.addFilteredAttribute(attribute1);
        Assert.assertEquals(context.getFilteredAttributes().size(), 3);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.removeFilteredAttribute("attribute2");
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertFalse(context.getFilteredAttributes().containsKey("attribute2"));
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.addFilteredAttribute(null);
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.removeFilteredAttribute(null);
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.setFilteredAttributes(null);
        Assert.assertNotNull(context.getFilteredAttributes());
        Assert.assertTrue(context.getFilteredAttributes().isEmpty());

        try {
            context.getFilteredAttributes().put(attribute2.getId(), attribute2);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }
    
    /** Testing getting and adding permitted attribute values. */ 
    @Test
    public void testPermittedAttributeValues() {
        AttributeFilterContext context = new AttributeFilterContext(null);
        
        Attribute attribute1 = new Attribute<String>("one");
        attribute1.addValue("a");
        attribute1.addValue("b");
        context.addPrefilteredAttribute(attribute1);
        
        context.addPermittedAttributeValues("one", CollectionSupport.toList("a"));
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);

        context.addPermittedAttributeValues("one", null);
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);
        
        context.addPermittedAttributeValues("one", new ArrayList<String>());
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);
        
        context.addPermittedAttributeValues("one", CollectionSupport.toList("b"));
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 2);
        
        try{
            context.addPermittedAttributeValues(null, CollectionSupport.toList("a"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
        
        try{
            context.addPermittedAttributeValues("", CollectionSupport.toList("a"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
        
        try{
            context.addPermittedAttributeValues("two", CollectionSupport.toList("a"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
        
        try{
            context.addPermittedAttributeValues("one", CollectionSupport.toList("c"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
    }
    
    /** Testing getting and adding denied attribute values. */ 
    @Test
    public void testDeniedAttributeValues() {
        AttributeFilterContext context = new AttributeFilterContext(null);
        
        Attribute attribute1 = new Attribute<String>("one");
        attribute1.addValue("a");
        attribute1.addValue("b");
        context.addPrefilteredAttribute(attribute1);
        
        context.addDeniedAttributeValues("one", CollectionSupport.toList("a"));
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);
        
        context.addDeniedAttributeValues("one", null);
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);
        
        context.addDeniedAttributeValues("one", new ArrayList<String>());
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);
        
        context.addDeniedAttributeValues("one", CollectionSupport.toList("b"));
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 2);
        
        try{
            context.addDeniedAttributeValues(null, CollectionSupport.toList("a"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
        
        try{
            context.addDeniedAttributeValues("", CollectionSupport.toList("a"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
        
        try{
            context.addDeniedAttributeValues("two", CollectionSupport.toList("a"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
        
        try{
            context.addDeniedAttributeValues("one", CollectionSupport.toList("c"));
            Assert.fail();
        }catch(IllegalArgumentException e){
            //expected this
        }
    }
}
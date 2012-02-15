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

package net.shibboleth.idp.attribute.filtering;

import java.util.ArrayList;
import java.util.List;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** Unit test for {@link AttributeFilterContext}. */
public class AttributeFilterContextTest {
    
    private final StringAttributeValue aStringAttributeValue = new StringAttributeValue("a");
    private final StringAttributeValue bStringAttributeValue = new StringAttributeValue("b");
    private final StringAttributeValue cStringAttributeValue = new StringAttributeValue("c");
    
    /** Test that post-construction state is what is expected. */
    @Test public void testPostConstructionState() {
        AttributeFilterContext context = new AttributeFilterContext();
        Assert.assertNotNull(context.getFilteredAttributes());
        Assert.assertTrue(context.getFilteredAttributes().isEmpty());
        Assert.assertNull(context.getParent());
        Assert.assertNotNull(context.getPrefilteredAttributes());
        Assert.assertTrue(context.getPrefilteredAttributes().isEmpty());
        Assert.assertNotNull(context.getPermittedAttributeValues());
        Assert.assertTrue(context.getPermittedAttributeValues().isEmpty());
        Assert.assertNotNull(context.getDeniedAttributeValues());
        Assert.assertTrue(context.getDeniedAttributeValues().isEmpty());
    }

    /** Test methods related to prefiltered attributes. */
    @Test public void testPrefilteredAttributes() {
        AttributeFilterContext context = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        context.getPrefilteredAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);

        Attribute attribute2 = new Attribute("attribute2");
        Attribute attribute3 = new Attribute("attribute3");
        List<Attribute> attributes = Lists.newArrayList(attribute2, attribute3);
        context.setPrefilteredAttributes(attributes);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertFalse(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.getPrefilteredAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 3);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.getPrefilteredAttributes().remove("attribute2");
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertFalse(context.getPrefilteredAttributes().containsKey("attribute2"));
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.getPrefilteredAttributes().put(null, new Attribute("foo"));
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.getPrefilteredAttributes().put("foo", null);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.getPrefilteredAttributes().remove(null);
        Assert.assertEquals(context.getPrefilteredAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredAttributes().get("attribute3"), attribute3);

        context.setPrefilteredAttributes(null);
        Assert.assertNotNull(context.getPrefilteredAttributes());
        Assert.assertTrue(context.getPrefilteredAttributes().isEmpty());
    }

    /** Test methods related to filtered attributes. */
    @Test public void testFilteredAttributes() {
        AttributeFilterContext context = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        context.getFilteredAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getFilteredAttributes().size(), 1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);

        Attribute attribute2 = new Attribute("attribute2");
        Attribute attribute3 = new Attribute("attribute3");
        List<Attribute> attributes = Lists.newArrayList(attribute2, attribute3);
        context.setFilteredAttributes(attributes);
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertFalse(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.getFilteredAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getFilteredAttributes().size(), 3);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.getFilteredAttributes().remove("attribute2");
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertFalse(context.getFilteredAttributes().containsKey("attribute2"));
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.getFilteredAttributes().put(null, new Attribute("foo"));
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.getFilteredAttributes().put("foo", null);
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.getFilteredAttributes().remove(null);
        Assert.assertEquals(context.getFilteredAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredAttributes().get("attribute3"), attribute3);

        context.setFilteredAttributes(null);
        Assert.assertNotNull(context.getFilteredAttributes());
        Assert.assertTrue(context.getFilteredAttributes().isEmpty());
    }

    /** Testing getting and adding permitted attribute values. */
    @Test public void testPermittedAttributeValues() {
        AttributeFilterContext context = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("one");
        attribute1.getValues().add(aStringAttributeValue);
        attribute1.getValues().add(bStringAttributeValue);
        context.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        context.addPermittedAttributeValues("one", Lists.newArrayList(aStringAttributeValue));
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);

        context.addPermittedAttributeValues("one", null);
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);

        context.addPermittedAttributeValues("one", new ArrayList<String>());
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);

        context.addPermittedAttributeValues("one", Lists.newArrayList(bStringAttributeValue));
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 2);

        try {
            context.addPermittedAttributeValues(null, Lists.newArrayList(aStringAttributeValue));
            Assert.fail();
        } catch (AssertionError e) {
            // expected this
        }

        try {
            context.addPermittedAttributeValues("", Lists.newArrayList(aStringAttributeValue));
            Assert.fail();
        } catch (AssertionError e) {
            // expected this
        }

        try {
            context.addPermittedAttributeValues("two", Lists.newArrayList(aStringAttributeValue));
            Assert.fail();
        } catch (AssertionError e) {
            // expected this
        }

        try {
            context.addPermittedAttributeValues("one", Lists.newArrayList(cStringAttributeValue));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected this
        }
    }

    /** Testing getting and adding denied attribute values. */
    @Test public void testDeniedAttributeValues() {
        AttributeFilterContext context = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("one");
        attribute1.getValues().add(aStringAttributeValue);
        attribute1.getValues().add(bStringAttributeValue);
        context.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        context.addDeniedAttributeValues("one", Lists.newArrayList(aStringAttributeValue));
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);

        context.addDeniedAttributeValues("one", null);
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);

        context.addDeniedAttributeValues("one", new ArrayList<String>());
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);

        context.addDeniedAttributeValues("one", Lists.newArrayList(bStringAttributeValue));
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 2);

        try {
            context.addDeniedAttributeValues(null, Lists.newArrayList(bStringAttributeValue));
            Assert.fail();
        } catch (AssertionError e) {
            // expected this
        }

        try {
            context.addDeniedAttributeValues("", Lists.newArrayList(bStringAttributeValue));
            Assert.fail();
        } catch (AssertionError e) {
            // expected this
        }

        try {
            context.addDeniedAttributeValues("two", Lists.newArrayList(bStringAttributeValue));
            Assert.fail();
        } catch (AssertionError e) {
            // expected this
        }

        try {
            context.addDeniedAttributeValues("one", Lists.newArrayList(cStringAttributeValue));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected this
        }
    }
}
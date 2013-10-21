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

package net.shibboleth.idp.attribute.filter;

import java.util.ArrayList;
import java.util.List;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/** Unit test for {@link AttributeFilterContext}. */
public class AttributeFilterContextTest {

    private final StringAttributeValue aStringAttributeValue = new StringAttributeValue("a");

    private final StringAttributeValue bStringAttributeValue = new StringAttributeValue("b");

    private final StringAttributeValue cStringAttributeValue = new StringAttributeValue("c");

    /** Test that post-construction state is what is expected. */
    @Test public void testPostConstructionState() {
        AttributeFilterContext context = new AttributeFilterContext();
        Assert.assertNotNull(context.getFilteredIdPAttributes());
        Assert.assertTrue(context.getFilteredIdPAttributes().isEmpty());
        Assert.assertNull(context.getParent());
        Assert.assertNotNull(context.getPrefilteredIdPAttributes());
        Assert.assertTrue(context.getPrefilteredIdPAttributes().isEmpty());
        Assert.assertNotNull(context.getPermittedAttributeValues());
        Assert.assertTrue(context.getPermittedAttributeValues().isEmpty());
        Assert.assertNotNull(context.getDeniedAttributeValues());
        Assert.assertTrue(context.getDeniedAttributeValues().isEmpty());
    }

    /** Test methods related to prefiltered attributes. */
    @Test public void testPrefilteredAttributes() {
        AttributeFilterContext context = new AttributeFilterContext();

        IdPAttribute attribute1 = new IdPAttribute("attribute1");
        context.getPrefilteredIdPAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getPrefilteredIdPAttributes().size(), 1);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute1"), attribute1);

        IdPAttribute attribute2 = new IdPAttribute("attribute2");
        IdPAttribute attribute3 = new IdPAttribute("attribute3");
        List<IdPAttribute> attributes = Lists.newArrayList(attribute2, attribute3);
        context.setPrefilteredIdPAttributes(attributes);
        Assert.assertEquals(context.getPrefilteredIdPAttributes().size(), 2);
        Assert.assertFalse(context.getPrefilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute3"), attribute3);

        context.getPrefilteredIdPAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getPrefilteredIdPAttributes().size(), 3);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute3"), attribute3);

        context.getPrefilteredIdPAttributes().remove("attribute2");
        Assert.assertEquals(context.getPrefilteredIdPAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute1"), attribute1);
        Assert.assertFalse(context.getPrefilteredIdPAttributes().containsKey("attribute2"));
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute3"), attribute3);

        try {
            context.getPrefilteredIdPAttributes().put(null, new IdPAttribute("foo"));
            Assert.fail("null attribute id not allowed");
        } catch (NullPointerException e) {
        }

        try {
            context.getPrefilteredIdPAttributes().put("foo", null);
            Assert.fail("null attribute not allowed");
        } catch (NullPointerException e) {
        }

        context.getPrefilteredIdPAttributes().remove(null);
        Assert.assertEquals(context.getPrefilteredIdPAttributes().size(), 2);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getPrefilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getPrefilteredIdPAttributes().get("attribute3"), attribute3);

        context.setPrefilteredIdPAttributes(null);
        Assert.assertNotNull(context.getPrefilteredIdPAttributes());
        Assert.assertTrue(context.getPrefilteredIdPAttributes().isEmpty());
    }

    /** Test methods related to filtered attributes. */
    @Test public void testFilteredAttributes() {
        AttributeFilterContext context = new AttributeFilterContext();

        IdPAttribute attribute1 = new IdPAttribute("attribute1");
        context.getFilteredIdPAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getFilteredIdPAttributes().size(), 1);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute1"), attribute1);

        IdPAttribute attribute2 = new IdPAttribute("attribute2");
        IdPAttribute attribute3 = new IdPAttribute("attribute3");
        List<IdPAttribute> attributes = Lists.newArrayList(attribute2, attribute3);
        context.setFilteredIdPAttributes(attributes);
        Assert.assertEquals(context.getFilteredIdPAttributes().size(), 2);
        Assert.assertFalse(context.getFilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute3"), attribute3);

        context.getFilteredIdPAttributes().put(attribute1.getId(), attribute1);
        Assert.assertEquals(context.getFilteredIdPAttributes().size(), 3);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute2"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute2"), attribute2);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute3"), attribute3);

        context.getFilteredIdPAttributes().remove("attribute2");
        Assert.assertEquals(context.getFilteredIdPAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute1"), attribute1);
        Assert.assertFalse(context.getFilteredIdPAttributes().containsKey("attribute2"));
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute3"), attribute3);

        try {
            context.getFilteredIdPAttributes().put(null, new IdPAttribute("foo"));
            Assert.fail("null attribute id not allowed");
        } catch (NullPointerException e) {
        }

        try {
            context.getFilteredIdPAttributes().put("foo", null);
            Assert.fail("null attribute not allowed");
        } catch (NullPointerException e) {
        }

        context.getFilteredIdPAttributes().remove(null);
        Assert.assertEquals(context.getFilteredIdPAttributes().size(), 2);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute1"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute1"), attribute1);
        Assert.assertTrue(context.getFilteredIdPAttributes().containsKey("attribute3"));
        Assert.assertEquals(context.getFilteredIdPAttributes().get("attribute3"), attribute3);

        context.setFilteredIdPAttributes(null);
        Assert.assertNotNull(context.getFilteredIdPAttributes());
        Assert.assertTrue(context.getFilteredIdPAttributes().isEmpty());
    }

    /** Testing getting and adding permitted attribute values. */
    @Test public void testPermittedAttributeValues() {
        AttributeFilterContext context = new AttributeFilterContext();

        IdPAttribute attribute1 = new IdPAttribute("one");
        attribute1.getValues().add(aStringAttributeValue);
        attribute1.getValues().add(bStringAttributeValue);
        context.getPrefilteredIdPAttributes().put(attribute1.getId(), attribute1);

        context.addPermittedAttributeValues("one", Lists.newArrayList(aStringAttributeValue));
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);

        context.addPermittedAttributeValues("one", null);
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);

        context.addPermittedAttributeValues("one", new ArrayList<AttributeValue>());
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 1);

        context.addPermittedAttributeValues("one", Lists.newArrayList(bStringAttributeValue));
        Assert.assertEquals(context.getPermittedAttributeValues().get("one").size(), 2);

        try {
            context.addPermittedAttributeValues(null, Lists.newArrayList(aStringAttributeValue));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            context.addPermittedAttributeValues("", Lists.newArrayList(aStringAttributeValue));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            context.addPermittedAttributeValues("two", Lists.newArrayList(aStringAttributeValue));
            Assert.fail();
        } catch (ConstraintViolationException e) {
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

        IdPAttribute attribute1 = new IdPAttribute("one");
        attribute1.getValues().add(aStringAttributeValue);
        attribute1.getValues().add(bStringAttributeValue);
        context.getPrefilteredIdPAttributes().put(attribute1.getId(), attribute1);

        context.addDeniedAttributeValues("one", Lists.newArrayList(aStringAttributeValue));
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);

        context.addDeniedAttributeValues("one", null);
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);

        context.addDeniedAttributeValues("one", new ArrayList<AttributeValue>());
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 1);

        context.addDeniedAttributeValues("one", Lists.newArrayList(bStringAttributeValue));
        Assert.assertEquals(context.getDeniedAttributeValues().get("one").size(), 2);

        try {
            context.addDeniedAttributeValues(null, Lists.newArrayList(bStringAttributeValue));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            context.addDeniedAttributeValues("", Lists.newArrayList(bStringAttributeValue));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            context.addDeniedAttributeValues("two", Lists.newArrayList(bStringAttributeValue));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            context.addDeniedAttributeValues("one", Lists.newArrayList(cStringAttributeValue));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected this
        }
    }
    
    @Test public void testRequestedAttributes() {
        AttributeFilterContext context = new AttributeFilterContext();
        Multimap map = ArrayListMultimap.create(); 
        context.setRequestedAttributes(map);
        Assert.assertEquals(context.getRequestedIdPAttributes(), map);
    }
}
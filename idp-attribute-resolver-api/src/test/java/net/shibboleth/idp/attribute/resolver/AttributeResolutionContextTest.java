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

package net.shibboleth.idp.attribute.resolver;

import java.util.HashSet;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AttributeResolutionContext}. */
public class AttributeResolutionContextTest {

    /** Test instantiation and post-instantiation state. */
    @Test public void instantiation() {

        AttributeResolutionContext context = new AttributeResolutionContext();
        Assert.assertNull(context.getParent());
        Assert.assertNotNull(context.getRequestedIdPAttributeNames());
        Assert.assertTrue(context.getRequestedIdPAttributeNames().isEmpty());
    }
    
    /** Test {@link AttributeResolutionContext#setRequestedIdPAttributeNames(java.util.Set)}. */
    @Test public void setRequesedAttributes() {
        AttributeResolutionContext context = new AttributeResolutionContext();

        HashSet<String> attributes = new HashSet<>();
        context.setRequestedIdPAttributeNames(attributes);
        Assert.assertNotNull(context.getRequestedIdPAttributeNames());
        Assert.assertTrue(context.getRequestedIdPAttributeNames().isEmpty());

        attributes.add(null);
        context.setRequestedIdPAttributeNames(attributes);
        Assert.assertNotNull(context.getRequestedIdPAttributeNames());
        Assert.assertTrue(context.getRequestedIdPAttributeNames().isEmpty());

        attributes.add("foo");
        attributes.add(null);
        attributes.add("bar");
        context.setRequestedIdPAttributeNames(attributes);
        Assert.assertNotNull(context.getRequestedIdPAttributeNames());
        Assert.assertEquals(context.getRequestedIdPAttributeNames().size(), 2);

        attributes.clear();
        attributes.add("baz");
        context.setRequestedIdPAttributeNames(attributes);
        Assert.assertNotNull(context.getRequestedIdPAttributeNames());
        Assert.assertEquals(context.getRequestedIdPAttributeNames().size(), 1);
    }

    /** Test {@link AttributeResolutionContext#setRequestedIdPAttributeNames(java.util.Set)}. */
    @Test public void setResolvedAttributes() {
        AttributeResolutionContext context = new AttributeResolutionContext();

        context.setResolvedIdPAttributes(null);
        Assert.assertNotNull(context.getResolvedIdPAttributes());
        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());

        HashSet<IdPAttribute> attributes = new HashSet<IdPAttribute>();
        context.setResolvedIdPAttributes(attributes);
        Assert.assertNotNull(context.getResolvedIdPAttributes());
        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());

        attributes.add(null);
        context.setResolvedIdPAttributes(attributes);
        Assert.assertNotNull(context.getResolvedIdPAttributes());
        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());

        attributes.add(new IdPAttribute("foo"));
        attributes.add(null);
        attributes.add(new IdPAttribute("bar"));
        context.setResolvedIdPAttributes(attributes);
        Assert.assertNotNull(context.getResolvedIdPAttributes());
        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 2);

        attributes.clear();
        attributes.add(new IdPAttribute("baz"));
        context.setResolvedIdPAttributes(attributes);
        Assert.assertNotNull(context.getResolvedIdPAttributes());
        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 1);
    }

}
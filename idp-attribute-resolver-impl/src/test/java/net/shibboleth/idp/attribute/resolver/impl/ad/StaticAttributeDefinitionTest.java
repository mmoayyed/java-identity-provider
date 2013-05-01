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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.ad.StaticAttributeDefinition;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/** Tests for {@link StaticAttributeDefinition}
 *
 */
public class StaticAttributeDefinitionTest {

    @Test public void resolve() throws ComponentInitializationException, ResolutionException {
        StaticAttributeDefinition attrDef = new StaticAttributeDefinition();
        attrDef.setId("Static");
        Assert.assertFalse(attrDef.getValue().isPresent());
        
        attrDef.setValue(null);
        Assert.assertFalse(attrDef.getValue().isPresent());

        try {
            attrDef.initialize();
            Assert.fail("Cannot initialize without an attribute");
        } catch (ComponentInitializationException e) {
            //OK
        }

        Attribute attribute = new Attribute("attribute");
        attribute.setValues(Lists.newArrayList((AttributeValue) new StringAttributeValue("one"), new StringAttributeValue("two")));

        attrDef.setValue(attribute);
        Assert.assertTrue(attrDef.getValue().isPresent());

        try {
            attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("Need to be initialized to resolve");
        } catch (UninitializedComponentException e) {
            // OK
        }
        
        attrDef.initialize();

        Assert.assertTrue(attrDef.getValue().isPresent());

        AttributeResolutionContext context = new AttributeResolutionContext();
        Optional<Attribute> result = attrDef.doAttributeDefinitionResolve(context);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getId(), "attribute");
        
        try {
            attrDef.setValue(new Attribute("other"));
            Assert.fail();
        } catch (UnmodifiableComponentException e) {
            // OK
        }

        attrDef.destroy();
        
        try {
            attrDef.initialize();
            Assert.fail();
        } catch (DestroyedComponentException e) {
            //OK
        }

        try {
            attrDef.setValue(new Attribute("other"));
            Assert.fail();
        } catch (UnmodifiableComponentException e) {
            // OK
        } catch (DestroyedComponentException e) {
            //OK
        }

        try {
            attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail();
        } catch (DestroyedComponentException e) {
            // OK
        }

    }
}

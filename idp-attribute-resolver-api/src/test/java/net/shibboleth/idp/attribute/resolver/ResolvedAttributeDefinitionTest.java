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

import java.util.Collections;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

/**
 * Largely boilerplate test for {@link ResolvedAttributeDefinition}
 * 
 */
public class ResolvedAttributeDefinitionTest {

    @Test public void testInit() {
        Attribute attribute = new Attribute("foo");
        TestAttributeDefinition attrDef = new TestAttributeDefinition();

        try {
            new ResolvedAttributeDefinition(null, Optional.of(attribute));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

        try {
            new ResolvedAttributeDefinition(attrDef, null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

        try {
            new ResolvedAttributeDefinition(new TestAttributeDefinition(), Optional.of(attribute));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // OK
        }

    }

    @Test public void testEqualsHashToString() throws ComponentInitializationException {
        Attribute attribute = new Attribute("foo");
        TestAttributeDefinition attrDef = new TestAttributeDefinition();
        attrDef.setValue(attribute);
        attrDef.setId("Defn");
        attrDef.initialize();
        ResolvedAttributeDefinition resolvedAttributeDefinition =
                new ResolvedAttributeDefinition(attrDef, Optional.of(new Attribute("foo")));

        resolvedAttributeDefinition.toString();

        ResolvedAttributeDefinition otherResolvedAttributeDefinition;
        TestAttributeDefinition otherDef = new TestAttributeDefinition();
        otherDef.setValue(new Attribute("bar"));
        otherDef.setId("OtherDefn");
        otherDef.initialize();
        otherResolvedAttributeDefinition = new ResolvedAttributeDefinition(otherDef, Optional.of(new Attribute("bar")));

        Assert.assertFalse(resolvedAttributeDefinition.equals(null));
        Assert.assertFalse(resolvedAttributeDefinition.equals(this));
        Assert.assertFalse(resolvedAttributeDefinition.equals(otherResolvedAttributeDefinition));
        Assert.assertTrue(resolvedAttributeDefinition.equals(resolvedAttributeDefinition));
        Assert.assertTrue(resolvedAttributeDefinition.equals(attrDef));

        Assert.assertNotSame(resolvedAttributeDefinition.hashCode(), otherResolvedAttributeDefinition.hashCode());
        Assert.assertEquals(resolvedAttributeDefinition.hashCode(), attrDef.hashCode());

    }

    @Test public void testNoops() throws ComponentInitializationException, ComponentValidationException {

        Attribute attribute = new Attribute("foo");
        TestAttributeDefinition attrDef = new TestAttributeDefinition();
        attrDef.setValue(attribute);
        attrDef.setId("Defn");
        ResolverPluginDependency dep = new ResolverPluginDependency("doo", "foo");
        attrDef.setDependencies(Collections.singleton(dep));
        attrDef.setPropagateResolutionExceptions(false);
        attrDef.initialize();

        ResolvedAttributeDefinition resolvedAttributeDefinition =
                new ResolvedAttributeDefinition(attrDef, Optional.of(new Attribute("foo")));
        resolvedAttributeDefinition.getActivationCriteria();

        Assert.assertEquals(resolvedAttributeDefinition.getDependencies(), attrDef.getDependencies());
        Assert.assertTrue(resolvedAttributeDefinition.getActivationCriteria().apply(null));
        Assert.assertFalse(resolvedAttributeDefinition.isPropagateResolutionExceptions());

        //
        // TODO - do we want to do more about seeing that these are indeed noops?
        //
        resolvedAttributeDefinition.setDependencyOnly(true);
        resolvedAttributeDefinition.setDisplayDescriptions(null);
        resolvedAttributeDefinition.setDisplayNames(null);

        resolvedAttributeDefinition.setPropagateResolutionExceptions(true);
        Assert.assertFalse(resolvedAttributeDefinition.isPropagateResolutionExceptions());

        resolvedAttributeDefinition.doValidate();
    }
}

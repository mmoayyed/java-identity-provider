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

package net.shibboleth.idp.attribute.resolver;

import java.util.ArrayList;

import net.shibboleth.idp.attribute.AttributeEncoder;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link BaseAttributeDefinition}. This test does not test any methods inherited from
 * {@link BaseResolverPlugin}, those are covered in {@link BaseResolverPluginTest}.
 */
public class BaseAttributeDefinitionTest {

    /** Tests the state of a newly instantiated object. */
    @Test
    public void testInstantiation() {
        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);
        
        Assert.assertEquals(definition.getId(), "foo");
        Assert.assertFalse(definition.isDependencyOnly());
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());
        Assert.assertNotNull(definition.getDisplayDescriptions());
        Assert.assertTrue(definition.getDisplayDescriptions().isEmpty());
        Assert.assertNotNull(definition.getDisplayNames());
        Assert.assertTrue(definition.getDisplayNames().isEmpty());
    }
    
    /** Tests setting and retrieving the dependency only option. */
    @Test
    public void testDependecyOnly(){
        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);
        Assert.assertFalse(definition.isDependencyOnly());
        
        definition.setDependencyOnly(true);
        Assert.assertTrue(definition.isDependencyOnly());
        
        definition.setDependencyOnly(true);
        Assert.assertTrue(definition.isDependencyOnly());
        
        definition.setDependencyOnly(false);
        Assert.assertFalse(definition.isDependencyOnly());
        
        definition.setDependencyOnly(false);
        Assert.assertFalse(definition.isDependencyOnly());
    }
    
    /** Tests setting and retrieving encoders. */
    @Test
    public void testEncoders(){
        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);
        
        MockAttributeEncoder enc1 = new MockAttributeEncoder(null, null);
        MockAttributeEncoder enc2 = new MockAttributeEncoder(null, null); 
        
        ArrayList<AttributeEncoder> encoders = new ArrayList<AttributeEncoder>();
        
        definition.setAttributeEncoders(null);
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());
        
        definition.setAttributeEncoders(encoders);
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());

        encoders.add(enc1);
        encoders.add(null);
        encoders.add(enc2);
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());
        
        definition.setAttributeEncoders(encoders);
        Assert.assertEquals(definition.getAttributeEncoders().size(), 2);
        Assert.assertTrue(definition.getAttributeEncoders().contains(enc1));
        Assert.assertTrue(definition.getAttributeEncoders().contains(enc2));
        
        encoders.clear();
        encoders.add(enc2);
        definition.setAttributeEncoders(encoders);
        Assert.assertEquals(definition.getAttributeEncoders().size(), 1);
        Assert.assertFalse(definition.getAttributeEncoders().contains(enc1));
        Assert.assertTrue(definition.getAttributeEncoders().contains(enc2));
    }
}
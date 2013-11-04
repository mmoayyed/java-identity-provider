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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.idp.attribute.resolver.impl.ad.SimpleAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.SimpleAttributeDefinitionParser;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link SimpleAttributeDefinitionParser} and by extension
 * {@link BaseAttributeDefinitionParser}.
 */
public class SimpleAttributeParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void simple() {
        AttributeDefinition attrDef = getAttributeDefn("simpleAttributeUnpopulated.xml", SimpleAttributeDefinition.class);
        
        Assert.assertEquals(attrDef.getId(), "simpleUnpopulated");
        Assert.assertFalse(attrDef.isDependencyOnly(), "isDependencyOnly");
        Assert.assertTrue(attrDef.getDisplayDescriptions().isEmpty(),"getDisplayDescriptions().isEmpty()");
        Assert.assertTrue(attrDef.getDisplayNames().isEmpty(),"getDisplayNames().isEmpty()");
        Assert.assertTrue(attrDef.getDependencies().isEmpty(),"getDependencies().isEmpty()");
        Assert.assertTrue(attrDef.getAttributeEncoders().isEmpty(),"getgetAttributeEncoders().isEmpty()");
}
    
    @Test public void populated() throws ComponentInitializationException {
        AttributeDefinition attrDef = getAttributeDefn("simpleAttributePopulated.xml", SimpleAttributeDefinition.class, true);
        
        attrDef.initialize();
        
        Assert.assertEquals(attrDef.getId(), "simplePopulated");
        Assert.assertTrue(attrDef.isDependencyOnly(), "isDependencyOnly");
        
        final Map<Locale, String> descriptions = attrDef.getDisplayDescriptions();
        Assert.assertEquals(descriptions.size(), 3, "getDisplayDescriptions");
        Assert.assertEquals(descriptions.get(new Locale("en")), "DescInEnglish");
        Assert.assertEquals(descriptions.get(new Locale("fr")), "DescEnFrancais");
        Assert.assertEquals(descriptions.get(new Locale("ca")), "DescInCanadian");

        final Map<Locale, String> names = attrDef.getDisplayNames();
        Assert.assertEquals(names.size(), 2, "getDisplayNames");
        Assert.assertEquals(names.get(new Locale("en")), "NameInEnglish");
        Assert.assertEquals(names.get(new Locale("fr")), "NameEnFrancais");

        Set<ResolverPluginDependency> dependencies = attrDef.getDependencies();
        Assert.assertEquals(dependencies.size(), 3, "getDisplayDescriptions");
        Assert.assertTrue(dependencies.contains(TestSources.makeResolverPluginDependency("dep1", "flibble")));
        Assert.assertTrue(dependencies.contains(TestSources.makeResolverPluginDependency("dep2", "flibble")));
        Assert.assertTrue(dependencies.contains(TestSources.makeResolverPluginDependency("dep3", "flibble")));

        //
        // TODO
        //
        Assert.assertTrue(attrDef.getAttributeEncoders().isEmpty(),"getgetAttributeEncoders().isEmpty()");
        
    }

    @Test public void populated2() throws ComponentInitializationException {
        AttributeDefinition attrDef = getAttributeDefn("simpleAttributePopulated2.xml", SimpleAttributeDefinition.class);
        
        attrDef.initialize();
        
        Assert.assertEquals(attrDef.getId(), "simplePopulated2");
        Assert.assertFalse(attrDef.isDependencyOnly(), "isDependencyOnly");
        
        Assert.assertTrue(attrDef.getDisplayDescriptions().isEmpty(),"getDisplayDescriptions().isEmpty()");

        final Map<Locale, String> names = attrDef.getDisplayNames();
        Assert.assertEquals(names.size(), 1, "getDisplayNames");
        Assert.assertEquals(names.get(new Locale("en")), "NameInAmerican");


        Set<ResolverPluginDependency> dependencies = attrDef.getDependencies();
        Assert.assertEquals(dependencies.size(), 1, "getDisplayDescriptions");
        Assert.assertTrue(dependencies.contains(TestSources.makeResolverPluginDependency("dep3", "simplePopulated2")));

        //
        // TODO
        //
        Assert.assertTrue(attrDef.getAttributeEncoders().isEmpty(),"getgetAttributeEncoders().isEmpty()");
    }

    @Test public void bad() throws ComponentInitializationException {
        getAttributeDefn("simpleAttributeBadValues.xml", SimpleAttributeDefinition.class, true);
    }
}

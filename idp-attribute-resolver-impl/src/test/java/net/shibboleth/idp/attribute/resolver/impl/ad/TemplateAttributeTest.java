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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazyList;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.app.VelocityEngine;
import org.testng.Assert;
import org.testng.annotations.Test;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.TemplateAttribute}. */
@ThreadSafe
public class TemplateAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_BASE_NAME = "TEMPLATE";

    /** Simple result. */
    private static final String SIMPLE_VALUE_STRING = "simple";
    private static final StringAttributeValue SIMPLE_VALUE_RESULT = new StringAttributeValue(SIMPLE_VALUE_STRING);

    /** A simple script to set a constant value. */
    private static final String TEST_SIMPLE_TEMPLATE = SIMPLE_VALUE_STRING;

    /** A simple script to set a value based on input values. */
    private static final String TEST_ATTRIBUTES_TEMPLATE_ATTR = "Att " + "${" + TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + "}-"
            + "${" + TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME + "}";


    private static final String TEST_ATTRIBUTES_TEMPLATE_CONNECTOR = "Att " + "${" + TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR + "}-"
            + "${" + TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME + "}";

    
    /** Our singleton engine. */
    private static VelocityEngine engineSingleton;

    /**
     * Create new or return the velocity engine with enough hardwired properties to get us going.
     * 
     * @return a new engine suitable groomed
     */
    private VelocityEngine getEngine() {
        if (null == engineSingleton) {
            engineSingleton = new VelocityEngine();
            try {
                engineSingleton.addProperty("string.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
                engineSingleton.addProperty("classpath.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
                engineSingleton.addProperty("resource.loader", "classpath, string");
                engineSingleton.init();
            } catch (Exception e) {
                Assert.fail("couldn't create engine", e);
            }
        }
        return engineSingleton;
    }

    /**
     * Test resolution of an template script (statically generated data).
     * 
     * @throws AttributeResolutionException id resolution fails
     * @throws ComponentInitializationException only if bad things thingas
     */
    @Test public void testSimple() throws AttributeResolutionException, ComponentInitializationException {

        final String name = TEST_ATTRIBUTE_BASE_NAME + "1";
        final TemplateAttributeDefinition attr = new TemplateAttributeDefinition();

        attr.setId(name);
        attr.setDependencies(Collections.singleton(new ResolverPluginDependency("foo", "bar")));
        attr.setTemplate(Template.fromTemplate(getEngine(), TEST_ATTRIBUTES_TEMPLATE_ATTR));
        attr.initialize();
        Assert.assertFalse(attr.resolve(new AttributeResolutionContext()).isPresent());
    }

    /**
     * Test resolution of an template script (statically generated data). By giving it attributes we create some values.
     * 
     * @throws AttributeResolutionException if resolution fails
     * @throws ComponentInitializationException only if things go wrong
     */
    @Test public void testSimpleWithValues() throws AttributeResolutionException, ComponentInitializationException {

        final String name = TEST_ATTRIBUTE_BASE_NAME + "2";
        final List<String> sources = new LazyList<String>();
        sources.add(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setTemplate(Template.fromTemplate(getEngine(), TEST_SIMPLE_TEMPLATE));

        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));
        templateDef.setDependencies(ds);
        templateDef.initialize();

        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Attribute a = context.getResolvedAttributes().get(name);
        final Collection results = a.getValues();
        Assert.assertEquals(results.size(), 1, "Templated value count");
        Assert.assertTrue(results.contains(SIMPLE_VALUE_RESULT), "Single value context is correct");

    }

    /**
     * Test resolution of an template script with data generated from the attributes.
     * 
     * @throws AttributeResolutionException if it goes wrong.
     * @throws ComponentInitializationException if it goes wrong.
     */
    @Test public void testTemplateWithValues() throws AttributeResolutionException, ComponentInitializationException {

        final String name = TEST_ATTRIBUTE_BASE_NAME + "3";
        final List<String> sources = new LazyList<String>();
        sources.add(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        sources.add(TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME);

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setTemplate(Template.fromTemplate(getEngine(), TEST_ATTRIBUTES_TEMPLATE_CONNECTOR));

        Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        ds.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME));
        templateDef.setDependencies(ds);
        templateDef.initialize();

        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();
        
        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        final Attribute a = context.getResolvedAttributes().get(name);
        final Collection results = a.getValues();
        Assert.assertEquals(results.size(), 2, "Templated value count");
        String s = "Att " + TestSources.COMMON_ATTRIBUTE_VALUE_STRING + "-" + TestSources.SECOND_ATTRIBUTE_VALUE_STRINGS[0];
        Assert.assertTrue(results.contains(new StringAttributeValue(s)), "First Match");
        s = "Att " + TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING + "-" + TestSources.SECOND_ATTRIBUTE_VALUE_STRINGS[1];
        Assert.assertTrue(results.contains(new StringAttributeValue(s)), "Second Match");
    }

}

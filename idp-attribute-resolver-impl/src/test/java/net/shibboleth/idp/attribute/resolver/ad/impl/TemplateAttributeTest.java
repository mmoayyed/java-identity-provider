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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.velocity.app.VelocityEngine;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImplTest;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.TemplateAttribute}. */
@ThreadSafe
@SuppressWarnings("deprecation")
public class TemplateAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_BASE_NAME = "TEMPLATE";

    /** Simple result. */
    private static final String SIMPLE_VALUE_STRING = "simple";

    private static final StringAttributeValue SIMPLE_VALUE_RESULT = new StringAttributeValue(SIMPLE_VALUE_STRING);

    /** A simple script to set a constant value. */
    private static final String TEST_SIMPLE_TEMPLATE = SIMPLE_VALUE_STRING;

    /** A simple script to set a value based on input values. */
    private static final String TEST_ATTRIBUTES_TEMPLATE_ATTR = "Att " + "${"
            + TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + "}-" + "${" + TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME
            + "}";

    private static final String TEST_ATTRIBUTES_TEMPLATE_CONNECTOR = "Att " + "${"
            + TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + "}-" + "${"
            + TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME + "}";

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
            } catch (final Exception e) {
                fail("couldn't create engine", e);
            }
        }
        return engineSingleton;
    }

    /**
     * Test resolution of an template script (statically generated data).
     * 
     * @throws ResolutionException id resolution fails
     * @throws ComponentInitializationException only if bad things thingas
     */
    @Test public void simple() throws ResolutionException, ComponentInitializationException {

        final String name = TEST_ATTRIBUTE_BASE_NAME + "1";
        TemplateAttributeDefinition attr = new TemplateAttributeDefinition();
        attr.setId(name);
        assertNull(attr.getTemplate());
        attr.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));
        try {
            attr.initialize();
            fail("No template");
        } catch (final ComponentInitializationException ex) {
            // OK
        }
        attr = new TemplateAttributeDefinition();
        attr.setId(name);
        assertNull(attr.getTemplateText());
        attr.setTemplateText(TEST_ATTRIBUTES_TEMPLATE_ATTR);
        assertNull(attr.getVelocityEngine());
        attr.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));
        try {
            attr.initialize();
            fail("engine");
        } catch (final ComponentInitializationException ex) {
            // OK
        }

        attr = new TemplateAttributeDefinition();
        attr.setId(name);
        attr.setVelocityEngine(getEngine());
        attr.setTemplateText(TEST_ATTRIBUTES_TEMPLATE_ATTR);
        try {
            attr.initialize();
            fail("No dependencies");
        } catch (final ComponentInitializationException ex) {
            // OK
        }
        assertNotNull(attr.getTemplateText());

        attr.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));
        
        attr.initialize();
        assertNotNull(attr.getTemplate());
        final AttributeResolutionContext context = new AttributeResolutionContext();
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        final IdPAttribute val = attr.resolve(context);
        final Collection<?> results = val.getValues();

        assertEquals(results.size(), 0, "Templated value count");

        attr = new TemplateAttributeDefinition();
        attr.setId(name);
        attr.setVelocityEngine(getEngine());
        attr.setTemplateText(TEST_ATTRIBUTES_TEMPLATE_ATTR);
        attr.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));
        
        attr.setSourceAttributes(Collections.singletonList(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        attr.initialize();
        assertNotNull(attr.getTemplate());
        try {
            attr.resolve(context);
        } catch (final ResolutionException e) {
            // OK
        }

        attr = new TemplateAttributeDefinition();
        attr.setId(name);
        attr.setVelocityEngine(getEngine());
        attr.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR)));
        try {
            attr.initialize();
            fail("No Text or attributes");
        } catch (final ComponentInitializationException ex) {
            // OK
        }
        attr.setSourceAttributes(Collections.singletonList(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        attr.setTemplateText( "${" + TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + "}");
        attr.initialize();
        assertEquals(attr.getTemplateText(), "${" + TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + "}");
        assertEquals(attr.getSourceAttributes().get(0), TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        assertEquals(attr.getSourceAttributes().size(), 1);

    }

    /**
     * Test resolution of an template script (statically generated data). By giving it attributes we create some values.
     * 
     * @throws ResolutionException if resolution fails
     * @throws ComponentInitializationException only if things go wrong
     */
    @Test public void simpleWithValues() throws ResolutionException, ComponentInitializationException {

        final String name = TEST_ATTRIBUTE_BASE_NAME + "2";

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setVelocityEngine(getEngine());
        templateDef.setTemplateText(TEST_SIMPLE_TEMPLATE);

        final Set<ResolverAttributeDefinitionDependency> ds = new LazySet<>();
        ds.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        templateDef.setAttributeDependencies(ds);
        templateDef.initialize();

        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        final Set<DataConnector> dataDefinitions = new LazySet<>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        final IdPAttribute a = context.getResolvedIdPAttributes().get(name);
        final Collection results = a.getValues();
        assertEquals(results.size(), 1, "Templated value count");
        assertTrue(results.contains(SIMPLE_VALUE_RESULT), "Single value context is correct");

    }

    /**
     * Test resolution of an template script with data generated from the attributes.
     * 
     * @throws ResolutionException if it goes wrong.
     * @throws ComponentInitializationException if it goes wrong.
     */
    @Test public void templateWithValues() throws ResolutionException, ComponentInitializationException {
        templateWithValues(false);
    }

    /**
     * Test resolution of an template script with data generated from the attributes, but with
     * explicit setting of source attributes.
     *
     * This is a canary for IDP-1386
     *
     * @throws ResolutionException if it goes wrong.
     * @throws ComponentInitializationException if it goes wrong.
     */
    @Test public void templateWithValuesTestSources() throws ResolutionException, ComponentInitializationException {
        templateWithValues(true);
    }

    /** Worker function for the templateWithValues and templateWithValuesTestSources tests.
     *
     * @param setSources whether to all {@link TemplateAttributeDefinition#setSourceAttributes(List)}
     * @throws ResolutionException if it goes wrong.
     * @throws ComponentInitializationException if it goes wrong.
     */
    private final void templateWithValues(boolean setSources) throws ResolutionException, ComponentInitializationException {

        final String name = TEST_ATTRIBUTE_BASE_NAME + "3";

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setVelocityEngine(getEngine());
        templateDef.setTemplateText(TEST_ATTRIBUTES_TEMPLATE_CONNECTOR);

        final Set<ResolverAttributeDefinitionDependency> ds = new LazySet<>();
        
        ds.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        if (setSources) {
            ds.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR+"2"));
        }
        templateDef.setAttributeDependencies(ds);
        templateDef.setDataConnectorDependencies(Collections.singleton(
                TestSources.makeDataConnectorDependency(TestSources.STATIC_CONNECTOR_NAME,
                            TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME)));
        if (setSources) {
            templateDef.setSourceAttributes(Arrays.asList(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR,
                    TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME));
        }
        templateDef.initialize();

        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        attrDefinitions.add(TestSources.populatedStaticAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR+"2", 3));
        final Set<DataConnector> dataDefinitions = new LazySet<>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        final IdPAttribute a = context.getResolvedIdPAttributes().get(name);
        final Collection results = a.getValues();
        assertEquals(results.size(), 2, "Templated value count");
        String s =
                "Att " + TestSources.COMMON_ATTRIBUTE_VALUE_STRING + "-"
                        + TestSources.SECOND_ATTRIBUTE_VALUE_STRINGS[0];
        assertTrue(results.contains(new StringAttributeValue(s)), "First Match");
        s = "Att " + TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING + "-" + TestSources.SECOND_ATTRIBUTE_VALUE_STRINGS[1];
        assertTrue(results.contains(new StringAttributeValue(s)), "Second Match");
    }

    @Test public void emptyValues() throws ResolutionException, ComponentInitializationException {
        final String name = TEST_ATTRIBUTE_BASE_NAME + "3";

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setVelocityEngine(getEngine());
        templateDef.setTemplateText("Att ${at1}");

        final Set<ResolverAttributeDefinitionDependency> ds = new LazySet<>();
        ds.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        templateDef.setAttributeDependencies(ds);
        templateDef.initialize();

        final List<IdPAttributeValue> values = new ArrayList<>();
        values.add(EmptyAttributeValue.ZERO_LENGTH);
        values.add(EmptyAttributeValue.NULL);
        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        attr.setValues(values);
        final StaticAttributeDefinition simple = new StaticAttributeDefinition();
        simple.setId(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        simple.setValue(attr);
        simple.initialize();

        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(simple);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attrDefinitions, Collections.EMPTY_SET);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        final IdPAttribute a = context.getResolvedIdPAttributes().get(name);
        final Collection results = a.getValues();
        assertEquals(results.size(), 2, "Templated value count");
        assertTrue(results.contains(new StringAttributeValue("Att ")), "First Match");
        assertTrue(results.contains(new StringAttributeValue("Att ${at1}")), "Second Match");
    }

    @Test public void failMisMatchCount() throws ResolutionException, ComponentInitializationException {
        final String name = TEST_ATTRIBUTE_BASE_NAME + "3";

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setVelocityEngine(getEngine());
        templateDef.setTemplateText(TEST_ATTRIBUTES_TEMPLATE_CONNECTOR);
        final String otherAttrName = TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + "2";

        final Set<ResolverAttributeDefinitionDependency> ds = new LazySet<>();
        ds.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        ds.add(TestSources.makeAttributeDefinitionDependency(otherAttrName));
        templateDef.setAttributeDependencies(ds);
        templateDef.initialize();

        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        attrDefinitions.add(TestSources.populatedStaticAttribute(otherAttrName, 1));

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attrDefinitions, Collections.EMPTY_SET);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            fail();
        } catch (final ResolutionException ex) {
            // OK
        }
    }

    @Test public void allowingOneEmpty() throws ResolutionException, ComponentInitializationException {
        final String name = TEST_ATTRIBUTE_BASE_NAME + "3";

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setVelocityEngine(getEngine());
        templateDef.setTemplateText(TEST_ATTRIBUTES_TEMPLATE_CONNECTOR);
        final String otherAttrName = TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + "2";

        final Set<ResolverAttributeDefinitionDependency> ds = new LazySet<>();
        ds.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        ds.add(TestSources.makeAttributeDefinitionDependency(otherAttrName));
        templateDef.setAttributeDependencies(ds);
        templateDef.initialize();

        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        attrDefinitions.add(TestSources.populatedStaticAttribute(otherAttrName, 0));

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attrDefinitions, Collections.EMPTY_SET);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
    }

    @Test public void wrongType() throws ResolutionException, ComponentInitializationException {
        final String name = TEST_ATTRIBUTE_BASE_NAME + "3";

        final TemplateAttributeDefinition templateDef = new TemplateAttributeDefinition();
        templateDef.setId(name);
        templateDef.setVelocityEngine(getEngine());
        templateDef.setTemplateText(TEST_ATTRIBUTES_TEMPLATE_ATTR);

        final Set<ResolverAttributeDefinitionDependency> ds = new LazySet<>();
        ds.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        templateDef.setAttributeDependencies(ds);
        
        templateDef.initialize();

        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        attr.setValues(Collections.singletonList(new ByteAttributeValue(new byte[] {1, 2, 3})));
        final StaticAttributeDefinition simple = new StaticAttributeDefinition();
        simple.setId(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        simple.setValue(attr);
        simple.initialize();
        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(simple);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attrDefinitions, Collections.EMPTY_SET);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            fail();
        } catch (final ResolutionException ex) {
            // OK
        }
    }

}

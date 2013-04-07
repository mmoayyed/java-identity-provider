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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.script.ScriptException;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.idp.attribute.resolver.impl.dc.SAMLAttributeDataConnector;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.apache.commons.codec.digest.DigestUtils;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.ad.ScriptedAttribute}. */
public class ScriptedAttributeTest extends XMLObjectBaseTestCase {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "Scripted";

    /** The language */
    private static final String SCRIPT_LANGUAGE = "JavaScript";

    /** Simple result. */
    private static final String SIMPLE_VALUE = "simple";

    private String fileNameToPath(String fileName) {
        return "/data/net/shibboleth/idp/attribute/resolver/impl/ad/" + fileName;
    }

    private String getScript(String fileName) throws IOException {
        return StringSupport.inputStreamToString(getClass().getResourceAsStream(fileNameToPath(fileName)), null);
    }

    /**
     * Test resolution of an simple script (statically generated data).
     * 
     * @throws ResolutionException
     * @throws ComponentInitializationException only if the test will fail
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void testSimple() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        final Attribute test = new Attribute(TEST_ATTRIBUTE_NAME);

        test.getValues().add(new StringAttributeValue(SIMPLE_VALUE));

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        Assert.assertNull(attr.getScript());
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("simple.script")));
        attr.initialize();
        Assert.assertNotNull(attr.getScript());

        final Attribute val = attr.doAttributeDefinitionResolve(generateContext()).get();
        final Set<AttributeValue> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertEquals(results.iterator().next().getValue(), SIMPLE_VALUE, "Scripted result contains known value");
    }

    /**
     * Test resolution of an simple script (statically generated data).
     * 
     * @throws ResolutionException
     * @throws ComponentInitializationException only if the test will fail
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void testSimple2() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        final Attribute test = new Attribute(TEST_ATTRIBUTE_NAME);

        test.getValues().add(new StringAttributeValue(SIMPLE_VALUE));

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        Assert.assertNull(attr.getScript());
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("simple2.script")));
        attr.initialize();
        Assert.assertNotNull(attr.getScript());

        final Attribute val = attr.doAttributeDefinitionResolve(generateContext()).get();
        final Set<AttributeValue> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertEquals(results.iterator().next().getValue(), SIMPLE_VALUE, "Scripted result contains known value");
    }

    @Test public void testSimpleWithPredef() throws ResolutionException, ComponentInitializationException,
            ScriptException, IOException {

        final Attribute test = new Attribute(TEST_ATTRIBUTE_NAME);
        final AttributeValue attributeValue = new StringAttributeValue(SIMPLE_VALUE);

        test.getValues().add(attributeValue);

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        Assert.assertNull(attr.getScript());
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("simpleWithPredef.script")));
        attr.initialize();
        Assert.assertNotNull(attr.getScript());

        final Attribute val = attr.doAttributeDefinitionResolve(generateContext()).get();
        final Set<AttributeValue> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertEquals(results.iterator().next(), attributeValue, "Scripted result contains known value");
    }

    private ScriptedAttributeDefinition buildTest(String failingScript) throws ScriptException, IOException,
            ComponentInitializationException {
        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        attr.setId(TEST_ATTRIBUTE_NAME);
        try {
            attr.initialize();
            Assert.fail("No script defined");
        } catch (ComponentInitializationException ex) {
            // OK
        }

        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript(failingScript)));
        attr.initialize();

        return attr;
    }

    private void failureTest(String failingScript, String failingMessage) throws ScriptException, IOException,
            ComponentInitializationException {
        try {
            buildTest(failingScript).doAttributeDefinitionResolve(generateContext());
            Assert.fail("Script: '" + failingScript + "' should have thrown an exception: " + failingMessage);
        } catch (ResolutionException ex) {
            // OK
        }
    }

    @Test public void testFails() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        failureTest("fail1.script", "Unknown method");
        failureTest("fail2.script", "Bad output type");
        Assert.assertFalse(buildTest("fail3.script").doAttributeDefinitionResolve(generateContext()).isPresent(),
                "returns nothing");
        failureTest("fail4.script", "getValues, then getNativeAttributes");
        failureTest("fail5.script", "getNativeAttributes, then getValues");

        failureTest("fail6.script", "bad type added");
        failureTest("fail7.script", "null added ");
    }

    @Test public void testAddAfterGetValues() throws ResolutionException, ScriptException, IOException,
            ComponentInitializationException {

        final Attribute result =
                buildTest("addAfterGetValues.script").doAttributeDefinitionResolve(generateContext()).get();
        final Set<AttributeValue> values = result.getValues();
        Assert.assertEquals(values.size(), 1);
        Assert.assertTrue(values.contains(new StringAttributeValue("newValue")));
    }

    /**
     * Test resolution of an script which looks at the provided attributes.
     * 
     * @throws ResolutionException if the resolve fails
     * @throws ComponentInitializationException only if things go wrong
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void testWithAttributes() throws ResolutionException, ComponentInitializationException,
            ScriptException, IOException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("attributes.script")));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = generateContext();
        resolver.resolveAttributes(context);
        final Attribute attribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        final Set<AttributeValue> values = attribute.getValues();

        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT));
        Assert.assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_RESULT));

    }

    @Test public void testWithNonString() throws ResolutionException, ComponentInitializationException,
            ScriptException, IOException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(TestSources.makeResolverPluginDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, null));
        
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("attributes2.script")));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new HashSet<BaseAttributeDefinition>(3);
        attrDefinitions.add(scripted);
        BaseAttributeDefinition nonString = TestSources.nonStringAttributeDefiniton(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR); 
        attrDefinitions.add(nonString);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = generateContext();
        resolver.resolveAttributes(context);
        final Attribute attribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        final Set<AttributeValue> values = attribute.getValues();

        Assert.assertEquals(values.size(), 2);
        for (AttributeValue value: values) {
            if (!(value instanceof XMLObjectAttributeValue)) {
                Assert.fail("Wrong type: " + value.getClass().getName());
            }
        }
    }

    /**
     * Test resolution of an script which looks at the provided request context.
     * 
     * @throws ResolutionException if the resolve fails
     * @throws ComponentInitializationException only if the test has gone wrong
     * @throws ScriptException
     * @throws IOException
     */
    @Test public void testWithContext() throws ResolutionException, ComponentInitializationException, ScriptException,
            IOException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("context.script")));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        TestContextContainer parent = new TestContextContainer();
        final AttributeResolutionContext context = generateContext();
        parent.addSubcontext(context);

        try {
            resolver.resolveAttributes(context);
        } catch (ResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        // The script just put the resolution context in as the attribute value. Yea it makes
        // no sense but it is easy to test.
        final Attribute attribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        final Collection<AttributeValue> values = attribute.getValues();

        Assert.assertEquals(values.size(), 1, "looking for context");
        Assert.assertEquals(values.iterator().next().getValue(), "TestContainerContextid");
    }

    protected Attribute runExample(String exampleScript, String exampleData, String attributeName)
            throws ScriptException, IOException, ComponentInitializationException {
        SAMLAttributeDataConnector connector = new SAMLAttributeDataConnector();
        connector.setAttributesStrategy(new Locator(exampleData));
        connector.setId("Connector");

        final Set<ResolverPluginDependency> ds = Collections.singleton(TestSources.makeResolverPluginDependency("Connector", null));

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(attributeName);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript(exampleScript)));
        scripted.setDependencies(ds);

        final Set<BaseDataConnector> dataDefinitions = Collections.singleton((BaseDataConnector) connector);
        final Set<BaseAttributeDefinition> attrDefinitions = Collections.singleton((BaseAttributeDefinition) scripted);

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext("principal", "issuer", "recipient");

        try {
            resolver.resolveAttributes(context);
        } catch (ResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        return context.getResolvedAttributes().get(attributeName);

    }

    @Test public void testExamples() throws ScriptException, IOException, ComponentInitializationException {

        Attribute attribute = runExample("example1.script", "example1.attribute.xml", "swissEduPersonUniqueID");

        Assert.assertEquals(attribute.getValues().iterator().next().getValue(),
                DigestUtils.md5Hex("12345678some#salt#value#12345679") + "@switch.ch");

        attribute = runExample("example2.script", "example2.attribute.xml", "eduPersonAffiliation");
        HashSet<AttributeValue> set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 3);
        Assert.assertTrue(set.contains(new StringAttributeValue("affiliate")));
        Assert.assertTrue(set.contains(new StringAttributeValue("student")));
        Assert.assertTrue(set.contains(new StringAttributeValue("staff")));

        attribute = runExample("example3.script", "example3.attribute.xml", "eduPersonAffiliation");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 2);
        Assert.assertTrue(set.contains(new StringAttributeValue("member")));
        Assert.assertTrue(set.contains(new StringAttributeValue("staff")));

        attribute = runExample("example3.script", "example3.attribute.2.xml", "eduPersonAffiliation");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 3);
        Assert.assertTrue(set.contains(new StringAttributeValue("member")));
        Assert.assertTrue(set.contains(new StringAttributeValue("staff")));
        Assert.assertTrue(set.contains(new StringAttributeValue("walkin")));

        attribute = runExample("example4.script", "example4.attribute.xml", "eduPersonEntitlement");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 1);
        Assert.assertTrue(set.contains(new StringAttributeValue("urn:mace:dir:entitlement:common-lib-terms")));

        attribute = runExample("example4.script", "example4.attribute.2.xml", "eduPersonEntitlement");
        set = new HashSet(attribute.getValues());
        Assert.assertEquals(set.size(), 2);
        Assert.assertTrue(set.contains(new StringAttributeValue("urn:mace:dir:entitlement:common-lib-terms")));
        Assert.assertTrue(set.contains(new StringAttributeValue("LittleGreenMen")));

        attribute = runExample("example4.script", "example4.attribute.3.xml", "eduPersonEntitlement");
        Assert.assertNull(attribute);

    }

    @Test public void testV2Context() throws IOException, ComponentInitializationException, ResolutionException,
            ScriptException {

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId("scripted");
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("requestContext.script")));
        scripted.initialize();

        Optional<Attribute> result = scripted.doAttributeDefinitionResolve(generateContext());
        HashSet<AttributeValue> set = new HashSet(result.get().getValues());
        Assert.assertEquals(set.size(), 3);
        Assert.assertTrue(set.contains(new StringAttributeValue(TestSources.PRINCIPAL_ID)));
        Assert.assertTrue(set.contains(new StringAttributeValue(TestSources.IDP_ENTITY_ID)));
        Assert.assertTrue(set.contains(new StringAttributeValue(TestSources.SP_ENTITY_ID)));

    }

    @Test public void testUnimplementedV2Context() throws IOException, ComponentInitializationException,
            ResolutionException, ScriptException {

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId("scripted");
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, getScript("requestContextUnimplemented.script")));
        scripted.initialize();

        Optional<Attribute> result = scripted.doAttributeDefinitionResolve(generateContext());
        Assert.assertEquals(result.get().getValues().iterator().next(), new StringAttributeValue("AllDone"));

    }

    private static AttributeResolutionContext generateContext() {
        return TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                TestSources.SP_ENTITY_ID);
    }

    final class Locator implements Function<AttributeResolutionContext, List<org.opensaml.saml.saml2.core.Attribute>> {

        final EntityAttributes obj;

        public Locator(String file) {
            obj = (EntityAttributes) unmarshallElement(fileNameToPath(file));
        }

        /** {@inheritDoc} */
        @Nullable public List<org.opensaml.saml.saml2.core.Attribute> apply(@Nullable AttributeResolutionContext input) {
            return obj.getAttributes();
        }

    }
}

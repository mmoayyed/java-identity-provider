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

package net.shibboleth.idp.saml.attribute.transcoding.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.shibboleth.ext.spring.testing.MockApplicationContext;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.BasicNamingFunction;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML1AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAML1AttributeTranscoder;
import net.shibboleth.idp.saml.xmlobject.ScopedValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeDesignator;
import org.opensaml.saml.saml1.core.AttributeValue;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** {@link SAML1ScopedStringAttributeTranscoder} unit test. */
public class SAML1ScopedStringAttributeTranscoderTest extends OpenSAMLInitBaseTestCase {

    private AttributeTranscoderRegistryImpl registry;

    private XMLObjectBuilder<XSString> stringBuilder;

    private XMLObjectBuilder<ScopedValue> scopedBuilder;

    private SAMLObjectBuilder<Attribute> attributeBuilder;

    private SAMLObjectBuilder<AttributeDesignator> designatorBuilder;

    private final static String ATTR_NAME = "foo";
    private final static String ATTR_NAMESPACE = "Namespace";
    private final static String STRING_1 = "Value The First";
    private final static String STRING_2 = "Second string the value is";
    private final static String SCOPE_1 = "scope1.example.org";
    private final static String SCOPE_2 = "scope2";
    private final static String DELIMITER = "#";

    @BeforeClass public void setUp() throws ComponentInitializationException {
        
        stringBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().<XSString>getBuilderOrThrow(
                XSString.TYPE_NAME);

        scopedBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().<ScopedValue>getBuilderOrThrow(
                ScopedValue.TYPE_NAME);
        
        attributeBuilder = (SAMLObjectBuilder<Attribute>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Attribute>getBuilderOrThrow(
                        Attribute.TYPE_NAME);
        designatorBuilder = (SAMLObjectBuilder<AttributeDesignator>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<AttributeDesignator>getBuilderOrThrow(
                        AttributeDesignator.TYPE_NAME);
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");
        
        final SAML1ScopedStringAttributeTranscoder transcoder = new SAML1ScopedStringAttributeTranscoder();
        transcoder.initialize();
        
        registry.setNamingRegistry(Collections.singletonList(
                new BasicNamingFunction<>(transcoder.getEncodedType(), new AbstractSAML1AttributeTranscoder.NamingFunction())));
        
        final Map<String,Object> ruleset1 = new HashMap<>();
        ruleset1.put(AttributeTranscoderRegistry.PROP_ID, ATTR_NAME);
        ruleset1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset1.put(SAML1AttributeTranscoder.PROP_NAME, ATTR_NAME);
        ruleset1.put(SAML1AttributeTranscoder.PROP_NAMESPACE, ATTR_NAMESPACE);
        ruleset1.put(SAML1ScopedStringAttributeTranscoder.PROP_SCOPE_DELIMITER, DELIMITER);
        ruleset1.put(SAML1ScopedStringAttributeTranscoder.PROP_SCOPE_TYPE, "attribute");
        
        registry.setTranscoderRegistry(Collections.singletonList(new TranscodingRule(ruleset1)));
        registry.setApplicationContext(new MockApplicationContext());        
        registry.initialize();
    }
    
    @AfterClass public void tearDown() {
        registry.destroy();
        registry = null;
    }

    @Test(expectedExceptions = {AttributeEncodingException.class,}) public void emptyEncode() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(null, inputAttribute, Attribute.class, ruleset);
    }

    @Test public void emptyDecode() throws Exception {
        
        // This isn't technically legal in SAML, but it should functionally work in this direction.
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertTrue(attr.getValues().isEmpty());
    }

    @Test public void emptyRequestedDecode() throws Exception {
        
        final AttributeDesignator samlAttribute = designatorBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<AttributeDesignator>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertTrue(attr instanceof IdPRequestedAttribute);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertFalse(((IdPRequestedAttribute) attr).isRequired());
        Assert.assertTrue(attr.getValues().isEmpty());
    }
    
    @Test(expectedExceptions = {AttributeEncodingException.class,}) public void inappropriate() throws Exception {
        final int[] intArray = {1, 2, 3, 4};
        final List<IdPAttributeValue> values =
                Arrays.asList(new ByteAttributeValue(new byte[] {1, 2, 3,}), new IdPAttributeValue() {
                    @Override
                    public Object getNativeValue() {
                        return intArray;
                    }
                    @Override
                    public String getDisplayValue() {
                        return intArray.toString();
                    }
                });

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(null, inputAttribute, Attribute.class, ruleset);
    }
    
    @Test public void single() throws Exception {
        final List<IdPAttributeValue> values =
                Arrays.asList(new ByteAttributeValue(new byte[] {1, 2, 3,}),
                        new ScopedStringAttributeValue(STRING_1, SCOPE_1),
                        new StringAttributeValue(STRING_1),
                        new StringAttributeValue(STRING_1 + "@" + SCOPE_1));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(values);
        
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getAttributeName(), ATTR_NAME);
        Assert.assertEquals(attr.getAttributeNamespace(), ATTR_NAMESPACE);

        final List<XMLObject> children = attr.getOrderedChildren();

        Assert.assertEquals(children.size(), 1, "Encoding one entry");

        final XMLObject child = children.get(0);

        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");

        Assert.assertTrue(child instanceof ScopedValue, "Child of result attribute should be a ScopedValue");
        // xsi:type should be absent because encodeType should default off for attribute-syntax scope
        Assert.assertNull(child.getSchemaType(), "xsi:type was set");

        final ScopedValue childAsScopedValue = (ScopedValue) child;

        Assert.assertEquals(childAsScopedValue.getValue(), STRING_1, "Input equals output");
        Assert.assertEquals(childAsScopedValue.getScope(), SCOPE_1, "Input equals output");
    }

    @Test public void singleRequested() throws Exception {
        final List<IdPAttributeValue> values =
                Arrays.asList(new ByteAttributeValue(new byte[] {1, 2, 3,}),
                        new ScopedStringAttributeValue(STRING_1, SCOPE_1));

        final IdPRequestedAttribute inputAttribute = new IdPRequestedAttribute(ATTR_NAME);
        inputAttribute.setValues(values);
        
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();

        final AttributeDesignator attr = TranscoderSupport.<AttributeDesignator>getTranscoder(ruleset).encode(
                null, inputAttribute, AttributeDesignator.class, ruleset);

        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getAttributeName(), ATTR_NAME);
        Assert.assertEquals(attr.getAttributeNamespace(), ATTR_NAMESPACE);
    }
    
    @Test public void singleDecode() throws Exception {
                
        final ScopedValue scopedValue = scopedBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        scopedValue.setScopeAttributeName("Scope");
        scopedValue.setValue(STRING_1);
        scopedValue.setScope(SCOPE_1);
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);
        samlAttribute.getAttributeValues().add(scopedValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 1);
        
        final ScopedStringAttributeValue value = (ScopedStringAttributeValue) attr.getValues().get(0);
        Assert.assertEquals(value.getValue(), STRING_1);
        Assert.assertEquals(value.getScope(), SCOPE_1);
    }
        
    @Test public void multi() throws Exception {
        final List<IdPAttributeValue> values =
                Arrays.asList(new ByteAttributeValue(new byte[] {1, 2, 3,}),
                        new ScopedStringAttributeValue(STRING_1, SCOPE_1),
                        new ScopedStringAttributeValue(STRING_2, SCOPE_2));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        Assert.assertNotNull(attr);

        final List<XMLObject> children = attr.getOrderedChildren();
        Assert.assertEquals(children.size(), 2, "Encoding 2 entries");

        Assert.assertTrue(children.get(0) instanceof ScopedValue && children.get(1) instanceof ScopedValue,
                "Child of result attribute should be a string");

        final ScopedValue child1 = (ScopedValue) children.get(0);
        Assert.assertEquals(child1.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");

        final ScopedValue child2 = (ScopedValue) children.get(1);
        Assert.assertEquals(child2.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        //
        // order of results is not guaranteed so sense the result from the length
        //
        if (child1.getValue().length() == STRING_1.length()) {
            Assert.assertEquals(child1.getValue(), STRING_1, "Input matches output");
            Assert.assertEquals(child2.getValue(), STRING_2, "Input matches output");
            Assert.assertEquals(child1.getScope(), SCOPE_1, "Input matches output");
            Assert.assertEquals(child2.getScope(), SCOPE_2, "Input matches output");
        } else if (child1.getValue().length() == STRING_2.length()) {
            Assert.assertEquals(child2.getValue(), STRING_1, "Input matches output");
            Assert.assertEquals(child1.getValue(), STRING_2, "Input matches output");
            Assert.assertEquals(child2.getScope(), SCOPE_1, "Input matches output");
            Assert.assertEquals(child1.getScope(), SCOPE_2, "Input matches output");
        } else {
            Assert.fail("Value mismatch");
        }
    }

    @Test public void multiDecode() throws Exception {
        
        final ScopedValue scopedValue = scopedBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        scopedValue.setScopeAttributeName("Scope");
        scopedValue.setValue(STRING_1);
        scopedValue.setScope(SCOPE_1);

        final ScopedValue scopedValue2 = scopedBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        scopedValue2.setScopeAttributeName("Scope");
        scopedValue2.setValue(STRING_2);
        scopedValue2.setScope(SCOPE_2);

        final XSString stringValue3 = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue3.setValue(STRING_2 + "@" + SCOPE_2);

        final XSString stringValue4 = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue4.setValue(STRING_2);

        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);
        samlAttribute.getAttributeValues().add(scopedValue);
        samlAttribute.getAttributeValues().add(scopedValue2);
        samlAttribute.getAttributeValues().add(stringValue3);
        samlAttribute.getAttributeValues().add(stringValue4);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 2);
        
        final ScopedStringAttributeValue value1 = (ScopedStringAttributeValue) attr.getValues().get(0);
        final ScopedStringAttributeValue value2 = (ScopedStringAttributeValue) attr.getValues().get(1);
        Assert.assertTrue(STRING_1.equals(value1.getValue()) || STRING_1.equals(value2.getValue()));
        Assert.assertTrue(STRING_2.equals(value1.getValue()) || STRING_2.equals(value2.getValue()));
        Assert.assertTrue(SCOPE_1.equals(value1.getScope()) || SCOPE_1.equals(value2.getScope()));
        Assert.assertTrue(SCOPE_2.equals(value1.getScope()) || SCOPE_2.equals(value2.getScope()));
    }

}

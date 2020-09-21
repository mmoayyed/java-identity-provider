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
import net.shibboleth.idp.attribute.AttributeDecodingException;
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
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeValue;
import org.opensaml.saml.saml1.core.AttributeDesignator;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** {@link SAML1ByteAttributeTranscoder} unit test. */
public class SAML1ByteAttributeTranscoderTest extends OpenSAMLInitBaseTestCase {

    private AttributeTranscoderRegistryImpl registry;
    
    private XMLObjectBuilder<XSString> stringBuilder;

    private SAMLObjectBuilder<Attribute> attributeBuilder;

    private SAMLObjectBuilder<AttributeDesignator> designatorBuilder;

    private final static String ATTR_NAME = "foo";
    private final static String ATTR_NAMESPACE = "Namespace";
    private final static byte[] BYTE_ARRAY_1 = {1, 2, 3, 4, 5};
    private final static byte[] BYTE_ARRAY_2 = {4, 3, 2, 1};
    
    /** Invalid base64 string as it has invalid trailing digits. */
    private final static String INVALID_BASE64_TRAILING = "AB==";

    @BeforeClass public void setUp() throws ComponentInitializationException {
        
        stringBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().<XSString>getBuilderOrThrow(XSString.TYPE_NAME);
        
        attributeBuilder = (SAMLObjectBuilder<Attribute>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Attribute>getBuilderOrThrow(
                        Attribute.TYPE_NAME);
        designatorBuilder = (SAMLObjectBuilder<AttributeDesignator>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<AttributeDesignator>getBuilderOrThrow(
                        AttributeDesignator.TYPE_NAME);
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");
                
        final SAML1ByteAttributeTranscoder transcoder = new SAML1ByteAttributeTranscoder();
        transcoder.initialize();
        
        registry.setNamingRegistry(Collections.singletonList(
                new BasicNamingFunction<>(transcoder.getEncodedType(), new AbstractSAML1AttributeTranscoder.NamingFunction())));
        
        final Map<String,Object> ruleset1 = new HashMap<>();
        ruleset1.put(AttributeTranscoderRegistry.PROP_ID, ATTR_NAME);
        ruleset1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset1.put(SAML1AttributeTranscoder.PROP_ENCODE_TYPE, true);
        ruleset1.put(SAML1AttributeTranscoder.PROP_NAME, ATTR_NAME);
        ruleset1.put(SAML1AttributeTranscoder.PROP_NAMESPACE, ATTR_NAMESPACE);
        
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
        
        TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);
    }

    @Test public void emptyRequestedEncode() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, AttributeDesignator.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final AttributeDesignator attr = TranscoderSupport.<AttributeDesignator>getTranscoder(ruleset).encode(
                null, inputAttribute, AttributeDesignator.class, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getAttributeName(), ATTR_NAME);
        Assert.assertEquals(attr.getAttributeNamespace(), ATTR_NAMESPACE);
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
                Arrays.asList(new StringAttributeValue("foo"), new ScopedStringAttributeValue("foo", "bar"),
                        new IdPAttributeValue() {
                            public Object getNativeValue() {
                                return intArray;
                            }
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
                Arrays.asList(new StringAttributeValue("foo"), new ByteAttributeValue(BYTE_ARRAY_1));

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
        Assert.assertTrue(child instanceof XSBase64Binary, "Child of result attribute should be a base64Binary");

        XSBase64Binary childAsString = (XSBase64Binary) child;

        byte childAsBa[] = Base64Support.decode(childAsString.getValue());

        Assert.assertEquals(childAsBa, BYTE_ARRAY_1, "Input equals output");
    }

    @Test public void singleRequested() throws Exception {
        final List<IdPAttributeValue> values =
                Arrays.asList(new StringAttributeValue("foo"), new ByteAttributeValue(BYTE_ARRAY_1));

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
                
        final XSString stringValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue.setValue(Base64Support.encode(BYTE_ARRAY_1, Base64Support.UNCHUNKED));
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);
        samlAttribute.getAttributeValues().add(stringValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 1);
        Assert.assertEquals(((ByteAttributeValue)attr.getValues().get(0)).getValue(), BYTE_ARRAY_1);
    }

    @Test(expectedExceptions = {AttributeDecodingException.class,}) public void badDecode() throws Exception {
        
        final XSString stringValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue.setValue("******");
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);
        samlAttribute.getAttributeValues().add(stringValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
    }
    
    @Test(expectedExceptions = AttributeDecodingException.class) public void badDecodeInvalidBase64() throws AttributeDecodingException{
        final XSString stringValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue.setValue(INVALID_BASE64_TRAILING);
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);
        samlAttribute.getAttributeValues().add(stringValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
    }
    
    @Test public void multi() throws Exception {
        final List<IdPAttributeValue> values =
                Arrays.asList(new ByteAttributeValue(BYTE_ARRAY_1), new ByteAttributeValue(BYTE_ARRAY_2));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        Assert.assertNotNull(attr);

        final List<XMLObject> children = attr.getOrderedChildren();
        Assert.assertEquals(children.size(), 2, "Encoding three entries");

        XMLObject child = children.get(0);
        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertTrue(child instanceof XSBase64Binary, "Child of result attribute should be a base64Binary");

        XSBase64Binary childAsString = (XSBase64Binary) child;
        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        final byte[] res0 = Base64Support.decode(childAsString.getValue());
        
        child = children.get(1);
        Assert.assertTrue(child instanceof XSBase64Binary, "Child of result attribute should be a base64Binary");

        childAsString = (XSBase64Binary) child;
        final byte[] res1 = Base64Support.decode(childAsString.getValue());

        //
        // order of results is not guaranteed so sense the result from the length
        //
        if (BYTE_ARRAY_1.length == res0.length) {
            Assert.assertEquals(BYTE_ARRAY_1, res0, "Input matches output");
            Assert.assertEquals(BYTE_ARRAY_2, res1, "Input matches output");
        } else if (BYTE_ARRAY_1.length == res1.length) {
            Assert.assertEquals(BYTE_ARRAY_1, res1, "Input matches output");
            Assert.assertEquals(BYTE_ARRAY_2, res0, "Input matches output");
        } else {
            Assert.assertTrue(BYTE_ARRAY_1.length == res1.length || BYTE_ARRAY_2.length == res1.length,
                    "One of the output's size should match an input size");
        }
    }

    @Test public void multiDecode() throws Exception {
        
        final XSString stringValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue.setValue(Base64Support.encode(BYTE_ARRAY_1, Base64Support.UNCHUNKED));

        final XSString stringValue2 = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue2.setValue(Base64Support.encode(BYTE_ARRAY_2, Base64Support.UNCHUNKED));
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(ATTR_NAME);
        samlAttribute.setAttributeNamespace(ATTR_NAMESPACE);
        samlAttribute.getAttributeValues().add(stringValue);
        samlAttribute.getAttributeValues().add(stringValue2);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 2);
        Assert.assertEquals(((ByteAttributeValue)attr.getValues().get(0)).getValue(), BYTE_ARRAY_1);
        Assert.assertEquals(((ByteAttributeValue)attr.getValues().get(1)).getValue(), BYTE_ARRAY_2);
    }

}

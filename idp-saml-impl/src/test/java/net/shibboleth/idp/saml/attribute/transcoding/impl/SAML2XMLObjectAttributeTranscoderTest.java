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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.testing.MockApplicationContext;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML2AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAML2AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAMLEncoderSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** {@link SAML2XMLObjectAttributeTranscoder} unit test. */
public class SAML2XMLObjectAttributeTranscoderTest extends OpenSAMLInitBaseTestCase {

    private AttributeTranscoderRegistryImpl registry;
    
    private SAMLObjectBuilder<AttributeValue> anyBuilder;
    
    private XMLObjectBuilder<XSString> stringBuilder;

    private SAMLObjectBuilder<Attribute> attributeBuilder;

    private SAMLObjectBuilder<RequestedAttribute> reqAttributeBuilder;

    private final static String ATTR_NAME = "foo";
    private final static String ATTR_NAMEFORMAT = "Namespace";
    private final static String ATTR_FRIENDLYNAME = "friendly";
    private final static String STRING_1 = "Value The First";
    private final static String STRING_2 = "Second string the value is";

    @BeforeClass public void setUp() throws ComponentInitializationException {
        
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        
        anyBuilder = (SAMLObjectBuilder<AttributeValue>) bf.<AttributeValue>getBuilderOrThrow(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringBuilder = bf.<XSString>getBuilderOrThrow(XSString.TYPE_NAME);
        
        attributeBuilder = (SAMLObjectBuilder<Attribute>) bf.<Attribute>getBuilderOrThrow(Attribute.TYPE_NAME);
        reqAttributeBuilder = (SAMLObjectBuilder<RequestedAttribute>) bf.<RequestedAttribute>getBuilderOrThrow(RequestedAttribute.TYPE_NAME);
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");

        final SAML2XMLObjectAttributeTranscoder transcoder = new SAML2XMLObjectAttributeTranscoder();
        transcoder.initialize();
        
        registry.setNamingRegistry(Collections.singletonMap(transcoder.getEncodedType(),
                new AbstractSAML2AttributeTranscoder.NamingFunction()));
        
        final Map<String,Object> ruleset1 = new HashMap<>();
        ruleset1.put(AttributeTranscoderRegistry.PROP_ID, ATTR_NAME);
        ruleset1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset1.put(SAML2AttributeTranscoder.PROP_ENCODE_TYPE, true);
        ruleset1.put(SAML2AttributeTranscoder.PROP_NAME, ATTR_NAME);
        ruleset1.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, ATTR_NAMEFORMAT);
        ruleset1.put(SAML2AttributeTranscoder.PROP_FRIENDLY_NAME, ATTR_FRIENDLYNAME);
        
        registry.setTranscoderRegistry(Collections.singletonList(new TranscodingRule(ruleset1)));
        registry.setApplicationContext(new MockApplicationContext());      
        registry.initialize();
    }
    
    @AfterClass public void tearDown() {
        registry.destroy();
        registry = null;
    }

    @Test public void emptyEncode() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getName(), ATTR_NAME);
        Assert.assertEquals(attr.getNameFormat(), ATTR_NAMEFORMAT);
        Assert.assertEquals(attr.getFriendlyName(), ATTR_FRIENDLYNAME);
        Assert.assertTrue(attr.getAttributeValues().isEmpty());
    }

    @Test public void emptyDecode() throws Exception {
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertTrue(attr.getValues().isEmpty());
    }

    @Test public void emptyRequestedDecode() throws Exception {
        
        final RequestedAttribute samlAttribute = reqAttributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);
        samlAttribute.setIsRequired(true);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertTrue(attr instanceof IdPRequestedAttribute);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertTrue(((IdPRequestedAttribute) attr).isRequired());
        Assert.assertTrue(attr.getValues().isEmpty());
    }
    
    @Test(expectedExceptions = {AttributeEncodingException.class,}) public void inappropriate() throws Exception {
        final int[] intArray = {1, 2, 3, 4};
        final List<IdPAttributeValue> values =
                List.of(new ByteAttributeValue(new byte[] {1, 2, 3,}), new IdPAttributeValue() {
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
                List.of(new ByteAttributeValue(new byte[] {1, 2, 3,}), objectFor(STRING_1));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(values);
        
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getName(), ATTR_NAME);
        Assert.assertEquals(attr.getNameFormat(), ATTR_NAMEFORMAT);
        Assert.assertEquals(attr.getFriendlyName(), ATTR_FRIENDLYNAME);

        final List<XMLObject> children = attr.getOrderedChildren();
        Assert.assertEquals(children.size(), 1, "Encoding one entry");
        Assert.assertEquals(children.get(0).getElementQName(),
                AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertEquals(children.get(0).getOrderedChildren().size(), 1,
                "Expected exactly one child inside the <AttributeValue/>");
        
        checkValues(children.get(0).getOrderedChildren().get(0), STRING_1);
    }

    @Test public void singleRequested() throws Exception {
        final List<IdPAttributeValue> values =
                List.of(new ByteAttributeValue(new byte[] {1, 2, 3,}), objectFor(STRING_1));

        final IdPRequestedAttribute inputAttribute = new IdPRequestedAttribute(ATTR_NAME);
        inputAttribute.setRequired(true);
        inputAttribute.setValues(values);
        
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();

        final RequestedAttribute attr = TranscoderSupport.<RequestedAttribute>getTranscoder(ruleset).encode(
                null, inputAttribute, RequestedAttribute.class, ruleset);

        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getName(), ATTR_NAME);
        Assert.assertEquals(attr.getNameFormat(), ATTR_NAMEFORMAT);
        Assert.assertEquals(attr.getFriendlyName(), ATTR_FRIENDLYNAME);
        Assert.assertTrue(attr.isRequired());

        final List<XMLObject> children = attr.getOrderedChildren();
        Assert.assertEquals(children.size(), 1, "Encoding one entry");
        Assert.assertEquals(children.get(0).getElementQName(),
                AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertEquals(children.get(0).getOrderedChildren().size(), 1,
                "Expected exactly one child inside the <AttributeValue/>");
        
        checkValues(children.get(0).getOrderedChildren().get(0), STRING_1);
    }
    
    @Test public void singleDecode() throws Exception {
                
        final XSString stringValue = stringBuilder.buildObject(new QName("Foo"));
        stringValue.setValue(STRING_1);
        
        final AttributeValue attrValue = anyBuilder.buildObject();
        attrValue.getUnknownXMLObjects().add(stringValue);
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);
        samlAttribute.getAttributeValues().add(attrValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 1);
        
        final XMLObjectAttributeValue value = (XMLObjectAttributeValue) attr.getValues().get(0);
        Assert.assertTrue(value.getValue() instanceof XSString);
        Assert.assertEquals(value.getValue().getElementQName().getLocalPart(), "Foo");
        Assert.assertEquals(((XSString) value.getValue()).getValue(), STRING_1);
    }
    
    
    @Test public void singleRequestedDecode() throws Exception {
        
        final XSString stringValue = stringBuilder.buildObject(new QName("Foo"));
        stringValue.setValue(STRING_1);
        
        final AttributeValue attrValue = anyBuilder.buildObject();
        attrValue.getUnknownXMLObjects().add(stringValue);
        
        final RequestedAttribute samlAttribute = reqAttributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);
        samlAttribute.setIsRequired(true);
        samlAttribute.getAttributeValues().add(attrValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertTrue(attr instanceof IdPRequestedAttribute);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertTrue(((IdPRequestedAttribute) attr).isRequired());
        Assert.assertEquals(attr.getValues().size(), 1);

        final XMLObjectAttributeValue value = (XMLObjectAttributeValue) attr.getValues().get(0);
        Assert.assertTrue(value.getValue() instanceof XSString);
        Assert.assertEquals(((XSString) value.getValue()).getValue(), STRING_1);
    }
    
    @Test public void multi() throws Exception {
        final List<IdPAttributeValue> values =
                List.of(objectFor(STRING_1), objectFor(STRING_2));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        Assert.assertNotNull(attr);

        final List<XMLObject> children = attr.getOrderedChildren();
        Assert.assertEquals(children.size(), 2, "Encoding two entries");

        Assert.assertEquals(children.get(0).getElementQName(),
                AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertEquals(children.get(0).getOrderedChildren().size(), 1,
                "Expected exactly one child inside the <AttributeValue/> for first Attribute");

        Assert.assertEquals(children.get(1).getElementQName(),
                AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertEquals(children.get(1).getOrderedChildren().size(), 1,
                "Expected exactly one child inside the <AttributeValue/> for second Attribute");

        checkValues(children.get(0).getOrderedChildren().get(0), STRING_1, STRING_2);
        checkValues(children.get(1).getOrderedChildren().get(0), STRING_1, STRING_2);
    }

    @Test public void multiDecode() throws Exception {

        final XSString stringValue = stringBuilder.buildObject(new QName("Foo"));
        stringValue.setValue(STRING_1);
        
        final AttributeValue attrValue = anyBuilder.buildObject();
        attrValue.getUnknownXMLObjects().add(stringValue);

        final XSString stringValue2 = stringBuilder.buildObject(new QName("Bar"));
        stringValue2.setValue(STRING_2);
        
        final AttributeValue attrValue2 = anyBuilder.buildObject();
        attrValue2.getUnknownXMLObjects().add(stringValue2);
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);
        samlAttribute.getAttributeValues().add(attrValue);
        samlAttribute.getAttributeValues().add(attrValue2);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 2);
        
        final XMLObjectAttributeValue value = (XMLObjectAttributeValue) attr.getValues().get(0);
        Assert.assertTrue(value.getValue() instanceof XSString);

        final XMLObjectAttributeValue value2 = (XMLObjectAttributeValue) attr.getValues().get(1);
        Assert.assertTrue(value2.getValue() instanceof XSString);

        final String s1 = ((XSString) value.getValue()).getValue();
        final String s2 = ((XSString) value2.getValue()).getValue();
        
        Assert.assertTrue(STRING_1.equals(s1) || STRING_2.equals(s1));
        Assert.assertTrue(STRING_1.equals(s2) || STRING_2.equals(s2));
    }

    /**
     * Create an XML object from a string which we can test against later.
     * 
     * @param value that we encode
     * @return an XML object
     */
    private static XMLObjectAttributeValue objectFor(final String value) {
        
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(Collections.singletonList(new StringAttributeValue(value)));
        return new XMLObjectAttributeValue(
                SAMLEncoderSupport.encodeStringValue(inputAttribute, new QName("Foo"), value, true));
    }

    /**
     * Check that the input XML object is what was expected.
     * 
     * @param input the objects in question.
     * @param possibles the strings that they might be encoding.
     */
    private static void checkValues(final XMLObject input, final String... possibles) {

        Assert.assertTrue(input instanceof XSString);
        final String s = ((XSString) input).getValue();

        for (String possible : possibles) {
            if (s.equals(possible)) {
                return;
            }
        }
        Assert.assertTrue(false, "No potential match");
    }

}

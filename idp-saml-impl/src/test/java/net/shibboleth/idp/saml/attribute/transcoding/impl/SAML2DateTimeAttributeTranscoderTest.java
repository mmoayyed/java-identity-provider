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

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.shibboleth.ext.spring.testing.MockApplicationContext;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.DateTimeAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.BasicNamingFunction;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML2AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAML2AttributeTranscoder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** {@link SAML2DateTimeAttributeTranscoder} unit test. */
public class SAML2DateTimeAttributeTranscoderTest extends OpenSAMLInitBaseTestCase {

    private AttributeTranscoderRegistryImpl registry;
    
    private XMLObjectBuilder<XSString> stringBuilder;

    private XMLObjectBuilder<XSDateTime> dateTimeBuilder;

    private SAMLObjectBuilder<Attribute> attributeBuilder;

    private SAMLObjectBuilder<RequestedAttribute> reqAttributeBuilder;

    private final static String ATTR_NAME = "foo";
    private final static String ATTR_NAMEFORMAT = "Namespace";
    private final static String ATTR_FRIENDLYNAME = "friendly";
    private final static String STRING_SECS = "1659979872";
    private final static String STRING_MSECS = "1659979872969";
    private final static String STRING_ISO = "2022-08-08T17:31:12.969Z";
    private final static String STRING_INVALID = "invalid";
        
    @BeforeClass public void setUp() throws ComponentInitializationException {
        
        stringBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().<XSString>getBuilderOrThrow(XSString.TYPE_NAME);
        dateTimeBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().<XSDateTime>getBuilderOrThrow(XSDateTime.TYPE_NAME);
        
        attributeBuilder = (SAMLObjectBuilder<Attribute>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Attribute>getBuilderOrThrow(
                        Attribute.TYPE_NAME);
        reqAttributeBuilder = (SAMLObjectBuilder<RequestedAttribute>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<RequestedAttribute>getBuilderOrThrow(
                        RequestedAttribute.TYPE_NAME);
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");

        final SAML2DateTimeAttributeTranscoder transcoder = new SAML2DateTimeAttributeTranscoder();
        transcoder.initialize();
        
        registry.setNamingRegistry(Collections.singletonList(
                new BasicNamingFunction<>(transcoder.getEncodedType(), new AbstractSAML2AttributeTranscoder.NamingFunction())));
        
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


    @Test public void invalidDecode() throws Exception {
        
        final XSString stringValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue.setValue(STRING_INVALID);

        final XSString stringValue2 = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue2.setValue(STRING_ISO);

        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);
        samlAttribute.getAttributeValues().add(stringValue);
        samlAttribute.getAttributeValues().add(stringValue2);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 1);
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
                List.of(new ByteAttributeValue(new byte[] {1, 2, 3,}), new DateTimeAttributeValue(Instant.parse(STRING_ISO)));

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

        final XMLObject child = children.get(0);

        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");

        Assert.assertTrue(child instanceof XSDateTime, "Child of result attribute should be a string");

        final XSDateTime childAsString = (XSDateTime) child;

        Assert.assertEquals(childAsString.getValue(), Instant.parse(STRING_ISO));
    }

    @Test public void singleRequested() throws Exception {
        final List<IdPAttributeValue> values =
                List.of(new ByteAttributeValue(new byte[] {1, 2, 3,}),
                        new DateTimeAttributeValue(Instant.ofEpochSecond(Long.valueOf(STRING_SECS))));

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

        final XMLObject child = children.get(0);

        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");

        Assert.assertTrue(child instanceof XSDateTime, "Child of result attribute should be a string");

        final XSDateTime childAsString = (XSDateTime) child;

        Assert.assertEquals(childAsString.getValue().getEpochSecond(), Long.valueOf(STRING_SECS));
    }
    
    @Test public void singleDecodeString() throws Exception {
                
        final XSString stringValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        stringValue.setValue(STRING_ISO);
        
        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);
        samlAttribute.getAttributeValues().add(stringValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertEquals(attr.getValues().size(), 1);
        Assert.assertEquals(((DateTimeAttributeValue)attr.getValues().get(0)).getValue().toString(), STRING_ISO);
    }
    
    
    @Test public void singleRequestedDecode() throws Exception {
        
        final XSDateTime dateTimeValue = dateTimeBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        dateTimeValue.setValue(Instant.ofEpochMilli(Long.valueOf(STRING_MSECS)));
        
        final RequestedAttribute samlAttribute = reqAttributeBuilder.buildObject();
        samlAttribute.setName(ATTR_NAME);
        samlAttribute.setNameFormat(ATTR_NAMEFORMAT);
        samlAttribute.setIsRequired(true);
        samlAttribute.getAttributeValues().add(dateTimeValue);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(samlAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).decode(null, samlAttribute, ruleset);
        
        Assert.assertTrue(attr instanceof IdPRequestedAttribute);
        Assert.assertEquals(attr.getId(), ATTR_NAME);
        Assert.assertTrue(((IdPRequestedAttribute) attr).isRequired());
        Assert.assertEquals(attr.getValues().size(), 1);
        Assert.assertEquals(((DateTimeAttributeValue)attr.getValues().get(0)).getValue().toString(), STRING_ISO);
    }
    
}
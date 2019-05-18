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

package net.shibboleth.idp.cas.attribute.transcoding.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.cas.attribute.AbstractCASAttributeTranscoder;
import net.shibboleth.idp.cas.attribute.Attribute;
import net.shibboleth.idp.cas.attribute.CASAttributeTranscoder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** {@link CASScopedStringAttributeTranscoder} unit test. */
public class CASScopedStringAttributeTranscoderTest {

    private AttributeTranscoderRegistryImpl registry;
    
    private final static String ATTR_ID = "foo";
    private final static String ATTR_NAME = "bar";
    private final static String STRING_1 = "Value The First";
    private final static String STRING_2 = "Second string the value is";
    private final static String SCOPE_1 = "scope1.example.org";
    private final static String SCOPE_2 = "scope2";
    private final static String DELIMITER = "#";

    @BeforeClass public void setUp() throws ComponentInitializationException {
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");
                
        final CASScopedStringAttributeTranscoder transcoder = new CASScopedStringAttributeTranscoder();
        transcoder.initialize();
        
        registry.setNamingRegistry(Collections.singletonMap(transcoder.getEncodedType(),
                new AbstractCASAttributeTranscoder.NamingFunction()));
        
        final Map<String,Object> ruleset1 = new HashMap<>();
        ruleset1.put(AttributeTranscoderRegistry.PROP_ID, ATTR_ID);
        ruleset1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset1.put(CASAttributeTranscoder.PROP_NAME, ATTR_NAME);
        ruleset1.put(CASScopedStringAttributeTranscoder.PROP_SCOPE_DELIMITER, DELIMITER);
        
        registry.setTranscoderRegistry(Collections.singletonList(new TranscodingRule(ruleset1)));
        
        registry.initialize();
    }
    
    @AfterClass public void tearDown() {
        registry.destroy();
        registry = null;
    }

    @Test public void emptyEncode() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getName(), ATTR_NAME);
        Assert.assertTrue(attr.getValues().isEmpty());
    }

    @Test public void emptyDecode() throws Exception {
        
        final Attribute casAttribute = new Attribute(ATTR_NAME);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(casAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.getTranscoder(ruleset).decode(null, casAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_ID);
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

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        TranscoderSupport.getTranscoder(ruleset).encode(null, inputAttribute, Attribute.class, ruleset);
    }
    
    @Test public void single() throws Exception {
        final List<IdPAttributeValue> values =
                Arrays.asList(new ByteAttributeValue(new byte[] {1, 2, 3,}),
                        new ScopedStringAttributeValue(STRING_1, SCOPE_1),
                        new StringAttributeValue(STRING_1),
                        new StringAttributeValue(STRING_1 + "@" + SCOPE_1));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);
        inputAttribute.setValues(values);
        
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getName(), ATTR_NAME);

        final Collection<String> children = attr.getValues();

        Assert.assertEquals(children.size(), 1, "Encoding one entry");

        final String child = children.iterator().next();

        Assert.assertEquals(child, STRING_1 + DELIMITER + SCOPE_1, "Input equals output");
    }
    
    @Test public void singleDecode() throws Exception {
        
        final Attribute casAttribute = new Attribute(ATTR_NAME);
        casAttribute.getValues().add(STRING_1 + DELIMITER + SCOPE_1);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(casAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.getTranscoder(ruleset).decode(null, casAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_ID);
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

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_ID);
        inputAttribute.setValues(values);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(inputAttribute, Attribute.class);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final Attribute attr = TranscoderSupport.<Attribute>getTranscoder(ruleset).encode(
                null, inputAttribute, Attribute.class, ruleset);

        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getName(), ATTR_NAME);

        final Collection<String> children = attr.getValues();
        Assert.assertEquals(children.size(), 2, "Encoding 2 entries");

        final Iterator<String> iter = children.iterator();
        final String child1 = iter.next();
        final String child2 = iter.next();
        
        final String value1 = STRING_1 + DELIMITER + SCOPE_1;
        final String value2 = STRING_2 + DELIMITER + SCOPE_2;

        Assert.assertTrue(child1.equals(value1) || child1.equals(value2));
        Assert.assertTrue(child2.equals(value1) || child2.equals(value2));
    }

    @Test public void multiDecode() throws Exception {
        
        final Attribute casAttribute = new Attribute(ATTR_NAME);
        casAttribute.getValues().add(STRING_1 + DELIMITER + SCOPE_1);
        casAttribute.getValues().add(STRING_2 + DELIMITER + SCOPE_2);
        casAttribute.getValues().add(STRING_2 + '@' + SCOPE_2);
        casAttribute.getValues().add(STRING_2);

        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(casAttribute);
        Assert.assertEquals(rulesets.size(), 1);
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final IdPAttribute attr = TranscoderSupport.getTranscoder(ruleset).decode(null, casAttribute, ruleset);
        
        Assert.assertNotNull(attr);
        Assert.assertEquals(attr.getId(), ATTR_ID);
        Assert.assertEquals(attr.getValues().size(), 2);

        final ScopedStringAttributeValue value1 = (ScopedStringAttributeValue) attr.getValues().get(0);
        final ScopedStringAttributeValue value2 = (ScopedStringAttributeValue) attr.getValues().get(1);
        Assert.assertTrue(STRING_1.equals(value1.getValue()) || STRING_1.equals(value2.getValue()));
        Assert.assertTrue(STRING_2.equals(value1.getValue()) || STRING_2.equals(value2.getValue()));
        Assert.assertTrue(SCOPE_1.equals(value1.getScope()) || SCOPE_1.equals(value2.getScope()));
        Assert.assertTrue(SCOPE_2.equals(value1.getScope()) || SCOPE_2.equals(value2.getScope()));
    }

}

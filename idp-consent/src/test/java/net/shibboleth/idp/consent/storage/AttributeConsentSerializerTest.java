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

package net.shibboleth.idp.consent.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.consent.AttributeConsent;
import net.shibboleth.idp.consent.logic.AttributeValuesHashFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.collect.Sets;


/** Unit tests for {@link AttributeConsentSerializer}. */
public class AttributeConsentSerializerTest {

    /** Class logger. */
    @Nonnull protected final Logger log = LoggerFactory.getLogger(AttributeConsentSerializerTest.class);
    
    private static final String CONTEXT = "_context";
    
    private static final String KEY = "_key";

    protected AttributeConsentSerializer serializer;

    protected IdPAttribute attribute1;

    protected IdPAttribute attribute2;

    protected IdPAttributeValue<?> value1;

    protected IdPAttributeValue<?> value2;

    protected IdPAttributeValue<?> value3;

    protected AttributeConsent consent1;

    protected AttributeConsent consent2;
    
    protected Map<String, AttributeConsent> consents;

    protected Function<Collection<IdPAttributeValue<?>>, String> attributeValuesHashFunction;

    @BeforeMethod public void setUp() {
        serializer = new AttributeConsentSerializer();

        attributeValuesHashFunction = new AttributeValuesHashFunction();

        value1 = new StringAttributeValue("value1");
        value2 = new StringAttributeValue("value2");

        attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(Sets.newHashSet(value1));

        attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(Sets.newHashSet(value1, value2));

        consent1 = new AttributeConsent();
        consent1.setAttributeId("attribute1");
        consent1.setValuesHash(attributeValuesHashFunction.apply(attribute1.getValues()));

        consent2 = new AttributeConsent();
        consent2.setAttributeId("attribute2");
        consent2.setValuesHash(attributeValuesHashFunction.apply(attribute2.getValues()));
        
        consents = new LinkedHashMap<>();
        consents.put(consent1.getAttributeId(), consent1);
        consents.put(consent2.getAttributeId(), consent2);
    }

    @Test public void testSimple() throws IOException {

        final String serialized = serializer.serialize(consents);
        
        final Map<String, AttributeConsent> deserialized = serializer.deserialize(1, CONTEXT, KEY, serialized, null);
        
        Assert.assertEquals(consents, deserialized);
    }

}

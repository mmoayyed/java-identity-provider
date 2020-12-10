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

package net.shibboleth.idp.consent.logic.impl;

import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;

/** {@link AttributeValuesHashFunction} unit test. */
public class AttributeValuesHashFunctionTest extends XMLObjectBaseTestCase {

    private AttributeValuesHashFunction function;

    @BeforeMethod public void setUp() {
        function = new AttributeValuesHashFunction();
    }

    @Test public void testNullInput() {
        Assert.assertNull(function.apply(null));
    }

    @Test public void testEmptyInput() {
        Assert.assertNull(function.apply(Collections.emptyList()));
    }

    @Test(enabled = true) public void testNullValue() {
        // NOTE Any change is an ODS drift
        assertEquals(function.apply(List.of(EmptyAttributeValue.NULL)), "QlRl6kTdT/0tD4h3xpwJn+hoFjZssUN15Bc6DO/CXws=");
    }

    @Test(enabled = true) public void testEmptyValue() {
        // NOTE Any change is an ODS drift
        assertEquals(function.apply(List.of(EmptyAttributeValue.ZERO_LENGTH)), "vwBWiidH5q5XZY3uEPcnJYeDTgqJnpV9WpmgWZS9wR4=");
    }

    @Test public void testSingleValue() {
        // NOTE Any change is an ODS drift
        final String hash = function.apply(ConsentTestingSupport.newAttributeMap().get("attribute1").getValues());
        assertEquals(hash, "qY1Ely22YLjD7hy4/HFSlErfjWNtVNJTZDral2Bs3Q8=");
    }

    @Test public void testMultipleValues() {
        final String hash = function.apply(ConsentTestingSupport.newAttributeMap().get("attribute2").getValues());
        assertEquals(hash, "w4A7kgpy8PAiMfNkM8yR68zLF9ngILQDWDy+n2l59zk=");
    }

    @Test public void testScoped() {
        // NOTE Any change is an ODS drift
        final IdPAttributeValue val = new ScopedStringAttributeValue("Value", "Scope");
        assertEquals(function.apply(Collections.singletonList(val)), "WFoLzGdi3WmUjhWe3Q6uSyoHVZJXukDWeOUb7CyH5V8=");
    }

    @Test public void testByte() {
        // NOTE Any change is an ODS drift
        final byte[] theBytes = {1,2,3};
        final IdPAttributeValue val = new ByteAttributeValue(theBytes);
        assertEquals(function.apply(Collections.singletonList(val)), "saP1UTQcyQPZHOPI6tVhVWMKOmB3BDCTn/l5QFSsyX4=");
    }

    @Test public void testXML() {
        // NOTE Any change is an ODS drift
        final XMLObjectBuilder<XSString> builder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().<XSString>getBuilderOrThrow(
                        XSString.TYPE_NAME);
        final XSString xmlString = builder.buildObject(XSString.TYPE_NAME);
        xmlString.setValue("value");
        final IdPAttributeValue val = new XMLObjectAttributeValue(xmlString);
        assertEquals(function.apply(Collections.singletonList(val)), "c+NqWOijlvFBpla4r1q3F0RkpYZK7phCNe2gKb0r57o=");
    }

    private IdPAttributeValue testAV(Object type) {
        return new IdPAttributeValue() {

            public Object getNativeValue() {
                return type;
            }

            public String getDisplayValue() {
                return "Display";
            }};
    }

    @Test public void unknownTypeValue() {
        assertEquals(function.apply(Collections.singletonList(testAV("42"))), "Lt6BAjtq4qQJ6ADEZKf/s5XZxzBh6mShY/UCphriugY=");
    }

    @Test public void unknownTypeNoValue() {
        assertEquals(function.apply(Collections.singletonList(testAV(null))), "xPtMT+sJsVtAtjNLzPrBBlfbY/yUsAQ7Ncxxc7Q5k70=");
    }
}

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

package net.shibboleth.idp.saml.impl.attribute.encoding;

import java.util.Collections;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml2.core.NameID;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link Saml2StringSubjectNameIDEncoder}.
 */
public class Saml2StringSubjectNameIDEncoderTest extends OpenSAMLInitBaseTestCase {

    @Test public void settersGetters() {
        final Saml2StringSubjectNameIDEncoder encoder = new Saml2StringSubjectNameIDEncoder();

        Assert.assertNull(encoder.getNameFormat());
        Assert.assertNull(encoder.getNameQualifier());

        encoder.setNameFormat("nameFormat");
        encoder.setNameQualifier("nameQualifier");

        Assert.assertEquals(encoder.getNameFormat(), "nameFormat");
        Assert.assertEquals(encoder.getNameQualifier(), "nameQualifier");
    }

    @Test public void equalsHash() {
        final Saml2StringSubjectNameIDEncoder enc1 = new Saml2StringSubjectNameIDEncoder();
        enc1.setNameFormat("nameFormat");
        enc1.setNameQualifier("nameQualifier");

        final Saml2StringSubjectNameIDEncoder enc2 = new Saml2StringSubjectNameIDEncoder();
        enc2.setNameFormat("nameFormat");
        enc2.setNameQualifier("nameQualifier");

        final Saml2StringSubjectNameIDEncoder enc3 = new Saml2StringSubjectNameIDEncoder();
        enc3.setNameFormat("nameQualifier");
        enc3.setNameQualifier("nameFormat");

        final Saml1StringSubjectNameIdentifierEncoder enc4 = new Saml1StringSubjectNameIdentifierEncoder();
        enc4.setNameFormat("nameFormat");
        enc4.setNameQualifier("nameQualifier");

        Assert.assertFalse(enc1.equals(null));
        Assert.assertFalse(enc1.equals(enc4));
        Assert.assertFalse(enc1.equals(enc3));
        Assert.assertTrue(enc1.equals(enc2));
        Assert.assertTrue(enc1.equals(enc1));

        Assert.assertEquals(enc1.hashCode(), enc2.hashCode());
        Assert.assertNotEquals(enc1.hashCode(), enc3.hashCode());
        Assert.assertNotEquals(enc1.hashCode(), enc4.hashCode());
    }

    @Test public void encode() throws AttributeEncodingException {
        IdPAttribute attribute = new IdPAttribute("id");
        attribute.setValues(Collections.singleton((AttributeValue) new StringAttributeValue("value")));

        final Saml2StringSubjectNameIDEncoder enc1 = new Saml2StringSubjectNameIDEncoder();

        NameID nameId = enc1.encode(attribute);
        Assert.assertEquals(nameId.getValue(), "value");
        Assert.assertNull(nameId.getFormat());
        Assert.assertNull(nameId.getNameQualifier());

        enc1.setNameFormat("nameFormat");
        enc1.setNameQualifier("nameQualifier");
        nameId = enc1.encode(attribute);
        Assert.assertEquals(nameId.getValue(), "value");
        Assert.assertEquals(nameId.getFormat(), "nameFormat");
        Assert.assertEquals(nameId.getNameQualifier(), "nameQualifier");
    }

    @Test(expectedExceptions = {AttributeEncodingException.class,}) public void empty()
            throws AttributeEncodingException {
        IdPAttribute attribute = new IdPAttribute("id");

        final Saml2StringSubjectNameIDEncoder enc1 = new Saml2StringSubjectNameIDEncoder();

        enc1.encode(attribute);
    }

    @Test(expectedExceptions = {AttributeEncodingException.class,}) public void nullValues()
            throws AttributeEncodingException {
        IdPAttribute attribute = new IdPAttribute("id");

        final Saml2StringSubjectNameIDEncoder enc1 = new Saml2StringSubjectNameIDEncoder();

        final AttributeValue empty = new AttributeValue<String>() {
            @Nonnull public String getValue() {
                return null;
            }
        };

        attribute.setValues(Collections.singleton(empty));
        enc1.encode(attribute);
    }

    @Test(expectedExceptions = {AttributeEncodingException.class,}) public void wrongType()
            throws AttributeEncodingException {
        IdPAttribute attribute = new IdPAttribute("id");
        final Saml2StringSubjectNameIDEncoder enc1 = new Saml2StringSubjectNameIDEncoder();
        final AttributeValue wrong = new AttributeValue<Integer>() {
            @Nonnull public Integer getValue() {
                return new Integer(3);
            }
        };

        attribute.setValues(Collections.singleton(wrong));
        enc1.encode(attribute);
    }

}

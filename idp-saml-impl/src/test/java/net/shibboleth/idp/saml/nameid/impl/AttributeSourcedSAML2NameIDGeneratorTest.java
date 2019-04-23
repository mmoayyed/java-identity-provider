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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.Collections;
import java.util.List;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml2.core.NameID;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Unit test for {@link AttributeSourcedSAML2NameIDGenerator}. */
public class AttributeSourcedSAML2NameIDGeneratorTest extends OpenSAMLInitBaseTestCase {

    /** The name we give the test attribute. */
    private final static String ATTR_NAME = "foo";

    private static SAMLObjectBuilder<NameIdentifier> saml1Builder;

    private static SAMLObjectBuilder<NameID> saml2Builder;

    /** test values. */
    private final static String NAME_1 = "NameId1";

    private final static String OTHERID = "NameOtherProtocol";

    private final static String QUALIFIER = "Qualifier";

    private static IdPAttributeValue<?> saml1NameIdFor(final String ident) {
        NameIdentifier id = saml1Builder.buildObject();

        id.setValue(ident);
        id.setFormat(NameID.X509_SUBJECT);
        id.setNameQualifier(QUALIFIER);
        return new XMLObjectAttributeValue(id);
    }

    private static IdPAttributeValue<?> saml2NameIdFor(final String ident) {
        NameID id = saml2Builder.buildObject();

        id.setValue(ident);
        id.setFormat(NameID.X509_SUBJECT);
        id.setNameQualifier(QUALIFIER);
        return new XMLObjectAttributeValue(id);
    }

    private AttributeSourcedSAML2NameIDGenerator generator;
    
    private ProfileRequestContext prc;

    @BeforeMethod public void initTest() throws ComponentInitializationException {

        generator = new AttributeSourcedSAML2NameIDGenerator();
        generator.setId("test");
        generator.setFormat(NameID.X509_SUBJECT);
        saml1Builder = (SAMLObjectBuilder<NameIdentifier>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameIdentifier>getBuilderOrThrow(
                        NameIdentifier.DEFAULT_ELEMENT_NAME);

        saml2Builder = (SAMLObjectBuilder<NameID>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameID>getBuilderOrThrow(
                        NameID.DEFAULT_ELEMENT_NAME);
        prc = new RequestContextBuilder().buildProfileRequestContext();
    }
    
    @Test(expectedExceptions = {ComponentInitializationException.class,}) public void testInvalidConfig()
            throws Exception {
        generator.initialize();
    }
    
    @Test public void testNoSource() throws ComponentInitializationException, SAMLException {
        generator.setAttributeSourceIds(Collections.singletonList("bar"));
        generator.initialize();
        Assert.assertNull(generator.generate(prc, generator.getFormat()));
    }

    @Test public void testWrongType() throws Exception {
        final int[] intArray = {1, 2, 3, 4};
        final var values = List.of(new IdPAttributeValue<Object>() {
                            public Object getValue() {
                                return intArray;
                            }
                            public String getDisplayValue() {
                                return intArray.toString();
                            }
                        }, saml1NameIdFor(OTHERID));

        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(values);
        prc.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class, true).setIdPAttributes(
                Collections.singleton(inputAttribute));

        generator.setAttributeSourceIds(Collections.singletonList(ATTR_NAME));
        generator.initialize();
        
        Assert.assertNull(generator.generate(prc, generator.getFormat()));
    }

    @Test public void testWrongFormat() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(List.of(saml2NameIdFor(NAME_1)));
        prc.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class, true).setIdPAttributes(
                Collections.singleton(inputAttribute));

        generator.setFormat(NameID.EMAIL);
        generator.setAttributeSourceIds(Collections.singletonList(ATTR_NAME));
        generator.initialize();
        Assert.assertNull(generator.generate(prc, generator.getFormat()));
    }
    
    @Test public void testNameIDValued() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(List.of(saml2NameIdFor(NAME_1)));
        prc.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class, true).setIdPAttributes(
                Collections.singleton(inputAttribute));

        generator.setAttributeSourceIds(Collections.singletonList(ATTR_NAME));
        generator.initialize();
        final NameID outputNameId = generator.generate(prc, generator.getFormat());

        Assert.assertNotNull(outputNameId);
        Assert.assertEquals(outputNameId.getValue(), NAME_1);
        Assert.assertEquals(outputNameId.getFormat(), NameID.X509_SUBJECT);
        Assert.assertEquals(outputNameId.getNameQualifier(), QUALIFIER);
    }

    @Test public void testMultiNameIDValued() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(List.of(saml2NameIdFor(OTHERID), saml1NameIdFor(NAME_1)));
        prc.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class, true).setIdPAttributes(
                Collections.singleton(inputAttribute));

        generator.setAttributeSourceIds(Collections.singletonList(ATTR_NAME));
        generator.initialize();
        final NameID outputNameId = generator.generate(prc, generator.getFormat());

        Assert.assertNotNull(outputNameId);
        Assert.assertEquals(outputNameId.getValue(), OTHERID);
        Assert.assertEquals(outputNameId.getFormat(), NameID.X509_SUBJECT);
        Assert.assertEquals(outputNameId.getNameQualifier(), QUALIFIER);
    }
    
    @Test public void testStringValued() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(List.of(new StringAttributeValue(NAME_1)));
        prc.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class, true).setIdPAttributes(
                Collections.singleton(inputAttribute));

        generator.setAttributeSourceIds(Collections.singletonList(ATTR_NAME));
        generator.initialize();
        final NameID outputNameId = generator.generate(prc, generator.getFormat());

        Assert.assertNotNull(outputNameId);
        Assert.assertEquals(outputNameId.getValue(), NAME_1);
        Assert.assertEquals(outputNameId.getFormat(), NameID.X509_SUBJECT);
        Assert.assertEquals(outputNameId.getNameQualifier(),
                prc.getSubcontext(RelyingPartyContext.class).getConfiguration().getResponderId(prc));
    }

    @Test public void testScopeValued() throws Exception {
        final IdPAttribute inputAttribute = new IdPAttribute(ATTR_NAME);
        inputAttribute.setValues(List.of(new ScopedStringAttributeValue(NAME_1, QUALIFIER)));
        prc.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class, true).setIdPAttributes(
                Collections.singleton(inputAttribute));

        generator.setAttributeSourceIds(Collections.singletonList(ATTR_NAME));
        generator.initialize();
        final NameID outputNameId = generator.generate(prc, generator.getFormat());

        Assert.assertNotNull(outputNameId);
        Assert.assertEquals(outputNameId.getValue(), NAME_1 + '@' + QUALIFIER);
        Assert.assertEquals(outputNameId.getFormat(), NameID.X509_SUBJECT);
        Assert.assertEquals(outputNameId.getNameQualifier(),
                prc.getSubcontext(RelyingPartyContext.class).getConfiguration().getResponderId(prc));
        Assert.assertEquals(outputNameId.getSPNameQualifier(),
                prc.getSubcontext(RelyingPartyContext.class).getRelyingPartyId());
    }
}
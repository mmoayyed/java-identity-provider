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

package net.shibboleth.idp.test.flows.saml2;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.test.flows.AbstractFlowTest;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.net.IPRange;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.AttributeValue;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.EncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.testng.Assert;

import com.google.common.net.InetAddresses;

/**
 * SAML 2 {@link Response} validator.
 */
@SuppressWarnings({"javadoc", "null"})
public class SAML2TestResponseValidator extends SAML2TestStatusResponseTypeValidator {

    /** Authentication context class reference. */
    @Nonnull public String authnContextClassRef = AuthnContext.PPT_AUTHN_CTX;

    /** SP credential. */
    @Nullable public Credential spCredential;

    /** Expected name identifier. */
    @Nonnull public NameID nameID;

    /** Expected subject confirmation method. */
    @Nonnull public String subjectConfirmationMethod = SubjectConfirmation.METHOD_BEARER;
    
    /** Expected subject confirmation data address range for IPv4 addresses. Defaults to "127.0.0.1/32". */
    @Nonnull public List<IPRange> subjectConfirmationDataAddressRanges = new ArrayList<>(Arrays.asList(IPRange.parseCIDRBlock("127.0.0.1/32")));

    /** Expected subject confirmation data address range for IPv6 addresses. Defaults to "::1/128". */
    @Nonnull public IPRange subjectConfirmationDataAddressRangeV6 = IPRange.parseCIDRBlock("::1/128");

    /** Whether authn statements should be validated. */
    public boolean validateAuthnStatements = true;

    /** Whether subject confirmation data should be validated. */
    public boolean validateSubjectConfirmationData = true;

    /** Whether attributes were limited by designators. */
    public boolean usedAttributeDesignators = false;

    /** Expected attributes. */
    @Nonnull public List<Attribute> expectedAttributes;

    /** Expected attributes. */
    @Nonnull public List<Attribute> expectedDesignatedAttributes;

    /** Expected schacHomeOrganization attribute. */
    @Nonnull public Attribute homeOrgAttribute;
    
    /** Expected uid attribute. */
    @Nonnull public Attribute uidAttribute;

    /** Expected eppn attribute. */
    @Nonnull public Attribute eppnAttribute;

    /** Expected mail attribute. */
    @Nonnull public Attribute mailAttribute;

    /** Expected eduPersonScopedAffiliation attribute. */
    @Nonnull public Attribute eduPersonScopedAffiliationAttribute;

    /** Constructor. */
    public SAML2TestResponseValidator() {
        final SAMLObjectBuilder<NameID> builder = (SAMLObjectBuilder<NameID>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameID>ensureBuilder(
                        NameID.DEFAULT_ELEMENT_NAME);
        nameID = builder.buildObject();
        nameID.setFormat(NameID.TRANSIENT);
        nameID.setNameQualifier(idpEntityID);
        nameID.setSPNameQualifier(spEntityID);

        buildExpectedAttributes();
        // fool code analyzer
        uidAttribute = Constraint.isNotNull(uidAttribute, "");
        homeOrgAttribute = Constraint.isNotNull(homeOrgAttribute, "");
        eppnAttribute = Constraint.isNotNull(eppnAttribute, "");
        mailAttribute = Constraint.isNotNull(mailAttribute, "");
        eduPersonScopedAffiliationAttribute = Constraint.isNotNull(eduPersonScopedAffiliationAttribute, "");
        expectedDesignatedAttributes = Constraint.isNotNull(expectedDesignatedAttributes, "");
        expectedAttributes = Constraint.isNotNull(expectedAttributes, "");
    }

    /** Build expected attributes. */
    protected void buildExpectedAttributes() {

        final SAMLObjectBuilder<Attribute> builder = (SAMLObjectBuilder<Attribute>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Attribute>ensureBuilder(
                        Attribute.DEFAULT_ELEMENT_NAME);

        final XMLObjectBuilder<XSAny> anyBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().<XSAny>ensureBuilder(
                        XSAny.TYPE_NAME);
        
        // the expected schacHomeOrganization attribute
        homeOrgAttribute = builder.buildObject();
        homeOrgAttribute.setName("urn:oid:1.3.6.1.4.1.25178.1.2.9");
        homeOrgAttribute.setNameFormat(Attribute.URI_REFERENCE);
        homeOrgAttribute.setFriendlyName("schacHomeOrganization");
        final XSAny orgValue = anyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        orgValue.setTextContent("example.org");
        homeOrgAttribute.getAttributeValues().add(orgValue);
        
        // the expected uid attribute
        uidAttribute = builder.buildObject();
        uidAttribute.setName("urn:oid:0.9.2342.19200300.100.1.1");
        uidAttribute.setNameFormat(Attribute.URI_REFERENCE);
        uidAttribute.setFriendlyName("uid");
        final XSAny uidValue = anyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        uidValue.setTextContent("jdoe");
        uidAttribute.getAttributeValues().add(uidValue);

        // the expected eppn attribute
        eppnAttribute = builder.buildObject();
        eppnAttribute.setName("urn:oid:1.3.6.1.4.1.5923.1.1.1.6");
        eppnAttribute.setNameFormat(Attribute.URI_REFERENCE);
        eppnAttribute.setFriendlyName("eduPersonPrincipalName");
        final XSAny eppnValue = anyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        eppnValue.setTextContent("jdoe@example.org");
        eppnAttribute.getAttributeValues().add(eppnValue);

        // the expected mail attribute
        mailAttribute = builder.buildObject();
        mailAttribute.setName("urn:oid:0.9.2342.19200300.100.1.3");
        mailAttribute.setNameFormat(Attribute.URI_REFERENCE);
        mailAttribute.setFriendlyName("mail");
        final XSAny mailValue = anyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        mailValue.setTextContent("jdoe@example.org");
        mailAttribute.getAttributeValues().add(mailValue);

        // the expected eduPersonScopedAffiliation attribute
        eduPersonScopedAffiliationAttribute = builder.buildObject();
        eduPersonScopedAffiliationAttribute.setName("urn:oid:1.3.6.1.4.1.5923.1.1.1.9");
        eduPersonScopedAffiliationAttribute.setNameFormat(Attribute.URI_REFERENCE);
        eduPersonScopedAffiliationAttribute.setFriendlyName("eduPersonScopedAffiliation");
        final XSAny eduPersonScopedAffiliationAttributeValue = anyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        eduPersonScopedAffiliationAttributeValue.setTextContent("member@example.org");
        eduPersonScopedAffiliationAttribute.getAttributeValues().add(eduPersonScopedAffiliationAttributeValue);

        expectedAttributes = new ArrayList<>();
        expectedAttributes.add(homeOrgAttribute);
        expectedAttributes.add(uidAttribute);
        expectedAttributes.add(mailAttribute);
        expectedAttributes.add(eppnAttribute);
        expectedAttributes.add(eduPersonScopedAffiliationAttribute);

        expectedDesignatedAttributes = new ArrayList<>();
        expectedDesignatedAttributes.add(mailAttribute);
}

    private Assertion decryptAssertion(@Nonnull final EncryptedAssertion encrypted) throws DecryptionException {
        ArrayList<EncryptedKeyResolver> resolverChain = new ArrayList<>();
        resolverChain.add(new InlineEncryptedKeyResolver());
        resolverChain.add(new EncryptedElementTypeEncryptedKeyResolver());
        final ChainingEncryptedKeyResolver chain = new ChainingEncryptedKeyResolver(resolverChain);
        assert spCredential!=null;
        final Decrypter decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(spCredential), chain);
        return decrypter.decrypt(encrypted);
    }

    /**
     * Validate the response.
     * <p>
     * Calls validate methods :
     * <ul>
     * <li>{@link #validateSubject(Subject)}</li>
     * <li>{@link #validateConditions(Assertion)}</li>
     * <li>{@link #validateAuthnStatements(Assertion)}</li>
     * <li>{@link #validateAttributeStatements(Assertion)}</li>
     * </ul>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertResponse(org.opensaml.saml.saml2.core.StatusResponseType)}</li>
     * <li>{@link #assertStatus(Status)}</li>
     * <li>{@link #assertAssertions(List)}</li>
     * <li>{@link #assertAssertion(Assertion)}</li>
     * </ul>
     * 
     * @param response the flow execution result
     */
    public void validateResponse(@Nonnull final Response response) {

        super.validateResponse(response);

        // short circuit validation upon error
        if (statusCode != StatusCode.SUCCESS) {
            return;
        }

        if (!response.getEncryptedAssertions().isEmpty()) {
            try {
                response.getAssertions().add(decryptAssertion(response.getEncryptedAssertions().get(0)));
                response.getEncryptedAssertions().clear();
            } catch (DecryptionException e) {
                Assert.fail(e.getMessage());
            }
        }

        final List<Assertion> assertions = response.getAssertions();
        assertAssertions(assertions);

        final Assertion assertion = assertions.get(0);
        assertAssertion(assertion);

        validateSubject(assertion.getSubject());

        validateConditions(assertion);

        if (validateAuthnStatements) {
            validateAuthnStatements(assertion);
        }

        validateAttributeStatements(assertion);
    }

    /**
     * Validate the subject.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertSubject(Subject)}</li>
     * <li>{@link #assertNameID(NameID)}</li>
     * <li>{@link #assertSubjectConfirmations(List)}</li>
     * <li>{@link #assertSubjectConfirmation(SubjectConfirmation)}</li>
     * <li>{@link #assertSubjectConfirmationMethod(SubjectConfirmation)}</li>
     * <li>{@link #assertSubjectConfirmationData(SubjectConfirmationData)}</li>
     * </ul>
     * 
     * @param subject the subject
     */
    public void validateSubject(@Nullable final Subject subject) {
        assertSubject(subject);
        assertNameID(subject != null ? subject.getNameID() : null);
        assertSubjectConfirmations(subject != null ? subject.getSubjectConfirmations() : null);
        final SubjectConfirmation subjectConfirmation = subject != null ? subject.getSubjectConfirmations().get(0) : null;
        assertSubjectConfirmation(subjectConfirmation);
        assertSubjectConfirmationMethod(subjectConfirmation);
        if (validateSubjectConfirmationData) {
            assertSubjectConfirmationData(subjectConfirmation != null ? subjectConfirmation.getSubjectConfirmationData() : null);
        }
    }

    /**
     * Validate the assertion conditions.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertConditions(Conditions)}</li>
     * <li>{@link #assertAudienceRestrictions(List)}</li>
     * <li>{@link #assertAudienceRestriction(AudienceRestriction)}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateConditions(@Nullable Assertion assertion) {
        assert assertion!=null;

        final Conditions conditions = assertion.getConditions();
        assertConditions(conditions);

        assert conditions != null;
        final List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
        assertAudienceRestrictions(audienceRestrictions);

        assertAudienceRestriction(audienceRestrictions.get(0));
    }

    /**
     * Validate the assertion authentication statements.
     * 
     * @param assertion the assertion
     */
    public void validateAuthnStatements(@Nullable final Assertion assertion) {
        assert assertion!=null;

        final List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
        assertAuthnStatements(authnStatements);

        final AuthnStatement authnStatement = authnStatements.get(0);
        assertAuthnStatement(authnStatement);
        assertAuthnContextClassRef(authnStatement.getAuthnContext());
    }

    /**
     * Validate the assertion attribute statements.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAttributeStatements(List)}</li>
     * <li>{@link #assertAttributeStatement(AttributeStatement)}</li>
     * <li>{@link #assertAttributes(List)}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateAttributeStatements(@Nullable final Assertion assertion) {
        assert assertion!=null;

        final List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        assertAttributeStatements(attributeStatements);

        final AttributeStatement attributeStatement = attributeStatements.get(0);
        assertAttributeStatement(attributeStatement);

        final List<Attribute> attributes = attributeStatement.getAttributes();
        assertAttributes(attributes);
    }

    /**
     * Assert that a single assertion is present.
     * 
     * @param assertions the assertions
     */
    public void assertAssertions(@Nullable List<Assertion> assertions) {
        assert assertions!=null;
        Assert.assertFalse(assertions.isEmpty());
        Assert.assertEquals(assertions.size(), 1);
        Assert.assertNotNull(assertions.get(0));
    }

    /**
     * Assert that :
     * <ul>
     * <li>the assertion ID is not null nor empty</li>
     * <li>the assertion issue instant is not null</li>
     * <li>the assertion version is {@link SAMLVersion#VERSION_20}</li>
     * <li>the issuer is the expected IdP entity ID</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void assertAssertion(@Nullable final Assertion assertion) {
        assert assertion!=null;
        final String id = assertion.getID();
        Assert.assertTrue(id != null && !id.isEmpty());
        Assert.assertNotNull(assertion.getIssueInstant());
        Assert.assertEquals(assertion.getVersion(), SAMLVersion.VERSION_20);
        final Issuer issuer = assertion.getIssuer();
        assert issuer != null;
        Assert.assertEquals(issuer.getValue(), idpEntityID);
    }

    /**
     * Assert that the subject has a nameID and subject confirmations.
     * 
     * @param subject the subject
     */
    public void assertSubject(@Nullable final Subject subject) {
        assert subject!=null;
        Assert.assertNotNull(subject.getNameID());
        Assert.assertNotNull(subject.getSubjectConfirmations());
    }

    /**
     * Assert that a single subject confirmation is present.
     * 
     * @param subjectConfirmations the subject confirmations
     */
    public void assertSubjectConfirmations(@Nullable final List<SubjectConfirmation> subjectConfirmations) {
        assert subjectConfirmations!=null;
        Assert.assertEquals(subjectConfirmations.size(), 1);
    }

    /**
     * Assert that the subject confirmation has a confirmation method.
     * 
     * @param subjectConfirmation the subject confirmation
     */
    public void assertSubjectConfirmation(@Nullable final SubjectConfirmation subjectConfirmation) {
        assert subjectConfirmation!=null;
        Assert.assertNotNull(subjectConfirmation.getMethod());
    }

    /**
     * Assert that the subject confirmation method is {@link SubjectConfirmation#METHOD_BEARER}.
     * 
     * @param method the subject confirmation
     */
    public void assertSubjectConfirmationMethod(@Nullable final SubjectConfirmation method) {
        assert method!=null;
        Assert.assertEquals(method.getMethod(), subjectConfirmationMethod);
    }

    /**
     * Assert that :
     * <ul>
     * <li>the subject confirmation data address is in the expected range</li>
     * <li>the subject confirmation data NotOnOrAfter is not null</li>
     * <li>the subject confirmation data recipient is not null nor empty</li>
     * </ul>
     * 
     * @param subjectConfirmationData the subject confirmation data
     */
    public void assertSubjectConfirmationData(@Nullable final SubjectConfirmationData subjectConfirmationData) {
        assert subjectConfirmationData!=null;
        final InetAddress address = InetAddresses.forString(subjectConfirmationData.getAddress());
        if (address instanceof Inet4Address) {
            boolean matches = false;
            for (final IPRange subjectConfirmationDataAddressRange : subjectConfirmationDataAddressRanges) {
                if (subjectConfirmationDataAddressRange.contains(address)) {
                    matches = true;
                }
            }
            Assert.assertTrue(matches, "SubjectConfirmationData Address does not match");
        } else if(address instanceof Inet6Address) {
            Assert.assertTrue(subjectConfirmationDataAddressRangeV6.contains(address));
        } else {
            Assert.fail("Unable to determine whether the IP address is V4 or V6");
        }
        // TODO only in some cases ? Assert.assertNotNull(subjectConfirmationData.getNotBefore());
        Assert.assertNotNull(subjectConfirmationData.getNotOnOrAfter());
        Assert.assertNotNull(subjectConfirmationData.getRecipient());
    }

    /**
     * Assert that:
     * 
     * <ul>
     * <li>the NameID is not null</li>
     * <li>the NameID value is not null</li>
     * <li>the NameID format is the expected format</li>
     * <li>the NameID value is the expected value if the format is not transient</li>
     * <li>the NameID name qualifier is the expected name qualifier</li>
     * <li>the NameID SP name qualifier is the expected SP name qualifier</li>
     * </ul>
     * 
     * @param id the NameID
     */
    public void assertNameID(@Nullable final NameID id) {
        assert id!=null;
        Assert.assertNotNull(id.getValue());
        final String format = id.getFormat();
        if (format != null && !format.equals(NameID.TRANSIENT)) {
            Assert.assertEquals(id.getValue(), nameID.getValue());
        }
        Assert.assertEquals(id.getFormat(), nameID.getFormat());
        Assert.assertEquals(id.getNameQualifier(), nameID.getNameQualifier());
        Assert.assertEquals(id.getSPNameQualifier(), nameID.getSPNameQualifier());
    }

    /**
     * Assert that the conditions has NotBefore and NotOnOrAfter attributes.
     * 
     * @param conditions the conditions
     */
    public void assertConditions(@Nullable final Conditions conditions) {
        assert conditions!=null;
        Assert.assertNotNull(conditions.getNotBefore());
        Assert.assertNotNull(conditions.getNotOnOrAfter());
        // TODO check time via some range ?
    }

    /**
     * Assert that a single audience restriction is present.
     * 
     * @param audienceRestrictions the audience restrictions
     */
    public void assertAudienceRestrictions(@Nullable final List<AudienceRestriction> audienceRestrictions) {
        assert audienceRestrictions!=null;
        Assert.assertEquals(audienceRestrictions.size(), 1);
    }

    /**
     * Assert that the audience restriction has a single audience whose URI is the expected SP entity ID.
     * 
     * @param audienceRestriction the audience restriction
     */
    public void assertAudienceRestriction(@Nullable final AudienceRestriction audienceRestriction) {
        assert audienceRestriction!=null;

        final List<Audience> audiences = audienceRestriction.getAudiences();
        Assert.assertEquals(audiences.size(), 1);

        final Audience audience = audiences.get(0);
        Assert.assertEquals(audience.getURI(), AbstractFlowTest.SP_ENTITY_ID);
    }

    /**
     * Assert that a single authn statement is present.
     * 
     * @param authnStatements the authn statements
     */
    public void assertAuthnStatements(@Nullable final List<AuthnStatement> authnStatements) {
        assert authnStatements!=null;
        Assert.assertEquals(authnStatements.size(), 1);
        Assert.assertNotNull(authnStatements.get(0));
    }

    /**
     * Assert that the authn statement has an authn instant and authn context class ref.
     * 
     * @param authnStatement the authn statement
     */
    public void assertAuthnStatement(@Nonnull final AuthnStatement authnStatement) {
        assert authnStatement!=null;
        Assert.assertNotNull(authnStatement.getAuthnInstant());
        // TODO check authn instant time ?
        
        final AuthnContext context = authnStatement.getAuthnContext();
        assert context != null;
        Assert.assertNotNull(context.getAuthnContextClassRef());
    }

    /**
     * Assert that the authn context class ref is {@link AuthnContext#IP_AUTHN_CTX}.
     * 
     * @param authnContext the authn context
     */
    public void assertAuthnContextClassRef(@Nullable final AuthnContext authnContext) {
        assert authnContext != null;
        final AuthnContextClassRef ref = authnContext.getAuthnContextClassRef();
        assert ref != null;
        Assert.assertEquals(ref.getURI(), authnContextClassRef);
    }

    /**
     * Assert that a single attribute statement is present.
     * 
     * @param attributeStatements the attribute statements
     */
    public void assertAttributeStatements(@Nullable final List<AttributeStatement> attributeStatements) {
        assert attributeStatements!=null;
        Assert.assertFalse(attributeStatements.isEmpty());
        Assert.assertEquals(attributeStatements.size(), 1);
        Assert.assertNotNull(attributeStatements.get(0));
    }

    /**
     * Assert that the attribute statement has attributes.
     * 
     * @param attributeStatement the attribute statement
     */
    public void assertAttributeStatement(@Nullable final AttributeStatement attributeStatement) {
        assert attributeStatement!=null;
        Assert.assertNotNull(attributeStatement.getAttributes());
    }

    /**
     * Assert that the attributes from the response match the expected attributes.
     * 
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAttributeName(Attribute, String, String, String)}</li>
     * <li>{@link #assertAttributeValue(Attribute, String)}</li>
     * </ul>
     * 
     * @param attributes the attributes
     */
    public void assertAttributes(@Nullable final List<Attribute> attributes) {
        assert attributes!=null;
        Assert.assertFalse(attributes.isEmpty());
        Assert.assertEquals(attributes.size(), usedAttributeDesignators ? expectedDesignatedAttributes.size() : expectedAttributes.size());

        // Ignore attribute ordering
        final Map<String, Attribute> actualAttributes = new HashMap<>();
        for(final Attribute attribute : attributes) {
            actualAttributes.put(attribute.getName(), attribute);
        }
        
        for (int i = 0; i < (usedAttributeDesignators ? expectedDesignatedAttributes.size() : expectedAttributes.size()); i++) {
            final Attribute expectedAttribute = usedAttributeDesignators ? expectedDesignatedAttributes.get(i) : expectedAttributes.get(i);
            final Attribute actualAttribute = actualAttributes.get(expectedAttribute.getName());
            Assert.assertNotNull(actualAttribute);
            assertAttributeName(actualAttribute, expectedAttribute.getName(), expectedAttribute.getNameFormat(),
                    expectedAttribute.getFriendlyName());
            final XSAny any = ((XSAny) expectedAttribute.getAttributeValues().get(0));
            assert any!=null;
            final String text = any.getTextContent();
            assert text!=null;
            assertAttributeValue(actualAttribute, text);
        }
    }

    /**
     * Assert that the attribute name, name format, and friendly name are the supplied names.
     * 
     * @param attribute the attribute
     * @param name the attribute name
     * @param nameFormat the attribute name format
     * @param friendlyName the attribute friendly name
     */
    public void assertAttributeName(@Nullable final Attribute attribute, @Nullable final String name,
            @Nullable final String nameFormat, @Nullable final String friendlyName) {
        assert attribute!=null;
        Assert.assertEquals(attribute.getName(), name);
        Assert.assertEquals(attribute.getNameFormat(), nameFormat);
        Assert.assertEquals(attribute.getFriendlyName(), friendlyName);
    }

    /**
     * Assert that the attribute value is the supplied String value.
     * 
     * @param attribute the attribute
     * @param attributeValue the attribute value
     */
    public void assertAttributeValue(@Nonnull final Attribute attribute, @Nonnull final String attributeValue) {
        Assert.assertEquals(attribute.getAttributeValues().size(), 1);
        Assert.assertTrue(attribute.getAttributeValues().get(0) instanceof XSAny);
        Assert.assertEquals(((XSAny) attribute.getAttributeValues().get(0)).getTextContent(), attributeValue);
    }

}

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

package net.shibboleth.idp.saml.impl.profile.saml1;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.profile.ActionTestingSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.saml.impl.attribute.encoding.Saml1StringAttributeEncoder;
import net.shibboleth.idp.saml.profile.SamlEventIds;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.impl.XSStringImpl;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.Response;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AddAttributeStatementToAssertion} unit test. */
public class AddAttributeStatementToAssertionTest extends OpenSAMLInitBaseTestCase {

    /** The test namespace. */
    private final static String MY_NAMESPACE = "myNamespace";

    /** The name of the first attribute. */
    private final static String MY_NAME_1 = "myName1";

    /** The name of the second attribute. */
    private final static String MY_NAME_2 = "myName2";

    /** The value of the first attribute. */
    private final static String MY_VALUE_1 = "myValue1";

    /** The value of the second attribute. */
    private final static String MY_VALUE_2 = "myValue2";

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action errors out properly if there is no response. */
    @Test public void testNoResponse() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        AttributeContext attribCtx = new AttributeContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, SamlEventIds.NO_RESPONSE);
    }

    /** Test that the action errors out properly if there is no attribute context. */
    @Test public void testNoAttributeContext() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_ATTRIBUTE_CTX);
    }

    /** Test that the action errors out properly if the attribute context does not contain attributes. */
    @Test public void testNoAttributes() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        AttributeContext attribCtx = new AttributeContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);
    }

    /** Test that the action ignores attribute encoding errors. */
    @Test public void testIgnoreAttributeEncodingErrors() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        MockSaml1StringAttributeEncoder attributeEncoder = new MockSaml1StringAttributeEncoder();

        IdPAttribute attribute = new IdPAttribute(MY_NAME_1);
        attribute.setValues(Arrays.asList((AttributeValue) new StringAttributeValue(MY_VALUE_1)));

        Collection collection = (Collection<AttributeEncoder>) Arrays.asList((AttributeEncoder) attributeEncoder);
        attribute.setEncoders(collection);

        AttributeContext attribCtx = new AttributeContext();
        attribCtx.setIdPAttributes(Arrays.asList(attribute));

        ((RelyingPartyContext) profileCtx.getSubcontext(RelyingPartyContext.class)).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.setIgnoringUnencodableAttributes(true);
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);
    }

    /** Test that the action returns the correct transition when an attribute encoding error occurs. */
    @Test public void failOnAttributeEncodingErrors() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        MockSaml1StringAttributeEncoder attributeEncoder = new MockSaml1StringAttributeEncoder();

        IdPAttribute attribute = new IdPAttribute(MY_NAME_1);
        attribute.setValues(Arrays.asList((AttributeValue) new StringAttributeValue(MY_VALUE_1)));

        Collection collection = (Collection<AttributeEncoder>) Arrays.asList((AttributeEncoder) attributeEncoder);
        attribute.setEncoders(collection);

        AttributeContext attribCtx = new AttributeContext();
        attribCtx.setIdPAttributes(Arrays.asList(attribute));

        ((RelyingPartyContext) profileCtx.getSubcontext(RelyingPartyContext.class)).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, SamlEventIds.UNABLE_ENCODE_ATTRIBUTE);
    }

    @Test public void testNonResponseOutboundMessage() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(new String()).buildProfileRequestContext();

        AttributeContext attribCtx = new AttributeContext();

        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        try {
            action.doExecute(new MockRequestContext(), profileCtx);
            Assert.fail();
        } catch (ClassCastException e) {
            // ok
        }
    }

    /**
     * Test that the attribute statement is correctly added as a new assertion of a response already containing an
     * assertion.
     */
    @Test public void testAddedAttributeStatement() throws Exception {

        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        ((Response) profileCtx.getOutboundMessageContext().getMessage()).getAssertions().add(
                Saml1ActionTestingSupport.buildAssertion());

        AttributeContext attribCtx = buildAttributeContext();
        ((RelyingPartyContext) profileCtx.getSubcontext(RelyingPartyContext.class)).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.setStatementInOwnAssertion(true);
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(profileCtx.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileCtx.getOutboundMessageContext().getMessage() instanceof Response);

        Response response = (Response) profileCtx.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 2);

        for (Assertion assertion : response.getAssertions()) {
            if (!assertion.getAttributeStatements().isEmpty()) {
                Assert.assertNotNull(assertion.getAttributeStatements());
                Assert.assertEquals(assertion.getAttributeStatements().size(), 1);

                AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
                testAttributeStatement(attributeStatement);
            }
        }
    }

    /** Test that the attribute statement is correctly added to an assertion which already exists in the response. */
    @Test public void testAssertionInResponse() throws Exception {

        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        ((Response) profileCtx.getOutboundMessageContext().getMessage()).getAssertions().add(
                Saml1ActionTestingSupport.buildAssertion());

        AttributeContext attribCtx = buildAttributeContext();
        ((RelyingPartyContext) profileCtx.getSubcontext(RelyingPartyContext.class)).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(profileCtx.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileCtx.getOutboundMessageContext().getMessage() instanceof Response);

        Response response = (Response) profileCtx.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);

        Assertion assertion = response.getAssertions().get(0);
        Assert.assertNotNull(assertion.getAttributeStatements());
        Assert.assertEquals(assertion.getAttributeStatements().size(), 1);

        AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);

        testAttributeStatement(attributeStatement);
    }

    /**
     * Test that the attribute statement is correctly added to a newly created assertion of the response which
     * originally contained no assertions.
     */
    @Test public void testNoAssertionInResponse() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .buildProfileRequestContext();

        AttributeContext attribCtx = buildAttributeContext();
        ((RelyingPartyContext) profileCtx.getSubcontext(RelyingPartyContext.class)).addSubcontext(attribCtx);

        AddAttributeStatementToAssertion action = new AddAttributeStatementToAssertion();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(profileCtx.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileCtx.getOutboundMessageContext().getMessage() instanceof Response);

        Response response = (Response) profileCtx.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);

        Assertion assertion = response.getAssertions().get(0);
        Assert.assertNotNull(assertion.getAttributeStatements());
        Assert.assertEquals(assertion.getAttributeStatements().size(), 1);

        AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
        testAttributeStatement(attributeStatement);
    }

    /**
     * Build the attribute context containing two test attributes to be used as an input to the action.
     * 
     * @return the attribute context to be used as an input to the action
     * @throws ComponentInitializationException thrown if the attribute encoders can not be initialized
     */
    private AttributeContext buildAttributeContext() throws ComponentInitializationException {

        IdPAttribute attribute1 = new IdPAttribute(MY_NAME_1);
        attribute1.setValues(Arrays.asList((AttributeValue) new StringAttributeValue(MY_VALUE_1)));

        Saml1StringAttributeEncoder attributeEncoder1 = new Saml1StringAttributeEncoder();
        attributeEncoder1.setName(MY_NAME_1);
        attributeEncoder1.setNamespace(MY_NAMESPACE);
        attributeEncoder1.initialize();

        Collection collection1 = (Collection<AttributeEncoder>) Arrays.asList((AttributeEncoder) attributeEncoder1);
        attribute1.setEncoders(collection1);

        IdPAttribute attribute2 = new IdPAttribute(MY_NAME_2);
        attribute2.setValues(Arrays.asList((AttributeValue) new StringAttributeValue(MY_VALUE_2)));

        Saml1StringAttributeEncoder attributeEncoder2 = new Saml1StringAttributeEncoder();
        attributeEncoder2.setName(MY_NAME_2);
        attributeEncoder2.setNamespace(MY_NAMESPACE);
        attributeEncoder2.initialize();

        Collection collection2 = (Collection<AttributeEncoder>) Arrays.asList((AttributeEncoder) attributeEncoder2);
        attribute2.setEncoders(collection2);

        AttributeContext attribCtx = new AttributeContext();
        attribCtx.setIdPAttributes(Arrays.asList(attribute1, attribute2));

        return attribCtx;
    }

    /**
     * Test that the attribute statement returned from the action is correct.
     * 
     * @param attributeStatement the attribute statement returned from the action to test
     */
    private void testAttributeStatement(AttributeStatement attributeStatement) {

        Assert.assertNotNull(attributeStatement.getAttributes());
        Assert.assertEquals(attributeStatement.getAttributes().size(), 2);

        for (Attribute samlAttr : attributeStatement.getAttributes()) {
            Assert.assertNotNull(samlAttr.getAttributeValues());
            Assert.assertEquals(samlAttr.getAttributeValues().size(), 1);
            XMLObject xmlObject = samlAttr.getAttributeValues().get(0);
            Assert.assertTrue(xmlObject instanceof XSStringImpl);
            if (samlAttr.getAttributeName().equals(MY_NAME_1)) {
                Assert.assertEquals(((XSStringImpl) xmlObject).getValue(), MY_VALUE_1);
            } else if (samlAttr.getAttributeName().equals(MY_NAME_2)) {
                Assert.assertEquals(((XSStringImpl) xmlObject).getValue(), MY_VALUE_2);
            } else {
                Assert.fail("Incorrect attribute name.");
            }
        }
    }

    /** A mock SAML1 string attribute encoder which always throws an {@link AttributeEncodingException}. */
    private class MockSaml1StringAttributeEncoder extends Saml1StringAttributeEncoder {

        /** {@inheritDoc} */
        @Nullable public Attribute encode(@Nonnull final IdPAttribute attribute)
                throws AttributeEncodingException {
            throw new AttributeEncodingException("Always thrown.");
        }
    }

}
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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.BasicNamingFunction;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML2AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAML2AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.impl.SAML2StringAttributeTranscoder;
import net.shibboleth.idp.saml.saml2.profile.testing.SAML2ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.MockApplicationContext;
import net.shibboleth.shared.testing.MockReloadableService;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.impl.XSStringImpl;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AddAttributeStatementToAssertion} unit test. */
public class AddAttributeStatementToAssertionTest extends OpenSAMLInitBaseTestCase {

    /** The test namespace. */
    private final static String MY_NAMESPACE = "myNamespace";

    /** The name of the first attribute. */
    private final static String MY_NAME_1 = "myName1";

    /** The name of the second attribute. */
    private final static String MY_NAME_2 = "myName2";

    /** The name of the third attribute. */
    private final static String MY_NAME_3 = "myName3";

    /** The second name of the first attribute. */
    private final static String MY_ALTNAME_1 = "myAltName1";

    /** The value of the first attribute. */
    private final static String MY_VALUE_1 = "myValue1";

    /** The value of the second attribute. */
    private final static String MY_VALUE_2 = "myValue2";
    
    private RequestContext rc;
    
    private ProfileRequestContext prc;
    
    private AddAttributeStatementToAssertion action;
    
    private AttributeTranscoderRegistryImpl registry;
    
    /**
     * Set up for tests.
     * 
     * @throws ComponentInitializationException on error
     */
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().setOutboundMessage(
                SAML2ActionTestingSupport.buildResponse()).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");
        
        registry.setNamingRegistry(Collections.singletonList(
                new BasicNamingFunction<>(Attribute.class, new AbstractSAML2AttributeTranscoder.NamingFunction())));

        final SAML2StringAttributeTranscoder transcoder = new SAML2StringAttributeTranscoder();
        transcoder.initialize();
        
        final Map<String,Object> rule1_1 = new HashMap<>();
        rule1_1.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule1_1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule1_1.put(SAML2AttributeTranscoder.PROP_NAME, MY_NAME_1);
        rule1_1.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, MY_NAMESPACE);

        final Map<String,Object> rule1_2 = new HashMap<>();
        rule1_2.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule1_2.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule1_2.put(SAML2AttributeTranscoder.PROP_NAME, MY_ALTNAME_1);
        rule1_2.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, MY_NAMESPACE);

        final Map<String,Object> rule2_1 = new HashMap<>();
        rule2_1.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_2);
        rule2_1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule2_1.put(SAML2AttributeTranscoder.PROP_NAME, MY_NAME_2);
        rule2_1.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, MY_NAMESPACE);

        final Map<String,Object> rule2_2 = new HashMap<>();
        rule2_2.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_2);
        rule2_2.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule2_2.put(SAML2AttributeTranscoder.PROP_NAME, MY_ALTNAME_1);
        rule2_2.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, MY_NAMESPACE);

        final Map<String,Object> rule3_1 = new HashMap<>();
        rule3_1.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_3);
        rule3_1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule3_1.put(SAML2AttributeTranscoder.PROP_NAME, MY_NAME_3);
        rule3_1.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, MY_NAMESPACE);

        registry.setTranscoderRegistry(Arrays.asList(
                new TranscodingRule(rule1_1),
                new TranscodingRule(rule1_2),
                new TranscodingRule(rule2_1),
                new TranscodingRule(rule2_2),
                new TranscodingRule(rule3_1)));
        registry.setApplicationContext(new MockApplicationContext());
        registry.initialize();
        action = new AddAttributeStatementToAssertion();
        action.setTranscoderRegistry(new MockReloadableService<>(registry));
    }

    /**
     * Test that the action errors out properly if there is no relying party context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoRelyingPartyContext() throws Exception {
        prc.removeSubcontext(RelyingPartyContext.class);

        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertEvent(result, EventIds.INVALID_PROFILE_CTX);
    }

    /**
     * Test that the action errors out properly if there is no outbound context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoOutboundContext() throws Exception {
        prc.setOutboundMessageContext(null);

        final AttributeContext attribCtx = buildAttributeContext();
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertEvent(result, EventIds.INVALID_MSG_CTX);
    }

    /**
     * Test that the action continues properly if there is no attribute context.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAttributeContext() throws Exception {
        action.initialize();
        Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);
    }

    /**
     * Test that the action continues properly if the attribute context does not contain attributes.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAttributes() throws Exception {
        final AttributeContext attribCtx = new AttributeContext();
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.initialize();
        Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);
    }

    /**
     * Test that the action ignores attribute encoding errors.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testIgnoreAttributeEncodingErrors() throws Exception {
        
        final AttributeTranscoderRegistryImpl localregistry = new AttributeTranscoderRegistryImpl();
        localregistry.setId("test");
        
        localregistry.setNamingRegistry(Collections.singletonList(
                new BasicNamingFunction<>(Attribute.class, new AbstractSAML2AttributeTranscoder.NamingFunction())));
        
        final MockSAML2StringAttributeTranscoder transcoder = new MockSAML2StringAttributeTranscoder();
        transcoder.initialize();

        final Map<String,Object> rule = new HashMap<>();
        rule.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule.put(SAML2AttributeTranscoder.PROP_NAME, MY_NAME_1);
        rule.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, MY_NAMESPACE);
        
        localregistry.setTranscoderRegistry(Collections.singletonList(new TranscodingRule(rule)));
        localregistry.setApplicationContext(new MockApplicationContext());
        localregistry.initialize();
        
        action.setTranscoderRegistry(new MockReloadableService<>(localregistry));

        final IdPAttribute attribute = new IdPAttribute(MY_NAME_1);
        attribute.setValues(Arrays.asList(new StringAttributeValue(MY_VALUE_1)));

        final AttributeContext attribCtx = new AttributeContext();
        attribCtx.setIdPAttributes(Arrays.asList(attribute));
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.setIgnoringUnencodableAttributes(true);
        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);
    }

    /**
     * Test that the action returns the correct transition when an attribute encoding error occurs.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void failOnAttributeEncodingErrors() throws Exception {
        
        final AttributeTranscoderRegistryImpl localregistry = new AttributeTranscoderRegistryImpl();
        localregistry.setId("test");
        
        localregistry.setNamingRegistry(Collections.singletonList(
                new BasicNamingFunction<>(Attribute.class, new AbstractSAML2AttributeTranscoder.NamingFunction())));

        final MockSAML2StringAttributeTranscoder transcoder = new MockSAML2StringAttributeTranscoder();
        transcoder.initialize();

        final Map<String,Object> rule = new HashMap<>();
        rule.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule.put(SAML2AttributeTranscoder.PROP_NAME, MY_NAME_1);
        rule.put(SAML2AttributeTranscoder.PROP_NAME_FORMAT, MY_NAMESPACE);
        
        localregistry.setTranscoderRegistry(Collections.singletonList(new TranscodingRule(rule)));
        localregistry.setApplicationContext(new MockApplicationContext());
        localregistry.initialize();
        
        action.setTranscoderRegistry(new MockReloadableService<>(localregistry));

        final IdPAttribute attribute = new IdPAttribute(MY_NAME_1);
        attribute.setValues(Arrays.asList(new StringAttributeValue(MY_VALUE_1)));

        final AttributeContext attribCtx = new AttributeContext();
        attribCtx.setIdPAttributes(Arrays.asList(attribute));
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.setIgnoringUnencodableAttributes(false);
        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertEvent(result, IdPEventIds.UNABLE_ENCODE_ATTRIBUTE);
    }

    /**
     * Test that the attribute statement is correctly added as a new assertion of a response already containing an
     * assertion.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testAddedAttributeStatement() throws Exception {

        ((Response) prc.getOutboundMessageContext().getMessage()).getAssertions().add(
                SAML2ActionTestingSupport.buildAssertion());

        final AttributeContext attribCtx = buildAttributeContext();
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.setStatementInOwnAssertion(true);
        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);

        final Response response = (Response) prc.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 2);

        for (final Assertion assertion : response.getAssertions()) {
            if (!assertion.getAttributeStatements().isEmpty()) {
                Assert.assertNotNull(assertion.getAttributeStatements());
                Assert.assertEquals(assertion.getAttributeStatements().size(), 1);

                final AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
                testAttributeStatement(attributeStatement);
            }
        }
    }

    /**
     * Test that the attribute statement is correctly added to an assertion which already exists in the response.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testAssertionInResponse() throws Exception {
        ((Response) prc.getOutboundMessageContext().getMessage()).getAssertions().add(
                SAML2ActionTestingSupport.buildAssertion());

        final AttributeContext attribCtx = buildAttributeContext();
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);

        final Response response = (Response) prc.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);

        final Assertion assertion = response.getAssertions().get(0);
        Assert.assertNotNull(assertion.getAttributeStatements());
        Assert.assertEquals(assertion.getAttributeStatements().size(), 1);

        final AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
        testAttributeStatement(attributeStatement);
    }

    /**
     * Test that the attribute statement is correctly added to a newly created assertion of the response which
     * originally contained no assertions.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testNoAssertionInResponse() throws Exception {
        final AttributeContext attribCtx = buildAttributeContext();
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);

        final Response response = (Response) prc.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);

        final Assertion assertion = response.getAssertions().get(0);
        Assert.assertNotNull(assertion.getAttributeStatements());
        Assert.assertEquals(assertion.getAttributeStatements().size(), 1);

        final AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
        testAttributeStatement(attributeStatement);
    }

    /**
     * Build the attribute context containing two test attributes to be used as an input to the action.
     * 
     * @return the attribute context to be used as an input to the action
     * @throws ComponentInitializationException thrown if the attribute encoders can not be initialized
     */
    private AttributeContext buildAttributeContext() throws ComponentInitializationException {

        final IdPAttribute attribute1 = new IdPAttribute(MY_NAME_1);
        attribute1.setValues(Arrays.asList(new StringAttributeValue(MY_VALUE_1)));

        final IdPAttribute attribute2 = new IdPAttribute(MY_NAME_2);
        attribute2.setValues(Collections.singletonList(new StringAttributeValue(MY_VALUE_2)));
        
        final IdPAttribute attribute3 = new IdPAttribute(MY_NAME_3);

        final AttributeContext attribCtx = new AttributeContext();
        attribCtx.setIdPAttributes(Arrays.asList(attribute1, attribute2, attribute3));

        return attribCtx;
    }

    /**
     * Test that the attribute statement returned from the action is correct.
     * 
     * @param attributeStatement the attribute statement returned from the action to test
     */
    private void testAttributeStatement(AttributeStatement attributeStatement) {

        Assert.assertNotNull(attributeStatement.getAttributes());
        Assert.assertEquals(attributeStatement.getAttributes().size(), 3);

        boolean one = false, altone = false, two = false;
        
        for (final Attribute samlAttr : attributeStatement.getAttributes()) {
            if (samlAttr.getName().equals(MY_NAME_1)) {
                Assert.assertEquals(samlAttr.getAttributeValues().size(), 1);
                final XMLObject xmlObject = samlAttr.getAttributeValues().get(0);
                Assert.assertEquals(((XSStringImpl) xmlObject).getValue(), MY_VALUE_1);
                one = true;
            } else if (samlAttr.getName().equals(MY_NAME_2)) {
                Assert.assertEquals(samlAttr.getAttributeValues().size(), 1);
                final XMLObject xmlObject = samlAttr.getAttributeValues().get(0);
                Assert.assertEquals(((XSStringImpl) xmlObject).getValue(), MY_VALUE_2);
                altone = true;
            } else if (samlAttr.getName().equals(MY_ALTNAME_1)) {
                Assert.assertEquals(samlAttr.getAttributeValues().size(), 2);
                final String val1 = ((XSStringImpl) samlAttr.getAttributeValues().get(0)).getValue();
                final String val2 = ((XSStringImpl) samlAttr.getAttributeValues().get(1)).getValue();
                if (val1.equals(MY_VALUE_1)) {
                    Assert.assertEquals(val2, MY_VALUE_2);
                    two = true;
                } else if (val2.equals(MY_VALUE_1)) {
                    Assert.assertEquals(val1, MY_VALUE_2);
                    two = true;
                }
            } else {
                Assert.fail("Incorrect attribute name.");
            }
        }

        
        if (!one || !altone || !two) {
            Assert.fail("Missing attribute/value");
        }
    }

    /** A mock SAML2 string attribute transcoder which always throws an {@link AttributeEncodingException}. */
    private class MockSAML2StringAttributeTranscoder extends SAML2StringAttributeTranscoder {

        /** {@inheritDoc} */
        @Override
        @Nullable public Attribute encode(@Nullable final ProfileRequestContext profileRequestContext,
                @Nonnull final IdPAttribute attribute, @Nonnull final Class<? extends Attribute> to,
                @Nonnull final TranscodingRule rule) throws AttributeEncodingException {
            throw new AttributeEncodingException("Always thrown.");
        }
    }

}

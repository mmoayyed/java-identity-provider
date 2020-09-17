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

package net.shibboleth.idp.saml.saml1.profile.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.testing.MockApplicationContext;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML1AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAML1AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.impl.SAML1StringAttributeTranscoder;
import net.shibboleth.idp.saml.saml1.profile.SAML1ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.test.service.MockReloadableService;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.impl.XSStringImpl;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeDesignator;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.Response;
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
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().setOutboundMessage(
                SAML1ActionTestingSupport.buildResponse()).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);
        
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");
        
        registry.setNamingRegistry(Collections.singletonMap(AttributeDesignator.class,
                new AbstractSAML1AttributeTranscoder.NamingFunction()));

        final SAML1StringAttributeTranscoder transcoder = new SAML1StringAttributeTranscoder();
        transcoder.initialize();
        
        final Map<String,Object> rule1_1 = new HashMap<>();
        rule1_1.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule1_1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule1_1.put(SAML1AttributeTranscoder.PROP_NAME, MY_NAME_1);
        rule1_1.put(SAML1AttributeTranscoder.PROP_NAMESPACE, MY_NAMESPACE);

        final Map<String,Object> rule1_2 = new HashMap<>();
        rule1_2.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule1_2.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule1_2.put(SAML1AttributeTranscoder.PROP_NAME, MY_ALTNAME_1);
        rule1_2.put(SAML1AttributeTranscoder.PROP_NAMESPACE, MY_NAMESPACE);

        final Map<String,Object> rule2_1 = new HashMap<>();
        rule2_1.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_2);
        rule2_1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule2_1.put(SAML1AttributeTranscoder.PROP_NAME, MY_NAME_2);
        rule2_1.put(SAML1AttributeTranscoder.PROP_NAMESPACE, MY_NAMESPACE);

        registry.setTranscoderRegistry(Arrays.asList(
                new TranscodingRule(rule1_1),
                new TranscodingRule(rule1_2),
                new TranscodingRule(rule2_1)));
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
        
        localregistry.setNamingRegistry(Collections.singletonMap(AttributeDesignator.class,
                new AbstractSAML1AttributeTranscoder.NamingFunction()));
        
        final MockSAML1StringAttributeTranscoder transcoder = new MockSAML1StringAttributeTranscoder();
        transcoder.initialize();
        
        final Map<String,Object> rule = new HashMap<>();
        rule.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule.put(SAML1AttributeTranscoder.PROP_NAME, MY_NAME_1);
        rule.put(SAML1AttributeTranscoder.PROP_NAMESPACE, MY_NAMESPACE);
        
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
        
        localregistry.setNamingRegistry(Collections.singletonMap(AttributeDesignator.class,
                new AbstractSAML1AttributeTranscoder.NamingFunction()));

        final MockSAML1StringAttributeTranscoder transcoder = new MockSAML1StringAttributeTranscoder();
        transcoder.initialize();

        final Map<String,Object> rule = new HashMap<>();
        rule.put(AttributeTranscoderRegistry.PROP_ID, MY_NAME_1);
        rule.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule.put(SAML1AttributeTranscoder.PROP_NAME, MY_NAME_1);
        rule.put(SAML1AttributeTranscoder.PROP_NAMESPACE, MY_NAMESPACE);
        
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
                SAML1ActionTestingSupport.buildAssertion());

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

                AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
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
                SAML1ActionTestingSupport.buildAssertion());

        final AttributeContext attribCtx = buildAttributeContext();
        prc.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        action.initialize();
        final Event result = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(result);

        final Response response = (Response) prc.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getAssertions().size(), 1);

        Assertion assertion = response.getAssertions().get(0);
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

        Assertion assertion = response.getAssertions().get(0);
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

        final AttributeContext attribCtx = new AttributeContext();
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
        Assert.assertEquals(attributeStatement.getAttributes().size(), 3);

        boolean one = false, altone = false, two = false;
        
        for (final Attribute samlAttr : attributeStatement.getAttributes()) {
            Assert.assertNotNull(samlAttr.getAttributeValues());
            Assert.assertEquals(samlAttr.getAttributeValues().size(), 1);
            final XMLObject xmlObject = samlAttr.getAttributeValues().get(0);
            Assert.assertTrue(xmlObject instanceof XSStringImpl);
            if (samlAttr.getAttributeName().equals(MY_NAME_1)) {
                Assert.assertEquals(((XSStringImpl) xmlObject).getValue(), MY_VALUE_1);
                one = true;
            } else if (samlAttr.getAttributeName().equals(MY_NAME_2)) {
                Assert.assertEquals(((XSStringImpl) xmlObject).getValue(), MY_VALUE_2);
                altone = true;
            } else if (samlAttr.getAttributeName().equals(MY_ALTNAME_1)) {
                Assert.assertEquals(((XSStringImpl) xmlObject).getValue(), MY_VALUE_1);
                two = true;
            } else {
                Assert.fail("Incorrect attribute name.");
            }
        }
    
        if (!one || !altone || !two) {
            Assert.fail("Missing attribute");
        }
    }

    /** A mock SAML1 string attribute encoder which always throws an {@link AttributeEncodingException}. */
    private class MockSAML1StringAttributeTranscoder extends SAML1StringAttributeTranscoder {

        /** {@inheritDoc} */
        @Override
        @Nullable public Attribute encode(@Nullable final ProfileRequestContext profileRequestContext,
                @Nonnull final IdPAttribute attribute, @Nonnull final Class<? extends AttributeDesignator> to,
                @Nonnull final TranscodingRule rule) throws AttributeEncodingException {
            throw new AttributeEncodingException("Always thrown.");
        }
    }

}
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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.profile.action.ProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.testing.ActionTestingSupport;
import org.opensaml.profile.testing.RequestContextBuilder;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationProcessingData;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.testing.ConstantSupplier;

/**
 * Unit test for {@link ProcessAssertionsForAuthentication} action.
 */
@SuppressWarnings("javadoc")
public class ProcessAssertionsForAuthenticationTest extends OpenSAMLInitBaseTestCase {
    
    private ProcessAssertionsForAuthentication action;
    
    private ProfileRequestContext prc, prcInner;
    
    private SAMLAuthnContext samlAuthnContext;
    
    private Response samlResponse;
    
    private MockHttpServletRequest httpRequest;
    private MockHttpServletResponse httpResponse;
    
    private Object nullObj;
    
    @BeforeMethod
    public void beforeMethod() {
        final MockHttpServletRequest req = httpRequest = new MockHttpServletRequest();
        httpResponse = new MockHttpServletResponse();
        
        action = new ProcessAssertionsForAuthentication();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(httpRequest));
        action.setHttpServletResponseSupplier(new ConstantSupplier<>(httpResponse));
        
        
        samlResponse = SAML2ActionTestingSupport.buildResponse();
        
        prcInner = new RequestContextBuilder().setInboundMessage(samlResponse).buildProfileRequestContext();
        
        prc = new RequestContextBuilder().buildProfileRequestContext();
        
        final AuthenticationContext authnContext = prc.ensureSubcontext(AuthenticationContext.class);
        samlAuthnContext = new SAMLAuthnContext(new MockProfileAction(), new MockMessageDecoderFunction());
        authnContext.addSubcontext(samlAuthnContext);
        authnContext.addSubcontext(prcInner);
    }
    
    @Test
    public void testValid() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion1.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion1.getAuthnStatements().get(0));
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test
    public void testInvalid() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.INVALID);
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.emptyList());
    }
    
    @Test
    public void testIndeterminate() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.INDETERMINATE);
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.emptyList());
    }
    
    @Test
    public void testMultipleValidNoSessionInstant() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        final Assertion assertion2 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion2);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion1.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion1.getAuthnStatements().get(0));
        Assert.assertEquals(samlResponse.getAssertions(), List.of(assertion1, assertion2));
    }
    
    @Test
    public void testMultipleValidMixedSessionInstant() throws ComponentInitializationException {
        // Assertion1 doesn't have session instant, so pick assertion 2
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        final Assertion assertion2 = buildAssertion(ValidationResult.VALID);
        assertion2.getAuthnStatements().get(0).setSessionNotOnOrAfter(Instant.now().plus(Duration.ofHours(2)));
        samlResponse.getAssertions().add(assertion2);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion2.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion2.getAuthnStatements().get(0));
        Assert.assertEquals(samlResponse.getAssertions(), List.of(assertion1, assertion2));
    }
    
    @Test
    public void testMixedValidity() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.INVALID);
        samlResponse.getAssertions().add(assertion1);
        final Assertion assertion2 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion2);
        final Assertion assertion3 = buildAssertion(ValidationResult.INDETERMINATE);
        samlResponse.getAssertions().add(assertion3);
        final Assertion assertion4 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion4);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion2.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion2.getAuthnStatements().get(0));
        Assert.assertEquals(samlResponse.getAssertions(), List.of(assertion2, assertion4));
    }
    
    @Test
    public void testMultipleValidBothSessionInstant() throws ComponentInitializationException {
        // Assertion1 has earlier session instant, so pick assertion 1
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        assertion1.getAuthnStatements().get(0).setSessionNotOnOrAfter(Instant.now().plus(Duration.ofHours(1)));
        samlResponse.getAssertions().add(assertion1);
        final Assertion assertion2 = buildAssertion(ValidationResult.VALID);
        assertion2.getAuthnStatements().get(0).setSessionNotOnOrAfter(Instant.now().plus(Duration.ofHours(2)));
        samlResponse.getAssertions().add(assertion2);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion1.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion1.getAuthnStatements().get(0));
        Assert.assertEquals(samlResponse.getAssertions(), List.of(assertion1, assertion2));
    }
    
    @Test
    public void testMultipleAuthnStatementsNoSessionInstant() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        assertion1.getAuthnStatements().add(SAML2ActionTestingSupport.buildAuthnStatement());
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion1.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion1.getAuthnStatements().get(0));
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test
    public void testMultipleAuthnStatementsMixedSessionInstant() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        assertion1.getAuthnStatements().add(SAML2ActionTestingSupport.buildAuthnStatement());
        assertion1.getAuthnStatements().get(1).setSessionNotOnOrAfter(Instant.now().plus(Duration.ofHours(2)));
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion1.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion1.getAuthnStatements().get(1));
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test
    public void testMultipleAuthnStatementsBothSessionInstant() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        assertion1.getAuthnStatements().get(0).setSessionNotOnOrAfter(Instant.now().plus(Duration.ofHours(1)));
        assertion1.getAuthnStatements().add(SAML2ActionTestingSupport.buildAuthnStatement());
        assertion1.getAuthnStatements().get(1).setSessionNotOnOrAfter(Instant.now().plus(Duration.ofHours(2)));
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertSame(samlAuthnContext.getSubject(), assertion1.getSubject());
        Assert.assertSame(samlAuthnContext.getAuthnStatement(), assertion1.getAuthnStatements().get(0));
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test
    public void testNoResponse() throws ComponentInitializationException {
        final MessageContext imc = prcInner.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(null);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.emptyList());
    }
    
    @Test
    public void testNoAssertions() throws ComponentInitializationException {
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.emptyList());
    }
    
    @Test
    public void testNoSAMLAuthnContext() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        
        AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac!=null;
        ac.removeSubcontext(SAMLAuthnContext.class);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test
    public void testAssertionNotValidated() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(null);
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.emptyList());
    }
    
    @Test
    public void testNoAuthnStatement() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        assertion1.getAuthnStatements().clear();
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test
    public void testNoConfirmedSubject() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        assertion1.getObjectMetadata().get(ValidationProcessingData.class).get(0)
            .getContext().getDynamicParameters().remove(SAML2AssertionValidationParameters.CONFIRMED_SUBJECT_CONFIRMATION);
        samlResponse.getAssertions().add(assertion1);
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_CREDENTIALS);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test
    public void testActionInactive() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        
        action.setActivationCondition(PredicateSupport.alwaysFalse());
        
        action.initialize();
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNull(samlAuthnContext.getSubject());
        Assert.assertNull(samlAuthnContext.getAuthnStatement());
        Assert.assertEquals(samlResponse.getAssertions(), Collections.singletonList(assertion1));
    }
    
    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testNullAuthnAssertionStrategy() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        
        action.setAuthnAssertionSelectionStrategy((Function<List<Assertion>, Assertion>) nullObj);
        
        action.initialize();
    }
    
    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testNullAuthnStatementStrategy() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        
        action.setAuthnStatementSelectionStrategy((Function<Assertion, AuthnStatement>) nullObj);
        
        action.initialize();
    }
    
    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testNullResponseResolver() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        
        action.setResponseResolver((Function<ProfileRequestContext, Response>) nullObj);
        
        action.initialize();
    }
    
    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testNullSAMLAuthnContextStrategy() throws ComponentInitializationException {
        final Assertion assertion1 = buildAssertion(ValidationResult.VALID);
        samlResponse.getAssertions().add(assertion1);
        
        action.setSAMLAuthnContextLookupStrategy((Function<ProfileRequestContext, SAMLAuthnContext>) nullObj);
        
        action.initialize();
    }
    
    
    // Helpers
    
    private Assertion buildAssertion(ValidationResult validationResult) {
        final SubjectConfirmation sc = (SubjectConfirmation) XMLObjectSupport.buildXMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        final Subject subject = SAML2ActionTestingSupport.buildSubject("testUser");
        subject.getSubjectConfirmations().add(sc);
        
        final Assertion assertion = SAML2ActionTestingSupport.buildAssertion();
        assertion.setSubject(subject);
        assertion.getAuthnStatements().add(SAML2ActionTestingSupport.buildAuthnStatement());
        
        if (validationResult != null) {
            assertion.getObjectMetadata().put(new ValidationProcessingData(buildValidationContext(sc), validationResult));
        }
        
       
        return assertion;
    }
    
    private ValidationContext buildValidationContext(SubjectConfirmation sc) {
        final ValidationContext vc = new ValidationContext();
        vc.getDynamicParameters().put(SAML2AssertionValidationParameters.CONFIRMED_SUBJECT_CONFIRMATION, sc);
        return vc;
    }
    
    private static class MockProfileAction implements ProfileAction {

        /** {@inheritDoc} */
        public boolean isInitialized() {
            return true;
        }

        /** {@inheritDoc} */
        public void initialize() throws ComponentInitializationException {
            
        }

        /** {@inheritDoc} */
        public void execute(ProfileRequestContext profileRequestContext) {
            
        }
        
    }
    
    private static class MockMessageDecoderFunction implements Function<String, MessageDecoder> {

        /** {@inheritDoc} */
        public MessageDecoder apply(String t) {
            return null;
        }
        
    }

}

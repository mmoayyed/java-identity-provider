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

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.profile.SAML2ActionTestingSupport;
import org.opensaml.security.messaging.ServletRequestX509CredentialAdapter;
import org.opensaml.soap.soap11.Envelope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SAML 2 attribute query flow test.
 */
public class SAML2AttributeQueryFlowTest extends AbstractSAML2FlowTest {

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "profile/SAML2/SOAP/AttributeQuery";

    /**
     * Test the SAML 2 Attribute Query flow.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2AttributeQueryFlow() throws Exception {

        buildRequest();

        request.setAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE,
                new X509Certificate[] {certFactoryBean.getObject()});

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        validateResult(result, FLOW_ID);
    }

    /**
     * Test the SAML 2 Attribute Query flow without an SP credential.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2AttributeQueryFlowNoCredential() throws Exception {
        try {
            // expect an error
            statusCode = StatusCode.REQUESTER_URI;

            buildRequest();

            overrideEndStateOutput(FLOW_ID);

            final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

            validateResult(result, FLOW_ID);
        } finally {
            // reset expectation of success
            statusCode = StatusCode.SUCCESS_URI;
        }
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @throws Exception if an error occurs
     */
    public void buildRequest() throws Exception {

        final Subject subject = SAML2ActionTestingSupport.buildSubject("jdoe");

        final AttributeQuery attributeQuery = SAML2ActionTestingSupport.buildAttributeQueryRequest(subject);
        attributeQuery.setIssueInstant(new DateTime());
        attributeQuery.getIssuer().setValue(SP_ENTITY_ID);

        final Envelope envelope = buildSOAP11Envelope(attributeQuery);

        final String requestContent =
                SerializeSupport.nodeToString(marshallerFactory.getMarshaller(envelope).marshall(envelope,
                        parserPool.newDocument()));

        request.setMethod("POST");

        request.setContent(requestContent.getBytes("UTF-8"));
    }

    /**
     * Assert that the nameID value is "jdoe" and that the name qualifier, SP name qualifier, and name format are null.
     * 
     * @param nameID the name ID
     */
    @Override public void assertNameID(@Nullable final NameID nameID) {
        Assert.assertNotNull(nameID);
        Assert.assertEquals(nameID.getValue(), "jdoe");
        Assert.assertNull(nameID.getNameQualifier());
        Assert.assertNull(nameID.getSPNameQualifier());
        Assert.assertNull(nameID.getFormat());
    }

    /**
     * Assert that the subject confirmation method is {@link SubjectConfirmation#METHOD_SENDER_VOUCHES}.
     * 
     * @param subjectConfirmation the subject confirmation
     */
    @Override public void assertSubjectConfirmationMethod(@Nullable final SubjectConfirmation subjectConfirmation) {
        Assert.assertEquals(subjectConfirmation.getMethod(), SubjectConfirmation.METHOD_SENDER_VOUCHES);
    }

    /**
     * Assert that the subject confirmation data is null.
     * 
     * @param subjectConfirmationData the subject confirmation data
     */
    @Override public void
            assertSubjectConfirmationData(@Nullable final SubjectConfirmationData subjectConfirmationData) {
        Assert.assertNull(subjectConfirmationData);
    }

    /**
     * Assert that authn statements are not present.
     * 
     * @param assertion the assertion
     */
    public void validateAuthnStatements(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);
        Assert.assertNotNull(assertion.getAuthnStatements());
        Assert.assertTrue(assertion.getAuthnStatements().isEmpty());
    }
}

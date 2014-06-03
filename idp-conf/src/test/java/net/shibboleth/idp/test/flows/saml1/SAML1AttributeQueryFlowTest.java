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

package net.shibboleth.idp.test.flows.saml1;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.joda.time.DateTime;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.StatusCode;
import org.opensaml.saml.saml1.core.Subject;
import org.opensaml.saml.saml1.profile.SAML1ActionTestingSupport;
import org.opensaml.security.messaging.ServletRequestX509CredentialAdapter;
import org.opensaml.soap.soap11.Envelope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SAML 1 attribute query flow test.
 */
public class SAML1AttributeQueryFlowTest extends AbstractSAML1FlowTest {

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "profile/SAML1/SOAP/AttributeQuery";

    /**
     * Test the SAML1 Attribute Query flow.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1AttributeQueryFlow() throws Exception {

        buildRequest();

        request.setAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE,
                new X509Certificate[] {certFactoryBean.getObject()});

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        validateResult(result, FLOW_ID);
    }

    /**
     * Test the SAML1 Attribute Query flow without an SP credential.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1AttributeQueryFlowNoCredential() throws Exception {
        try {
            // expect an error
            statusCode = StatusCode.REQUESTER;

            buildRequest();

            overrideEndStateOutput(FLOW_ID);

            final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

            validateResult(result, FLOW_ID);
        } finally {
            // reset expectation of success
            statusCode = StatusCode.SUCCESS;
        }
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @throws Exception if an error occurs
     */
    public void buildRequest() throws Exception {
        final Subject subject = SAML1ActionTestingSupport.buildSubject("jdoe");

        final Request attributeQuery = SAML1ActionTestingSupport.buildAttributeQueryRequest(subject);
        attributeQuery.setIssueInstant(new DateTime());
        attributeQuery.getAttributeQuery().setResource(SP_ENTITY_ID);
        attributeQuery.setID("TESTID");

        final Envelope envelope = buildSOAP11Envelope(attributeQuery);

        final String requestContent =
                SerializeSupport.nodeToString(marshallerFactory.getMarshaller(envelope).marshall(envelope,
                        parserPool.newDocument()));

        request.setMethod("POST");
        request.setContent(requestContent.getBytes("UTF-8"));
    }

    /**
     * {@inheritDoc}
     * 
     * Assert that InResponseTo is correct.
     */
    @Override public void assertResponse(@Nullable final Response response) {
        super.assertResponse(response);
        Assert.assertEquals(response.getInResponseTo(), "TESTID");
    }

    /**
     * Assert that the name identifier is correct and the format and name qualifiers are null.
     */
    @Override public void assertNameIdentifier(@Nullable final NameIdentifier nameIdentifier) {
        Assert.assertNotNull(nameIdentifier);
        Assert.assertEquals(nameIdentifier.getNameIdentifier(), "jdoe");
        Assert.assertEquals(nameIdentifier.getFormat(), null);
        Assert.assertEquals(nameIdentifier.getNameQualifier(), null);
    }

    /**
     * {@inheritDoc}
     * 
     * Assert that the confirmation method equals
     * {@link org.opensaml.saml.saml1.core.ConfirmationMethod#METHOD_SENDER_VOUCHES}.
     */
    @Override public void assertConfirmationMethod(@Nullable final ConfirmationMethod confirmationMethod) {
        Assert.assertNotNull(confirmationMethod);
        Assert.assertEquals(confirmationMethod.getConfirmationMethod(), ConfirmationMethod.METHOD_SENDER_VOUCHES);
    }

    /**
     * Assert that authentication statements are not present in the response.
     */
    @Override public void validateAuthenticationStatements(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);
        Assert.assertNotNull(assertion.getAuthenticationStatements());
        Assert.assertTrue(assertion.getAuthenticationStatements().isEmpty());
    }

}

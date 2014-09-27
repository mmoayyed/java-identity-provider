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
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml.saml2.profile.SAML2ActionTestingSupport;
import org.opensaml.security.messaging.ServletRequestX509CredentialAdapter;
import org.opensaml.soap.soap11.Envelope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * SAML 2 attribute query flow test.
 */
public class SAML2AttributeQueryFlowTest extends AbstractSAML2FlowTest {

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "SAML2/SOAP/AttributeQuery";

    /** SAML 2 Response validator. */
    @Nullable private SAML2TestResponseValidator validator;

    /** Initialize the SAML 2 Response validator. */
    @BeforeClass void setupValidator() {

        final NameID nameID = new NameIDBuilder().buildObject();
        nameID.setValue("jdoe");
        nameID.setNameQualifier(null);
        nameID.setSPNameQualifier(null);
        nameID.setFormat(null);

        validator = new SAML2TestResponseValidator();
        validator.nameID = nameID;
        validator.spCredential = spCredential;
        validator.subjectConfirmationMethod = SubjectConfirmation.METHOD_SENDER_VOUCHES;
        validator.validateAuthnStatements = false;
        validator.validateSubjectConfirmationData = false;
    }

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

        validateResult(result, FLOW_ID, validator);
    }

    /**
     * Test the SAML 2 Attribute Query flow without an SP credential.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2AttributeQueryFlowNoCredential() throws Exception {

        buildRequest();

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        validator.statusCode = StatusCode.REQUESTER;

        validateResult(result, FLOW_ID, validator);
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
}

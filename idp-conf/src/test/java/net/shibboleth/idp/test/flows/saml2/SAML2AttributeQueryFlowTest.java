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
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.opensaml.security.messaging.ServletRequestX509CredentialAdapter;
import org.opensaml.soap.soap11.Envelope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;
import net.shibboleth.shared.xml.SerializeSupport;

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

        final SAMLObjectBuilder<NameID> builder = (SAMLObjectBuilder<NameID>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameID>ensureBuilder(
                        NameID.DEFAULT_ELEMENT_NAME);
        final NameID nameID = builder.buildObject();
        nameID.setValue("jdoe");
        nameID.setNameQualifier(null);
        nameID.setSPNameQualifier(null);
        nameID.setFormat(null);

        final SAML2TestResponseValidator localValidator = this.validator = new SAML2TestResponseValidator();
        localValidator.nameID = nameID;
        localValidator.spCredential = spCredential;
        localValidator.subjectConfirmationMethod = SubjectConfirmation.METHOD_SENDER_VOUCHES;
        localValidator.validateAuthnStatements = false;
        localValidator.validateSubjectConfirmationData = false;
    }

    /**
     * Test the SAML 2 Attribute Query flow.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2AttributeQueryFlow() throws Exception {

        buildRequest(false);

        request.setAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE,
                new X509Certificate[] {certFactoryBean.getObject()});

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        assert result != null;
        final SAML2TestResponseValidator localValidator = validator;
        assert localValidator!= null;
        localValidator.statusCode = StatusCode.SUCCESS;
        localValidator.usedAttributeDesignators = false;

        validateResult(result, FLOW_ID, localValidator);
    }

    /**
     * Test the SAML 2 Attribute Query flow with designators.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2AttributeQueryFlowWithDesignators() throws Exception {

        buildRequest(true);

        request.setAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE,
                new X509Certificate[] {certFactoryBean.getObject()});

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        assert result != null;
        final SAML2TestResponseValidator localValidator = validator;
        assert localValidator!= null;

        localValidator.statusCode = StatusCode.SUCCESS;
        localValidator.usedAttributeDesignators = true;
        
        validateResult(result, FLOW_ID, localValidator);
    }

    /**
     * Test the SAML 2 Attribute Query flow without an SP credential.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2AttributeQueryFlowNoCredential() throws Exception {

        buildRequest(false);

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        assert result != null;
        final SAML2TestResponseValidator localValidator = validator;
        assert localValidator!= null;

        localValidator.statusCode = StatusCode.REQUESTER;
        localValidator.usedAttributeDesignators = false;

        validateResult(result, FLOW_ID, localValidator);
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @param includeDesignators ...
     * 
     * @throws Exception if an error occurs
     */
    public void buildRequest(final boolean includeDesignators) throws Exception {

        final Subject subject = SAML2ActionTestingSupport.buildSubject("jdoe");

        final AttributeQuery attributeQuery = SAML2ActionTestingSupport.buildAttributeQueryRequest(subject);
        attributeQuery.setID(IdentifierGenerationStrategy.getInstance(ProviderType.SECURE).generateIdentifier());
        attributeQuery.setIssueInstant(Instant.now());
        attributeQuery.setIssuer(SAML2ActionTestingSupport.buildIssuer(SP_ENTITY_ID));

        if (includeDesignators) {
            final SAMLObjectBuilder<Attribute> designatorBuilder = (SAMLObjectBuilder<Attribute>)
                    XMLObjectProviderRegistrySupport.getBuilderFactory().<Attribute>ensureBuilder(
                            Attribute.DEFAULT_ELEMENT_NAME);
            final XMLObjectBuilder<XSAny> valueBuilder =
                    XMLObjectProviderRegistrySupport.getBuilderFactory().<XSAny>ensureBuilder(
                            XSAny.TYPE_NAME);

            
            Attribute designator = designatorBuilder.buildObject();
            designator.setNameFormat(Attribute.URI_REFERENCE);
            designator.setName("urn:oid:0.9.2342.19200300.100.1.3");
            attributeQuery.getAttributes().add(designator);
            
            XSAny value = valueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
            value.setTextContent("jdoe@example.org");
            designator.getAttributeValues().add(value);

            designator = designatorBuilder.buildObject();
            designator.setNameFormat(Attribute.URI_REFERENCE);
            designator.setName("urn:mace:dir:attribute-def:foo");
            attributeQuery.getAttributes().add(designator);
        }

        final Envelope envelope = buildSOAP11Envelope(attributeQuery);
        final Marshaller m = marshallerFactory.getMarshaller(envelope);
        assert m != null;
        final String requestContent =
                SerializeSupport.nodeToString(m.marshall(envelope, parserPool.newDocument()));

        request.setMethod("POST");
        
        request.setContentType("text/xml");

        request.setContent(requestContent.getBytes("UTF-8"));
    }
}

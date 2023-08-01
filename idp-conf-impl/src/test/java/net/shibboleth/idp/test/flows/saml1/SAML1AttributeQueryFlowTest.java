/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.xml.SAMLConstants;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;
import net.shibboleth.shared.xml.SerializeSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.AttributeDesignator;
import org.opensaml.saml.saml1.core.AttributeQuery;
import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml1.core.StatusCode;
import org.opensaml.saml.saml1.core.Subject;
import org.opensaml.saml.saml1.testing.SAML1ActionTestingSupport;
import org.opensaml.security.messaging.ServletRequestX509CredentialAdapter;
import org.opensaml.soap.soap11.Envelope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * SAML 1 attribute query flow test.
 */
@SuppressWarnings({"null"})
public class SAML1AttributeQueryFlowTest extends AbstractSAML1FlowTest {

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "SAML1/SOAP/AttributeQuery";

    /** SAML 1 Response validator. */
    @Nullable private SAML1TestResponseValidator validator;

    /** Initialize the SAML 1 Response validator. */
    @BeforeClass void setupValidator() {

        final SAMLObjectBuilder<NameIdentifier> builder = (SAMLObjectBuilder<NameIdentifier>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameIdentifier>ensureBuilder(
                        NameIdentifier.DEFAULT_ELEMENT_NAME);
        final NameIdentifier nameIdentifier = builder.buildObject();
        nameIdentifier.setValue("jdoe");
        nameIdentifier.setFormat(null);
        nameIdentifier.setNameQualifier(null);

        final SAML1TestResponseValidator val = validator = new SAML1TestResponseValidator();
        val.validateAuthenticationStatements = false;
        val.nameIdentifier = nameIdentifier;
        val.confirmationMethod = ConfirmationMethod.METHOD_SENDER_VOUCHES;
    }

    /**
     * Test the SAML1 Attribute Query flow.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1AttributeQueryFlow() throws Exception {

        buildRequest(false);

        request.setAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE,
                new X509Certificate[] {certFactoryBean.getObject()});

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final SAML1TestResponseValidator val = validator;
        assert val!=null;
        val.statusCode = StatusCode.SUCCESS;
        val.usedAttributeDesignators = false;

        validateResult(result, FLOW_ID, val);
    }

    /**
     * Test the SAML1 Attribute Query flow with designators included.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1AttributeQueryFlowWithDesignators() throws Exception {

        buildRequest(true);

        request.setAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE,
                new X509Certificate[] {certFactoryBean.getObject()});

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final SAML1TestResponseValidator val = validator;
        assert val!=null;

        val.statusCode = StatusCode.SUCCESS;
        val.usedAttributeDesignators = true;

        validateResult(result, FLOW_ID, val);
    }

    /**
     * Test the SAML1 Attribute Query flow without an SP credential.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1AttributeQueryFlowNoCredential() throws Exception {

        buildRequest(false);

        overrideEndStateOutput(FLOW_ID);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        final SAML1TestResponseValidator val = validator;
        assert val!=null;

        val.statusCode = StatusCode.REQUESTER;
        val.usedAttributeDesignators = false;

        validateResult(result, FLOW_ID, val);
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @param includeDesignators ...
     * 
     * @throws Exception if an error occurs
     */
    public void buildRequest(final boolean includeDesignators) throws Exception {
        final Subject subject = SAML1ActionTestingSupport.buildSubject("jdoe");

        final Request attributeQuery = SAML1ActionTestingSupport.buildAttributeQueryRequest(subject);
        final AttributeQuery query = attributeQuery.getAttributeQuery();
        assert query != null;
        attributeQuery.setIssueInstant(Instant.now());
        query.setResource(SP_ENTITY_ID);
        attributeQuery.setID(IdentifierGenerationStrategy.getInstance(ProviderType.SECURE).generateIdentifier());
        
        if (includeDesignators) {
            final SAMLObjectBuilder<AttributeDesignator> designatorBuilder = (SAMLObjectBuilder<AttributeDesignator>)
                    XMLObjectProviderRegistrySupport.getBuilderFactory().<AttributeDesignator>ensureBuilder(
                            AttributeDesignator.DEFAULT_ELEMENT_NAME);
            
            AttributeDesignator designator = designatorBuilder.buildObject();
            designator.setAttributeNamespace(SAMLConstants.SAML1_ATTR_NAMESPACE_URI);
            designator.setAttributeName("urn:mace:dir:attribute-def:eduPersonScopedAffiliation");
            query.getAttributeDesignators().add(designator);
    
            designator = designatorBuilder.buildObject();
            designator.setAttributeNamespace(SAMLConstants.SAML1_ATTR_NAMESPACE_URI);
            designator.setAttributeName("urn:mace:dir:attribute-def:mail");
            query.getAttributeDesignators().add(designator);

            designator = designatorBuilder.buildObject();
            designator.setAttributeNamespace(SAMLConstants.SAML1_ATTR_NAMESPACE_URI);
            designator.setAttributeName("urn:mace:dir:attribute-def:foo");
            query.getAttributeDesignators().add(designator);
        }

        final Envelope envelope = buildSOAP11Envelope(attributeQuery);
        final Marshaller m = marshallerFactory.getMarshaller(envelope);
        assert m!=null;
        final String requestContent =
                SerializeSupport.nodeToString(m.marshall(envelope,
                        parserPool.newDocument()));

        request.setMethod("POST");
        request.setContentType("text/xml");
        request.setContent(requestContent.getBytes("UTF-8"));
    }

}

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

import java.net.MalformedURLException;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.saml.saml2.encryption.Encrypter.KeyPlacement;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.encryption.support.DataEncryptionParameters;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.test.flows.AbstractFlowTest;
import net.shibboleth.shared.net.SimpleURLCanonicalizer;
import net.shibboleth.shared.net.URLBuilder;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Abstract SAML 2 SSO flow test.
 */
@SuppressWarnings("javadoc")
public abstract class AbstractSAML2SSOFlowTest extends AbstractSAML2FlowTest {

    /** Class logger. */
    @Nonnull protected final Logger log = LoggerFactory.getLogger(AbstractSAML2SSOFlowTest.class);

    public String getDestinationRedirect(@Nonnull HttpServletRequest servletRequest) {
        // TODO servlet context
        String destinationPath = "/idp/profile/SAML2/Redirect/SSO";
        try {
            String baseUrl = SimpleURLCanonicalizer.canonicalize(getBaseUrl(servletRequest));
            URLBuilder urlBuilder = new URLBuilder(baseUrl);
            urlBuilder.setPath(destinationPath);
            return urlBuilder.buildURL();
        } catch (final MalformedURLException e) {
            log.error("Couldn't parse base URL, reverting to internal default destination");
            return "http://localhost:8080" + destinationPath;
        }
    }

    public String getDestinationPost(@Nonnull HttpServletRequest servletRequest) {
        // TODO servlet context
        String destinationPath = "/idp/profile/SAML2/POST/SSO";
        String baseUrl = getBaseUrl(servletRequest);
        try {
            URLBuilder urlBuilder = new URLBuilder(baseUrl);
            urlBuilder.setPath(destinationPath);
            return urlBuilder.buildURL();
        } catch (final MalformedURLException e) {
            log.error("Couldn't parse base URL, reverting to internal default destination: {}", baseUrl);
            return "http://localhost:8080" + destinationPath;
        }
    }

    public String getDestinationPostSimpleSign(@Nonnull HttpServletRequest servletRequest) {
        // TODO servlet context
        String destinationPath = "/idp/profile/SAML2/POST-SimpleSign/SSO";
        String baseUrl = getBaseUrl(servletRequest);
        try {
            URLBuilder urlBuilder = new URLBuilder(baseUrl);
            urlBuilder.setPath(destinationPath);
            return urlBuilder.buildURL();
        } catch (final MalformedURLException e) {
            log.error("Couldn't parse base URL, reverting to internal default destination: {}", baseUrl);
            return "http://localhost:8080" + destinationPath;
        }
    }

    public String getDestinationECP(HttpServletRequest servletRequest) {
        // TODO servlet context
        String destinationPath = "/idp/profile/SAML2/SOAP/ECP";
        try {
            String baseUrl = SimpleURLCanonicalizer.canonicalize(getBaseUrl(servletRequest));
            URLBuilder urlBuilder = new URLBuilder(baseUrl);
            urlBuilder.setPath(destinationPath);
            return urlBuilder.buildURL();
        } catch (final MalformedURLException e) {
            log.error("Couldn't parse base URL, reverting to internal default destination");
            return "http://localhost:8080" + destinationPath;
        }
    }

    public AuthnRequest buildAuthnRequest(final HttpServletRequest servletRequest) throws EncryptionException {
        return buildAuthnRequest(servletRequest, getAcsUrl(servletRequest), SAMLConstants.SAML2_POST_BINDING_URI);
    }
    
    public AuthnRequest buildAuthnRequest(final HttpServletRequest servletRequest, final String acsURL, final String outboundBinding)
            throws EncryptionException {
        final XMLObjectBuilder<?> authnRequestBuilder = builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);
        assert authnRequestBuilder!=null;
        final AuthnRequest authnRequest = (AuthnRequest) authnRequestBuilder.buildObject(AuthnRequest.DEFAULT_ELEMENT_NAME);

        authnRequest.setID(idGenerator.generateIdentifier());
        authnRequest.setIssueInstant(Instant.now());
        authnRequest.setAssertionConsumerServiceURL(acsURL);
        authnRequest.setProtocolBinding(outboundBinding);

        final XMLObjectBuilder<?> issuerBuilder = builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        assert issuerBuilder!=null;
        final Issuer issuer = (Issuer) issuerBuilder.buildObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(AbstractFlowTest.SP_ENTITY_ID);
        authnRequest.setIssuer(issuer);

        final XMLObjectBuilder<?> nidPolicyBuilder = builderFactory.getBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        assert nidPolicyBuilder!=null;
        final NameIDPolicy nameIDPolicy = (NameIDPolicy) nidPolicyBuilder.buildObject(
                        NameIDPolicy.DEFAULT_ELEMENT_NAME);
        nameIDPolicy.setAllowCreate(true);
        authnRequest.setNameIDPolicy(nameIDPolicy);

        final XMLObjectBuilder<?> nidBuilder = builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        assert nidBuilder!=null;
        final NameID nameID = (NameID) nidBuilder.buildObject(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue("jdoe");

        final XMLObjectBuilder<?> subjectBuilder = builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
        assert subjectBuilder!=null;
        final Subject subject = (Subject) subjectBuilder.buildObject(Subject.DEFAULT_ELEMENT_NAME);
        subject.setEncryptedID(getEncrypter().encrypt(nameID));
        authnRequest.setSubject(subject);

        final XMLObjectBuilder<?> racBuilder = builderFactory.getBuilder(RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
        assert racBuilder!=null;
        final RequestedAuthnContext reqAC = (RequestedAuthnContext) racBuilder.buildObject(RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
        final XMLObjectBuilder<?> accRefBuilder = builderFactory.getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        assert accRefBuilder!=null;
        final AuthnContextClassRef ac = (AuthnContextClassRef) accRefBuilder.buildObject(
                        AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        ac.setURI(AuthnContext.UNSPECIFIED_AUTHN_CTX);
        reqAC.getAuthnContextClassRefs().add(ac);
        authnRequest.setRequestedAuthnContext(reqAC);
        
        return authnRequest;
    }

    public Encrypter getEncrypter() {
        final DataEncryptionParameters encParams = new DataEncryptionParameters();
        encParams.setAlgorithm(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128);
        final KeyEncryptionParameters kencParams = new KeyEncryptionParameters();
        kencParams.setAlgorithm(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
        kencParams.setEncryptionCredential(idpCredential);
        final X509KeyInfoGeneratorFactory generator = new X509KeyInfoGeneratorFactory();
        generator.setEmitEntityCertificate(true);
        kencParams.setKeyInfoGenerator(generator.newInstance());
        final Encrypter encrypter = new Encrypter(encParams, kencParams);
        encrypter.setKeyPlacement(KeyPlacement.PEER);
        return encrypter;
    }

    public String getAcsUrl(final HttpServletRequest servletRequest) {
        return getAcsUrl(servletRequest, "/sp/SAML2/POST/ACS"); 
    }
    
    public String getAcsUrl(final HttpServletRequest servletRequest, final String acsURL) {
        // TODO servlet context
        String baseUrl = getBaseUrl(servletRequest);
        assert baseUrl!=null;
        try {
            URLBuilder urlBuilder = new URLBuilder(SimpleURLCanonicalizer.canonicalize(baseUrl));
            urlBuilder.setPath(acsURL);
            return urlBuilder.buildURL();
        } catch (MalformedURLException e) {
            log.error("Couldn't parse base URL, reverting to internal default ACS: {}", baseUrl);
            return "http://localhost:8080" + acsURL;
        }
    }

    public SingleSignOnService buildIdpSsoEndpoint(String binding, String destination) {
        final XMLObjectBuilder<?> builder = builderFactory.getBuilder(SingleSignOnService.DEFAULT_ELEMENT_NAME);
        assert builder!=null;
        SingleSignOnService ssoEndpoint = (SingleSignOnService) builder.buildObject(SingleSignOnService.DEFAULT_ELEMENT_NAME);
        ssoEndpoint.setBinding(binding);
        ssoEndpoint.setLocation(destination);
        return ssoEndpoint;
    }

    public String getBaseUrl(HttpServletRequest servletRequest) {
        // TODO servlet context
        String requestUrl = servletRequest.getRequestURL().toString();
        try {
            URLBuilder urlBuilder = new URLBuilder(requestUrl);
            urlBuilder.setUsername(null);
            urlBuilder.setPassword(null);
            urlBuilder.setPath(null);
            urlBuilder.getQueryParams().clear();
            urlBuilder.setFragment(null);
            return urlBuilder.buildURL();
        } catch (MalformedURLException e) {
            log.error("Couldn't parse request URL, reverting to internal default base URL: {}", requestUrl);
            return "http://localhost:8080";
        }

    }

    public MessageContext buildOutboundMessageContext(AuthnRequest authnRequest, String bindingUri) {
        final MessageContext messageContext = new MessageContext();
        messageContext.setMessage(authnRequest);

        SAMLPeerEntityContext peerContext = messageContext.ensureSubcontext(SAMLPeerEntityContext.class);
        peerContext.setEntityId(AbstractFlowTest.IDP_ENTITY_ID);

        SAMLEndpointContext endpointContext = peerContext.ensureSubcontext(SAMLEndpointContext.class);
        endpointContext.setEndpoint(buildIdpSsoEndpoint(bindingUri, authnRequest.getDestination()));

        SignatureSigningParameters signingParameters = new SignatureSigningParameters();
        signingParameters.setSigningCredential(spCredential);
        SecurityParametersContext secParamsContext =
                messageContext.ensureSubcontext(SecurityParametersContext.class);
        secParamsContext.setSignatureSigningParameters(signingParameters);

        return messageContext;
    }

}

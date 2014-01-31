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

import javax.annotation.Nonnull;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.messaging.SecurityParametersContext;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Action that adds an outbound {@link MessageContext} to the current {@link ProfileRequestContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 */
// TODO Finish Javadoc.
public class InitializeOutboundMessageContext extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(InitializeOutboundMessageContext.class);

    // TODO Remove autowired credential
    /** Test signing credential. */
    @Autowired @Qualifier("idp.Credential") private Credential testSigningCredential;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {

        MessageContext inboundMessageContext = profileRequestContext.getInboundMessageContext();
        if (inboundMessageContext == null) {
            log.debug("{} No inbound message context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }

        // TODO incomplete

        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {

        // TODO Incomplete, see https://wiki.shibboleth.net/confluence/display/IDP30/SAML+1.1+Browser+SSO

        final MessageContext<SAMLObject> msgCtx = new MessageContext<SAMLObject>();
        profileRequestContext.setOutboundMessageContext(msgCtx);

        final SAMLPeerEntityContext peerContext = msgCtx.getSubcontext(SAMLPeerEntityContext.class, true);
        peerContext.setEntityId(getPeerEntityId(profileRequestContext));

        SAMLEndpointContext endpointContext = peerContext.getSubcontext(SAMLEndpointContext.class, true);
        // TODO not correct
        endpointContext.setEndpoint(buildSpAcsEndpoint(getBindingURI(profileRequestContext),
                "https://sp.example.org/ACSURL"));

        final SAMLBindingContext bindingCtx = msgCtx.getSubcontext(SAMLBindingContext.class, true);
        bindingCtx.setBindingUri(getBindingURI(profileRequestContext));
        bindingCtx.setRelayState(SAMLBindingSupport.getRelayState(profileRequestContext.getInboundMessageContext()));

        final SecurityParametersContext secParamCtx = msgCtx.getSubcontext(SecurityParametersContext.class, true);
        secParamCtx.setSignatureSigningParameters(getSignatureSigningParameters(profileRequestContext));

        log.debug("{} Initialized outbound message context", this.getLogPrefix());
    }

    /**
     * TODO not correct
     * 
     * @param binding
     * @param destination
     * @return
     */
    private AssertionConsumerService buildSpAcsEndpoint(String binding, String destination) {
        // TODO wrong
        AssertionConsumerService acsEndpoint =
                (AssertionConsumerService) XMLObjectSupport.getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME)
                        .buildObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        acsEndpoint.setBinding(binding);
        acsEndpoint.setLocation(destination);
        return acsEndpoint;
    }

    /**
     * Get the binding URI.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @return the binding URI
     */
    @Nonnull private String getBindingURI(@Nonnull final ProfileRequestContext profileRequestContext) {
        // TODO Get binding URI from somewhere
        return SAMLConstants.SAML1_POST_BINDING_URI;
    }

    /**
     * Get the peer entity id.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @return the peer entity id
     */
    @Nonnull private String getPeerEntityId(@Nonnull final ProfileRequestContext profileRequestContext) {
        // TODO Get peer entity id from somewhere
        return "http://sp.example.org";
    }

    /**
     * Get the signing credential.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @return the signing credential
     */
    @Nonnull private Credential getSigningCredential(@Nonnull final ProfileRequestContext profileRequestContext) {
        // TODO Get signing credential from security configuration.
        return testSigningCredential;
    }

    /**
     * Get the signature signing parameters.
     * 
     * @param profileRequestContext the current IdP profile request context
     * @return the signature signing parameters
     */
    @Nonnull private SignatureSigningParameters getSignatureSigningParameters(
            @Nonnull final ProfileRequestContext profileRequestContext) {

        final Credential signingCredential = getSigningCredential(profileRequestContext);

        final KeyInfoGenerator kiGenerator =
                SecurityConfigurationSupport.getGlobalXMLSecurityConfiguration().getKeyInfoGeneratorManager()
                        .getDefaultManager().getFactory(signingCredential).newInstance();

        final SignatureSigningParameters signingParameters = new SignatureSigningParameters();
        signingParameters.setSigningCredential(signingCredential);

        // TODO We know it's an RSA key, so just hardcoding for now.
        signingParameters.setSignatureAlgorithmURI(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signingParameters.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
        signingParameters.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signingParameters.setKeyInfoGenerator(kiGenerator);

        return signingParameters;
    }
}

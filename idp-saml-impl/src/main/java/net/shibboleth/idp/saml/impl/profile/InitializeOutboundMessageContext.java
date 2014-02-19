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

package net.shibboleth.idp.saml.impl.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
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
// I think this is going to be an OpenSAML action, assuming we're not setting any security related info here.
public class InitializeOutboundMessageContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeOutboundMessageContext.class);

    // TODO Remove autowired credential
    /** Test signing credential. */
    @Autowired @Qualifier("idp.Credential") private Credential testSigningCredential;

    /** Inbound {@link SAMLPeerEntityContext}. */
    @Nullable private SAMLPeerEntityContext inboundPeerEntityCtx;
    
    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {

        final MessageContext inboundMessageContext = profileRequestContext.getInboundMessageContext();
        if (inboundMessageContext == null) {
            log.debug("{} No inbound message context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        inboundPeerEntityCtx = inboundMessageContext.getSubcontext(SAMLPeerEntityContext.class, false);
        if (inboundPeerEntityCtx == null) {
            log.debug("{} No inbound SAML peer entity context", getLogPrefix());
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

        final MessageContext msgCtx = new MessageContext();
        profileRequestContext.setOutboundMessageContext(msgCtx);

        final SAMLPeerEntityContext peerContext = msgCtx.getSubcontext(SAMLPeerEntityContext.class, true);
        peerContext.setEntityId(inboundPeerEntityCtx.getEntityId());
        
        // There's no defined third-party request mechanism, so copy the metadata context, if any,
        // from inbound to outbound.
        final SAMLMetadataContext inboundMetadataCtx =
                inboundPeerEntityCtx.getSubcontext(SAMLMetadataContext.class, false);
        if (inboundMetadataCtx != null) {
            final SAMLMetadataContext outboundMetadataCtx = peerContext.getSubcontext(SAMLMetadataContext.class, true);
            outboundMetadataCtx.setEntityDescriptor(inboundMetadataCtx.getEntityDescriptor());
            outboundMetadataCtx.setRoleDescriptor(inboundMetadataCtx.getRoleDescriptor());
        }

        // TODO this will be handled by a separate action, leaving for now to keep testbed working
        final SecurityParametersContext secParamCtx = msgCtx.getSubcontext(SecurityParametersContext.class, true);
        secParamCtx.setSignatureSigningParameters(getSignatureSigningParameters(profileRequestContext));

        log.debug("{} Initialized outbound message context", getLogPrefix());
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
        
        // TODO: check for null to allow testing without Spring auto-wiring.
        if (signingCredential == null) {
            return new SignatureSigningParameters();
        }

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
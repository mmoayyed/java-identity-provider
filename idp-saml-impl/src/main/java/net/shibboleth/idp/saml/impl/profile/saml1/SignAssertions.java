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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.profile.SAMLEventIds;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Action that signs {@link Assertion}s in a {@link Response}.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link SAMLEventIds.NO_RESPONSE}
 * @event {@link SAMLEventIds.NO_ASSERTION}
 */
// TODO Complete Javadoc
public class SignAssertions extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SignAssertions.class);

    /** The signature signing parameters. */
    @Nullable private SignatureSigningParameters signatureSigningParameters;

    /** The assertions to be signed. */
    @Nullable private List<Assertion> assertions;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {

        final MessageContext<Response> outboundMsgCtx = profileRequestContext.getOutboundMessageContext();
        if (outboundMsgCtx == null) {
            log.debug("{} No outbound message context available", getId());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }

        final SecurityParametersContext secParamCtx = outboundMsgCtx.getSubcontext(SecurityParametersContext.class);
        if (secParamCtx == null) {
            log.debug("{} Will not sign assertions because no security parameters context is available", getId());
            return false;
        }

        signatureSigningParameters = secParamCtx.getSignatureSigningParameters();
        if (signatureSigningParameters == null) {
            log.debug("{} Will not sign assertions because no signature signing parameters available", getId());
            return false;
        }

        final Response response = outboundMsgCtx.getMessage();
        if (response == null) {
            log.debug("{} No response available", getId());
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.NO_RESPONSE);
            return false;
        }

        assertions = response.getAssertions();
        if (assertions == null || assertions.isEmpty()) {
            log.debug("{} No assertions available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.NO_ASSERTION);
            return false;
        }

        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {
        try {
            // TODO Maybe the response should not be logged ?
            if (log.isTraceEnabled()) {
                final MessageContext<Response> outboundMsgCtx = profileRequestContext.getOutboundMessageContext();
                logResponse("Response before signing:", outboundMsgCtx.getMessage());
            }

            for (Assertion assertion : assertions) {
                SignatureSupport.signObject(assertion, signatureSigningParameters);
            }

            // TODO Maybe the response should not be logged ?
            if (log.isTraceEnabled()) {
                final MessageContext<Response> outboundMsgCtx = profileRequestContext.getOutboundMessageContext();
                logResponse("Response after signing:", outboundMsgCtx.getMessage());
            }
        } catch (SecurityException | MarshallingException | SignatureException e) {
            log.warn("{} Error encountered while signing assertions", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_TO_SIGN);
        }
    }

    /**
     * Log the {@link Response} with the given message at trace level.
     * 
     * @param message the log message
     * @param response the response to be logged
     */
    private void logResponse(@Nonnull final String message, @Nonnull final Response response) {
        if (message != null && response != null) {
            try {
                Element dom = XMLObjectSupport.marshall(response);
                log.trace(message + "\n" + SerializeSupport.prettyPrintXML(dom));
            } catch (MarshallingException e) {
                log.warn("Unable to marshall message for logging purposes", e);
            }
        }
    }

}

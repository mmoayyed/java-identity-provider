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

package net.shibboleth.idp.saml.saml2.profile.impl;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.ExternalAuthenticationContext;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An action that checks for an {@link ExternalAuthenticationContext} for a signaled event via the
 * {@link ExternalAuthenticationContext#getAuthnError()} method, and otherwise enforces the presence
 * of an inbound SAML Response to process.
 * 
 * <p>This is a bridge from the external portion of the SAML proxy implementation to transition
 * back into the flow and pick up any signaled errors if necessary.</p>
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#MESSAGE_PROC_ERROR}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @event various
 */
public class ContinueSAMLAuthentication extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ContinueSAMLAuthentication.class);
    
// Checkstyle: CyclomaticComplexity OFF    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final ExternalAuthenticationContext extContext =
                authenticationContext.getSubcontext(ExternalAuthenticationContext.class);
        if (extContext == null) {
            log.debug("{} No ExternalAuthenticationContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return;
        }

        final MessageContext imc =
                profileRequestContext != null ? profileRequestContext.getInboundMessageContext() : null;  
        assert extContext!= null;
        final String authnError = extContext.getAuthnError(); 
        if (authnError != null) {
            log.info("{} SAML authentication attempt signaled an error: {}", getLogPrefix(),
                    authnError);
            ActionSupport.buildEvent(profileRequestContext, authnError);
            return;
        } 
        if (imc == null) {
            log.info("{} No inbound SAML Response found", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        } 
        final Response response = (Response) imc.getMessage();
        if (response == null || !(response instanceof Response)) {
            log.info("{} Inbound message was not a SAML Response", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.MESSAGE_PROC_ERROR);
            return;
        }
        
        final Status status = response.getStatus() ;
        final StatusCode statusCode = status == null ? null : status.getStatusCode(); 
        if (status == null || statusCode == null || statusCode.getValue() == null) {
            log.info("{} SAML response did not contain a StatusCode", getLogPrefix());
            authenticationContext.removeSubcontext(SAMLAuthnContext.class);
            ActionSupport.buildEvent(profileRequestContext, EventIds.MESSAGE_PROC_ERROR);
            return;
        }
        if (!StatusCode.SUCCESS.equals(statusCode.getValue())) {
            log.info("{} SAML response contained error status: {}", getLogPrefix(), statusCode.getValue());
            authenticationContext.removeSubcontext(SAMLAuthnContext.class);
            ActionSupport.buildEvent(profileRequestContext, EventIds.MESSAGE_PROC_ERROR);
            return;
        }
    }
// Checkstyle: CyclomaticComplexity ON
}
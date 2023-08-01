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

package net.shibboleth.idp.cas.flow.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.AssertionArtifact;
import org.opensaml.saml.saml1.core.Request;
import org.slf4j.Logger;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.cas.config.ValidateConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProtocolParam;
import net.shibboleth.idp.cas.protocol.SamlParam;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Processes the ticket validation request message from decoded SAML 1.1 message and request parameters.
 * Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#ProtocolViolation ProtocolViolation}</li>
 *     <li>{@link ProtocolError#ServiceNotSpecified ServiceNotSpecified}</li>
 *     <li>{@link ProtocolError#TicketNotSpecified TicketNotSpecified}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class ProcessSamlMessageAction extends
        AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessSamlMessageAction.class);

    /** {@inheritDoc} */
    @Override
    @Nullable protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) {

        profileRequestContext.setProfileId(ValidateConfiguration.PROFILE_ID);

        final ParameterMap params = springRequestContext.getRequestParameters();
        final String service = params.get(SamlParam.TARGET.name());
        if (service == null) {
            return ProtocolError.ServiceNotSpecified.event(this);
        }

        // Extract ticket from SAML request
        final MessageContext msgContext = profileRequestContext.ensureInboundMessageContext();
        String ticket = null;
        final Object message = msgContext.getMessage();
        if (message != null && message instanceof Request) {
            final Request request = (Request) message;
            for (final AssertionArtifact artifact : request.getAssertionArtifacts()) {
                ticket = artifact.getValue();
                break;
            }
        } else if (message == null) {
            log.warn("{} Unexpected null message", getLogPrefix());
            return ProtocolError.ProtocolViolation.event(this);            
        } else {
            log.warn("{} Unexpected SAMLObject type {}", getLogPrefix(), message.getClass().getName());
            return ProtocolError.ProtocolViolation.event(this);
        }
        if (ticket == null) {
            return ProtocolError.TicketNotSpecified.event(this);
        }
        final TicketValidationRequest ticketValidationRequest = new TicketValidationRequest(service, ticket);

        final String renew = params.get(ProtocolParam.Renew.id());
        if (renew != null) {
            ticketValidationRequest.setRenew(true);
        }

        try {
            setCASRequest(profileRequestContext, ticketValidationRequest);
        } catch (final EventException e) {
            return ActionSupport.buildEvent(this, e.getEventID());
        }

        return ActionSupport.buildProceedEvent(this);
    }

}
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

package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProtocolParam;
import net.shibboleth.idp.cas.protocol.SamlParam;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.profile.AbstractProfileAction;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml1.core.AssertionArtifact;
import org.opensaml.saml.saml1.core.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Processes the ticket validation request message from decoded SAML 1.1 message and request parameters.
 *
 * @author Marvin S. Addison
 */
public class ProcessSamlMessageAction extends AbstractProfileAction<SAMLObject, Object> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProcessSamlMessageAction.class);

    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext<SAMLObject, Object> profileRequestContext) {

        final ParameterMap params = springRequestContext.getRequestParameters();
        final String service = params.get(SamlParam.TARGET.name());
        if (service == null) {
            return ProtocolError.ServiceNotSpecified.event(this);
        }

        // Extract ticket from SAML request
        final MessageContext<SAMLObject> msgContext = profileRequestContext.getInboundMessageContext();
        String ticket = null;
        if (msgContext.getMessage() instanceof Request) {
            final Request request = ((Request) msgContext.getMessage());
            for (AssertionArtifact artifact : request.getAssertionArtifacts()) {
                ticket = artifact.getAssertionArtifact();
                break;
            }
        } else {
            log.info("Unexpected SAMLObject type {}", msgContext.getMessage().getClass().getName());
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

        FlowStateSupport.setTicketValidationRequest(springRequestContext, ticketValidationRequest);
        return Events.Proceed.event(this);
    }
}

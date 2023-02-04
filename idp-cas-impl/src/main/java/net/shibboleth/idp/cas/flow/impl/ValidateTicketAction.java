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

package net.shibboleth.idp.cas.flow.impl;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.cas.config.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.LoginConfiguration;
import net.shibboleth.idp.cas.config.ProxyConfiguration;
import net.shibboleth.idp.cas.config.ValidateConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * CAS protocol service ticket validation action. Emits one of the following events based on validation result:
 *
 * <ul>
 *     <li>{@link Events#ServiceTicketValidated ServiceTicketValidated}</li>
 *     <li>{@link Events#ProxyTicketValidated ProxyTicketValidated}</li>
 *     <li>{@link ProtocolError#InvalidTicketFormat InvalidTicketFormat}</li>
 *     <li>{@link ProtocolError#ServiceMismatch ServiceMismatch}</li>
 *     <li>{@link ProtocolError#TicketExpired TicketExpired}</li>
 *     <li>{@link ProtocolError#TicketRetrievalError TicketRetrievalError}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class ValidateTicketAction extends AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateTicketAction.class);

    /** Profile configuration lookup function. */
    @Nonnull private final ConfigLookupFunction<ValidateConfiguration> configLookupFunction;

    /** Manages CAS tickets. */
    @Nonnull private final TicketService casTicketService;

    /** Profile config. */
    @Nullable private ValidateConfiguration validateConfig;

    /** CAS request. */
    @Nullable private TicketValidationRequest request;
    
    /**
     * Constructor.
     *
     * @param ticketService ticket service component
     */
    public ValidateTicketAction(@Nonnull final TicketService ticketService) {
        casTicketService = Constraint.isNotNull(ticketService, "TicketService cannot be null");
        configLookupFunction = new ConfigLookupFunction<>(ValidateConfiguration.class);
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        validateConfig = configLookupFunction.apply(profileRequestContext);
        if (validateConfig == null) {
            ActionSupport.buildEvent(profileRequestContext,IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        try {
            request = getCASRequest(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }

        return true;
    }
    
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final Ticket ticket;
        try {
            final String ticketId = request.getTicket();
            log.debug("Attempting to validate {}", ticketId);
            if (ticketId.startsWith(LoginConfiguration.DEFAULT_TICKET_PREFIX)) {
                ticket = casTicketService.removeServiceTicket(request.getTicket());
            } else if (ticketId.startsWith(ProxyConfiguration.DEFAULT_TICKET_PREFIX)) {
                ticket = casTicketService.removeProxyTicket(ticketId);
            } else {
                ActionSupport.buildEvent(profileRequestContext, ProtocolError.InvalidTicketFormat.event(this));
                return;
            }
            if (ticket != null) {
                log.debug("{} Found and removed {}/{} from ticket store", getLogPrefix(), ticket,
                        ticket.getSessionId());
            }
        } catch (final RuntimeException e) {
            log.debug("{} CAS ticket retrieval failed with error: {}", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.TicketRetrievalError.event(this));
            return;
        }

        if (ticket == null || Instant.now().isAfter(ticket.getExpirationInstant())) {
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.TicketExpired.event(this));
            return;
        }

        if (validateConfig.getServiceComparator(profileRequestContext).compare(
                ticket.getService(), request.getService()) != 0) {
            log.debug("{} Service issued for {} does not match {}", getLogPrefix(), ticket.getService(),
                    request.getService());
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.ServiceMismatch.event(this));
            return;
        }

        try {
            setCASResponse(profileRequestContext, new TicketValidationResponse());
            setCASTicket(profileRequestContext, ticket);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return;
        }

        log.info("{} Successfully validated {} for {}", getLogPrefix(), request.getTicket(), request.getService());
        
        if (ticket instanceof ProxyTicket) {
            ActionSupport.buildEvent(profileRequestContext, Events.ProxyTicketValidated.event(this));
            return;
        }
        
        ActionSupport.buildEvent(profileRequestContext, Events.ServiceTicketValidated.event(this));
    }

}
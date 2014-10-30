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

import net.shibboleth.idp.cas.config.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.ServiceTicketConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketResponse;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.joda.time.DateTime;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Generates and stores a CAS protocol service ticket. Possible outcomes:
 * <ul>
 *     <li>{@link Events#Success success}</li>
 *     <li>{@link ProtocolError#TicketCreationError ticketCreationError}</li>
 * </ul>
 * In the success case a {@link ServiceTicketResponse} message is created and stored
 * as request scope parameter under the key {@value FlowStateSupport#SERVICE_TICKET_RESPONSE_KEY}.
 *
 * @author Marvin S. Addison
 */
public class GrantServiceTicketAction extends AbstractProfileAction<ServiceTicketRequest, ServiceTicketRequest> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(GrantServiceTicketAction.class);

    /** Profile configuration lookup function. */
    private final ConfigLookupFunction<ServiceTicketConfiguration> configLookupFunction =
            new ConfigLookupFunction<>(ServiceTicketConfiguration.class);

    /** Manages CAS tickets. */
    @Nonnull
    private final TicketService ticketService;


    /**
     * Creates a new instance.
     *
     * @param ticketService Ticket service component.
     */
    public GrantServiceTicketAction(@Nonnull TicketService ticketService) {
        this.ticketService = Constraint.isNotNull(ticketService, "TicketService cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext<ServiceTicketRequest, ServiceTicketRequest> profileRequestContext) {

        final ServiceTicketRequest request = FlowStateSupport.getServiceTicketRequest(springRequestContext);
        final SessionContext sessionCtx = profileRequestContext.getSubcontext(SessionContext.class, false);
        if (sessionCtx == null || sessionCtx.getIdPSession() == null) {
            log.info("Cannot locate IdP session");
            return ProtocolError.IllegalState.event(this);
        }
        final ServiceTicketConfiguration config = configLookupFunction.apply(profileRequestContext);
        if (config == null) {
            log.info("Service ticket configuration undefined");
            return ProtocolError.IllegalState.event(this);
        }
        if (config.getSecurityConfiguration() == null || config.getSecurityConfiguration().getIdGenerator() == null) {
            log.info("Invalid service ticket configuration: SecurityConfiguration#idGenerator undefined");
            return ProtocolError.IllegalState.event(this);
        }
        final ServiceTicket ticket;
        try {
            log.debug("Granting service ticket for {}", request.getService());
            ticket = ticketService.createServiceTicket(
                    config.getSecurityConfiguration().getIdGenerator().generateIdentifier(),
                    DateTime.now().plus(config.getTicketValidityPeriod()).toInstant(),
                    sessionCtx.getIdPSession().getId(),
                    request.getService(),
                    request.isRenew());
        } catch (RuntimeException e) {
            log.error("Failed granting service ticket due to error.", e);
            return ProtocolError.TicketCreationError.event(this);
        }
        log.info("Granted service ticket for {}", request.getService());
        final ServiceTicketResponse response = new ServiceTicketResponse(request.getService(), ticket.getId());
        if (request.isSaml()) {
            response.setSaml(true);
        }
        FlowStateSupport.setServiceTicketResponse(springRequestContext, response);
        return Events.Success.event(this);
    }
}

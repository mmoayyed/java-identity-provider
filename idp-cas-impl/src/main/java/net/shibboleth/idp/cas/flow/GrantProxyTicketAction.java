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
import net.shibboleth.idp.cas.config.ProxyTicketConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ProxyTicketResponse;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.TicketContext;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.joda.time.DateTime;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Generates and stores a CAS protocol proxy ticket. Possible outcomes:
 * <ul>
 *     <li>{@link Events#Success success}</li>
 *     <li>{@link ProtocolError#TicketCreationError ticketCreationError}</li>
 * </ul>
 * In the success case a {@link ProxyTicketResponse} message is created and stored
 * as request scope parameter under the key {@value FlowStateSupport#PROXY_TICKET_RESPONSE_KEY}.
 *
 * @author Marvin S. Addison
 */
public class GrantProxyTicketAction extends AbstractProfileAction<ProxyTicketRequest, ProxyTicketResponse> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(GrantProxyTicketAction.class);


    /** Profile configuration lookup function. */
    private final ConfigLookupFunction<ProxyTicketConfiguration> configLookupFunction =
            new ConfigLookupFunction<>(ProxyTicketConfiguration.class);

    /** Manages CAS tickets. */
    @Nonnull
    private TicketService ticketService;


    public void setTicketService(@Nonnull final TicketService ticketService) {
        this.ticketService = Constraint.isNotNull(ticketService, "Ticket service cannot be null.");
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext<ProxyTicketRequest, ProxyTicketResponse> profileRequestContext) {

        final ProxyTicketRequest request = FlowStateSupport.getProxyTicketRequest(springRequestContext);
        final TicketContext ticketContext = profileRequestContext.getSubcontext(TicketContext.class);
        if (ticketContext == null) {
            log.info("TicketContext not found in profile request context.");
            return ProtocolError.IllegalState.event(this);
        }
        final ProxyGrantingTicket pgt = (ProxyGrantingTicket) ticketContext.getTicket();
        final ProxyTicketConfiguration config = configLookupFunction.apply(profileRequestContext);
        if (config == null) {
            log.info("Proxy ticket configuration undefined");
            return ProtocolError.IllegalState.event(this);
        }
        if (config.getSecurityConfiguration() == null || config.getSecurityConfiguration().getIdGenerator() == null) {
            log.info("Invalid proxy ticket configuration: SecurityConfiguration#idGenerator undefined");
            return ProtocolError.IllegalState.event(this);
        }
        final ProxyTicket pt;
        try {
            log.debug("Granting proxy ticket for {}", request.getTargetService());
            pt = ticketService.createProxyTicket(
                    config.getSecurityConfiguration().getIdGenerator().generateIdentifier(),
                    DateTime.now().plus(config.getTicketValidityPeriod()).toInstant(),
                    pgt,
                    request.getTargetService());
        } catch (RuntimeException e) {
            log.error("Failed granting proxy ticket due to error.", e);
            return ProtocolError.TicketCreationError.event(this);
        }
        log.info("Granted proxy ticket for {}", request.getTargetService());
        FlowStateSupport.setProxyTicketResponse(springRequestContext, new ProxyTicketResponse(pt.getId()));
        return Events.Success.event(this);
    }
}

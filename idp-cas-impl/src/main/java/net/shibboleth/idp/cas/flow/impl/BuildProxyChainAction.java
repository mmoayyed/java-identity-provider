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

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
/**
 * Action that builds the chain of visited proxies for a successful proxy ticket validation event. Possible outcomes:
 *
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#BrokenProxyChain BrokenProxyChain}</li>
 *     <li>{@link ProtocolError#InvalidTicketType InvalidTicketType}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class BuildProxyChainAction
        extends AbstractCASProtocolAction<TicketValidationRequest,TicketValidationResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BuildProxyChainAction.class);

    /** Manages CAS tickets. */
    @Nonnull private final TicketService casTicketService;
    
    /** Response. */
    @Nullable private TicketValidationResponse response;
    
    /** Ticket. */
    @Nullable private Ticket ticket;

    /**
     * Constructor.
     *
     * @param ticketService ticket service component.
     */
    public BuildProxyChainAction(@Nonnull final TicketService ticketService) {
        casTicketService = Constraint.isNotNull(ticketService, "TicketService cannot be null");
    }
    
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        try {
            response = getCASResponse(profileRequestContext);
            ticket = getCASTicket(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }
        
        return true;
    }

    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!(ticket instanceof ProxyTicket)) {
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.InvalidTicketType.event(this));
            return;
        }
        final ProxyTicket pt = (ProxyTicket) ticket;
        ProxyGrantingTicket pgt;
        String pgtId = pt.getPgtId();
        do {
            pgt = casTicketService.fetchProxyGrantingTicket(pgtId);
            if (pgt == null || Instant.now().isAfter(pgt.getExpirationInstant())) {
                log.debug("{} PGT {} {}", getLogPrefix(), pgtId, pgt == null ? "not found" : "expired");
                ActionSupport.buildEvent(profileRequestContext, ProtocolError.BrokenProxyChain.event(this));
                return;
            }
            response.addProxy(pgt.getProxyCallbackUrl());
            pgtId = pgt.getParentId();
        } while (pgtId != null);
    }

}
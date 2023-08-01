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

import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProtocolParam;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ProxyTicketResponse;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.TicketContext;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Initializes the CAS protocol interaction at the <code>/proxy</code> URI. Returns one of the following events:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#ServiceNotSpecified ServiceNotSpecified}</li>
 *     <li>{@link ProtocolError#TicketExpired TicketExpired}</li>
 *     <li>{@link ProtocolError#TicketNotSpecified TicketNotSpecified}</li>
 *     <li>{@link ProtocolError#TicketRetrievalError TicketRetrievalError}</li>
 * </ul>
 * <p>
 * Creates the following contexts on success:
 * <ul>
 *     <li><code>ProfileRequestContext</code> -&gt; {@link net.shibboleth.idp.cas.protocol.ProtocolContext}</li>
 *     <li><code>ProfileRequestContext</code> -&gt; <code>ProtocolContext</code> -&gt; {@link TicketContext}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class InitializeProxyAction extends AbstractCASProtocolAction<ProxyTicketRequest, ProxyTicketResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeProxyAction.class);

    /** Manages CAS tickets. */
    @Nonnull private final TicketService casTicketService;

    /**
     * Constructor.
     *
     * @param ticketService ticket service
     */
    public InitializeProxyAction(@Nonnull final TicketService ticketService) {
        casTicketService = Constraint.isNotNull(ticketService, "Ticket service cannot be null.");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) {
        
        final ParameterMap params = springRequestContext.getRequestParameters();
        String service = params.get(ProtocolParam.TargetService.id());
        Event result = null;
        if (service == null) {
            service = ProtocolError.ServiceNotSpecified.getDetailCode();
            result = ProtocolError.ServiceNotSpecified.event(this);
        }
        String ticket = params.get(ProtocolParam.Pgt.id());
        if (ticket == null) {
            ticket = ProtocolError.TicketNotSpecified.getDetailCode();
            result = ProtocolError.TicketNotSpecified.event(this);
        }
        final ProxyTicketRequest proxyTicketRequest = new ProxyTicketRequest(ticket, service);
        try {
            setCASRequest(profileRequestContext, proxyTicketRequest);
        } catch (final EventException e) {
            return ActionSupport.buildEvent(this, e.getEventID());
        }
        
        if (result == null) {
            try {
                log.debug("{} Fetching proxy-granting ticket {}", getLogPrefix(), proxyTicketRequest.getPgt());
                final ProxyGrantingTicket pgt = casTicketService.fetchProxyGrantingTicket(proxyTicketRequest.getPgt());
                if (pgt == null) {
                    return ProtocolError.TicketExpired.event(this);
                }
                setCASTicket(profileRequestContext, pgt);
            } catch (final Exception e) {
                log.error("{} Failed looking up {}", getLogPrefix(), proxyTicketRequest.getPgt(), e);
                return ProtocolError.TicketRetrievalError.event(this);
            }
        }
        
        return result;
    }
    
}
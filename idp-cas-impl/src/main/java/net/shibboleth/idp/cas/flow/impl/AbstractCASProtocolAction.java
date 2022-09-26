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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for CAS protocol actions.
 * 
 * @param <RequestType> request
 * @param <ResponseType> response
 *
 * @author Marvin S. Addison
 */
public abstract class AbstractCASProtocolAction<RequestType, ResponseType> extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractCASProtocolAction.class);
    
    /** Looks up a CAS protocol context from IdP profile request context. */
    @Nonnull
    private final Function<ProfileRequestContext,ProtocolContext<RequestType,ResponseType>> protocolLookupFunction;

    /** Constructor. */
    public AbstractCASProtocolAction() {
        protocolLookupFunction = new ChildContextLookup(ProtocolContext.class, true);
    }

    /**
     * Get the CAS request.
     * 
     * @param prc profile request context
     * @return CAS request
     * 
     * @throws EventException to propagate an event 
     */
    @Nonnull protected RequestType getCASRequest(@Nullable final ProfileRequestContext prc) throws EventException {
        final RequestType request = getProtocolContext(prc).getRequest();
        if (request == null) {
            log.error("{} CAS protocol request not found", getLogPrefix());
            throw new EventException(EventIds.INVALID_MSG_CTX);
        }
        return request;
    }

    /**
     * Set the CAS request.
     * 
     * @param prc profile request context
     * @param request CAS request
     * 
     * @throws EventException to propagate an event 
     */
    protected void setCASRequest(@Nullable final ProfileRequestContext prc, @Nonnull final RequestType request)
            throws EventException {
        getProtocolContext(prc).setRequest(Constraint.isNotNull(request, "CAS request cannot be null"));
    }

    /**
     * Get the CAS response.
     * 
     * @param prc profile request context
     * @return CAS response
     * 
     * @throws EventException to propagate an event
     */
    @Nonnull protected ResponseType getCASResponse(@Nullable final ProfileRequestContext prc) throws EventException {
        final ResponseType response = getProtocolContext(prc).getResponse();
        if (response == null) {
            log.error("{} CAS protocol response not found", getLogPrefix());
            throw new EventException(EventIds.INVALID_MSG_CTX);
        }
        return response;
    }

    /**
     * Set the CAS response.
     * 
     * @param prc profile request context
     * @param response CAS response
     * 
     * @throws EventException to propagate an event 
     */
    protected void setCASResponse(@Nullable final ProfileRequestContext prc, @Nonnull final ResponseType response)
            throws EventException {
        getProtocolContext(prc).setResponse(Constraint.isNotNull(response, "CAS response cannot be null"));
    }

    /**
     * Get the CAS ticket.
     * 
     * @param prc profile request context
     * @return CAS ticket
     * 
     * @throws EventException to propagate an event 
     */
    @Nonnull protected Ticket getCASTicket(final ProfileRequestContext prc) throws EventException {
        final TicketContext context = getProtocolContext(prc).getSubcontext(TicketContext.class);
        if (context == null || context.getTicket() == null) {
            log.error("{} CAS protocol ticket not found", getLogPrefix());
            throw new EventException(EventIds.INVALID_MSG_CTX);
        }
        return context.getTicket();
    }

    /**
     * Set the CAS ticket.
     * 
     * @param prc profile request context
     * @param ticket CAS ticket
     * 
     * @throws EventException to propagate an event 
     */
    protected void setCASTicket(@Nullable final ProfileRequestContext prc, @Nonnull final Ticket ticket)
            throws EventException {
        getProtocolContext(prc).addSubcontext(
                new TicketContext(Constraint.isNotNull(ticket, "CAS ticket cannot be null")));
    }

    /**
     * Get the CAS service.
     * 
     * @param prc profile request context
     * @return CAS service
     * 
     * @throws EventException to propagate an event
     */
    @Nonnull protected Service getCASService(@Nullable final ProfileRequestContext prc) throws EventException {
        final ServiceContext context = getProtocolContext(prc).getSubcontext(ServiceContext.class);
        if (context == null || context.getService() == null) {
            log.error("{} CAS protocol service not found", getLogPrefix());
            throw new EventException(EventIds.INVALID_MSG_CTX);
        }
        return context.getService();
    }

    /**
     * Set the CAS service.
     * 
     * @param prc profile request context
     * @param service CAS service
     * 
     * @throws EventException to propagate an event
     */
    protected void setCASService(@Nullable final ProfileRequestContext prc, @Nonnull final Service service)
            throws EventException {
        getProtocolContext(prc).addSubcontext(
                new ServiceContext(Constraint.isNotNull(service, "CAS service cannot be null")));
    }

    /**
     * Get the CAS protocol context.
     * 
     * @param prc profile request context
     * @return CAS protocol context
     * 
     * @throws EventException to propagate an event 
     */
    @Nonnull protected ProtocolContext<RequestType,ResponseType> getProtocolContext(
            @Nullable final ProfileRequestContext prc) throws EventException {
        final ProtocolContext<RequestType,ResponseType> casCtx = protocolLookupFunction.apply(prc);
        if (casCtx == null) {
            log.error("{} CAS ProtocolContext not found in ProfileRequestContext", getLogPrefix());
            throw new EventException(EventIds.INVALID_PROFILE_CTX);
        }
        return casCtx;
    }

}
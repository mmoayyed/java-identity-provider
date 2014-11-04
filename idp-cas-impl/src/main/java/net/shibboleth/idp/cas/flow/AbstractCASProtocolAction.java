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

import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.profile.context.ProfileRequestContext;

import javax.annotation.Nonnull;

/**
 * Base class for CAS protocol actions.
 *
 * @author Marvin S. Addison
 */
public abstract class AbstractCASProtocolAction<RequestType, ResponseType> extends AbstractProfileAction {

    @Nonnull
    protected RequestType getCASRequest(final ProfileRequestContext prc) {
        final RequestType request = getProtocolContext(prc).getRequest();
        if (request == null) {
            throw new IllegalStateException("CAS protocol request not found");
        }
        return request;
    }

    protected void setCASRequest(final ProfileRequestContext prc, @Nonnull final RequestType request) {
        getProtocolContext(prc).setRequest(Constraint.isNotNull(request, "CAS request cannot be null"));
    }

    @Nonnull
    protected ResponseType getCASResponse(final ProfileRequestContext prc) {
        final ResponseType response = getProtocolContext(prc).getResponse();
        if (response == null) {
            throw new IllegalStateException("CAS protocol response not found");
        }
        return response;
    }

    protected void setCASResponse(final ProfileRequestContext prc, @Nonnull final ResponseType response) {
        getProtocolContext(prc).setResponse(Constraint.isNotNull(response, "CAS response cannot be null"));
    }

    @Nonnull
    protected Ticket getCASTicket(final ProfileRequestContext prc) {
        final TicketContext context = getProtocolContext(prc).getSubcontext(TicketContext.class);
        if (context == null || context.getTicket() == null) {
            throw new IllegalStateException("CAS protocol ticket not found");
        }
        return context.getTicket();
    }

    protected void setCASTicket(final ProfileRequestContext prc, @Nonnull final Ticket ticket) {
        getProtocolContext(prc).addSubcontext(
                new TicketContext(Constraint.isNotNull(ticket, "CAS ticket cannot be null")));
    }


    @Nonnull
    protected Service getCASService(final ProfileRequestContext prc) {
        final ServiceContext context = getProtocolContext(prc).getSubcontext(ServiceContext.class);
        if (context == null || context.getService() == null) {
            throw new IllegalStateException("CAS protocol service not found");
        }
        return context.getService();
    }

    protected void setCASService(final ProfileRequestContext prc, @Nonnull final Service service) {
        getProtocolContext(prc).addSubcontext(
                new ServiceContext(Constraint.isNotNull(service, "CAS service cannot be null")));
    }

    @Nonnull
    protected IdPSession getIdPSession(final ProfileRequestContext prc) {
        final SessionContext sessionContext = prc.getSubcontext(SessionContext.class);
        if (sessionContext == null || sessionContext.getIdPSession() == null) {
            throw new IllegalStateException("Cannot locate IdP session");
        }
        return sessionContext.getIdPSession();
    }

    protected ProtocolContext<RequestType, ResponseType> getProtocolContext(final ProfileRequestContext prc) {
        ProtocolContext<RequestType, ResponseType> casCtx = prc.getSubcontext(ProtocolContext.class, false);
        if (casCtx != null) {
            return casCtx;
        }
        casCtx = new ProtocolContext<>();
        prc.addSubcontext(casCtx);
        return casCtx;
    }
}

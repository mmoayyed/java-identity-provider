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

package net.shibboleth.idp.cas.ticket.impl;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.storage.StorageService;

/**
 * Simple CAS ticket management service that delegates storage to {@link org.opensaml.storage.StorageService}.
 *
 * @author Marvin S. Addison
 */
public class SimpleTicketService extends AbstractTicketService {

    /**
     * Creates a new instance.
     *
     * @param service Storage service to which tickets are persisted.
     */
    public SimpleTicketService(@Nonnull @ParameterName(name="service") final StorageService service) {
        super(service);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public ServiceTicket createServiceTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final String service,
            @Nullable final TicketState state,
            final boolean renew) {
        Constraint.isNotNull(state, "State cannot be null");
        final ServiceTicket st = new ServiceTicket(
                Constraint.isNotNull(id, "ID cannot be null"),
                Constraint.isNotNull(service, "Service cannot be null"),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                renew);
        st.setTicketState(state);
        store(st);
        return st;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public ServiceTicket removeServiceTicket(@Nonnull final String id) {
        Constraint.isNotNull(id, "Id cannot be null");
        return delete(id, ServiceTicket.class);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public ProxyTicket createProxyTicket(
            @Nonnull final String id,
            @Nonnull final Instant expiry,
            @Nonnull final ProxyGrantingTicket pgt,
            @Nonnull final String service) {
        Constraint.isNotNull(pgt, "ProxyGrantingTicket cannot be null");
        final ProxyTicket pt = new ProxyTicket(
                Constraint.isNotNull(id, "ID cannot be null"),
                Constraint.isNotNull(service, "Service cannot be null"),
                Constraint.isNotNull(expiry, "Expiry cannot be null"),
                pgt.getId());
        pt.setTicketState(pgt.getTicketState());
        store(pt);
        return pt;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public ProxyTicket removeProxyTicket(final @Nonnull String id) {
        return delete(id, ProxyTicket.class);
    }

}
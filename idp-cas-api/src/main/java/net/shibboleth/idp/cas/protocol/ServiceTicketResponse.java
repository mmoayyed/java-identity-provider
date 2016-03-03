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

package net.shibboleth.idp.cas.protocol;

import net.shibboleth.utilities.java.support.logic.Constraint;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;

/**
 * CAS protocol response message for a successfully granted service ticket.
 *
 * @author Marvin S. Addison
 */
public class ServiceTicketResponse {
    /** Service URL */
    @Nonnull
    private final String service;

    /** Granted service ticket. */
    @Nonnull
    private final String ticket;

    /** Flag indicating whether ticket request is via SAML 1.1 protocol. */
    private boolean saml;


    /**
     * Creates a CAS service ticket response message for a service and granted ticket.
     *
     * @param service Service that requested ticket.
     * @param ticket Granted service ticket.
     */
    public ServiceTicketResponse(@Nonnull final String service, @Nonnull final String ticket) {
        this.service = Constraint.isNotNull(service, "Service cannot be null");
        this.ticket = Constraint.isNotNull(ticket, "Ticket cannot be null");
    }

    @Nonnull public String getService() {
        return service;
    }

    @Nonnull public String getTicket() {
        return ticket;
    }

    public boolean isSaml() {
        return saml;
    }

    public void setSaml(final boolean saml) {
        this.saml = saml;
    }

    /**
     * @return The name of the ticket parameter returned to the requesting service.
     */
    public String getTicketParameterName() {
        if (saml) {
            return SamlParam.SAMLart.name();
        }
        return ProtocolParam.Ticket.id();
    }

    /**
     * @return URL that may be used to redirect to a service with a granted ticket.
     */
    public String getRedirectUrl() {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(service);
        builder.queryParam(getTicketParameterName(), ticket);
        return builder.build().toUriString();
    }
}

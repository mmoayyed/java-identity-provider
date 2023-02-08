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

import javax.annotation.Nonnull;

import org.springframework.web.util.UriComponentsBuilder;

import net.shibboleth.shared.logic.Constraint;

/**
 * CAS protocol response message for a successfully granted service ticket.
 *
 * @author Marvin S. Addison
 */
public class ServiceTicketResponse {
    /** Service URL. */
    @Nonnull private final String serviceURL;

    /** Granted service ticket. */
    @Nonnull private final String serviceTicket;

    /** Flag indicating whether ticket request is via SAML 1.1 protocol. */
    private boolean saml;


    /**
     * Creates a CAS service ticket response message for a service and granted ticket.
     *
     * @param service Service that requested ticket.
     * @param ticket Granted service ticket.
     */
    public ServiceTicketResponse(@Nonnull final String service, @Nonnull final String ticket) {
        serviceURL = Constraint.isNotNull(service, "Service cannot be null");
        serviceTicket = Constraint.isNotNull(ticket, "Ticket cannot be null");
    }

    /**
     * Get the service that requested the ticket.
     * 
     * @return service that requested the ticket
     */
    @Nonnull public String getService() {
        return serviceURL;
    }

    /**
     * Get the service that requested the ticket.
     * 
     * @return the service that requested the ticket
     */
    @Nonnull public String getTicket() {
        return serviceTicket;
    }

    /**
     * Get whether ticket request is via SAML 1.1 protocol.
     * 
     * @return whether ticket request is via SAML 1.1 protocol
     */
    public boolean isSaml() {
        return saml;
    }
    
    /**
     * Set whether ticket request is via SAML 1.1 protocol.
     * 
     * @param flag flag to set
     */
    public void setSaml(final boolean flag) {
        saml = flag;
    }

    /**
     * Get the name of the ticket parameter returned to the requesting service.
     * 
     * @return the name of the ticket parameter returned to the requesting service
     */
    @Nonnull public String getTicketParameterName() {
        final String result;
        if (saml) {
            result = SamlParam.SAMLart.name();
        } else {
            result = ProtocolParam.Ticket.id();
        }
        assert result != null;
        return result;
    }

    /**
     * Get the URL that may be used to redirect to a service with a granted ticket.
     * 
     * @return URL that may be used to redirect to a service with a granted ticket
     */
    public String getRedirectUrl() {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(serviceURL);
        builder.queryParam(getTicketParameterName(), serviceTicket);
        return builder.build().toUriString();
    }
}

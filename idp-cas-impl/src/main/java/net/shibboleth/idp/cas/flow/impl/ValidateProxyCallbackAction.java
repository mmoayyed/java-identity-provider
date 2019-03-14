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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.apache.http.client.utils.URIBuilder;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.cas.config.impl.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.impl.ValidateConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProtocolParam;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.proxy.ProxyIdentifiers;
import net.shibboleth.idp.cas.proxy.ProxyValidator;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketServiceEx;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Validates the proxy callback URL provided in the service ticket validation request and creates a PGT when
 * the proxy callback is successfully authenticated. Possible outcomes:
 *
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#ProxyCallbackAuthenticationFailure ProxyCallbackAuthenticationFailure}</li>
 * </ul>
 *
 * On success, the PGTIOU is accessible at {@link TicketValidationResponse#getPgtIou()}.
 *
 * @author Marvin S. Addison
 */
public class ValidateProxyCallbackAction
    extends AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ValidateProxyCallbackAction.class);

    /** Profile configuration lookup function. */
    private final ConfigLookupFunction<ValidateConfiguration> configLookupFunction =
            new ConfigLookupFunction<>(ValidateConfiguration.class);

    /** Validates the proxy callback endpoint. */
    @Nonnull
    private final ProxyValidator proxyValidator;

    /** Manages CAS tickets. */
    @Nonnull
    private final TicketServiceEx ticketServiceEx;


    /**
     * Creates a new instance.
     *
     * @param validator Component that validates the proxy callback endpoint.
     * @param ticketService Ticket service component.
     */
    public ValidateProxyCallbackAction(
            @Nonnull final ProxyValidator validator,
            @Nonnull final TicketServiceEx ticketService) {
        proxyValidator = Constraint.isNotNull(validator, "ProxyValidator cannot be null");
        ticketServiceEx = Constraint.isNotNull(ticketService, "TicketService cannot be null");
    }

    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final TicketValidationRequest request = getCASRequest(profileRequestContext);
        final TicketValidationResponse response = getCASResponse(profileRequestContext);
        final Ticket ticket = getCASTicket(profileRequestContext);
        final ValidateConfiguration config = configLookupFunction.apply(profileRequestContext);
        if (config == null) {
            throw new IllegalStateException("Proxy-granting ticket configuration undefined");
        }
        if (config.getSecurityConfiguration() == null || config.getSecurityConfiguration().getIdGenerator() == null) {
            throw new IllegalStateException(
                    "Invalid proxy-granting ticket configuration: SecurityConfiguration#idGenerator undefined");
        }
        if (config.getPGTIOUGenerator() == null) {
            throw new IllegalStateException("Invalid proxy-granting ticket configuration: PGTIOUGenerator undefined");
        }
        final ProxyIdentifiers proxyIds = new ProxyIdentifiers(
                config.getSecurityConfiguration().getIdGenerator().generateIdentifier(),
                config.getPGTIOUGenerator().generateIdentifier());
        final URI proxyCallbackUri;
        try {
            proxyCallbackUri = new URIBuilder(request.getPgtUrl())
                    .addParameter(ProtocolParam.PgtId.id(), proxyIds.getPgtId())
                    .addParameter(ProtocolParam.PgtIou.id(), proxyIds.getPgtIou())
                    .build();
        } catch (final URISyntaxException e) {
            throw new RuntimeException("Error creating proxy callback URL", e);
        }
        try {
            log.debug("Attempting proxy authentication to {}", proxyCallbackUri);
            proxyValidator.validate(profileRequestContext, proxyCallbackUri);
            final Instant expiration = Instant.now().plus(config.getTicketValidityPeriod());
            if (ticket instanceof ServiceTicket) {
                ticketServiceEx.createProxyGrantingTicket(proxyIds.getPgtId(), expiration, (ServiceTicket) ticket);
            } else {
                ticketServiceEx.createProxyGrantingTicket(proxyIds.getPgtId(), expiration, (ProxyTicket) ticket);
            }
            response.setPgtIou(proxyIds.getPgtIou());
        } catch (final Exception e) {
            log.info("Proxy authentication failed for " + request.getPgtUrl() + ": " + e);
            return ProtocolError.ProxyCallbackAuthenticationFailure.event(this);
        }
        return null;
    }
}

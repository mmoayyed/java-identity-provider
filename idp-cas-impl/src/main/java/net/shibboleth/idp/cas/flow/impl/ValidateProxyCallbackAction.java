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
import javax.annotation.Nullable;

import org.apache.http.client.utils.URIBuilder;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.cas.config.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.ValidateConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProtocolParam;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.proxy.ProxyIdentifiers;
import net.shibboleth.idp.cas.proxy.ProxyValidator;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.config.SecurityConfiguration;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

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
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateProxyCallbackAction.class);

    /** Profile configuration lookup function. */
    @Nonnull private final ConfigLookupFunction<ValidateConfiguration> configLookupFunction;

    /** Validates the proxy callback endpoint. */
    @Nonnull private final ProxyValidator proxyValidator;

    /** Manages CAS tickets. */
    @Nonnull private final TicketService casTicketService;

    /** Profile config. */
    @Nullable private ValidateConfiguration validateConfig;
    
    /** Security config. */
    @Nullable private SecurityConfiguration securityConfig;
    
    /** CAS ticket. */
    @Nullable private Ticket ticket;

    /** CAS request. */
    @Nullable private TicketValidationRequest request;

    /** CAS response. */
    @Nullable private TicketValidationResponse response;
    
    /**
     * Constructor.
     *
     * @param validator Component that validates the proxy callback endpoint.
     * @param ticketService Ticket service component.
     */
    public ValidateProxyCallbackAction(@Nonnull final ProxyValidator validator,
            @Nonnull final TicketService ticketService) {
        proxyValidator = Constraint.isNotNull(validator, "ProxyValidator cannot be null");
        casTicketService = Constraint.isNotNull(ticketService, "TicketService cannot be null");
        
        configLookupFunction = new ConfigLookupFunction<>(ValidateConfiguration.class);
    }
    
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        validateConfig = configLookupFunction.apply(profileRequestContext);
        if (validateConfig == null) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        securityConfig = validateConfig.getSecurityConfiguration(profileRequestContext);
        if (securityConfig == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_SEC_CFG);
            return false;
        }
        
        try {
            ticket = getCASTicket(profileRequestContext);
            request = getCASRequest(profileRequestContext);
            response = getCASResponse(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }

        return true;
    }

    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final IdentifierGenerationStrategy pgtGenerator = validateConfig.getPGTIOUGenerator(profileRequestContext);
        final ProxyIdentifiers proxyIds = new ProxyIdentifiers(
                securityConfig.getIdGenerator().generateIdentifier(),
                pgtGenerator.generateIdentifier());
        final URI proxyCallbackUri;
        try {
            proxyCallbackUri = new URIBuilder(request.getPgtUrl())
                    .addParameter(ProtocolParam.PgtId.id(), proxyIds.getPgtId())
                    .addParameter(ProtocolParam.PgtIou.id(), proxyIds.getPgtIou())
                    .build();
        } catch (final URISyntaxException e) {
            log.warn("{} Error creating proxy callback URL", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.RUNTIME_EXCEPTION);
            return;
        }
        
        try {
            log.debug("{} Attempting proxy authentication to {}", getLogPrefix(), proxyCallbackUri);
            proxyValidator.validate(profileRequestContext, proxyCallbackUri);
            final Instant expiration =
                    Instant.now().plus(validateConfig.getTicketValidityPeriod(profileRequestContext));
            if (ticket instanceof ServiceTicket) {
                casTicketService.createProxyGrantingTicket(proxyIds.getPgtId(), expiration, (ServiceTicket) ticket);
            } else {
                casTicketService.createProxyGrantingTicket(proxyIds.getPgtId(), expiration, (ProxyTicket) ticket);
            }
            response.setPgtIou(proxyIds.getPgtIou());
        } catch (final Exception e) {
            log.warn("{} Proxy authentication failed for {}", getLogPrefix(), request.getPgtUrl(), e);
            ActionSupport.buildEvent(profileRequestContext,
                    ProtocolError.ProxyCallbackAuthenticationFailure.event(this));
        }
    }

}
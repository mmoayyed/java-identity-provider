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
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectContextPrincipalLookupFunction;
import net.shibboleth.idp.cas.config.impl.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.impl.LoginConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketResponse;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.TicketServiceEx;
import net.shibboleth.idp.cas.ticket.TicketState;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Generates and stores a CAS protocol service ticket. Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#TicketCreationError TicketCreationError}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class GrantServiceTicketAction extends AbstractCASProtocolAction<ServiceTicketRequest, ServiceTicketResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(GrantServiceTicketAction.class);

    /** Profile configuration lookup function. */
    @Nonnull private final ConfigLookupFunction<LoginConfiguration> configLookupFunction =
            new ConfigLookupFunction<>(LoginConfiguration.class);

    /** Looks up an IdP session context from IdP profile request context. */
    @Nonnull
    private final Function<ProfileRequestContext, SessionContext> sessionContextFunction =
            new ChildContextLookup<>(SessionContext.class);

    /** AuthenticationContext lookup function. */
    @Nonnull
    private final Function<ProfileRequestContext, AuthenticationContext> authnCtxLookupFunction =
            new ChildContextLookup<>(AuthenticationContext.class);

    /** Function to retrieve subject principal name. */
    @Nonnull
    private final Function<ProfileRequestContext, String> principalLookupFunction =
            new SubjectContextPrincipalLookupFunction().compose(
                    new ChildContextLookup<>(SubjectContext.class));

    /** Manages CAS tickets. */
    @Nonnull
    private final TicketServiceEx ticketServiceEx;


    /**
     * Creates a new instance.
     *
     * @param ticketService Ticket service component.
     */
    public GrantServiceTicketAction(@Nonnull final TicketServiceEx ticketService) {
        ticketServiceEx = Constraint.isNotNull(ticketService, "TicketService cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final ServiceTicketRequest request = getCASRequest(profileRequestContext);
        final IdPSession session = getIdPSession(profileRequestContext);
        final LoginConfiguration config = configLookupFunction.apply(profileRequestContext);
        if (config == null) {
            throw new IllegalStateException("Service ticket configuration undefined");
        }
        if (config.getSecurityConfiguration() == null || config.getSecurityConfiguration().getIdGenerator() == null) {
            throw new IllegalStateException(
                    "Invalid service ticket configuration: SecurityConfiguration#idGenerator undefined");
        }
        final AuthenticationContext authnCtx = authnCtxLookupFunction.apply(profileRequestContext);
        final AuthenticationResult authnResult;
        if (authnCtx != null) {
            authnResult = authnCtx.getAuthenticationResult();
        } else {
            authnResult = getLatestAuthenticationResult(session);
        }
        final ServiceTicket ticket;
        try {
            log.debug("Granting service ticket for {}", request.getService());
            final TicketState state = new TicketState(
                    session.getId(),
                    getPrincipalName(profileRequestContext),
                    authnResult.getAuthenticationInstant(),
                    authnResult.getAuthenticationFlowId());
            ticket = ticketServiceEx.createServiceTicket(
                    config.getSecurityConfiguration().getIdGenerator().generateIdentifier(),
                    Instant.now().plusMillis(config.getTicketValidityPeriod()),
                    request.getService(),
                    state,
                    request.isRenew());
        } catch (final RuntimeException e) {
            log.error("Failed granting service ticket due to error.", e);
            return ProtocolError.TicketCreationError.event(this);
        }
        log.info("Granted service ticket for {}", request.getService());
        final ServiceTicketResponse response = new ServiceTicketResponse(request.getService(), ticket.getId());
        if (request.isSAML()) {
            response.setSaml(true);
        }
        setCASResponse(profileRequestContext, response);
        return null;
    }

    /**
     * Get the IdP session.
     *
     * @param prc profile request context.
     * @return IdP session
     */
    @Nonnull
    private IdPSession getIdPSession(final ProfileRequestContext prc) {
        final SessionContext sessionContext = sessionContextFunction.apply(prc);
        if (sessionContext == null || sessionContext.getIdPSession() == null) {
            throw new IllegalStateException("Cannot locate IdP session");
        }
        return sessionContext.getIdPSession();
    }

    /**
     * Get the IdP subject principal name.
     *
     * @param prc profile request context.
     * @return Principal name.
     */
    @Nonnull
    private String getPrincipalName(final ProfileRequestContext prc) {
        final String principal = principalLookupFunction.apply(prc);
        if (principal == null ) {
            throw new IllegalStateException("Cannot determine IdP subject principal name.");
        }
        return principal;
    }

    /**
     * Gets the most recent authentication result from the IdP session.
     *
     * @param session IdP session to ask for authentication results.
     *
     * @return Latest authentication result.
     *
     * @throws IllegalStateException If no authentication results are found.
     */
    private AuthenticationResult getLatestAuthenticationResult(final IdPSession session) {
        AuthenticationResult latest = null;
        for (final AuthenticationResult result : session.getAuthenticationResults()) {
            if (latest == null || result.getAuthenticationInstant().isAfter(latest.getAuthenticationInstant())) {
                latest = result;
            }
        }
        if (latest == null) {
            throw new IllegalStateException("Cannot find authentication results in IdP session");
        }
        return latest;
    }
}

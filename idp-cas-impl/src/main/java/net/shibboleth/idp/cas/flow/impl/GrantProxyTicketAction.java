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

import javax.annotation.Nonnull;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.shibboleth.idp.cas.config.impl.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.impl.ProxyConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ProxyTicketResponse;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.TicketServiceEx;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.joda.time.DateTime;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Generates and stores a CAS protocol proxy ticket. Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#TicketCreationError TicketCreationError}</li>
 *     <li>{@link ProtocolError#IllegalState IllegalState}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class GrantProxyTicketAction extends AbstractCASProtocolAction<ProxyTicketRequest, ProxyTicketResponse> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(GrantProxyTicketAction.class);


    /** Profile configuration lookup function. */
    private final ConfigLookupFunction<ProxyConfiguration> configLookupFunction =
            new ConfigLookupFunction<>(ProxyConfiguration.class);

    /** Manages CAS tickets. */
    @Nonnull
    private final TicketServiceEx casTicketService;

    /** Looks up IdP sessions. */
    @Nonnull
    private final SessionResolver sessionResolver;

    /** Whether to resolve and validate IdP session as part of granting a proxy ticket. */
    private Predicate<ProfileRequestContext> validateIdPSessionPredicate = Predicates.alwaysFalse();


    /**
     * Creates a new instance.
     *
     * @param ticketService Ticket service component.
     */
    public GrantProxyTicketAction(
            @Nonnull final TicketServiceEx ticketService, @Nonnull final SessionResolver resolver) {
        casTicketService = Constraint.isNotNull(ticketService, "TicketService cannot be null");
        sessionResolver = Constraint.isNotNull(resolver, "SessionResolver cannot be null");
    }

    /**
     * Sets the predicate used to determine whether IdP session validation is performed during the process of granting
     * a proxy ticket. When the predicate evaluates to true, an IdP session is resolved and validated prior to granting
     * a proxy ticket. This feature prevents issuing proxy tickets when an IdP session is expired, but comes
     * at the cost of requiring server-side storage of IdP session data. If this is configured to a predicate that
     * evaluates to true under any condition, a server-side storage service must be enabled for IdP session
     * storage.
     *
     * @param predicate Session validation predicate. Default is <code>Predicates.alwaysFalse()</code>.
     */
    public void setValidateIdPSessionPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        validateIdPSessionPredicate = predicate;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final ProxyGrantingTicket pgt = (ProxyGrantingTicket) getCASTicket(profileRequestContext);
        if (pgt == null || pgt.getExpirationInstant().isBeforeNow()) {
            return ProtocolError.TicketExpired.event(this);
        }
        final ProxyConfiguration config = configLookupFunction.apply(profileRequestContext);
        if (config == null) {
            log.warn("Proxy ticket configuration undefined");
            return ProtocolError.IllegalState.event(this);
        }
        if (config.getSecurityConfiguration() == null || config.getSecurityConfiguration().getIdGenerator() == null) {
            log.warn("Invalid proxy ticket configuration: SecurityConfiguration#idGenerator undefined");
            return ProtocolError.IllegalState.event(this);
        }
        if (validateIdPSessionPredicate.apply(profileRequestContext)) {
            IdPSession session = null;
            try {
                log.debug("Attempting to retrieve session {}", pgt.getSessionId());
                session = sessionResolver.resolveSingle(new CriteriaSet(new SessionIdCriterion(pgt.getSessionId())));
            } catch (ResolverException e) {
                log.warn("IdPSession resolution error: {}", e);
            }
            boolean expired = true;
            if (session == null) {
                log.info("IdPSession {} not found", pgt.getSessionId());
            } else {
                try {
                    expired = !session.checkTimeout();
                    log.debug("Session {} expired={}", pgt.getSessionId(), expired);
                } catch (SessionException e) {
                    log.warn("Error performing session timeout check: {}. Assuming session has expired.", e);
                }
            }
            if (expired) {
                return ProtocolError.SessionExpired.event(this);
            }
        }
        final ProxyTicketRequest request = getCASRequest(profileRequestContext);
        final ProxyTicket pt;
        try {
            log.debug("Granting proxy ticket for {}", request.getTargetService());
            pt = casTicketService.createProxyTicket(
                    config.getSecurityConfiguration().getIdGenerator().generateIdentifier(),
                    DateTime.now().plus(config.getTicketValidityPeriod()).toInstant(),
                    pgt,
                    request.getTargetService());
        } catch (RuntimeException e) {
            log.error("Failed granting proxy ticket due to error.", e);
            return ProtocolError.TicketCreationError.event(this);
        }
        log.info("Granted proxy ticket for {}", request.getTargetService());
        setCASResponse(profileRequestContext, new ProxyTicketResponse(pt.getId()));
        return null;
    }
}

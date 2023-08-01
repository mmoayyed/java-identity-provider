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

import java.time.Instant;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.security.config.SecurityConfiguration;
import org.slf4j.Logger;

import net.shibboleth.shared.primitive.LoggerFactory;

import net.shibboleth.idp.cas.config.ConfigLookupFunction;
import net.shibboleth.idp.cas.config.ProxyConfiguration;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ProxyTicketResponse;
import net.shibboleth.idp.cas.ticket.ProxyGrantingTicket;
import net.shibboleth.idp.cas.ticket.ProxyTicket;
import net.shibboleth.idp.cas.ticket.TicketService;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.PredicateSupport;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;

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
    @Nonnull private final Logger log = LoggerFactory.getLogger(GrantProxyTicketAction.class);

    /** Profile configuration lookup function. */
    @Nonnull private final ConfigLookupFunction<ProxyConfiguration> configLookupFunction;

    /** Manages CAS tickets. */
    @Nonnull private final TicketService casTicketService;

    /** Looks up IdP sessions. */
    @Nonnull private final SessionResolver sessionResolver;

    /** Whether to resolve and validate IdP session as part of granting a proxy ticket. */
    @Nonnull private Predicate<ProfileRequestContext> validateIdPSessionPredicate;

    /** Profile config. */
    @NonnullBeforeExec private ProxyConfiguration proxyConfig;
    
    /** Security config. */
    @NonnullBeforeExec private SecurityConfiguration securityConfig;
    
    /** CAS ticket. */
    @NonnullBeforeExec private ProxyGrantingTicket proxyGrantingTicket;
    
    /** CAS request. */
    @NonnullBeforeExec private ProxyTicketRequest request;

    /**
     * Constructor.
     *
     * @param ticketService Ticket service component.
     * @param resolver session resolver
     */
    public GrantProxyTicketAction(@Nonnull final TicketService ticketService,
            @Nonnull final SessionResolver resolver) {
        casTicketService = Constraint.isNotNull(ticketService, "TicketService cannot be null");
        sessionResolver = Constraint.isNotNull(resolver, "SessionResolver cannot be null");
        
        validateIdPSessionPredicate = PredicateSupport.alwaysFalse();
        configLookupFunction = new ConfigLookupFunction<>(ProxyConfiguration.class);
    }

    /**
     * Sets the predicate used to determine whether IdP session validation is performed during the process of granting
     * a proxy ticket. When the predicate evaluates to true, an IdP session is resolved and validated prior to granting
     * a proxy ticket. This feature prevents issuing proxy tickets when an IdP session is expired, but comes
     * at the cost of requiring server-side storage of IdP session data. If this is configured to a predicate that
     * evaluates to true under any condition, a server-side storage service must be enabled for IdP session
     * storage.
     *
     * @param predicate Session validation predicate. Default is {@link PredicateSupport#alwaysFalse()}.
     */
    public void setValidateIdPSessionPredicate(@Nonnull final Predicate<ProfileRequestContext> predicate) {
        checkSetterPreconditions();
        validateIdPSessionPredicate = Constraint.isNotNull(predicate, "Session validation condition cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        final ProxyConfiguration pCfg = proxyConfig = configLookupFunction.apply(profileRequestContext);
        if (pCfg == null) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        securityConfig = pCfg.getSecurityConfiguration(profileRequestContext);
        if (securityConfig == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_SEC_CFG);
            return false;
        }
        
        try {
            request = getCASRequest(profileRequestContext);
            proxyGrantingTicket = (ProxyGrantingTicket) getCASTicket(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }
        
        return true;
    }

    /** 
     * Null-safe getter.
     * 
     * @return the proxyGrantingTicket
     */
    @SuppressWarnings("null")
    @Nonnull private ProxyGrantingTicket getProxyGrantingTicket() {
        assert isPreExecuteCalled();
        return proxyGrantingTicket;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (proxyGrantingTicket.getExpirationInstant().isBefore(Instant.now())) {
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.TicketExpired.event(this));
            return;
        }
        
        if (validateIdPSessionPredicate.test(profileRequestContext)) {
            IdPSession session = null;
            try {
                log.debug("{} Attempting to retrieve session {}", getLogPrefix(), proxyGrantingTicket.getSessionId());
                session = sessionResolver.resolveSingle(new CriteriaSet(new SessionIdCriterion(
                        Constraint.isNotNull(proxyGrantingTicket.getSessionId(),
                                "ProxyGrantingTicket session id was null"))));
            } catch (final ResolverException e) {
                log.warn("{} IdPSession resolution error: {}", getLogPrefix(), e);
            }
            boolean expired = true;
            if (session == null) {
                log.info("{} IdPSession {} not found", getLogPrefix(), proxyGrantingTicket.getSessionId());
            } else {
                try {
                    expired = !session.checkTimeout();
                    log.debug("{} Session {} expired={}", getLogPrefix(), proxyGrantingTicket.getSessionId(), expired);
                } catch (final SessionException e) {
                    log.warn("{} Error performing session timeout check: {}. Assuming session has expired.",
                            getLogPrefix(), e);
                }
            }
            if (expired) {
                ActionSupport.buildEvent(profileRequestContext, ProtocolError.SessionExpired.event(this));
                return;
            }
        }
        final ProxyTicket pt;
        try {
            log.debug("{} Granting proxy ticket for {}", getLogPrefix(), request.getTargetService());
            final Instant then = Instant.now().plus(proxyConfig.getTicketValidityPeriod(profileRequestContext));
            assert then != null;
            pt = casTicketService.createProxyTicket(
                    securityConfig.getIdGenerator().generateIdentifier(),
                    then,
                    getProxyGrantingTicket(),
                    request.getTargetService());
        } catch (final RuntimeException e) {
            log.error("Failed granting proxy ticket due to error.", e);
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.TicketCreationError.event(this));
            return;
        }
        
        try {
            setCASResponse(profileRequestContext, new ProxyTicketResponse(pt.getId()));
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return;
        }
        
        log.info("{} Granted proxy ticket for {}", getLogPrefix(), request.getTargetService());
    }
    
}
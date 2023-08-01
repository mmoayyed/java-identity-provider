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

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.session.impl.CASSPSession;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;

/**
 * Conditionally updates the {@link IdPSession} with a {@link CASSPSession} to support SLO.
 * 
 * <p>If the service granted access to indicates participation in SLO via {@link Service#isSingleLogoutParticipant()},
 * then a {@link CASSPSession} is created to track the SP session in order that it may receive SLO messages upon
 * a request to the CAS <code>/logout</code> URI.</p>
 * 
 * @param <RequestType> request
 * @param <ResponseType> response
 *
 * @author Marvin S. Addison
 */
public class UpdateIdPSessionWithSPSessionAction<RequestType,ResponseType>
        extends AbstractCASProtocolAction<RequestType,ResponseType> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(UpdateIdPSessionWithSPSessionAction.class);

    /** Looks up IdP sessions. */
    @Nonnull private final SessionResolver sessionResolver;

    /** Lifetime of sessions to create. */
    @Nonnull private final Duration sessionLifetime;

    /** Strategy for obtaining the relying party ID for sessions. */
    @Nonnull private Function<ProfileRequestContext,String> relyingPartyIdLookupStrategy;
    
    /** Ticket. */
    @NonnullBeforeExec private Ticket ticket;
    
    /** CAS service. */
    @NonnullBeforeExec private Service service;

    /**
     * Constructor.
     *
     * @param resolver Session resolver component
     * @param lifetime determines upper bound for expiration of the {@link CASSPSession} to be created
     */
    public UpdateIdPSessionWithSPSessionAction(@Nonnull final SessionResolver resolver,
            @Nonnull final Duration lifetime) {
        sessionResolver = Constraint.isNotNull(resolver, "Session resolver cannot be null");
        sessionLifetime = Constraint.isNotNull(lifetime, "Lifetime cannot be null");
        relyingPartyIdLookupStrategy = new RelyingPartyIdLookupFunction();
    }
    
    /**
     * Set lookup strategy for relying party ID.
     * 
     * @param strategy lookup strategy
     * 
     * @since 5.0.0
     */
    public void setRelyingPartyIdLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        relyingPartyIdLookupStrategy = Constraint.isNotNull(strategy, "RelyingParty ID lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        try {
            service = getCASService(profileRequestContext);
            if (!service.isSingleLogoutParticipant()) {
                return false;
            }
            
            ticket = getCASTicket(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }

        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        IdPSession session = null;
        try {
            log.debug("{} Attempting to retrieve session {}", getLogPrefix(), ticket.getSessionId());
            session = sessionResolver.resolveSingle(new CriteriaSet(new SessionIdCriterion(
                    Constraint.isNotNull(ticket.getSessionId(), "Null Session Id"))));
        } catch (final ResolverException e) {
            log.warn("{} Possible sign of misconfiguration, IdPSession resolution error: {}", getLogPrefix(), e);
        }
        if (session != null) {
            final Instant now = Instant.now();
            assert now != null;
            
            final Instant expiration = now.plus(sessionLifetime); 
            assert expiration != null;
            
            final String relyingPartyId = relyingPartyIdLookupStrategy.apply(profileRequestContext);
            
            final SPSession sps = new CASSPSession(
                    relyingPartyId != null ? relyingPartyId : ticket.getService(),
                    now,
                    expiration,
                    ticket.getId(),
                    ticket.getService());
            log.debug("{} Created SP session {}", getLogPrefix(), sps);
            try {
                session.addSPSession(sps);
            } catch (final SessionException e) {
                log.warn("{} Failed updating IdPSession with CASSPSession", getLogPrefix(), e);
            }
        } else {
            log.info("{} Cannot store CASSPSession since IdPSession not found", getLogPrefix());
        }
    }

}
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

import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.session.impl.CASSPSession;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.SessionResolver;
import net.shibboleth.idp.session.criterion.SessionIdCriterion;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Conditionally updates the {@link net.shibboleth.idp.session.IdPSession} with a {@link CASSPSession} to support SLO.
 * If the service granted access to indicates participation in SLO via {@link Service#singleLogoutParticipant},
 * then a {@link CASSPSession} is created to track the SP session in order that it may receive SLO messages upon
 * a request to the CAS <code>/logout</code> URI.
 *
 * @author Marvin S. Addison
 */
public class UpdateIdPSessionWithSPSessionAction extends AbstractCASProtocolAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(UpdateIdPSessionWithSPSessionAction.class);

    /** Looks up IdP sessions. */
    @Nonnull
    private final SessionResolver sessionResolver;

    /** Lifetime of sessions to create. */
    @Positive
    @Duration
    private final long sessionLifetime;


    /**
     * Creates a new instance with given parameters.
     *
     * @param resolver Session resolver component
     * @param lifetime lifetime in milliseconds, determines upper bound for expiration of the
     * {@link CASSPSession} to be created
     */
    public UpdateIdPSessionWithSPSessionAction(
            @Nonnull final SessionResolver resolver,
            @Positive@Duration final long lifetime) {
        sessionResolver = Constraint.isNotNull(resolver, "Session resolver cannot be null.");
        sessionLifetime = Constraint.isGreaterThan(0, lifetime, "Lifetime must be greater than 0");
    }

    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final Ticket ticket = getCASTicket(profileRequestContext);
        final Service service = getCASService(profileRequestContext);
        if (!service.isSingleLogoutParticipant()) {
            return null;
        }
        IdPSession session = null;
        try {
            log.debug("Attempting to retrieve session {}", ticket.getSessionId());
            session = sessionResolver.resolveSingle(new CriteriaSet(new SessionIdCriterion(ticket.getSessionId())));
        } catch (final Exception e) {
            log.warn("IdPSession resolution error: {}. Possible sign of misconfiguration.", e.getMessage());
        }
        if (session != null) {
            final long now = System.currentTimeMillis();
            final SPSession sps = new CASSPSession(
                    ticket.getService(),
                    now,
                    now + sessionLifetime,
                    ticket.getId());
            log.debug("Created SP session {}", sps);
            try {
                session.addSPSession(sps);
            } catch (final SessionException e) {
                log.warn("Failed updating IdPSession with CASSPSession", e);
            }
        } else {
            log.info("Cannot store CASSPSession since IdPSession not found");
        }
        return null;
    }
}

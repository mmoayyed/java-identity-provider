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

package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.SessionContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Determines whether authentication is required by examining both SSO session state and CAS
 * service ticket request message. Returns one of the following events:
 *
 * <ul>
 *     <li>{@link Events#GatewayRequested gatewayRequested} - Authentication not required since no ticket is requested.</li>
 *     <li>{@link Events#RenewRequested renewRequested} - Authentication required regardless of existing session.</li>
 *     <li>{@link Events#SessionFound sessionFound} - Authentication not required since session already exists.</li>
 *     <li>{@link Events#SessionNotFound sessionNotFound} - Authentication required since no active session exists.</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class CheckAuthenticationRequiredAction extends AbstractProfileAction<ServiceTicketRequest, Object> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CheckAuthenticationRequiredAction.class);

    /** {@inheritDoc} */
    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext<ServiceTicketRequest, Object> profileRequestContext) {

        final ServiceTicketRequest request = FlowStateSupport.getServiceTicketRequest(springRequestContext);

        // Per http://www.jasig.org/cas/protocol section 2.1.1
        // It is RECOMMENDED that renew supersede gateway
        if (request.isRenew()) {
            return new Event(this, Events.RenewRequested.id());
        }

        if (request.isGateway()) {
            return new Event(this, Events.GatewayRequested.id());
        }

        final SessionContext sessionCtx = profileRequestContext.getSubcontext(SessionContext.class, false);
        Events result;
        if (sessionCtx != null) {
            final IdPSession session = sessionCtx.getIdPSession();
            if (session != null) {
                log.debug("Found session ID {}", session.getId());
                try {
                    // Timeout check updates session lastActivityInstant field
                    if (session.checkTimeout()) {
                        result = Events.SessionFound;
                    } else {
                        result = Events.SessionNotFound;
                    }
                } catch (SessionException e) {
                    log.debug("Error performing session timeout check. Assuming session has expired.", e);
                    result = Events.SessionNotFound;
                }
            } else {
                log.debug("Session not found.");
                result = Events.SessionNotFound;

            }
        } else {
            log.debug("Session context not found.");
            result = Events.SessionNotFound;
        }
        return result.event(this);
    }
}

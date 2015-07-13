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
import net.shibboleth.idp.cas.protocol.ServiceTicketResponse;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.SessionException;
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
 *     <li>{@link Events#GatewayRequested GatewayRequested} - Authentication not required since no ticket is requested.</li>
 *     <li>{@link Events#RenewRequested RenewRequested} - Authentication required regardless of existing session.</li>
 *     <li>{@link Events#SessionFound SessionFound} - Authentication not required since session already exists.</li>
 *     <li>{@link Events#SessionNotFound SessionNotFound} - Authentication required since no active session exists.</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class CheckAuthenticationRequiredAction extends
        AbstractCASProtocolAction<ServiceTicketRequest, ServiceTicketResponse> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CheckAuthenticationRequiredAction.class);

    /** {@inheritDoc} */
    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final ServiceTicketRequest request = getCASRequest(profileRequestContext);

        // Per http://www.jasig.org/cas/protocol section 2.1.1
        // It is RECOMMENDED that renew supersede gateway
        if (request.isRenew()) {
            return Events.RenewRequested.event(this);
        }

        if (request.isGateway()) {
            return Events.GatewayRequested.event(this);
        }

        try {
            final IdPSession session = getIdPSession(profileRequestContext);
            log.debug("Found session ID {}", session.getId());
            try {
                // Timeout check updates session lastActivityInstant field
                if (session.checkTimeout()) {
                    return Events.SessionFound.event(this);
                }
            } catch (SessionException e) {
                log.debug("Error performing session timeout check. Assuming session has expired.", e);
            }
        } catch (IllegalStateException e) {
            log.debug("IdP session not found");
        }
        return Events.SessionNotFound.event(this);
    }
}

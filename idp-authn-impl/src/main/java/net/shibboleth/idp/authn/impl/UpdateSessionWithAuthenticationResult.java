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

package net.shibboleth.idp.authn.impl;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.ServiceSession;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/** A stage that checks that authentication has completed and, if so, records it in the user's IdP session. */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_MSG_CTX, description = "Inbound message context does not exist"),
        @Event(id = EventIds.INVALID_MSG_MD, description = "Inbound message metadata does not exist"),
        @Event(id = EventIds.INVALID_PROFILE_CTX,
                description = "Authentication context doesn't indicated an attempted workflow")})
public class UpdateSessionWithAuthenticationResult extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(UpdateSessionWithAuthenticationResult.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        final MessageContext messageCtx = profileRequestContext.getInboundMessageContext();
        if (messageCtx == null) {
            log.debug("Action {}: no inbound message context available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final BasicMessageMetadataContext msgMdCtx = messageCtx.getSubcontext(BasicMessageMetadataContext.class, false);
        if (msgMdCtx == null) {
            log.debug("Action {}: no inbound message metadata available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_MD);
        }

        if (authenticationContext.getAttemptedFlow() == null) {
            log.debug("Action {}: no attempted workflow descriptor available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }

        // TODO replace
        /*
        if (!authenticationContext.getAuthenticatedPrincipal().isPresent()) {
            log.debug("Action {}: no authenticated principal available", getId());
            return ActionSupport.buildEvent(this, AuthnEventIds.INVALID_AUTHN_CTX);
        }
        */

        updateIdpSession(authenticationContext, msgMdCtx.getMessageIssuer());

        authenticationContext.setCompletionInstant();

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Updates the session associated with the authenticated user. The following steps are performed:
     * <ul>
     * <li>creating the {@link IdPSession} if the user does not yet have one</li>
     * <li>creating a {@link ServiceSession} to associate the user with the service for which authentication was
     * performed</li>
     * <li>updating the last activity time of the IdP session</li>
     * </ul>
     * 
     * @param authenticationContext current authentication context
     * @param serviceId ID of the service for which the user was authenticated
     */
    protected void updateIdpSession(@Nonnull final AuthenticationContext authenticationContext,
            @Nonnull final String serviceId) {
        
        // TODO probably move all this out to a separate action
        
        /*
        IdPSession idpSession = null;

        if (!authenticationContext.getActiveSession().isPresent()) {
            idpSession = authenticationContext.getActiveSession().get();
        } else {
            // TODO(lajoie) create session
        }

        ServiceSession serviceSession = new ServiceSession(serviceId, authenticationContext.buildAuthenticationEvent());
        idpSession.addServiceSession(serviceSession);

        idpSession.setLastActivityInstantToNow();
        */
    }
}
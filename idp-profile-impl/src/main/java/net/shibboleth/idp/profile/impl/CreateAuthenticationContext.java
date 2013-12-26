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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * An action that creates an {@link AuthenticationContext} and attaches it to the current {@link ProfileRequestContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link org.opensaml.profile.action.EventIds#INVALID_MSG_CTX}
 */
public class CreateAuthenticationContext extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CreateAuthenticationContext.class);

    /** {@inheritDoc} */
    @Override @Nonnull protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final MessageContext<?> msgCtx = profileRequestContext.getInboundMessageContext();
        if (msgCtx == null) {
            log.debug("{} No inbound message context", getLogPrefix());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final Object message = msgCtx.getMessage();
        if (message == null) {
            log.debug("{} No inbound message", getLogPrefix());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        if (!(message instanceof AuthnRequest)) {
            log.debug("{} Inbound message was not a SAML 2 AuthnRequest", getLogPrefix());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final AuthnRequest request = (AuthnRequest) message;

        AuthenticationContext authnCtx = new AuthenticationContext();
        authnCtx.setForceAuthn(request.isForceAuthn());
        authnCtx.setIsPassive(request.isPassive());

        // TODO set other properties, etc.

        profileRequestContext.addSubcontext(authnCtx, true);

        log.debug("{} Created authentication context {}", getLogPrefix(), authnCtx);

        return getResult(this, profileRequestContext);
    }
}

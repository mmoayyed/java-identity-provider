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

package net.shibboleth.idp.saml.impl.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that creates an {@link AuthenticationContext} and attaches it to the current {@link ProfileRequestContext}.
 * 
 * <p>If the incoming message is a SAML 2.0 {@link AuthnRequest}, then basic authentication policy (IsPassive,
 * ForceAuthn) is copied into the context from the request.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != true
 * @post SAML 2.0 AuthnRequest policy flags are copied to the {@link AuthenticationContext}
 */
public class InitializeAuthenticationContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeAuthenticationContext.class);

    /** Incoming SAML 2.0 request, if present. */
    @Nullable private AuthnRequest authnRequest;

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        final MessageContext<?> msgCtx = profileRequestContext.getInboundMessageContext();
        if (msgCtx == null) {
            log.debug("{} No inbound message context", getLogPrefix());
            return super.doPreExecute(profileRequestContext);
        }

        final Object message = msgCtx.getMessage();
        if (message == null) {
            log.debug("{} No inbound message", getLogPrefix());
            return super.doPreExecute(profileRequestContext);
        }

        if (!(message instanceof AuthnRequest)) {
            log.debug("{} Inbound message was not a SAML 2 AuthnRequest", getLogPrefix());
            return super.doPreExecute(profileRequestContext);
        }

        authnRequest = (AuthnRequest) message;
        
        return super.doPreExecute(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        AuthenticationContext authnCtx = new AuthenticationContext();
        
        if (authnRequest != null) {
            authnCtx.setForceAuthn(authnRequest.isForceAuthn());
            authnCtx.setIsPassive(authnRequest.isPassive());
        }

        profileRequestContext.addSubcontext(authnCtx, true);

        log.debug("{} Created authentication context {}", getLogPrefix(), authnCtx);
    }
    
}
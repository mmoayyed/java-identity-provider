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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Adds a {@link RelyingPartySubcontext} to the current {@link ProfileRequestContext}. The relying party ID is assumed
 * to be the inbound message issuer as determined by the {@link BasicMessageMetadataSubcontext#getMessageIssuer()}
 * located on the {@link ProfileRequestContext#getInboundMessageContext()}.
 */
public class InitializeRelyingPartySubcontextBasedOnInboundMessageIssuer extends AbstractIdentityProviderAction {

    /** Class logger. */
    private final Logger log = LoggerFactory
            .getLogger(InitializeRelyingPartySubcontextBasedOnInboundMessageIssuer.class);

    /** {@inheritDoc} */
    protected Class<BasicMessageMetadataSubcontext> getSubcontextType() {
        return BasicMessageMetadataSubcontext.class;
    }

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext profileRequestContext) throws ProfileException {

        final BasicMessageMetadataSubcontext messageSubcontext =
                ActionSupport.getRequiredInboundMessageMetadata(this, profileRequestContext);

        log.debug("Action {}: Attaching RelyingPartySubcontext with relying party ID {} to ProfileRequestContext",
                getId(), messageSubcontext.getMessageIssuer());
        new RelyingPartySubcontext(profileRequestContext, messageSubcontext.getMessageIssuer());

        return ActionSupport.buildProceedEvent(this);
    }
}
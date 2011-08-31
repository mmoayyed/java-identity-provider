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

package net.shibboleth.idp.saml.impl.profile.saml1;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml1.core.Response;
import org.opensaml.util.StringSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Adds the <code>InResponseTo</code> attribute to outgoing {@link Response} retrieved from the
 * {@link ProfileRequestContext#getOutboundMessageContext()}. If there was no message ID on the inbound message than
 * nothing is added to the response.
 */
public class AddInResponseToToResponse extends AbstractIdentityProviderAction<Object, Response> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddInResponseToToResponse.class);

    /** {@inheritDoc} */
    public Event doExecute(final RequestContext springRequestContext,
            final ProfileRequestContext<Object, Response> profileRequestContext) {
        log.debug("Action {}: attempting to add InResponseTo to outgoing Response", getId());

        final String inMsgId = getInboundMessageId(profileRequestContext);
        if (inMsgId == null) {
            log.debug("Action {}: inbound message did not have an ID, no InResponse to added to response", getId());
            return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
        }

        final MessageContext<Response> outMsgCtx = profileRequestContext.getOutboundMessageContext();
        if (outMsgCtx == null) {
            log.debug("Action {}: no outbound message context available, no InResponse to added to response", getId());
            return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
        }

        final Response response = outMsgCtx.getMessage();
        if (response == null) {
            log.debug("Action {}: no outbound message available, no InResponse to added to response", getId());
            return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
        }

        log.debug("Action {}: add in response to message ID {} to response {}", new Object[] {getId(), inMsgId,
                response.getID(),});
        response.setInResponseTo(inMsgId);
        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }

    /**
     * Gets the ID of the inbound message.
     * 
     * @param profileRequestContext current request context
     * 
     * @return the inbound message ID or null if the was no ID
     */
    private String getInboundMessageId(final ProfileRequestContext<Object, Response> profileRequestContext) {
        final MessageContext inMsgCtx = profileRequestContext.getInboundMessageContext();
        if (inMsgCtx == null) {
            log.debug("Action {}: no inbound message context available", getId());
            return null;
        }

        final BasicMessageMetadataSubcontext inMsgMetadataCtx =
                inMsgCtx.getSubcontext(BasicMessageMetadataSubcontext.class);
        if (inMsgMetadataCtx == null) {
            log.debug("Action {}: no inbound message metadata context available", getId());
            return null;
        }

        return StringSupport.trimOrNull(inMsgMetadataCtx.getMessageId());
    }
}
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

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.util.storage.ReplayCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/** Checks that the given message has not be replayed. */
public final class CheckMessageReplay extends AbstractProfileAction {

    /** ID of event returned if a message is being replayed. */
    public static final String REPLAYED_MSG = "ReplayedMessage";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CheckMessageReplay.class);

    /** Cache used to store message issuer/id pairs and check to see if a message is being replayed. */
    private ReplayCache replayCache;

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event
            doExecute(@Nonnull final RequestContext springRequestContext,
                    @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final MessageContext msgCtx = profileRequestContext.getInboundMessageContext();
        if (msgCtx == null) {
            log.debug("Action {}: No inbound message context, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_CTX);
        }

        final BasicMessageMetadataContext msgMdCtx = msgCtx.getSubcontext(BasicMessageMetadataContext.class, false);
        if (msgMdCtx == null) {
            log.debug("Action {}: No inbound message metadata context, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_MD);
        }

        final String msgIssuer = msgMdCtx.getMessageIssuer();
        if (msgIssuer == null) {
            log.debug("Action {}: Message metadata does not contain an issuer, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_MD);
        }

        final String msgId = msgMdCtx.getMessageId();
        if (msgId == null) {
            log.debug("Action {}: Message metadata does not contain a message ID, unable to proceed", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_MSG_MD);
        }

        if (replayCache.isReplay(msgIssuer, msgId)) {
            log.debug("Action {}: Message {} issued by {} has been replayed", new Object[] {getId(), msgId, msgIssuer});
            return ActionSupport.buildEvent(this, REPLAYED_MSG);
        }

        return ActionSupport.buildProceedEvent(this);
    }
}
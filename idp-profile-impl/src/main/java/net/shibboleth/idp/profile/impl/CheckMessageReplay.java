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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.InvalidProfileRequestContextStateException;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.util.storage.ReplayCache;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Checks that the given message has not be replayed. */
public final class CheckMessageReplay extends AbstractIdentityProviderAction {

    /** Cache used to store message issuer/id pairs and check to see if a message is being replayed. */
    private ReplayCache replayCache;

    /** Constructor. The ID of this component is set to the name of this class. */
    public CheckMessageReplay() {
        setId(CheckMessageReplay.class.getName());
    }

    /** {@inheritDoc} */
    public Class<BasicMessageMetadataContext> getSubcontextType() {
        return BasicMessageMetadataContext.class;
    }

    /** {@inheritDoc} */
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext profileRequestContext) throws ProfileException {

        final BasicMessageMetadataContext messageSubcontext =
                ActionSupport.getRequiredInboundMessageMetadata(this, profileRequestContext);

        if (messageSubcontext.getMessageIssuer() == null) {
            throw new InvalidProfileRequestContextStateException(
                    "Basic message metadata subcontext does not contain a message issuer");
        }

        if (messageSubcontext.getMessageId() == null) {
            throw new InvalidProfileRequestContextStateException(
                    "Basic message metadata subcontext does not contain a message ID");
        }

        if (replayCache.isReplay(messageSubcontext.getMessageIssuer(), messageSubcontext.getMessageId())) {
            throw new ReplayedMessageException(messageSubcontext.getMessageId(), messageSubcontext.getMessageIssuer());
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /** Profile processing error that occurred because the given request was detected as a replay. */
    public class ReplayedMessageException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -7358811994922990143L;

        /**
         * Constructor.
         * 
         * @param messageId ID of the message, never null
         * @param messageIssuer issuer of the message, never null
         */
        public ReplayedMessageException(String messageId, String messageIssuer) {
            super("Action " + getId() + ": Message ID " + messageId + " from issuer " + messageIssuer
                    + " is a replayed message");
        }
    }
}
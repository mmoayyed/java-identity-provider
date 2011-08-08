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

package net.shibboleth.idp.profile;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.util.storage.ReplayCache;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Checks that the given message has not be replayed. */
public class CheckMessageReplay extends AbstractInboundMessageSubcontextAction<BasicMessageMetadataSubcontext> {

    /** Cache used to store message issuer/id pairs and check to see if a message is being replayed. */
    private ReplayCache replayCache;

    /** Constructor. The ID of this component is set to the name of this class. */
    public CheckMessageReplay() {
        setId(CheckMessageReplay.class.getName());
    }

    /** {@inheritDoc} */
    public Class<BasicMessageMetadataSubcontext> getSubcontextType() {
        return BasicMessageMetadataSubcontext.class;
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext,
            BasicMessageMetadataSubcontext messageSubcontext) {

        if (messageSubcontext.getMessageIssuer() == null) {
            return ActionSupport.buildErrorEvent(this, null,
                    "Basic message metadata subcontext does not contain a message issuer");
        }

        if (messageSubcontext.getMessageId() == null) {
            return ActionSupport.buildErrorEvent(this, null,
                    "Basic message metadata subcontext does not contain a message ID");
        }

        if (replayCache.isReplay(messageSubcontext.getMessageIssuer(), messageSubcontext.getMessageId())) {
            return ActionSupport.buildErrorEvent(this, null, "Message ID " + messageSubcontext.getMessageId()
                    + " from issuer " + messageSubcontext.getMessageIssuer() + " is a replayed message");
        }

        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }
}
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
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** An action that checks that the inbound message should be considered valid based upon when it was issued. */
public class CheckMessageLifetime extends AbstractInboundMessageSubcontextAction<BasicMessageMetadataSubcontext> {

    /** Allowed clock skew, in milliseconds. */
    private long clockskew;

    /** Amount of time, in milliseconds, for which a message is valid. */
    private long messageLifetime;

    /** Constructor. The ID of this component is set to the name of this class. */
    public CheckMessageLifetime() {
        setId(CheckMessageLifetime.class.getName());
    }

    /** {@inheritDoc} */
    public Class<BasicMessageMetadataSubcontext> getSubcontextType() {
        return BasicMessageMetadataSubcontext.class;
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext,
            BasicMessageMetadataSubcontext messageSubcontext) {

        if (messageSubcontext.getMessageIssueInstant() <= 0) {
            return ActionSupport.buildErrorEvent(this, null,
                    "Basic message metadata subcontext does not contain a message issue instant");
        }

        long issueInstant = messageSubcontext.getMessageIssueInstant();
        long currentTime = System.currentTimeMillis();

        if (issueInstant < currentTime - clockskew) {
            return ActionSupport.buildErrorEvent(this, null, "Message " + messageSubcontext.getMessageId()
                    + " was expired");
        }

        if (issueInstant > currentTime + messageLifetime + clockskew) {
            return ActionSupport.buildErrorEvent(this, null, "Message " + messageSubcontext.getMessageId()
                    + " is not yet valid");
        }

        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }
}
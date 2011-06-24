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

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.Subcontext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * A base class for actions which check or use the information from a {@link Subcontext} of the incoming
 * {@link MessageContext}.
 * 
 * @param <SubcontextType> the message subcontext type upon which this action operates
 */
public abstract class AbstractInboundMessageSubcontextAction<SubcontextType extends Subcontext> extends
        AbstractIdentityProviderAction {

    /**
     * Constructor.
     * 
     * @param componentId unique ID for this action
     */
    public AbstractInboundMessageSubcontextAction(String componentId) {
        super(componentId);
    }

    /**
     * Retrieves the incoming message subcontext specified by {@link #getSubcontextType()}. If no incoming message
     * context or the specified subcontext is not available an error is returned.
     * 
     * {@inheritDoc}
     */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext)
            throws Throwable {

        if (profileRequestContext == null) {
            return ActionSupport.buildErrorEvent(this, null, "Profile request context is null");
        }

        MessageContext<?> inboundMessageContext = profileRequestContext.getInboundMessageContext();
        if (inboundMessageContext == null) {
            return ActionSupport.buildErrorEvent(this, null, "Inbound message context is null");
        }

        SubcontextType messageSubcontext = inboundMessageContext.getSubcontext(getSubcontextType());
        if (messageSubcontext == null) {
            return ActionSupport.buildErrorEvent(this, null, "Message subcontext is null");
        }

        return doExecute(springRequestContext, profileRequestContext, messageSubcontext);
    }

    /**
     * Gets the type of message subcontext upon which this action operates.
     * 
     * @return type of message subcontext upon which this action operates
     */
    protected abstract Class<SubcontextType> getSubcontextType();

    /**
     * Performs the action operation.
     * 
     * @param springRequestContext current WebFlow request context, never null
     * @param profileRequestContext current identity provider profile request context, never null
     * @param subcontext subcontext upon which this action operates, never null
     * 
     * @return the result of this action
     * 
     * @throws Throwable thrown if there is some problem with this action
     */
    protected abstract Event doExecute(RequestContext springRequestContext,
            ProfileRequestContext profileRequestContext, SubcontextType subcontext) throws Throwable;
}

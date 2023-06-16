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

package net.shibboleth.idp.cas.flow.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.ActionSupport;

/**
 * Action to publish the CAS protocol request or response messages, i.e.
 * {@link net.shibboleth.idp.cas.protocol.ProtocolContext#getResponse()}, in Spring Webflow
 * flow scope to make available in views. The key name is the protocol object simple class name
 * converted to variable case, e.g. <code>TicketValidationResponse</code> is accessible as
 * <code>flowScope.ticketValidationResponse</code>.
 *
 * @param <RequestType> request
 * @param <ResponseType> response
 *
 * @author Marvin S. Addison
 */
public class PublishProtocolMessageAction<RequestType,ResponseType>
        extends AbstractCASProtocolAction<RequestType,ResponseType> {

    /** Request/response flag. */
    private boolean requestFlag;

    /**
     * Creates a new instance to publish request or response messages to Webflow request scope.
     *
     * @param isRequest True for request messages, false for response messages.
     */
    public PublishProtocolMessageAction(final boolean isRequest) {
        requestFlag = isRequest;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) {

        final Object message;
        
        try {
            if (requestFlag) {
                message = getCASRequest(profileRequestContext);
            } else {
                message = getCASResponse(profileRequestContext);
            }
        } catch (final EventException e) {
            return ActionSupport.buildEvent(this, e.getEventID());
        }
        
        final String className = message.getClass().getSimpleName();
        final String keyName = className.substring(0, 1).toLowerCase() + className.substring(1);
        springRequestContext.getFlowScope().put(keyName, message);
        
        return ActionSupport.buildProceedEvent(this);
    }

}
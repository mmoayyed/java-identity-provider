/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.cas.protocol.AbstractProtocolResponse;
import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ProxyTicketResponse;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Populates error information needed for protocol error messages.
 *
 * @author Marvin S. Addison
 * 
 * @param <RequestType> request
 */
public class PopulateProtocolErrorAction<RequestType>
        extends AbstractCASProtocolAction<RequestType,AbstractProtocolResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateProtocolErrorAction.class);
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) {

        final Object request;
        try {
            request = getCASRequest(profileRequestContext);
        } catch (final EventException e) {
            return ActionSupport.buildEvent(this, e.getEventID());
        }

        final AbstractProtocolResponse response;
        if (request instanceof ProxyTicketRequest) {
            response = new ProxyTicketResponse();
        } else if (request instanceof TicketValidationRequest) {
            response = new TicketValidationResponse();
        } else {
            log.error("{} Invalid request type: {}", getLogPrefix(), request);
            return ActionSupport.buildEvent(this, EventIds.INVALID_MESSAGE);
        }
        
        String code = (String) springRequestContext.getCurrentEvent().getAttributes().get("code");
        String detail = (String) springRequestContext.getCurrentEvent().getAttributes().get("detailCode");
        if (code == null) {
            code = ProtocolError.IllegalState.getCode();
        }
        if (detail == null) {
            detail = ProtocolError.IllegalState.getDetailCode();
        }
        
        response.setErrorCode(code);
        response.setErrorDetail(detail);
        try {
            setCASResponse(profileRequestContext, response);
        } catch (final EventException e) {
            return ActionSupport.buildEvent(this, e.getEventID());
        }
        
        return ActionSupport.buildProceedEvent(this);
    }

}
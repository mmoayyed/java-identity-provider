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

package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.ProtocolParam;
import net.shibboleth.idp.cas.protocol.SamlParam;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Initializes the CAS protocol interaction at the <code>/login</code> URI. Possible outcomes:
 * <ul>
 *     <li>{@link Events#Proceed proceed}</li>
 *     <li>{@link ProtocolError#ServiceNotSpecified serviceNotSpecified}</li>
 * </ul>
 * On success places a {@link ServiceTicketRequest} object in request scope under the key
 * {@value FlowStateSupport#SERVICE_TICKET_REQUEST_KEY}.
 *
 * @author Marvin S. Addison
 */
public class InitializeLoginAction extends AbstractProfileAction<ServiceTicketRequest, Object> {

    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext<ServiceTicketRequest, Object> profileRequestContext) {

        final ParameterMap params = springRequestContext.getRequestParameters();
        String service = params.get(ProtocolParam.Service.id());
        boolean isSaml = false;
        if (service == null) {
            service = params.get(SamlParam.TARGET.name());
            if (service == null) {
                return ProtocolError.ServiceNotSpecified.event(this);
            }
            isSaml = true;
        }
        final ServiceTicketRequest serviceTicketRequest = new ServiceTicketRequest(service);
        serviceTicketRequest.setSaml(isSaml);

        final String renew = params.get(ProtocolParam.Renew.id());
        if (renew != null) {
            serviceTicketRequest.setRenew(true);
        }

        // http://www.jasig.org/cas/protocol, section 2.1.1
        // It is RECOMMENDED that CAS implementations ignore the "gateway" parameter if "renew" is set.
        final String gateway = params.get(ProtocolParam.Gateway.id());
        if (gateway != null && renew == null) {
            serviceTicketRequest.setGateway(true);
        }

        final MessageContext<ServiceTicketRequest> messageContext = new MessageContext<>();
        messageContext.setMessage(serviceTicketRequest);
        profileRequestContext.setInboundMessageContext(messageContext);
        FlowStateSupport.setServiceTicketRequest(springRequestContext, serviceTicketRequest);
        return ActionSupport.buildProceedEvent(this);
    }
}

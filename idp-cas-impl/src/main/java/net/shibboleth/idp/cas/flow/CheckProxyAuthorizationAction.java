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
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Checks the current {@link ServiceContext} to determine whether the service is authorized to proxy.
 * Raises {@link ProtocolError#ProxyNotAuthorized} if not authorized.
 *
 * @author Marvin S. Addison
 */
public class CheckProxyAuthorizationAction
    extends AbstractProfileAction<TicketValidationRequest, TicketValidationResponse> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(CheckProxyAuthorizationAction.class);

    @Nonnull
    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final ServiceContext serviceContext = profileRequestContext.getSubcontext(ServiceContext.class);
        if (serviceContext == null) {
            log.info("ServiceContext not found in profile request context.");
            return ProtocolError.IllegalState.event(this);
        }
        if (!serviceContext.getService().isAuthorizedToProxy()) {
            log.info("{} is not authorized to proxy", serviceContext.getService().getName());
            return ProtocolError.ProxyNotAuthorized.event(this);
        }
        return ActionSupport.buildProceedEvent(this);
    }
}

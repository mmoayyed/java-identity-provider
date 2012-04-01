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

package net.shibboleth.idp.authn.impl;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.net.InetAddresses;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.UserAgentAddressContext;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

/**
 * A stage that extracts the user-agent's IP address from the incoming requests, creates an
 * {@link UserAgentAddressContext}, and attaches it to the {@link AuthenticationRequestContext}.
 */
public class ExtractUserAgentAddress extends AbstractAuthenticationAction {

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws ProfileException {

        final String addressString = httpRequest.getRemoteAddr();
        if (!InetAddresses.isInetAddress(addressString)) {
            // TODO(lajoie) error out
        }

        authenticationContext.getSubcontext(UserAgentAddressContext.class, true).setUserAgentAddress(
                InetAddresses.forString(addressString));

        return ActionSupport.buildProceedEvent(this);
    }
}
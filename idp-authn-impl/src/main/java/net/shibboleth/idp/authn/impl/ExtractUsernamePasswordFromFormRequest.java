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

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.UsernamePasswordContext;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/**
 * A stage that extracts a username/password from the request. This stage is expected to be used in conjunction with
 * {@link DisplayUsernamePasswordPage} but will work with any stage that properly passes in the username and password.
 */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = AuthnEventIds.NO_CREDENTIALS, description = "request does not contain username or password")})
public class ExtractUsernamePasswordFromFormRequest extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ExtractUsernamePasswordFromFormRequest.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

        final String username = httpRequest.getParameter(DisplayUsernamePasswordPage.USERNAME_FIELD_NAME);
        if (username == null) {
            log.debug("Action {}: no username in request", getId());
            return ActionSupport.buildEvent(this, AuthnEventIds.NO_CREDENTIALS);
        }

        final String password = httpRequest.getParameter(DisplayUsernamePasswordPage.PASSWORD_FIELD_NAME);
        if (password == null) {
            log.debug("Action {}: no password in request", getId());
            return ActionSupport.buildEvent(this, AuthnEventIds.NO_CREDENTIALS);
        }

        authenticationContext.getSubcontext(UsernamePasswordContext.class, true).setUsername(username)
                .setPassword(password);

        return ActionSupport.buildProceedEvent(this);
    }
}
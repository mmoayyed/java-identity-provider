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

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.UsernamePasswordContext;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * A stage that extracts a username/password from the request. This stage is expected to be used in conjunction with
 * {@link DisplayUsernamePasswordPage} but will work with any stage that properly passes in the username and password.
 */
public class ExtractUsernamePasswordFromFormResponse extends AbstractAuthenticationAction {

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws ProfileException {

        final String username = httpRequest.getParameter(DisplayUsernamePasswordPage.USERNAME_FIELD_NAME);
        if (username == null) {
            throw new UnableToExtractUsernamePasswordException("Request did not contain a username");
        }

        final String password = httpRequest.getParameter(DisplayUsernamePasswordPage.PASSWORD_FIELD_NAME);
        if (password == null) {
            throw new UnableToExtractUsernamePasswordException("Request did not contain a password");
        }

        authenticationContext.getSubcontext(UsernamePasswordContext.class, true).setUsername(username)
                .setPassword(password);

        return ActionSupport.buildProceedEvent(this);
    }

    /** Thrown if there is a problem with the incoming request. */
    public static final class UnableToExtractUsernamePasswordException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -2470302287986807006L;

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public UnableToExtractUsernamePasswordException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         * @param wrappedException exception to be wrapped by this one
         */
        public UnableToExtractUsernamePasswordException(String message, Exception wrappedException) {
            super(message, wrappedException);
        }
    }
}
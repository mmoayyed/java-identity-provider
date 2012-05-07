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
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * An authentication stage that checks that the request's REMOTE_USER is set and, if so, sets the identified user as the
 * authenticated principal.
 */
public class ValidateRemoteUser extends AbstractAuthenticationAction {

    /** Transition name returned when {@link HttpServletRequest#getRemoteUser()} returns a null or empty string. */
    public static final String TRANSITION_NO_REMOTE_USER = "NoRemoteUser";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ValidateRemoteUser.class);

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

        final String remoteUser = StringSupport.trimOrNull(httpRequest.getRemoteUser());
        if (remoteUser == null) {
            log.debug("Action {}: HTTP request did not contain a remote user", getId());
            return ActionSupport.buildEvent(this, TRANSITION_NO_REMOTE_USER, null);
        }

        log.debug("Action{}: HTTP request identified remote user as '{}'", getId(), remoteUser);
        authenticationContext.setAuthenticatedPrincipal(new UsernamePrincipal(remoteUser));

        return ActionSupport.buildProceedEvent(this);
    }
}
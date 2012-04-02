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
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.net.HttpHeaders;

/** A stage that prompts for HTTP Basic authentication credentials. */
public class SendBasicHttpAuthenticationChallenge extends AbstractAuthenticationAction {

    /** The identifier for Basic authentication scheme. */
    public static final String BASIC = "Basic";

    /** The realm property identifier used during basic authentication. */
    public static final String REALM = "realm";

    /** The value used for the {@value HttpHeaders#WWW_AUTHENTICATE}. */
    private final String authenticateValue;

    /**
     * Constructor.
     * 
     * @param realm the Basic authentication realm identifier
     */
    public SendBasicHttpAuthenticationChallenge(@Nonnull @NotEmpty final String realm) {
        super();

        String checkedRealm = Assert.isNotNull(StringSupport.trimOrNull(realm), "Realm name can not be null or empty");
        authenticateValue = BASIC + " " + REALM + "=\"" + checkedRealm + "\"";
    }

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

        HttpServletSupport.addNoCacheHeaders(httpResponse);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setHeader(HttpHeaders.WWW_AUTHENTICATE, authenticateValue);

        return ActionSupport.buildProceedEvent(this);
    }
}
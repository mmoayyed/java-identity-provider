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
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;

/**
 * A stage that extracts a username and password from the HTTP {@link HttpHeaders#AUTHORIZATION} header. This stage
 * would generally be used after the {@link SendBasicHttpAuthenticationChallenge} but should be usable by anything that
 * properly populates the HTTP header.
 */
public class ExtractUsernamePasswordFromBasicAuthorizationHeader extends AbstractAuthenticationAction {

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws ProfileException {

        final String encodedCredentials = extractAuthorizationCredentials(httpRequest);
        final Pair<String, String> decodedCredentials = decodeAuthorizationCredentials(encodedCredentials);

        authenticationContext.getSubcontext(UsernamePasswordContext.class, true)
                .setUsername(decodedCredentials.getFirst()).setPassword(decodedCredentials.getSecond());

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Gets the encoded credentials passed in via the {@link HttpHeaders#AUTHORIZATION} header. This method checks to
     * ensure that the authentication scheme is {@link SendBasicHttpAuthenticationChallenge#BASIC} and then strips off
     * and returns the follow on Base64-encoded credentials.
     * 
     * @param httpRequest current HTTP request
     * 
     * @return the Base64 encoded credentials
     * 
     * @throws ProfileException thrown if the authorization header is missing or malformed or if the credentials are for
     *             the incorrect scheme
     */
    protected String extractAuthorizationCredentials(@Nonnull final HttpServletRequest httpRequest)
            throws ProfileException {
        final String authorizationHeader = StringSupport.trimOrNull(httpRequest.getHeader(HttpHeaders.AUTHORIZATION));
        // TODO(lajoie) check if null or if there is more than one header

        final String[] splitHeader = authorizationHeader.split(" ");
        // TODO(lajoie) check size and error out if incorrect

        final String scheme = StringSupport.trimOrNull(splitHeader[0]);
        if (!SendBasicHttpAuthenticationChallenge.BASIC.equalsIgnoreCase(scheme)) {
            // TODO(lajoie) error condition
        }

        return StringSupport.trimOrNull(splitHeader[1]);
    }

    /**
     * Decodes the credential string provided in the HTTP header, splits it in to a username and password, and returns
     * them.
     * 
     * @param encodedCredentials the Base64 encoded credentials
     * 
     * @return a pair containing the username and password, respectively
     * 
     * @throws ProfileException thrown if the encoded credentials were improperly encoded or are malformed
     */
    protected Pair<String, String> decodeAuthorizationCredentials(@Nonnull @NotEmpty final String encodedCredentials)
            throws ProfileException {
        final String decodedUserPass = new String(Base64Support.decode(encodedCredentials), Charsets.US_ASCII);

        if (!decodedUserPass.contains(":")) {
            // TODO(lajoie) error condition
        }

        final String username = decodedUserPass.substring(0, decodedUserPass.indexOf(':'));
        final String password = decodedUserPass.substring(decodedUserPass.indexOf(':'));
        // TODO(lajoie) check to see if username or password is empty and error out if so

        return new Pair<String, String>(username, password);
    }
}
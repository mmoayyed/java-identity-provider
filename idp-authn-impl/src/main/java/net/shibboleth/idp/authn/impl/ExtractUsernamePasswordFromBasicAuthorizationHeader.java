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

import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
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
            @Nonnull final HttpServletResponse httpResponse,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

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
            throws AuthenticationException {

        String[] splitValue;
        String authnScheme;
        for (Enumeration<String> header = httpRequest.getHeaders(HttpHeaders.AUTHORIZATION); header.hasMoreElements();) {
            splitValue = header.nextElement().split(" ");
            if (splitValue.length == 2) {
                authnScheme = StringSupport.trimOrNull(splitValue[0]);
                if (SendBasicHttpAuthenticationChallenge.BASIC.equalsIgnoreCase(authnScheme)) {
                    return StringSupport.trimOrNull(splitValue[1]);
                }
            }
        }

        throw new InvalidBasicAuthorizationHeaderException(
                "Request did not contain an Authorization header for Basic authentication");
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
            throws AuthenticationException {
        final String decodedUserPass = new String(Base64Support.decode(encodedCredentials), Charsets.US_ASCII);

        if (!decodedUserPass.contains(":")) {
            throw new InvalidBasicAuthorizationHeaderException(
                    "Request did not contain a well-formed Basic authorization header value");
        }

        final String username = decodedUserPass.substring(0, decodedUserPass.indexOf(':'));
        if (username == null) {
            throw new InvalidBasicAuthorizationHeaderException(
                    "Request did not contain a well-formed Basic authorization header value");
        }

        final String password = decodedUserPass.substring(decodedUserPass.indexOf(':'));
        if (password == null) {
            throw new InvalidBasicAuthorizationHeaderException(
                    "Request did not contain a well-formed Basic authorization header value");
        }

        return new Pair<String, String>(username, password);
    }

    /**
     * Thrown if the HTTP request did not contain an authorization header value that was valid for use with Basic
     * authentication.
     */
    public static class InvalidBasicAuthorizationHeaderException extends AuthenticationException {

        /** Serial version UID. */
        private static final long serialVersionUID = -1859600619050947760L;

        /**
         * Constructor.
         * 
         * @param message error message
         */
        public InvalidBasicAuthorizationHeaderException(String message) {
            super(message);
        }
    }
}
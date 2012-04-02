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

import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.UsernamePasswordContext;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.collection.Pair;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.Header;
import org.opensaml.soap.wssecurity.Password;
import org.opensaml.soap.wssecurity.Security;
import org.opensaml.soap.wssecurity.Username;
import org.opensaml.soap.wssecurity.UsernameToken;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

//TODO(lajoie) should we support nonce and created checks?  probably

/**
 * An authentication stage that extracts a username/password from the WSS Username/Password attached to a SOAP message.
 * As should be obvious, this assumes that the inbound message is a SOAP {@link Envelope}.
 */
public class ExtractUsernamePasswordFromWssToken extends AbstractAuthenticationAction {

    /** {@inheritDoc} */
    protected Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

        final Envelope inboundMessage = (Envelope) profileRequestContext.getInboundMessageContext().getMessage();

        final UsernameToken usernameToken = getUsernameToken(inboundMessage);

        final Pair<String, String> usernamePassword = extractUsernamePassword(usernameToken);

        authenticationContext.getSubcontext(UsernamePasswordContext.class, true)
                .setUsername(usernamePassword.getFirst()).setPassword(usernamePassword.getSecond());

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Extracts the {@link UsernameToken} from the given {@link Envelope}.
     * 
     * @param message the message from which the token should be extracted
     * 
     * @return the extracted token
     */
    private UsernameToken getUsernameToken(Envelope message) {
        final Header header = message.getHeader();

        final List<XMLObject> securityHeaders = header.getUnknownXMLObjects(Security.ELEMENT_NAME);
        // TODO(lajoie) check that there is only one and if not, error out

        final List<XMLObject> usernameTokens =
                ((Security) securityHeaders.get(0)).getUnknownXMLObjects(UsernameToken.ELEMENT_NAME);
        // TODO(lajoie) check that there is only one and if not, error out

        return (UsernameToken) usernameTokens.get(0);
    }

    /**
     * Extracts a username/password from the given {@link UsernameToken}.
     * 
     * @param usernameToken the token from which the username/password should be extracted
     * 
     * @return the username and password
     */
    private Pair<String, String> extractUsernamePassword(UsernameToken usernameToken) {
        final Username username = usernameToken.getUsername();
        // TODO(lajoie) check not null and not empty

        final List<XMLObject> passwords = usernameToken.getUnknownXMLObjects(Password.ELEMENT_NAME);
        // TODO(lajoie) check that there is only one and if not, error out

        Password password = (Password) passwords.get(0);
        if (password.getType() != null && !password.getType().equals(Password.TYPE_PASSWORD_TEXT)) {
            // TODO(lajoie) error, we can't support digest
        }

        return new Pair<String, String>(username.getValue(), password.getValue());
    }
}
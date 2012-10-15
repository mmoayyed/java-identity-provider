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

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationRequestContext;
import net.shibboleth.idp.authn.AuthnEventIds;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO(lajoie) should we support nonce and created checks?  probably

/**
 * An authentication stage that extracts a username/password from the WSS Username/Password attached to a SOAP message.
 * As should be obvious, this assumes that the inbound message is a SOAP {@link Envelope}.
 */
public class ExtractUsernamePasswordFromWssToken extends AbstractAuthenticationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ExtractUsernamePasswordFromWssToken.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final HttpServletRequest httpRequest,
            @Nonnull final HttpServletResponse httpResponse,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationRequestContext authenticationContext) throws AuthenticationException {

        // TODO(lajoie) get the envelope from the inbound message context
        final Envelope inboundMessage = null;

        final Pair<String, String> usernamePassword = extractUsernamePassword(inboundMessage);
        if (usernamePassword == null) {
            log.debug("Action {}: inbound message does not contain a username and password", getId());
            return ActionSupport.buildEvent(this, AuthnEventIds.NO_CREDENTIALS);
        }

        authenticationContext.getSubcontext(UsernamePasswordContext.class, true)
                .setUsername(usernamePassword.getFirst()).setPassword(usernamePassword.getSecond());

        return ActionSupport.buildProceedEvent(this);
    }

    /**
     * Extracts a username/password from the inbound message.
     * 
     * @param message the inbound message
     * 
     * @return the username and password
     */
    @Nullable protected Pair<String, String> extractUsernamePassword(@Nonnull final Envelope message) {
        final UsernameToken usernameToken = getUsernameToken(message);
        if (usernameToken == null) {
            return null;
        }

        final Username username = usernameToken.getUsername();
        if (username == null) {
            log.debug("Action {}: <UsernameToken> does not contain a <Username>", getId());
            return null;
        }

        final List<XMLObject> passwords = usernameToken.getUnknownXMLObjects(Password.ELEMENT_NAME);
        if (passwords == null || passwords.size() == 0) {
            log.debug("Action {}: <UsernameToken> does not contain a <Password>", getId());
            return null;
        }

        final Iterator<XMLObject> passwordsItr = passwords.iterator();
        Password password = null;
        while (passwordsItr.hasNext()) {
            password = (Password) passwordsItr.next();
            if (password.getType() != null && !password.getType().equals(Password.TYPE_PASSWORD_TEXT)) {
                log.debug("Action {}: skipping password with unsupported type {}", getId(), password.getType());
                password = null;
            }
        }

        if (password == null) {
            log.debug("Action {}: <UsernameToken> does not contain a support <Password>", getId());
            return null;
        }
        return new Pair<String, String>(username.getValue(), password.getValue());
    }

    /**
     * Extracts the {@link UsernameToken} from the given {@link Envelope}.
     * 
     * @param message the message from which the token should be extracted
     * 
     * @return the extracted token
     */
    @Nullable protected UsernameToken getUsernameToken(Envelope message) {
        final Header header = message.getHeader();

        final List<XMLObject> securityHeaders = header.getUnknownXMLObjects(Security.ELEMENT_NAME);
        if (securityHeaders == null || securityHeaders.size() == 0) {
            log.debug("Action {}: inbound message does not contain <Security>", getId());
            return null;
        }

        final List<XMLObject> usernameTokens =
                ((Security) securityHeaders.get(0)).getUnknownXMLObjects(UsernameToken.ELEMENT_NAME);
        if (usernameTokens == null || usernameTokens.size() == 0) {
            log.debug("Action {}: inbound message security header does not contain <UsernameToken>", getId());
            return null;
        }

        return (UsernameToken) usernameTokens.get(0);
    }
}
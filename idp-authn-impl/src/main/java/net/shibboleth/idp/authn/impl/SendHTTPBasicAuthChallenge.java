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
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;

/**
 * An action that prompts for HTTP Basic authentication credentials.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @pre <pre>ProfileRequestContext.getHttpResponse() != null</pre>
 * @post A challenge response is issued to the client.
 */
public class SendHTTPBasicAuthChallenge extends AbstractProfileAction {

    /** The identifier for Basic authentication scheme. */
    @Nonnull public static final String BASIC = "Basic";

    /** The realm property identifier used during basic authentication. */
    @Nonnull public static final String REALM = "realm";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeAuthenticationContext.class);
        
    /** The authentication realm. */
    @Nonnull @NotEmpty private String realm;

    /** Constructor. */
    SendHTTPBasicAuthChallenge() {
        realm = "default";
    }
    
    /**
     * Get the authentication realm.
     * 
     * @return  authentication realm
     */
    @Nonnull @NotEmpty public String getRealm() {
        return realm;
    }
    
    /**
     * Set the authentication realm.
     * 
     * @param newRealm authentication realm
     */
    public void setRealm(@Nonnull @NotEmpty String newRealm) {
        realm = Constraint.isNotNull(StringSupport.trimOrNull(newRealm), "Realm name cannot be null or empty");
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final HttpServletResponse httpResponse = profileRequestContext.getHttpResponse();
        if (httpResponse == null) {
            log.debug("{} profile request context did not contain HttpServletResponse", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        HttpServletSupport.addNoCacheHeaders(httpResponse);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setHeader(HttpHeaders.WWW_AUTHENTICATE, BASIC + " " + REALM + "=\"" + getRealm() + "\"");
    }
}
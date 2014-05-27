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

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts REMOTE_USER and places it in a request attribute to be used by the IdP's external authentication
 * interface.
 */
public class RemoteUserAuthServlet extends HttpServlet {

    /** Serial UID. */
    private static final long serialVersionUID = -3162057736238514851L;
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RemoteUserAuthServlet.class);

    /** {@inheritDoc} */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
            throws ServletException, IOException {
        
        try {
            final String key = ExternalAuthentication.startExternalAuthentication(httpRequest);

            final String principalName = StringSupport.trimOrNull(httpRequest.getRemoteUser());
            if (principalName != null) {
                log.debug("Remote user identified as {}, returning control back to authentication flow", principalName);
                httpRequest.setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, principalName);
            } else {
                log.warn("No remote user information was present in the request");
            }
            
            ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
            
        } catch (final ExternalAuthenticationException e) {
            throw new ServletException("Error processing external authentication request", e);
        }
    }

}
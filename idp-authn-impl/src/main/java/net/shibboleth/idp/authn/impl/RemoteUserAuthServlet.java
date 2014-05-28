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
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Extracts REMOTE_USER and places it in a request attribute to be used by the IdP's external authentication
 * interface.
 */
public class RemoteUserAuthServlet extends HttpServlet {

    /** Serial UID. */
    private static final long serialVersionUID = -3162057736238514851L;
    
    /** Init parameter controlling whether to check for REMOTE_USER. */
    @Nonnull @NotEmpty private static final String CHECK_REMOTE_USER_PARAM = "checkRemoteUser";

    /** Init parameter controlling what attributes to check. */
    @Nonnull @NotEmpty private static final String CHECK_ATTRIBUTES_PARAM = "checkAttributes";
    
    /** Init parameter controlling what headers to check. */
    @Nonnull @NotEmpty private static final String CHECK_HEADERS_PARAM = "checkHeaders";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RemoteUserAuthServlet.class);
    
    /** Whether to check REMOTE_USER for an identity. Defaults to true. */
    private boolean checkRemoteUser;
    
    /** List of request attributes to check for an identity. */
    @Nonnull @NonnullElements private Collection<String> checkAttributes;

    /** List of request headers to check for an identity. */
    @Nonnull @NonnullElements private Collection<String> checkHeaders;

    /** Constructor. */
    public RemoteUserAuthServlet() {
        checkRemoteUser = true;
        checkAttributes = Collections.emptyList();
        checkHeaders = Collections.emptyList();
    }

    /**
     * Set whether to check REMOTE_USER for an identity.
     * 
     * @param flag value to set  
     */
    public void setCheckRemoteUser(final boolean flag) {
        checkRemoteUser = flag;
    }

    /**
     * Set the list of request attributes to check for an identity.
     * 
     * @param attributes    list of request attributes to check
     */
    public void setCheckAttributes(@Nonnull @NonnullElements final Collection<String> attributes) {
        checkAttributes = Lists.newArrayList(Collections2.filter(attributes, Predicates.notNull()));
    }

    /**
     * Set the list of request headers to check for an identity.
     * 
     * @param headers list of request headers to check
     */
    public void setCheckHeaders(@Nonnull @NonnullElements final Collection<String> headers) {
        checkHeaders = Lists.newArrayList(Collections2.filter(headers, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        
        String param = config.getInitParameter(CHECK_REMOTE_USER_PARAM);
        if (param != null) {
            checkRemoteUser = Boolean.parseBoolean(param);
        }
        
        param = config.getInitParameter(CHECK_ATTRIBUTES_PARAM);
        if (param != null) {
            final String[] attrs = param.split(" ");
            if (attrs != null) {
                checkAttributes = StringSupport.normalizeStringCollection(Lists.newArrayList(attrs));
            }
        }

        param = config.getInitParameter(CHECK_HEADERS_PARAM);
        if (param != null) {
            final String[] headers = param.split(" ");
            if (headers != null) {
                checkHeaders = StringSupport.normalizeStringCollection(Lists.newArrayList(headers));
            }
        }
        
        log.info("RemoteUserAuthServlet {} process REMOTE_USER, along with attributes {} and headers {}",
                new Object[] {checkRemoteUser ? "will" : "will not", checkAttributes, checkHeaders,});
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
            throws ServletException, IOException {
        
        try {
            final String key = ExternalAuthentication.startExternalAuthentication(httpRequest);

            String username = null;
            
            if (checkRemoteUser) {
                username = httpRequest.getRemoteUser();
                if (username != null && !username.isEmpty()) {
                    log.debug("User identity extracted from REMOTE_USER: {}", username);
                }
            }
            
            if (username == null) {
                for (final String s : checkAttributes) {
                    final Object attr = httpRequest.getAttribute(s);
                    if (attr != null && !attr.toString().isEmpty()) {
                        username = attr.toString();
                        log.debug("User identity extracted from attribute {}: {}", s, attr);
                        break;
                    }
                }
            }
            
            if (username == null) {
                for (final String s : checkHeaders) {
                    username = httpRequest.getHeader(s);
                    if (username != null && !username.isEmpty()) {
                        log.debug("User identity extracted from header {}: {}", s, username);
                        break;
                    }
                }
            }
            
            if (username != null) {
                httpRequest.setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, username);
            }
            ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
            
        } catch (final ExternalAuthenticationException e) {
            throw new ServletException("Error processing external authentication request", e);
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
}
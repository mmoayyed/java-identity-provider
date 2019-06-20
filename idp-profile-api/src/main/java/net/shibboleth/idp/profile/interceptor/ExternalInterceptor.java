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

package net.shibboleth.idp.profile.interceptor;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.net.UrlEscapers;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Public interface supporting external interceptor flows outside the webflow engine.
 * 
 * @since 4.0.0
 */
public class ExternalInterceptor {

    /** Parameter supplied to identify the per-conversation structure in the session. */
    @Nonnull @NotEmpty public static final String CONVERSATION_KEY = "conversation";

    /** Request attribute to which an event ID may be bound. */
    @Nonnull @NotEmpty public static final String EVENT_KEY = "event";
    
    /**
     * Computes the appropriate location to pass control to to invoke an external interceptor mechanism.
     * 
     *  <p>The input location should be suitable for use in a Spring "externalRedirect" expression, and may
     *  contain a query string. The result will include any additional parameters needed to invoke the
     *  mechanism.</p>
     * 
     * @param baseLocation the base location to build off of
     * @param conversationValue the value to include as a conversation ID
     * 
     * @return the computed location
     */
    @Nonnull @NotEmpty public static String getExternalRedirect(@Nonnull @NotEmpty final String baseLocation,
            @Nonnull @NotEmpty final String conversationValue) {
        Constraint.isNotEmpty(baseLocation, "Base location cannot be null or empty");
        
        final StringBuilder url = new StringBuilder(baseLocation);
        
        // Add a parameter separator for the conversation ID.
        url.append(baseLocation.indexOf('?') == -1 ? '?' : '&');
        url.append(CONVERSATION_KEY).append('=').append(
                UrlEscapers.urlFormParameterEscaper().escape(conversationValue));
        
        return url.toString();
    }
    
    /**
     * Initialize a request to an external interceptor by seeking out the information stored in
     * the servlet session and exposing it as request attributes.
     * 
     * @param request servlet request
     * 
     * @return a handle to subsequent use of
     *      {@link #finishExternalInterceptor(java.lang.String, HttpServletRequest, HttpServletResponse)}
     * 
     * @throws ExternalInterceptorException if an error occurs
     */
    @Nonnull @NotEmpty public static String startExternalInterceptor(@Nonnull final HttpServletRequest request)
            throws ExternalInterceptorException {
        final String conv = request.getParameter(CONVERSATION_KEY);
        if (conv == null || conv.isEmpty()) {
            throw new ExternalInterceptorException("No conversation key found in request");
        }
        
        final Object obj = request.getSession().getAttribute(CONVERSATION_KEY + conv);
        if (obj == null || !(obj instanceof ExternalInterceptor)) {
            throw new ExternalInterceptorException("No conversation state found in session for key (" + conv + ")");
        }
        
        ((ExternalInterceptor) obj).doStart(request);
        return conv;
    }
    
    /**
     * Complete a request to an external interceptor by seeking out the information stored in
     * request attributes and transferring to the session's conversation state, and then transfer
     * control back to the webflow.
     * 
     * @param key   the value returned by {@link #startExternalInterceptor(HttpServletRequest)}
     * @param request servlet request
     * @param response servlet response
     * 
     * @throws ExternalInterceptorException if an error occurs
     * @throws IOException if the redirect cannot be issued
     */
    public static void finishExternalInterceptor(@Nonnull @NotEmpty final String key,
            @Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response)
            throws ExternalInterceptorException, IOException {
        
        final Object obj = request.getSession().getAttribute(CONVERSATION_KEY + key);
        if (obj == null || !(obj instanceof ExternalInterceptor)) {
            throw new ExternalInterceptorException("No conversation state found in session for key (" + key + ")");
        }
        
        request.getSession().removeAttribute(CONVERSATION_KEY + key);
        
        ((ExternalInterceptor) obj).doFinish(request, response);
    }

    /**
     * Get the {@link ProfileRequestContext} associated with a request.
     * 
     * @param key   the value returned by {@link #startExternalAuthentication(HttpServletRequest)}
     * @param request servlet request
     * 
     * @return the profile request context
     * @throws ExternalInterceptorException if an error occurs
     */
    @Nonnull public static ProfileRequestContext getProfileRequestContext(@Nonnull @NotEmpty final String key,
            @Nonnull final HttpServletRequest request) throws ExternalInterceptorException {
        
        final Object obj = request.getSession().getAttribute(CONVERSATION_KEY + key);
        if (obj == null || !(obj instanceof ExternalInterceptor)) {
            throw new ExternalInterceptorException("No conversation state found in session");
        }
        
        return ((ExternalInterceptor) obj).getProfileRequestContext(request);
    }
    
    /**
     * Initialize a request to an external interceptor by seeking out the information stored in
     * the servlet session and exposing it as request attributes.
     * 
     * @param request servlet request
     * 
     * @throws ExternalInterceptorException if an error occurs
     */
    protected void doStart(@Nonnull final HttpServletRequest request) throws ExternalInterceptorException {
        throw new ExternalInterceptorException("Not implemented");
    }

    /**
     * Complete a request to an external interceptor by seeking out the information stored in
     * request attributes and transferring to the session's conversation state, and then transfer
     * control back to the webflow.
     * 
     * @param request servlet request
     * @param response servlet response
     * 
     * @throws ExternalInterceptorException if an error occurs
     * @throws IOException if the redirect cannot be issued
     */
    protected void doFinish(@Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response)
            throws ExternalInterceptorException, IOException {
        throw new ExternalInterceptorException("Not implemented");
    }
    
    /**
     * Get the {@link ProfileRequestContext} associated with a request.
     * 
     * @param request servlet request
     * 
     * @return the profile request context
     * @throws ExternalInterceptorException if an error occurs
     */
    @Nonnull protected ProfileRequestContext getProfileRequestContext(@Nonnull final HttpServletRequest request)
            throws ExternalInterceptorException {
        throw new ExternalInterceptorException("Not implemented");
    }
    
}
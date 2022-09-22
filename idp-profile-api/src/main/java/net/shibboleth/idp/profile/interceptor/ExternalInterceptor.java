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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.context.ExternalContextHolder;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.execution.repository.FlowExecutionRepositoryException;
import org.springframework.webflow.executor.FlowExecutorImpl;

import com.google.common.base.Strings;
import com.google.common.net.UrlEscapers;

import net.shibboleth.idp.profile.context.ExternalInterceptorContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Public interface supporting external interceptor flows outside the webflow engine.
 * 
 * @since 4.0.0
 */
public abstract class ExternalInterceptor {

    /** Parameter supplied to locate the SWF object needed in the servlet context. */
    @Nonnull @NotEmpty public static final String SWF_KEY = "net.shibboleth.idp.flowExecutor";
    
    /** Parameter supplied to identify the per-conversation parameter. */
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
        final String key = request.getParameter(CONVERSATION_KEY);
        if (Strings.isNullOrEmpty(key)) {
            throw new ExternalInterceptorException("No conversation key found in request");
        }
        
        final ProfileRequestContext profileRequestContext = getProfileRequestContext(key, request);
        final ExternalInterceptorContext extContext = getExternalInterceptorContext(profileRequestContext);
        extContext.getExternalInterceptor().doStart(request, profileRequestContext, extContext);
        
        return key;
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
        
        final ProfileRequestContext profileRequestContext = getProfileRequestContext(key, request);
        final ExternalInterceptorContext extContext = getExternalInterceptorContext(profileRequestContext);
        extContext.getExternalInterceptor().doFinish(request, response, profileRequestContext, extContext);
    }

    /**
     * Get the {@link ProfileRequestContext} associated with a request.
     * 
     * @param key   the value returned by {@link #startExternalInterceptor(HttpServletRequest)}
     * @param request servlet request
     * 
     * @return the profile request context
     * @throws ExternalInterceptorException if an error occurs
     */
    @Nonnull public static ProfileRequestContext getProfileRequestContext(@Nonnull @NotEmpty final String key,
            @Nonnull final HttpServletRequest request) throws ExternalInterceptorException {

        final Object obj = request.getServletContext().getAttribute(SWF_KEY);
        if (!(obj instanceof FlowExecutorImpl)) {
            throw new ExternalInterceptorException("No FlowExecutor available in servlet context");
        }

        try {
            final FlowExecutionRepository repo = ((FlowExecutorImpl) obj).getExecutionRepository();
            ExternalContextHolder.setExternalContext(
                    new ServletExternalContext(request.getServletContext(), request, null));
            
            final FlowExecution execution = repo.getFlowExecution(repo.parseFlowExecutionKey(key));
            final Object prc = execution.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
            if (!(prc instanceof ProfileRequestContext)) {
                throw new ExternalInterceptorException(
                        "ProfileRequestContext not available in webflow conversation scope");
            }
            
            return (ProfileRequestContext) prc;
        } catch (final FlowExecutionRepositoryException e) {
            throw new ExternalInterceptorException("Error retrieving flow conversation", e);
        } finally {
            ExternalContextHolder.setExternalContext(null);
        }
    }
    
    /**
     * Utility method to access the {@link ExternalInterceptorContext}.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return the {@link ExternalInterceptorContext} to operate on
     * 
     * @throws ExternalInterceptorException if the context is missing
     */
    @Nonnull private static ExternalInterceptorContext getExternalInterceptorContext(
            @Nonnull final ProfileRequestContext profileRequestContext) throws ExternalInterceptorException {
        
        final ProfileInterceptorContext piContext =
                profileRequestContext.getSubcontext(ProfileInterceptorContext.class);
        if (piContext == null) {
            throw new ExternalInterceptorException("No ProfileInterceptorContext found");
        }
        
        final ExternalInterceptorContext extContext = piContext.getSubcontext(ExternalInterceptorContext.class);
        if (extContext == null) {
            throw new ExternalInterceptorException("No ExternalInterceptorContext found");
        }
        
        return extContext;
    }
    
    /**
     * Initialize a request to an external interceptor by seeking out the information stored in
     * the servlet session and exposing it as request attributes.
     * 
     * @param request servlet request
     * @param profileRequestContext profile request context
     * @param externalInterceptorContext external interceptor context
     * 
     * @throws ExternalInterceptorException if an error occurs
     */
    protected void doStart(@Nonnull final HttpServletRequest request,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ExternalInterceptorContext externalInterceptorContext) throws ExternalInterceptorException {
        request.setAttribute(ProfileRequestContext.BINDING_KEY, profileRequestContext);
    }

    /**
     * Complete a request to an external interceptor by seeking out the information stored in
     * request attributes and transferring to the session's conversation state, and then transfer
     * control back to the webflow.
     * 
     * @param request servlet request
     * @param response servlet response
     * @param profileRequestContext profile request context
     * @param externalInterceptorContext external interceptor context
     * 
     * @throws ExternalInterceptorException if an error occurs
     * @throws IOException if the redirect cannot be issued
     */
    protected abstract void doFinish(@Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response, @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ExternalInterceptorContext externalInterceptorContext)
                    throws ExternalInterceptorException, IOException;

}
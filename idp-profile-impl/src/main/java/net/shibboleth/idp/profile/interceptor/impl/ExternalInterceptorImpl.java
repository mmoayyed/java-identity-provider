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

package net.shibboleth.idp.profile.interceptor.impl;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.profile.context.ExternalInterceptorContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ExternalInterceptor;
import net.shibboleth.idp.profile.interceptor.ExternalInterceptorException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Implementation of the {@link ExternalInterceptor} API that handles moving information in and out
 * of request attributes.
 * 
 * @since 4.0.0
 */
public class ExternalInterceptorImpl extends ExternalInterceptor {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExternalInterceptorImpl.class);
    
    /** State of request to pull from. */
    @Nonnull private final ProfileRequestContext profileRequestContext;

    /**
     * Constructor.
     * 
     * @param input profile request context to expose
     */
    public ExternalInterceptorImpl(@Nonnull final ProfileRequestContext input) {
        profileRequestContext = Constraint.isNotNull(input, "ProfileRequestContext cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doStart(@Nonnull final HttpServletRequest request) {
        request.setAttribute(ProfileRequestContext.BINDING_KEY, profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override
    protected void doFinish(@Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response)
            throws IOException, ExternalInterceptorException {
        final ProfileInterceptorContext piContext =
                profileRequestContext.getSubcontext(ProfileInterceptorContext.class);
        if (piContext == null) {
            throw new ExternalInterceptorException("No ProfileInterceptorContext found");
        }
        
        final ExternalInterceptorContext extContext = piContext.getSubcontext(ExternalInterceptorContext.class);
        if (extContext == null) {
            throw new ExternalInterceptorException("No ExternalInterceptorContext found");
        } else if (extContext.getFlowExecutionUrl() == null) {
            throw new ExternalInterceptorException("No flow execution URL found to return control");
        }
        
        final Object attr = request.getAttribute(EVENT_KEY);
        if (attr != null && attr instanceof String) {
            extContext.setEventId((String) attr);
        }
                
        response.sendRedirect(extContext.getFlowExecutionUrl());
    }

    /** {@inheritDoc} */
    @Override
    protected ProfileRequestContext getProfileRequestContext(@Nonnull final HttpServletRequest request)
            throws ExternalInterceptorException {
        return profileRequestContext;
    }
    
}
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

import net.shibboleth.idp.profile.context.ExternalInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ExternalInterceptor;
import net.shibboleth.idp.profile.interceptor.ExternalInterceptorException;

/**
 * Implementation of the {@link ExternalInterceptor} API that handles moving information in and out
 * of request attributes.
 * 
 * @since 4.0.0
 */
public class ExternalInterceptorImpl extends ExternalInterceptor {

    /** {@inheritDoc} */
    @Override
    protected void doFinish(@Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ExternalInterceptorContext externalContext)
                    throws IOException, ExternalInterceptorException {
        
        final Object attr = request.getAttribute(EVENT_KEY);
        if (attr != null && attr instanceof String) {
            externalContext.setEventId((String) attr);
        }
                
        response.sendRedirect(externalContext.getFlowExecutionUrl());
    }
    
}
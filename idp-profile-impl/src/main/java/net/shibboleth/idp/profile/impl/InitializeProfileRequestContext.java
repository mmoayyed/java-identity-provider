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

package net.shibboleth.idp.profile.impl;

import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action that creates a new {@link ProfileRequestContext} and binds it to the current conversation under the
 * {@link ProfileRequestContext#BINDING_KEY} key.
 */
@ThreadSafe
public final class InitializeProfileRequestContext extends AbstractProfileAction {

    /** {@inheritDoc} */
    public Event execute(final RequestContext springRequestContext) throws ProfileException {

        // We have to override execute() because the profile request context doesn't exist yet.
        
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final HttpServletRequest httpRequest =
                (HttpServletRequest) getHttpRequestLookupStrategy().apply(springRequestContext);
        final HttpServletResponse httpResponse =
                (HttpServletResponse) getHttpResponseLookupStrategy().apply(springRequestContext);

        ProfileRequestContext profileRequestContext = new ProfileRequestContext();
        profileRequestContext.setHttpRequest(httpRequest);
        profileRequestContext.setHttpResponse(httpResponse);
        springRequestContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, profileRequestContext);

        return ActionSupport.buildProceedEvent(this);
    }
}
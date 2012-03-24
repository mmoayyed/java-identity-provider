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

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action that creates a new {@link ProfileRequestContext} and binds it to the current conversation under the
 * {@link ProfileRequestContext#BINDING_KEY} key.
 */
@ThreadSafe
public final class InitializeProfileRequestContext extends AbstractIdentityProviderAction {

    /** Constructor. The ID of this component is set to the name of this class. */
    public InitializeProfileRequestContext() {
        setId(InitializeProfileRequestContext.class.getName());
    }

    /** {@inheritDoc} */
    protected Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext, final ProfileRequestContext profileRequestContext)
            throws ProfileException {
        springRequestContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, new ProfileRequestContext());
        return ActionSupport.buildProceedEvent(this);
    }
}
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

import java.util.Collection;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.AuthenticationWorkflowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * An action that creates a {@link AuthenticationContext} and sets it as a child of the current
 * {@link ProfileRequestContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 */
public class InitializeAuthenticationContext extends AbstractProfileAction {

    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        // TODO(lajoie) get configured authentication mechanisms
        Collection<AuthenticationWorkflowDescriptor> availableFlows = null;

        AuthenticationContext authnCtx = new AuthenticationContext(availableFlows);
        profileRequestContext.addSubcontext(authnCtx);
    }
}
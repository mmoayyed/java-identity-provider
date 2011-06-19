/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

package net.shibboleth.idp.profile;

import net.jcip.annotations.ThreadSafe;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action that creates a new {@link ProfileRequestContext} and binds it to the current conversation under the
 * {@link ProfileRequestContext#BINDING_KEY} key.
 */
@ThreadSafe
public class InitializeProfileRequestContext extends AbstractIdentityProviderAction {

    /**
     * Constructor.
     * 
     * @param componentId unique ID for this component
     */
    public InitializeProfileRequestContext(String componentId) {
        super(componentId);
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext) {
        springRequestContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, new ProfileRequestContext());
        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }
}
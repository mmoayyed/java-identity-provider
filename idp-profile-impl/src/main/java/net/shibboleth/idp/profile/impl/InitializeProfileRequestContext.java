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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action that creates a new {@link ProfileRequestContext} and binds it to the current conversation under the
 * {@link ProfileRequestContext#BINDING_KEY} key, and sets the profile ID, if provided.
 * 
 * <p>This is a native SWF action in order to access conversation scope.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post RequestContext.getConversationScope().get(ProfileRequestContext.BINDING_KEY) != null
 */
@ThreadSafe
public final class InitializeProfileRequestContext extends AbstractProfileAction {

    /** The profile ID to initialize the context to. */
    @Nullable private String profileId;
    
    /**
     * Set the profile ID to populate into the context.
     * 
     * @param id    profile ID to populate into the context
     */
    public void setProfileId(@Nullable final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        profileId = StringSupport.trimOrNull(id);
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull public Event execute(@Nonnull final RequestContext springRequestContext) throws ProfileException {

        // We have to override execute() because the profile request context doesn't exist yet.
        
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        ProfileRequestContext prc = new ProfileRequestContext();
        if (profileId != null) {
            prc.setProfileId(profileId);
        }

        springRequestContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, prc);

        return ActionSupport.buildProceedEvent(this);
    }
}
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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import jakarta.servlet.http.HttpServletRequest;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.messaging.context.ScratchContext;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action that creates a new {@link ProfileRequestContext} and binds it to the current conversation under the
 * {@link ProfileRequestContext#BINDING_KEY} key, and sets the profile and logging IDs, if provided.
 * 
 * <p>This is a native SWF action in order to access conversation scope.</p>
 * 
 * <p>Optionally saves off query parameters from request into a {@link ScratchContext}.</p> 
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post RequestContext.getConversationScope().get(ProfileRequestContext.BINDING_KEY) != null
 */
@ThreadSafe
public final class InitializeProfileRequestContext extends AbstractProfileAction {
    
    /** The profile ID to initialize the context to. */
    @Nullable private String profileId;

    /** Backup profile ID to populate as a legacy value. */
    @Nullable private String legacyProfileId;
    
    /** The logging ID to initialize the context to. */
    @Nullable private String loggingId;
    
    /** Whether this is a browser-based profile request. */
    private boolean browserProfile;
    
    /** Whether to capture and store off query parameters. */
    private boolean captureQueryParameters;
    
    /**
     * Set the profile ID to populate into the context.
     * 
     * @param id    profile ID to populate into the context
     */
    public void setProfileId(@Nullable final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        profileId = StringSupport.trimOrNull(id);
    }
    
    /**
     * Set the legacy/fallback profile ID to populate into the context.
     * 
     * @param id    legacy profile ID to populate into the context
     * 
     * @since 4.2.0
     */
    public void setLegacyProfileId(@Nullable final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        legacyProfileId = StringSupport.trimOrNull(id);
    }

    /**
     * Set the logging ID to populate into the context.
     * 
     * @param id    logging ID to populate into the context
     */
    public void setLoggingId(@Nullable final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        loggingId = StringSupport.trimOrNull(id);
    }
    
    /**
     * Set whether the request is browser-based, defaults to false.
     * 
     * @param browser flag to set
     */
    public void setBrowserProfile(final boolean browser) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        browserProfile = browser;
    }

    /**
     * Set whether to capture incoming query parameters in a {@link ScratchContext},
     * defaults to false.
     * 
     * @param flag flag to set
     * 
     * @since 4.1.0
     */
    public void setCaptureQueryParameters(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        captureQueryParameters = flag;
    }
    
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    @Nonnull public Event execute(@Nonnull final RequestContext springRequestContext) {

        // We have to override execute() because the profile request context doesn't exist yet.
        
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final ProfileRequestContext prc = new ProfileRequestContext();
        if (profileId != null) {
            prc.setProfileId(profileId);
        }
        
        if (legacyProfileId != null) {
            prc.setLegacyProfileId(legacyProfileId);
        }
        
        if (loggingId != null) {
            prc.setLoggingId(loggingId);
        }
        
        prc.setBrowserProfile(browserProfile);

        springRequestContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, prc);
        
        if (captureQueryParameters) {
            final HttpServletRequest request = getHttpServletRequest();
            if (request != null) {
                ((Map<Object,Object>) prc.getSubcontext(ScratchContext.class, true).getMap()).putAll(
                        request.getParameterMap());
            }
        }

        return ActionSupport.buildProceedEvent(this);
    }
    
}
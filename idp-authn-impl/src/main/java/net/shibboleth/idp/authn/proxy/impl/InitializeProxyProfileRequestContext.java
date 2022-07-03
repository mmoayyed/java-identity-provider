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

package net.shibboleth.idp.authn.proxy.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Action that creates a new {@link ProfileRequestContext} via a creation strategy,
 * and sets the profile and logging IDs, if provided.
 * 
 * <p>This is designed by default for use in creating a nested context tree
 * beneath an active {@link AuthenticationContext} for use in managing a nested
 * profile interaction with an external IdP.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @post context is created per the supplied strategy
 */
@ThreadSafe
public final class InitializeProxyProfileRequestContext extends AbstractProfileAction {

    /** Context creation strategy. */
    @Nonnull private Function<ProfileRequestContext,ProfileRequestContext> profileRequestContextCreationStrategy;
    
    /** The profile ID to initialize the context to. */
    @Nullable private String profileId;

    /** The logging ID to initialize the context to. */
    @Nullable private String loggingId;
    
    /** Whether this is a browser-based profile request. */
    private boolean browserProfile;
    
    /** Constructor. */
    public InitializeProxyProfileRequestContext() {
        
        // Defaults to PRC -> AuthenticationContext -> PRC
        profileRequestContextCreationStrategy = new DefaultPRCCreationStrategy();
    }
    
    /**
     * Set the strategy to use to locate/create the {@link ProfileRequestContext} to operate on.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setProfileRequestContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,ProfileRequestContext> strategy) {
        throwSetterPreconditionExceptions();
        profileRequestContextCreationStrategy = Constraint.isNotNull(strategy, "Creation strategy cannot be null");
    }
    
    /**
     * Set the profile ID to populate into the context.
     * 
     * @param id    profile ID to populate into the context
     */
    public void setProfileId(@Nullable final String id) {
        throwSetterPreconditionExceptions();
        profileId = StringSupport.trimOrNull(id);
    }

    /**
     * Set the logging ID to populate into the context.
     * 
     * @param id    logging ID to populate into the context
     */
    public void setLoggingId(@Nullable final String id) {
        throwSetterPreconditionExceptions();
        loggingId = StringSupport.trimOrNull(id);
    }
    
    /**
     * Set whether the request is browser-based, defaults to false.
     * 
     * @param browser   true iff the request is browser based
     */
    public void setBrowserProfile(final boolean browser) {
        throwSetterPreconditionExceptions();
        browserProfile = browser;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull public void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final ProfileRequestContext prc = profileRequestContextCreationStrategy.apply(profileRequestContext);
        if (prc == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        if (profileId != null) {
            prc.setProfileId(profileId);
        }
        
        if (loggingId != null) {
            prc.setLoggingId(loggingId);
        }
        
        prc.setBrowserProfile(browserProfile);
    }
    
    /**
     * Default strategy that nests the new PRC below the AC.
     */
    private class DefaultPRCCreationStrategy implements Function<ProfileRequestContext,ProfileRequestContext> {

        /** {@inheritDoc} */
        @Nullable public ProfileRequestContext apply(@Nullable final ProfileRequestContext input) {
            
            if (input != null) {
                final AuthenticationContext ac = input.getSubcontext(AuthenticationContext.class);
                if (ac != null) {
                    return (ProfileRequestContext) ac.addSubcontext(new ProfileRequestContext(), true);
                }
            }
            
            return null;
        }
        
    }
    
}
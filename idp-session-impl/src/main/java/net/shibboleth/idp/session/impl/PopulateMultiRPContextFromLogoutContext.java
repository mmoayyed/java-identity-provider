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

package net.shibboleth.idp.session.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.MultiRelyingPartyContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Profile action that populates a {@link MultiRelyingPartyContext} with the relying party
 * information from a {@link LogoutContext}.
 * 
 * <p>An existing {@link MultiRelyingPartyContext} will be replaced.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @post If (ProfileRequestContext.getSubcontext(LogoutContext.class) != null,
 *  then ProfileRequestContext.getSubcontext(MultiRelyingPartyContext.class) != null
 */
public class PopulateMultiRPContextFromLogoutContext extends AbstractProfileAction {
    
    /** Label for {@link MultiRelyingPartyContext} entries. */
    @Nonnull @NotEmpty private static final String LABEL = "logout";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateMultiRPContextFromLogoutContext.class);
    
    /** Lookup function for {@link LogoutContext}. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextLookupStrategy;
    
    /** {@link LogoutContext} to process. */
    @Nullable private LogoutContext logoutCtx;
    
    /** Constructor. */
    public PopulateMultiRPContextFromLogoutContext() {
        logoutContextLookupStrategy = new ChildContextLookup<>(LogoutContext.class);
    }
        
    /**
     * Set the lookup strategy for the LogoutContext to process.
     * 
     * @param strategy  lookup strategy
     */
    public void setLogoutContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        logoutContextLookupStrategy = Constraint.isNotNull(strategy, "LogoutContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        logoutCtx = logoutContextLookupStrategy.apply(profileRequestContext);
        if (logoutCtx == null) {
            log.debug("{} No LogoutContext found, nothing to do", getLogPrefix());
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final MultiRelyingPartyContext multiCtx = new MultiRelyingPartyContext();
        profileRequestContext.addSubcontext(multiCtx, true);
        
        for (final String relyingPartyId : logoutCtx.getSessionMap().keySet()) {
            final RelyingPartyContext rpCtx = new RelyingPartyContext();
            rpCtx.setRelyingPartyId(relyingPartyId);
            multiCtx.addRelyingPartyContext(LABEL, rpCtx);
        }
    }
    
}
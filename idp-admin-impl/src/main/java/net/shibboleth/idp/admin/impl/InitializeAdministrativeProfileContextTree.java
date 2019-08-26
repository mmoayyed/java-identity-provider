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

package net.shibboleth.idp.admin.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import net.shibboleth.idp.admin.AdministrativeFlowDescriptor;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;

/**
 * An action that processes settings from a supplied {@link AdministrativeFlowDescriptor} to prepare
 * the profile context tree for subsequent use by an administrative profile flow.
 * 
 * <p>This action finalizes settings like non-browser compatibility, and if instructed to do so,
 * decorates the context tree with a mocked up {@link RelyingPartyContext} and {@link RelyingPartyUIContext}.</p>
 * 
 * @pre The injected {@link AdministrativeFlowDescriptor}'s ID must match {@link ProfileRequestContext#getProfileId()}
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 * @post See above.
 */
public class InitializeAdministrativeProfileContextTree extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeAdministrativeProfileContextTree.class);
    
    /** Descriptor of the administrative flow being run. */
    @Nullable private AdministrativeFlowDescriptor flowDescriptor;

    /** The system wide languages to inspect if there is no match between metadata and browser. */
    @Nullable private List<String> fallbackLanguages;
    
    /**
     * Set the flow descriptor describing the administrative flow being run.
     * 
     * @param descriptor the flow descriptor to base the action on
     */
    public void setAdministrativeFlowDescriptor(@Nullable final AdministrativeFlowDescriptor descriptor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        flowDescriptor = descriptor;
    }

    /**
     * Set the system wide default languages.
     * 
     * @param langs a semi-colon separated string.
     */
    public void setFallbackLanguages(@Nonnull @NonnullElements final List<String> langs) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (langs != null) {
            fallbackLanguages = new ArrayList<>(Collections2.filter(langs, Predicates.notNull()));
        } else {
            fallbackLanguages = null;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        if (flowDescriptor == null) {
            log.warn("{} Administrative profile '{}' not enabled", getLogPrefix(),
                    profileRequestContext.getProfileId());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        } else if (!flowDescriptor.getId().equals(profileRequestContext.getProfileId())) {
            log.warn("{} Profile ID '{}' doesn't match descriptor ID '{}", getLogPrefix(),
                    profileRequestContext.getProfileId(), flowDescriptor.getId());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        profileRequestContext.setLoggingId(flowDescriptor.getLoggingId());
        profileRequestContext.setBrowserProfile(!flowDescriptor.isNonBrowserSupported(profileRequestContext));
        
        final RelyingPartyContext rpCtx = new RelyingPartyContext();
        profileRequestContext.addSubcontext(rpCtx, true);
        rpCtx.setRelyingPartyId(flowDescriptor.getId());
        rpCtx.setProfileConfig(flowDescriptor);
        
        final RelyingPartyUIContext uiCtx = rpCtx.getSubcontext(RelyingPartyUIContext.class, true);
        uiCtx.setRPUInfo(flowDescriptor.getUIInfo());
        uiCtx.setBrowserLanguageRanges(HttpServletSupport.getLanguageRange(getHttpServletRequest()));
        
        if (null != fallbackLanguages) {
            uiCtx.setFallbackLanguages(fallbackLanguages);
        }
    }
}
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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.admin.AdministrativeFlowDescriptor;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.NonnullSupplier;
import net.shibboleth.shared.spring.util.SpringSupport;

import jakarta.servlet.http.HttpServletRequest;

/**
 * An action that processes settings from a supplied {@link AdministrativeFlowDescriptor} to prepare
 * the profile context tree for subsequent use by an administrative profile flow.
 * 
 * <p>This action finalizes settings like non-browser compatibility, and decorates the context tree with
 * a mocked up {@link RelyingPartyContext} and {@link RelyingPartyUIContext}.</p>
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
        checkSetterPreconditions();

        flowDescriptor = descriptor;
    }

    /**
     * Set the system wide default languages.
     * 
     * @param langs a semi-colon separated string.
     */
    public void setFallbackLanguages(@Nonnull @NonnullElements final List<String> langs) {
        checkSetterPreconditions();
        
        if (langs != null) {
            fallbackLanguages = List.copyOf(langs);
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
        
        final AdministrativeFlowDescriptor descriptor = flowDescriptor;
        if (descriptor == null) {
            log.warn("{} Administrative profile '{}' not enabled", getLogPrefix(),
                    profileRequestContext.getProfileId());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        final String id = descriptor.getId(); 
        if (id == null ||!id.equals(profileRequestContext.getProfileId())) {
            log.warn("{} Profile ID '{}' doesn't match descriptor ID '{}", getLogPrefix(),
                    profileRequestContext.getProfileId(), id);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        if (getHttpServletRequest() == null) {
            log.warn("{} Profile ID '{}' No HttpRequestSupplier available", getLogPrefix(),
                    profileRequestContext.getProfileId());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final AdministrativeFlowDescriptor descriptor = flowDescriptor;
        assert descriptor != null;
        profileRequestContext.setLoggingId(descriptor.getLoggingId());
        profileRequestContext.setBrowserProfile(!descriptor.isNonBrowserSupported(profileRequestContext));
        
        final RelyingPartyContext rpCtx = new RelyingPartyContext();
        profileRequestContext.addSubcontext(rpCtx, true);
        rpCtx.setRelyingPartyId(descriptor.getId());
        rpCtx.setProfileConfig(descriptor);
        
        final RelyingPartyUIContext uiCtx = rpCtx.getOrCreateSubcontext(RelyingPartyUIContext.class);
        uiCtx.setRPUInfo(descriptor.getUIInfo());
        final NonnullSupplier<HttpServletRequest> supplier = getHttpServletRequestSupplier();
        assert supplier != null;
        uiCtx.setBrowserLanguageRanges(SpringSupport.getLanguageRange(supplier.get()));
        uiCtx.setRequestSupplier(supplier);
        
        if (null != fallbackLanguages) {
            uiCtx.setFallbackLanguages(fallbackLanguages);
        }
    }
}

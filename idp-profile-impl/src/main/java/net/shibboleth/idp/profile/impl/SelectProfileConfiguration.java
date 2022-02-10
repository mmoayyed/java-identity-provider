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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.config.ConditionalProfileConfiguration;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action that selects the {@link ProfileConfiguration} for the given request and sets it in the looked-up
 * {@link RelyingPartyContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CONFIG}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 */
public class SelectProfileConfiguration extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SelectProfileConfiguration.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Profile ID to use if not derived from context tree. */
    @Nullable @NotEmpty private String profileId;
    
    /** The RelyingPartyContext to operate on. */
    @Nullable private RelyingPartyContext rpCtx;
    
    /** Fail if no profile configuration is found. */
    private boolean failIfMissing;
    
    /** Constructor. */
    public SelectProfileConfiguration() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        failIfMissing = true;
    }

    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *         {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the profile identifier to use in selection.
     * 
     * <p>If not set, this defaults to using {@link ProfileRequestContext#getProfileId()}.</p>
     * 
     * @param id profile ID to use
     * 
     * @since 4.2.0
     */
    public void setProfileId(@Nullable @NotEmpty final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        profileId = StringSupport.trimOrNull(id);
    }
    
    /**
     * Set whether a missing profile configuration should result in an error event.
     * 
     * <p>Defaults to true.</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.2.0
     */
    public void setFailIfMissing(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        failIfMissing = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx == null) {
            log.debug("{} No relying party context associated with this profile request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }

        if (rpCtx.getConfiguration() == null) {
            log.debug("{} No relying party configuration associated with this profile request", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
            return false;
        }
        
        return true;
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        String targetId = profileId;
        if (targetId == null) {
            targetId = profileRequestContext.getProfileId();
        }
        
        final RelyingPartyConfiguration rpConfig = rpCtx.getConfiguration();

        ProfileConfiguration profileConfiguration = rpConfig.getProfileConfiguration(profileRequestContext, targetId);
        if (profileConfiguration == null && profileId == null && profileRequestContext.getLegacyProfileId() != null) {
            // Try the legacy ID.
            profileConfiguration = rpConfig.getProfileConfiguration(profileRequestContext,
                    profileRequestContext.getLegacyProfileId());
            if (profileConfiguration != null) {
                // Reset the primary profile ID to the legacy value for subsequent use.
                profileRequestContext.setProfileId(profileRequestContext.getLegacyProfileId());
            }
        }
        
        if (profileConfiguration == null) {
            if (failIfMissing) {
                log.warn("{} Profile {} is not available for RP configuration {} (RPID {})",
                        new Object[] {getLogPrefix(), targetId, rpConfig.getId(), rpCtx.getRelyingPartyId(),});
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            } else {
                log.debug("{} Profile {} is not available for RP configuration {} (RPID {})",
                        new Object[] {getLogPrefix(), targetId, rpConfig.getId(), rpCtx.getRelyingPartyId(),});
            }
        } else if (profileConfiguration instanceof ConditionalProfileConfiguration
                && !((ConditionalProfileConfiguration) profileConfiguration).getActivationCondition().test(
                        profileRequestContext)) {
            if (failIfMissing) {
                log.warn("{} Profile {} is not active for RP configuration {} (RPID {})",
                        new Object[] {getLogPrefix(), targetId, rpConfig.getId(), rpCtx.getRelyingPartyId(),});
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            } else {
                log.debug("{} Profile {} is not active for RP configuration {} (RPID {})",
                        new Object[] {getLogPrefix(), targetId, rpConfig.getId(), rpCtx.getRelyingPartyId(),});
            }
        } else {
            rpCtx.setProfileConfig(profileConfiguration);
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
}
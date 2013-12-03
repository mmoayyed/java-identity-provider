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

package net.shibboleth.idp.saml.impl.profile;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * A profile stage that selects the {@link ProfileConfiguration} for the given request and sets it in the looked-up
 * {@link RelyingPartyContext}.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CTX,
                description = "No relying party context associated with the request"),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CONFIG,
                description = "Relying party context didn't contain a relying party configuration"),
        @Event(id = EventIds.INVALID_PROFILE_CONFIG, description = "Profile is not configured for the relying party")})
public class SelectProfileConfiguration extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SelectProfileConfiguration.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Constructor. */
    public SelectProfileConfiguration() {
        super();

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);
    }

    /**
     * Gets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @return strategy used to locate the {@link RelyingPartyContext} associated with a given
     *         {@link ProfileRequestContext}
     */
    @Nonnull public Function<ProfileRequestContext, RelyingPartyContext> getRelyingPartyContextLookupStrategy() {
        return relyingPartyContextLookupStrategy;
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event
            doExecute(@Nonnull final RequestContext springRequestContext,
                    @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx == null) {
            log.debug("Action {}: No relying party context associated with this profile request", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CTX);
        }

        final RelyingPartyConfiguration rpConfig = rpCtx.getConfiguration();
        if (rpConfig == null) {
            log.debug("Action {}: No relying party configuration associated with this profile request", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CONFIG);
        }

        final String profileId = profileRequestContext.getProfileId();

        final ProfileConfiguration profileConfiguration = rpConfig.getProfileConfiguration(profileId);
        if (profileConfiguration == null) {
            log.debug("Action {}: Profile {} is not configured for relying party configuration {}", new Object[] {
                    getId(), profileId, rpConfig.getId(),});
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CONFIG);
        }

        rpCtx.setProfileConfiguration(profileConfiguration);

        return ActionSupport.buildProceedEvent(this);
    }
}
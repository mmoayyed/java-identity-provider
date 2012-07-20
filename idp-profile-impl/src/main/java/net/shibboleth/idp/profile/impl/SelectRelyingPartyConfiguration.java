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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.Resolver;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * This action attempts to resolve a {@link RelyingPartyConfiguration} and adds it to the {@link RelyingPartyContext}
 * that was looked up.
 */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CTX, description = "No relying party context return by lookup strategy"),
        @Event(id = EventIds.INVALID_RELYING_PARTY_CONFIG,
                description = "No relying party configuation can be associated with the profile request")})
public final class SelectRelyingPartyConfiguration extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SelectRelyingPartyConfiguration.class);

    /** Resolver used to look up relying party configurations. */
    private final Resolver<RelyingPartyConfiguration, ProfileRequestContext> rpConfigResolver;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /**
     * Constructor.
     * 
     * @param resolver resolver used to look up relying party configurations
     */
    public SelectRelyingPartyConfiguration(
            @Nonnull final Resolver<RelyingPartyConfiguration, ProfileRequestContext> resolver) {
        super();

        rpConfigResolver = Constraint.isNotNull(resolver, "Relying party configuration resolver can not be null");

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);
    }

    /**
     * Gets the resolver used to look up relying party configuration.
     * 
     * @return resolver used to look up relying party configuration, never null after initialization
     */
    @Nonnull public Resolver<RelyingPartyConfiguration, ProfileRequestContext> getRelyingPartyConfigurationResolver() {
        return rpConfigResolver;
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

    /**
     * Sets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy can not be null");
    }

    /** {@inheritDoc} */
    public org.springframework.webflow.execution.Event doExecute(@Nullable final HttpServletRequest httpRequest,
            @Nullable final HttpServletResponse httpResponse, @Nullable final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.debug("Action {}: No relying party context available for this request", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CTX);
        }

        try {
            final RelyingPartyConfiguration config = rpConfigResolver.resolveSingle(profileRequestContext);
            if (config == null) {
                log.debug("Action {}: No relying party configuration applies to this request", getId());
                return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CONFIG);
            }

            log.debug("Action {}: Found relying party configuration for request", getId());
            relyingPartyCtx.setRelyingPartyConfiguration(config);
            return ActionSupport.buildProceedEvent(this);
        } catch (ResolverException e) {
            log.error("Action {}: error trying to resolve relying party configuration", getId(), e);
            return ActionSupport.buildEvent(this, EventIds.INVALID_RELYING_PARTY_CONFIG);
        }
    }
}
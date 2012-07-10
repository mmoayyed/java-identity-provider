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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
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
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * This action attempts to resolve a {@link RelyingPartyConfiguration} and adds it to the {@link RelyingPartyContext}
 * located on the {@link ProfileRequestContext}.
 */
public final class AddRelyingPartyConfigurationToProfileRequestContext extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddRelyingPartyConfigurationToProfileRequestContext.class);

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
    public AddRelyingPartyConfigurationToProfileRequestContext(
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
    public Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext, final ProfileRequestContext profileRequestContext)
            throws ProfileException {

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);

        try {
            final RelyingPartyConfiguration config = rpConfigResolver.resolveSingle(profileRequestContext);
            if (config == null) {
                throw new NoRelyingPartyConfigurationException(
                        "No relying party configuration availabe for this request");
            }

            relyingPartyCtx.setRelyingPartyConfiguration(config);
            return ActionSupport.buildProceedEvent(this);
        } catch (ResolverException e) {
            log.error("Action {}: error trying to resolve relying party configuration", getId(), e);
            throw new NoRelyingPartyConfigurationException("Error trying to resolve relying party configuration", e);
        }
    }

    /**
     * A profile processing exception that occurred because there was no relying party configuration available for the
     * request.
     */
    public static class NoRelyingPartyConfigurationException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 8429783659476277482L;

        /** Constructor. */
        public NoRelyingPartyConfigurationException() {
            super();
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public NoRelyingPartyConfigurationException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param wrappedException exception to be wrapped by this one
         */
        public NoRelyingPartyConfigurationException(Exception wrappedException) {
            super(wrappedException);
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         * @param wrappedException exception to be wrapped by this one
         */
        public NoRelyingPartyConfigurationException(String message, Exception wrappedException) {
            super(message, wrappedException);
        }
    }
}
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

import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationSubcontext;
import net.shibboleth.idp.saml.profile.SamlMetadataSubcontext;

import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.opensaml.util.resolver.ResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** IdP action that adds the relying party configuration to the {@link ProfileRequestContext}. */
public final class AddRelyingPartyConfigurationToProfileRequestContext extends AbstractIdentityProviderAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AddRelyingPartyConfigurationToProfileRequestContext.class);

    /** Resolver used to look up relying party configurations. */
    private RelyingPartyConfigurationResolver rpConfigResolver;

    /**
     * Gets the resolver used to look up relying party configuration.
     * 
     * @return resolver used to look up relying party configuration, never null after initialization
     */
    public RelyingPartyConfigurationResolver getRelyingPartyConfigurationResolver() {
        return rpConfigResolver;
    }

    /**
     * Sets the resolver used to look up relying party configuration.
     * 
     * @param resolver resolver used to look up relying party configuration
     */
    public synchronized void setRelyingPartyConfigurationResolver(RelyingPartyConfigurationResolver resolver) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    AddRelyingPartyConfigurationToProfileRequestContext.class.getName() + " " + getId()
                            + ": already initialized, relying party configuration resolver can no longer be changed.");
        }
        rpConfigResolver = resolver;
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext) {
        SamlMetadataSubcontext metadataCtx =
                profileRequestContext.getInboundMessageContext().getSubcontext(SamlMetadataSubcontext.class, false);

        if (metadataCtx == null || metadataCtx.getEntityDescriptor() == null) {
            log.debug(
                    "Action {}: No metadata available for message issuer, using anonymous relying party configuration",
                    getId());
            new RelyingPartyConfigurationSubcontext(profileRequestContext,
                    rpConfigResolver.getAnonymousRelyingPartyConfiguration());
            return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
        }

        try {
            RelyingPartyConfiguration config = rpConfigResolver.resolveSingle(profileRequestContext);
            if (config == null) {
                return ActionSupport.buildErrorEvent(this, new NoRelyingPartyConfigurationException(),
                        "No relying party configuration availabe for this request");
            }

            new RelyingPartyConfigurationSubcontext(profileRequestContext, config);
            return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
        } catch (ResolverException e) {
            log.error("Action {}: error trying to resolve relying party configuration", getId(), e);
            return ActionSupport.buildErrorEvent(this, new NoRelyingPartyConfigurationException(e),
                    "Error trying to resolve relying party configuration");
        }
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (rpConfigResolver == null) {
            throw new ComponentInitializationException("Relying party configuration resolver can not be null");
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